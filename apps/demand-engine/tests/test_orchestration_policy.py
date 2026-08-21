"""Pruebas de la selección Prefect y el umbral auditable de reevaluación."""

from __future__ import annotations

import unittest
import importlib.util
from pathlib import Path

from reserly_demand_engine.orchestration_policy import (
    OrchestrationObservation,
    OrchestrationSelectionPolicy,
)


ROOT = Path(__file__).parents[1]
REPOSITORY_ROOT = ROOT.parents[1]
ENTRYPOINT = REPOSITORY_ROOT / "infrastructure" / "prefect" / "entrypoint.py"
SPEC = importlib.util.spec_from_file_location("reserly_prefect_entrypoint", ENTRYPOINT)
assert SPEC and SPEC.loader
prefect_entrypoint = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(prefect_entrypoint)


class OrchestrationPolicyTests(unittest.TestCase):
    """Mantiene Prefect mientras no exista evidencia sostenida o necesidad dura."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = OrchestrationSelectionPolicy.load(
            ROOT / "policies" / "orchestration-selection.v1.json"
        )

    def observation(self, **overrides: object) -> OrchestrationObservation:
        values: dict[str, object] = {
            "windowDays": 30,
            "activeDeployments": 20,
            "taskRunsPerDayP95": 5000,
            "concurrentTaskRunsP95": 25,
            "schedulerDelaySecondsP95": 5,
            "backfillDurationHoursP95": 1,
            "approvedHardRequirements": [],
        }
        values.update(overrides)
        return OrchestrationObservation.model_validate(values)

    def test_prefect_is_pinned_and_automatic_migration_is_forbidden(self) -> None:
        self.assertEqual("prefect", self.policy.selectedTool)
        self.assertEqual("3.8.2", self.policy.selectedVersion)
        self.assertFalse(self.policy.automaticMigrationAllowed)

    def test_one_breach_does_not_trigger_tool_churn(self) -> None:
        assessment = self.policy.assess(self.observation(activeDeployments=201))
        self.assertFalse(assessment.openAlternativeEvaluation)
        self.assertEqual(["activeDeployments"], assessment.quantitativeBreaches)

    def test_two_sustained_breaches_open_comparative_evaluation(self) -> None:
        assessment = self.policy.assess(
            self.observation(activeDeployments=201, schedulerDelaySecondsP95=61)
        )
        self.assertTrue(assessment.openAlternativeEvaluation)
        self.assertEqual("quantitativeEnvelopeExceeded", assessment.reason)

    def test_short_window_cannot_justify_migration(self) -> None:
        assessment = self.policy.assess(
            self.observation(windowDays=7, activeDeployments=1000, taskRunsPerDayP95=1_000_000)
        )
        self.assertFalse(assessment.openAlternativeEvaluation)
        self.assertEqual("insufficientMeasurementWindow", assessment.reason)

    def test_approved_hard_requirement_opens_evaluation_but_not_migration(self) -> None:
        assessment = self.policy.assess(
            self.observation(approvedHardRequirements=["activeActiveSchedulerRequired"])
        )
        self.assertTrue(assessment.openAlternativeEvaluation)
        self.assertFalse(assessment.automaticMigrationAllowed)
        self.assertEqual("hardRequirementNotCovered", assessment.reason)

    def test_unknown_hard_requirement_fails_closed(self) -> None:
        with self.assertRaisesRegex(ValueError, "ORCHESTRATION_HARD_REQUIREMENT_UNKNOWN"):
            self.policy.assess(self.observation(approvedHardRequirements=["preferredByEngineer"]))

    def test_compose_has_authenticated_server_worker_and_private_database(self) -> None:
        compose = (REPOSITORY_ROOT / "infrastructure" / "compose.yaml").read_text(encoding="utf-8")
        server = compose.split("  prefect-server:\n", 1)[1].split("\n  prefect-worker:", 1)[0]
        worker = compose.split("  prefect-worker:\n", 1)[1].split("\n  clamav:", 1)[0]
        database = compose.split("  prefect-postgres:\n", 1)[1].split("\n  prefect-server:", 1)[0]
        self.assertIn("prefecthq/prefect:3.8.2-python3.13@sha256:", server)
        self.assertIn("RESERLY_PREFECT_AUTH_PASSWORD", server)
        self.assertIn("127.0.0.1:${RESERLY_PREFECT_PORT:-4200}:4200", server)
        self.assertIn("infrastructure/prefect", str(ENTRYPOINT).replace("\\", "/"))
        self.assertIn("entrypoint.py", worker)
        self.assertNotIn("ports:", database)

    def test_prefect_entrypoint_encodes_database_and_configures_basic_auth(self) -> None:
        environment = {
            "RESERLY_PREFECT_ROLE": "server",
            "RESERLY_PREFECT_DATABASE_NAME": "prefect",
            "RESERLY_PREFECT_DATABASE_USERNAME": "prefect",
            "RESERLY_PREFECT_DATABASE_PASSWORD": "a/b?c#d%with-more-than-32-characters",
            "RESERLY_PREFECT_AUTH_USERNAME": "reserly-orchestration",
            "RESERLY_PREFECT_AUTH_PASSWORD": "auth-secret-with-at-least-32-characters",
        }
        configured, arguments = prefect_entrypoint.configure(environment)
        self.assertIn("a%2Fb%3Fc%23d%25", configured["PREFECT_SERVER_DATABASE_CONNECTION_URL"])
        self.assertEqual(configured["PREFECT_API_AUTH_STRING"], configured["PREFECT_SERVER_API_AUTH_STRING"])
        self.assertEqual(["prefect", "server", "start", "--host", "0.0.0.0"], arguments)


if __name__ == "__main__":
    unittest.main()
