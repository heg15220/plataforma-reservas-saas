"""Pruebas AIPW de intervalos, overlap, sensibilidad y separación de atribución."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.uplift_estimation import (
    DoublyRobustUpliftEvaluator,
    UpliftDataset,
    UpliftModelCard,
    UpliftPolicy,
)


ROOT = Path(__file__).resolve().parents[1]


def _dataset(*, production: bool = False, poor_overlap: bool = False) -> UpliftDataset:
    start = datetime(2026, 6, 1, tzinfo=UTC)
    units = []
    for arm in ("control", "treatment"):
        for index in range(160):
            treated = arm == "treatment"
            segment = "newCustomer" if index % 2 == 0 else "returningCustomer"
            threshold = 96 if treated else 64
            units.append(
                {
                    "unitId": str(uuid4()),
                    "arm": arm,
                    "assignmentPropensity": 0.02 if poor_overlap else 0.5,
                    "assignedAt": (start + timedelta(minutes=index)).isoformat(),
                    "outcomeObservedAt": (start + timedelta(minutes=index + 1)).isoformat(),
                    "featureValues": [(index % 10) / 10, float(index % 4), float(index % 7)],
                    "segment": segment,
                    "completedBooking": int(index < threshold),
                }
            )
    return UpliftDataset.model_validate(
        {
            "datasetVersion": "uplift-fixture-v1",
            "extractedAt": "2026-07-02T00:00:00Z",
            "productionEvidence": production,
            "purpose": "doublyRobustUpliftEvaluation",
            "experimentDesign": "randomizedControlledAb",
            "causalGatePolicyVersion": "causal-ab-validation-v1",
            "causalGateValidated": True,
            "preRegistered": True,
            "containsPersonalData": False,
            "consentRevocationsApplied": True,
            "featureSetVersion": "uplift-pre-treatment-v1",
            "featureNames": ["baselineAffinity", "priorVenueExposure", "weekday"],
            "observationalAttributionVersion": "commercial-attribution-v1",
            "observationalAttributedRateDifference": 0.35,
            "units": units,
        }
    )


class UpliftEstimationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = UpliftPolicy.load(ROOT / "policies" / "uplift-doubly-robust.v1.json")
        cls.card = UpliftModelCard.load(ROOT / "models" / "uplift-doubly-robust-candidate.v1.model-card.json")
        cls.evaluator = DoublyRobustUpliftEvaluator(cls.policy, cls.card)

    def test_cross_fitted_aipw_publishes_overall_and_segment_intervals(self) -> None:
        report = self.evaluator.evaluate(_dataset())
        self.assertAlmostEqual(0.2, report.overall.estimate, delta=0.03)
        self.assertLess(report.overall.confidenceLower, report.overall.estimate)
        self.assertGreater(report.overall.confidenceUpper, report.overall.estimate)
        self.assertEqual({"newCustomer", "returningCustomer"}, {item.scope for item in report.segments})
        self.assertTrue(report.overlapGatesPassed)
        self.assertFalse(report.causalInterpretationAllowed)
        self.assertFalse(report.upliftActionReviewAllowed)

    def test_observational_attribution_is_preserved_but_never_used_for_uplift(self) -> None:
        dataset = _dataset()
        report = self.evaluator.evaluate(dataset)
        self.assertEqual(0.35, report.observationalAttributedRateDifference)
        self.assertFalse(report.observationalAttributionUsedForUplift)
        self.assertNotEqual(report.observationalAttributedRateDifference, report.overall.estimate)
        self.assertEqual(report, self.evaluator.evaluate(dataset))

    def test_production_overlap_interval_and_sensitivity_allow_only_review(self) -> None:
        report = self.evaluator.evaluate(_dataset(production=True))
        self.assertTrue(report.causalInterpretationAllowed)
        self.assertTrue(report.signStableUnderSensitivity)
        self.assertGreater(report.overall.confidenceLower, 0)
        self.assertTrue(report.upliftActionReviewAllowed)
        self.assertTrue(report.modelCard.humanApprovalRequired)
        self.assertFalse(report.automaticActionAllowed)

    def test_poor_overlap_blocks_causal_interpretation_and_action(self) -> None:
        report = self.evaluator.evaluate(_dataset(production=True, poor_overlap=True))
        self.assertLess(report.overlapCoverage, self.policy.minimumOverlapCoverage)
        self.assertGreater(
            report.maximumObservedInversePropensityWeight,
            self.policy.maximumInversePropensityWeight,
        )
        self.assertFalse(report.overlapGatesPassed)
        self.assertFalse(report.causalInterpretationAllowed)
        self.assertFalse(report.upliftActionReviewAllowed)

    def test_causal_gate_and_feature_versions_fail_closed(self) -> None:
        payload = _dataset().model_dump(mode="json")
        payload["causalGateValidated"] = False
        with self.assertRaises(ValidationError):
            UpliftDataset.model_validate(payload)
        payload = _dataset().model_dump(mode="json")
        payload["featureNames"][0] = "customerAge"
        with self.assertRaisesRegex(ValueError, "UPLIFT_VERSION_MISMATCH"):
            self.evaluator.evaluate(UpliftDataset.model_validate(payload))

    def test_sample_and_mature_outcome_are_enforced(self) -> None:
        payload = _dataset().model_dump(mode="json")
        payload["units"] = payload["units"][:80] + payload["units"][160:]
        with self.assertRaisesRegex(ValueError, "UPLIFT_CONTROL_SAMPLE_INSUFFICIENT"):
            self.evaluator.evaluate(UpliftDataset.model_validate(payload))
        payload = _dataset().model_dump(mode="json")
        payload["units"][0]["outcomeObservedAt"] = "2026-08-01T00:00:00Z"
        with self.assertRaisesRegex(ValidationError, "UPLIFT_DATASET_INVALID"):
            UpliftDataset.model_validate(payload)


if __name__ == "__main__":
    unittest.main()
