"""Pruebas de gates, baseline, muestra y guardrails de promoción del ranking."""

from __future__ import annotations

import unittest
from pathlib import Path

from reserly_demand_engine.promotion import (
    BaselineArtifact,
    PromotionPolicy,
    PromotionSnapshot,
    RankingEvaluationDataset,
    evaluate_promotion,
)


ROOT = Path(__file__).resolve().parents[1]


class PromotionGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = PromotionPolicy.load(ROOT / "policies/promotion-gates.v1.json")
        cls.baseline = BaselineArtifact.load(
            ROOT / "evaluation/baselines/public-availability-fallback.v1.json"
        )
        cls.dataset = RankingEvaluationDataset.load(
            ROOT / "evaluation/ranking-mvp-evaluation.v1.json"
        )

    def test_canonical_dataset_is_synthetic_minimized_and_version_aligned(self) -> None:
        self.assertEqual(self.policy.datasetVersion, self.dataset.datasetVersion)
        self.assertEqual(12, len(self.dataset.cases))
        self.assertFalse(self.dataset.privacy.containsProductionData)
        self.assertFalse(self.dataset.privacy.containsPersonalData)

    def test_shadow_candidate_passes_only_with_complete_offline_and_operational_evidence(self) -> None:
        decision = evaluate_promotion(self.policy, self.baseline, self._shadow_snapshot())

        self.assertTrue(decision.promotable)
        self.assertTrue(all(gate.passed for gate in decision.gateResults))
        self.assertIn("baseline:recallAt1", [gate.gate for gate in decision.gateResults])

    def test_zero_tolerance_guardrail_blocks_pilot(self) -> None:
        snapshot = self._shadow_snapshot()
        values = dict(snapshot.metricValues)
        values["privacyViolationCount"] = 1.0

        decision = evaluate_promotion(
            self.policy, self.baseline, snapshot.model_copy(update={"metricValues": values})
        )

        self.assertFalse(decision.promotable)
        violation = next(
            gate for gate in decision.gateResults if gate.gate == "privacyViolationCount"
        )
        self.assertFalse(violation.passed)

    def test_missing_or_unknown_metrics_fail_closed(self) -> None:
        snapshot = self._shadow_snapshot()
        missing = dict(snapshot.metricValues)
        missing.pop("recallAt1")
        with self.assertRaisesRegex(ValueError, "PROMOTION_REQUIRED_METRIC_MISSING"):
            evaluate_promotion(
                self.policy, self.baseline, snapshot.model_copy(update={"metricValues": missing})
            )

        unknown = dict(snapshot.metricValues)
        unknown["email"] = 1.0
        with self.assertRaisesRegex(ValueError, "PROMOTION_UNKNOWN_METRIC"):
            evaluate_promotion(
                self.policy, self.baseline, snapshot.model_copy(update={"metricValues": unknown})
            )

    def test_pilot_requires_power_two_variants_and_business_success(self) -> None:
        metrics = self._all_threshold_values("pilotToRolloutThreshold")
        passing = PromotionSnapshot(
            snapshotVersion=1,
            targetStage="pilotToRollout",
            policyVersion=self.policy.policyVersion,
            datasetVersion=self.policy.datasetVersion,
            baselineVersion=self.policy.baselineVersion,
            consecutiveDays=42,
            sessionsByVariant={"control": 1000, "treatment": 1000},
            completedBookings=100,
            poweredSample=True,
            confidenceLevel=0.95,
            metricValues=metrics,
        )
        self.assertTrue(evaluate_promotion(self.policy, self.baseline, passing).promotable)

        underpowered = passing.model_copy(
            update={"poweredSample": False, "sessionsByVariant": {"control": 999, "treatment": 1200}}
        )
        decision = evaluate_promotion(self.policy, self.baseline, underpowered)
        self.assertFalse(decision.promotable)
        self.assertFalse(next(g for g in decision.gateResults if g.gate == "poweredSample").passed)
        self.assertFalse(
            next(g for g in decision.gateResults if g.gate == "minimumSessionsPerVariant").passed
        )

    def test_versions_cannot_be_mixed(self) -> None:
        snapshot = self._shadow_snapshot().model_copy(update={"datasetVersion": "other.v1"})
        with self.assertRaisesRegex(ValueError, "PROMOTION_VERSION_MISMATCH"):
            evaluate_promotion(self.policy, self.baseline, snapshot)

    def _shadow_snapshot(self) -> PromotionSnapshot:
        return PromotionSnapshot(
            snapshotVersion=1,
            targetStage="shadowToPilot",
            policyVersion=self.policy.policyVersion,
            datasetVersion=self.policy.datasetVersion,
            baselineVersion=self.policy.baselineVersion,
            consecutiveDays=7,
            sessionsByVariant={},
            completedBookings=0,
            poweredSample=False,
            confidenceLevel=0.95,
            metricValues=self._all_threshold_values("shadowToPilotThreshold"),
        )

    def _all_threshold_values(self, attribute: str) -> dict[str, float]:
        values: dict[str, float] = {}
        for metric in self.policy.metrics:
            threshold = getattr(metric, attribute)
            if threshold is not None:
                values[metric.metricKey] = threshold
        for key, value in self.baseline.metrics.items():
            values[key] = value
        return values


if __name__ == "__main__":
    unittest.main()
