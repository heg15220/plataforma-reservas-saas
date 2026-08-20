"""Pruebas del protocolo A/B, potencia, alpha spending, guardrails y parada."""

from __future__ import annotations

import unittest
from pathlib import Path

from reserly_demand_engine.ab_testing import RankingAbAnalyzer, RankingAbPolicy, RankingAbSnapshot

ROOT = Path(__file__).parents[1]


class RankingAbTests(unittest.TestCase):
    """Ejecuta análisis agregados sin convertir simulaciones en evidencia causal."""

    def setUp(self) -> None:
        self.policy = RankingAbPolicy.load(ROOT / "policies" / "ranking-ab-test.v1.json")
        self.analyzer = RankingAbAnalyzer(self.policy)

    def _snapshot(self, *, production: bool = True, sequence: int = 3, control_bookings: int = 1000, treatment_bookings: int = 1300):
        days = {1: 14, 2: 21, 3: 28, 4: 42}[sequence]
        def arm(variant, policy, completed):
            return {
                "variantKey": variant,
                "policyVersion": policy,
                "assignedSessions": 10_400,
                "exposedSessions": 10_000,
                "completedBookings": completed,
                "maturedBookings": 1000,
                "attendedBookings": 900,
                "cancelledBookings": 50,
                "totalImpressions": 20_000,
                "offPeakImpressions": 6000,
                "newVenueImpressions": 2000,
            }
        return RankingAbSnapshot.model_validate({
            "snapshotVersion": 1,
            "protocolVersion": self.policy.protocolVersion,
            "experimentKey": self.policy.experimentKey,
            "experimentDefinitionVersion": 1,
            "analysisSequence": sequence,
            "elapsedDays": days,
            "productionEvidence": production,
            "consentRevocationsApplied": True,
            "containsPersonalData": False,
            "control": arm("control", self.policy.controlPolicyVersion, control_bookings),
            "treatment": arm("treatment", self.policy.treatmentPolicyVersion, treatment_bookings),
            "crossOverCount": 0,
            "hardConstraintViolations": 0,
            "privacyViolations": 0,
        })

    def test_powered_meaningful_effect_stops_with_success(self) -> None:
        result = self.analyzer.analyze(self._snapshot())
        self.assertEqual("success", result.decision)
        self.assertTrue(result.powered)
        self.assertGreater(result.confidenceLower, 0)
        self.assertTrue(result.causalClaimAllowed)
        self.assertTrue(all(item.passed for item in result.guardrails))

    def test_synthetic_execution_never_allows_causal_claim(self) -> None:
        result = self.analyzer.analyze(self._snapshot(production=False))
        self.assertEqual("simulationOnly", result.decision)
        self.assertFalse(result.stoppingCriterionMet)
        self.assertFalse(result.causalClaimAllowed)
        self.assertIn("productionEvidenceMissing", result.blockingReasons)

    def test_underpowered_interim_look_continues(self) -> None:
        raw = self._snapshot(sequence=1).model_dump()
        for arm_name in ("control", "treatment"):
            raw[arm_name].update({"assignedSessions": 520, "exposedSessions": 500, "completedBookings": 50 if arm_name == "control" else 70})
        result = self.analyzer.analyze(RankingAbSnapshot.model_validate(raw))
        self.assertEqual("continue", result.decision)
        self.assertFalse(result.powered)
        self.assertFalse(result.stoppingCriterionMet)

    def test_final_powered_null_effect_stops_for_futility(self) -> None:
        result = self.analyzer.analyze(self._snapshot(treatment_bookings=1000))
        self.assertEqual("futility", result.decision)
        self.assertTrue(result.stoppingCriterionMet)
        self.assertFalse(result.causalClaimAllowed)

    def test_maximum_period_without_power_is_inconclusive(self) -> None:
        raw = self._snapshot(sequence=4).model_dump()
        for arm_name in ("control", "treatment"):
            raw[arm_name].update(
                {"assignedSessions": 520, "exposedSessions": 500, "completedBookings": 50}
            )
        result = self.analyzer.analyze(RankingAbSnapshot.model_validate(raw))
        self.assertEqual("inconclusive", result.decision)
        self.assertTrue(result.stoppingCriterionMet)

    def test_guardrail_breach_has_priority_and_stops(self) -> None:
        raw = self._snapshot().model_dump()
        raw["hardConstraintViolations"] = 1
        result = self.analyzer.analyze(RankingAbSnapshot.model_validate(raw))
        self.assertEqual("safetyStop", result.decision)
        self.assertTrue(result.stoppingCriterionMet)
        self.assertFalse(result.causalClaimAllowed)

    def test_unplanned_peek_and_wrong_policy_fail_closed(self) -> None:
        raw = self._snapshot(sequence=2).model_dump()
        raw["elapsedDays"] = 20
        with self.assertRaisesRegex(ValueError, "UNPLANNED_PEEK"):
            self.analyzer.analyze(RankingAbSnapshot.model_validate(raw))
        raw = self._snapshot().model_dump()
        raw["treatment"]["policyVersion"] = "unknown-policy-v1"
        with self.assertRaisesRegex(ValueError, "ARM_MISMATCH"):
            self.analyzer.analyze(RankingAbSnapshot.model_validate(raw))


if __name__ == "__main__":
    unittest.main()
