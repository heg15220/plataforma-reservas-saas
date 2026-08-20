"""Pruebas del forecast Poisson, calibración temporal, baseline y promoción segura."""

from __future__ import annotations

import math
import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.demand_forecasting import (
    DemandForecastDataset,
    DemandForecastEvaluator,
    DemandForecastModelCard,
    DemandForecastPolicy,
)


ROOT = Path(__file__).resolve().parents[1]


def _rows(start: datetime, count: int) -> list[dict[str, object]]:
    venue_id = uuid4()
    rows = []
    for index in range(count):
        bucket = start + timedelta(hours=index)
        hour = bucket.hour
        weekday = bucket.weekday()
        demand = 4 + (hour // 6) + (2 if weekday >= 5 else 0) + (index % 3)
        rows.append(
            {
                "bucketId": str(uuid4()),
                "venueId": str(venue_id),
                "categoryCode": "personalCare",
                "bucketStart": bucket.isoformat(),
                "outcomeObservedAt": (bucket + timedelta(minutes=30)).isoformat(),
                "featureValues": [
                    math.sin(2 * math.pi * hour / 24),
                    math.cos(2 * math.pi * hour / 24),
                    math.sin(2 * math.pi * weekday / 7),
                    math.cos(2 * math.pi * weekday / 7),
                    float(weekday >= 5),
                    0.0,
                    float(demand),
                    float(demand),
                    20.0,
                ],
                "baselineForecast": 3.0,
                "baselineLower": 0.0,
                "baselineUpper": 15.0,
                "observedDemand": demand,
            }
        )
    return rows


def _dataset(production: bool = False) -> DemandForecastDataset:
    return DemandForecastDataset.model_validate(
        {
            "datasetVersion": "demand-forecast-fixture-v1",
            "extractedAt": "2026-07-02T00:00:00Z",
            "productionEvidence": production,
            "purpose": "aggregateDemandForecastEvaluation",
            "containsPersonalData": False,
            "consentRevocationsApplied": True,
            "sourceTimezoneValidated": True,
            "sourceQualityValidated": True,
            "featureNames": [
                "hourSin",
                "hourCos",
                "weekdaySin",
                "weekdayCos",
                "weekend",
                "holiday",
                "lagSameSlot7d",
                "rollingMean28d",
                "availableCapacity",
            ],
            "rows": [
                *_rows(datetime(2026, 4, 1, tzinfo=UTC), 96),
                *_rows(datetime(2026, 5, 2, tzinfo=UTC), 72),
                *_rows(datetime(2026, 6, 2, tzinfo=UTC), 72),
            ],
        }
    )


class DemandForecastTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = DemandForecastPolicy.load(ROOT / "policies" / "demand-forecast-evaluation.v1.json")
        cls.card = DemandForecastModelCard.load(ROOT / "models" / "demand-forecast-candidate.v1.model-card.json")
        cls.evaluator = DemandForecastEvaluator(cls.policy, cls.card)

    def test_poisson_forecast_beats_baseline_and_calibrates_future_interval(self) -> None:
        report = self.evaluator.evaluate(_dataset())
        self.assertGreaterEqual(report.maeImprovement, self.policy.minimumMaeImprovement)
        self.assertLessEqual(report.wapeRegression, 0)
        self.assertGreaterEqual(report.candidateMetrics.intervalCoverage, 0.8)
        self.assertLess(report.candidateMetrics.meanIntervalWidth, report.baselineMetrics.meanIntervalWidth)
        self.assertTrue(report.qualityGatesPassed)
        self.assertFalse(report.reliableForecast)
        self.assertFalse(report.promotionReviewAllowed)
        self.assertFalse(report.automaticActionAllowed)

    def test_production_evidence_allows_review_but_never_automatic_action(self) -> None:
        report = self.evaluator.evaluate(_dataset(production=True))
        self.assertTrue(report.reliableForecast)
        self.assertTrue(report.promotionReviewAllowed)
        self.assertTrue(report.modelCard.humanApprovalRequired)
        self.assertFalse(report.automaticActionAllowed)
        self.assertFalse(report.automaticDeploymentAllowed)

    def test_training_is_reproducible_and_artifact_is_identified(self) -> None:
        report = self.evaluator.evaluate(_dataset())
        self.assertLessEqual(report.stabilityMaximumDelta, self.policy.maximumStabilityDelta)
        self.assertEqual(64, len(report.modelSha256))
        self.assertGreaterEqual(report.conformalAbsoluteResidual, 0)

    def test_feature_contract_and_import_quality_fail_closed(self) -> None:
        mismatched = _dataset().model_dump(mode="json")
        mismatched["featureNames"][0] = "customerAge"
        with self.assertRaisesRegex(ValueError, "DEMAND_FORECAST_FEATURE_VERSION_MISMATCH"):
            self.evaluator.evaluate(DemandForecastDataset.model_validate(mismatched))

        invalid_quality = _dataset().model_dump(mode="json")
        invalid_quality["sourceQualityValidated"] = False
        with self.assertRaises(Exception):
            DemandForecastDataset.model_validate(invalid_quality)

    def test_outcome_maturity_and_split_sample_are_enforced(self) -> None:
        immature = _dataset().model_dump(mode="json")
        immature["rows"][0]["outcomeObservedAt"] = "2026-05-01T00:00:00Z"
        with self.assertRaisesRegex(ValueError, "DEMAND_FORECAST_TRAIN_OUTCOME_NOT_MATURE"):
            self.evaluator.evaluate(DemandForecastDataset.model_validate(immature))

        insufficient = _dataset().model_dump(mode="json")
        insufficient["rows"] = insufficient["rows"][:20] + insufficient["rows"][96:]
        with self.assertRaisesRegex(ValueError, "DEMAND_FORECAST_TRAIN_SAMPLE_INSUFFICIENT"):
            self.evaluator.evaluate(DemandForecastDataset.model_validate(insufficient))


if __name__ == "__main__":
    unittest.main()
