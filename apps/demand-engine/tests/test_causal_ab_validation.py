"""Pruebas de la puerta RCT previa a estimadores causales y heterogéneos."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.causal_ab_validation import (
    CausalAbDataset,
    CausalAbPolicy,
    CausalAbValidator,
)


ROOT = Path(__file__).resolve().parents[1]


def _dataset(*, production: bool = False, imbalance: bool = False) -> CausalAbDataset:
    start = datetime(2026, 6, 1, tzinfo=UTC)
    units = []
    for arm in ("control", "treatment"):
        for index in range(120):
            treated = arm == "treatment"
            completed = index < (36 if treated else 24)
            units.append(
                {
                    "unitId": str(uuid4()),
                    "arm": arm,
                    "assignedAt": (start + timedelta(minutes=index)).isoformat(),
                    "exposedAt": (start + timedelta(minutes=index, seconds=5)).isoformat(),
                    "outcomeObservedAt": (start + timedelta(minutes=index + 1)).isoformat(),
                    "preTreatmentFeatures": {
                        "baselineAffinity": (index % 10) / 10 + (0.5 if imbalance and treated else 0),
                        "priorVenueExposure": float(index % 4),
                        "weekday": float(index % 7),
                    },
                    "completedBooking": int(completed),
                }
            )
    return CausalAbDataset.model_validate(
        {
            "datasetVersion": "causal-ab-fixture-v1",
            "extractedAt": "2026-07-02T00:00:00Z",
            "productionEvidence": production,
            "purpose": "causalAbDesignValidation",
            "experimentDesign": "randomizedControlledAb",
            "experimentPolicyVersion": "ranking-ab-test-v1",
            "outcomeDefinitionVersion": "completed-booking-per-exposed-session-v1",
            "analysisFeatureSetVersion": "causal-pre-treatment-v1",
            "containsPersonalData": False,
            "consentRevocationsApplied": True,
            "preRegistered": True,
            "stableRandomAssignment": True,
            "mutuallyExclusiveAssignment": True,
            "assignmentLoggedBeforeExposure": True,
            "experimentCompleted": True,
            "guardrailsPassed": True,
            "units": units,
        }
    )


class CausalAbValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = CausalAbPolicy.load(ROOT / "policies" / "causal-ab-validation.v1.json")
        cls.validator = CausalAbValidator(cls.policy)

    def test_valid_synthetic_ab_estimates_effect_but_blocks_causal_permission(self) -> None:
        report = self.validator.validate(_dataset())
        self.assertTrue(report.designGatesPassed)
        self.assertAlmostEqual(0.1, report.averageTreatmentEffect)
        self.assertEqual([], report.permittedEstimatorReviews)
        self.assertFalse(report.causalEstimationAllowed)
        self.assertTrue(report.observationalAttributionOnly)
        self.assertFalse(report.automaticEstimatorUseAllowed)

    def test_production_ab_opens_review_for_all_declared_estimators(self) -> None:
        report = self.validator.validate(_dataset(production=True))
        self.assertTrue(report.causalEstimationAllowed)
        self.assertEqual(self.policy.permittedEstimatorReviewsAfterGate, report.permittedEstimatorReviews)
        self.assertFalse(report.automaticEstimatorUseAllowed)

    def test_observational_design_and_failed_protocol_cannot_enter_contract(self) -> None:
        payload = _dataset().model_dump(mode="json")
        payload["experimentDesign"] = "observational"
        with self.assertRaises(ValidationError):
            CausalAbDataset.model_validate(payload)
        payload = _dataset().model_dump(mode="json")
        payload["stableRandomAssignment"] = False
        with self.assertRaises(ValidationError):
            CausalAbDataset.model_validate(payload)

    def test_assignment_must_precede_exposure_and_units_are_unique(self) -> None:
        payload = _dataset().model_dump(mode="json")
        payload["units"][0]["assignedAt"] = payload["units"][0]["outcomeObservedAt"]
        with self.assertRaises(ValidationError):
            CausalAbDataset.model_validate(payload)
        payload = _dataset().model_dump(mode="json")
        payload["units"][1]["unitId"] = payload["units"][0]["unitId"]
        with self.assertRaisesRegex(ValidationError, "CAUSAL_AB_DATASET_INVALID"):
            CausalAbDataset.model_validate(payload)

    def test_pre_treatment_imbalance_blocks_causal_estimation(self) -> None:
        report = self.validator.validate(_dataset(production=True, imbalance=True))
        self.assertGreater(
            report.maximumObservedAbsoluteSmd,
            self.policy.maximumAbsoluteStandardizedMeanDifference,
        )
        self.assertFalse(report.designGatesPassed)
        self.assertFalse(report.causalEstimationAllowed)
        self.assertEqual([], report.permittedEstimatorReviews)

    def test_sample_and_feature_leakage_fail_closed(self) -> None:
        payload = _dataset().model_dump(mode="json")
        payload["units"] = payload["units"][:80] + payload["units"][120:]
        with self.assertRaisesRegex(ValueError, "CAUSAL_AB_CONTROL_SAMPLE_INSUFFICIENT"):
            self.validator.validate(CausalAbDataset.model_validate(payload))

        payload = _dataset().model_dump(mode="json")
        payload["units"][0]["preTreatmentFeatures"]["converted"] = 1.0
        with self.assertRaisesRegex(ValueError, "CAUSAL_AB_FEATURE_SET_MISMATCH"):
            self.validator.validate(CausalAbDataset.model_validate(payload))


if __name__ == "__main__":
    unittest.main()
