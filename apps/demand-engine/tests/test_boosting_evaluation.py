"""Pruebas de comparación CatBoost contra baseline en calidad, latencia, estabilidad y equidad."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.boosting_evaluation import (
    BoostingComparisonPolicy,
    BoostingModelCard,
    CatBoostConversionEvaluator,
)
from reserly_demand_engine.conversion_training import (
    ConversionDataset,
    ConversionModelCard,
    ConversionTrainingPolicy,
)


ROOT = Path(__file__).parents[1]


class BoostingEvaluationTests(unittest.TestCase):
    """Ejecuta CatBoost real y conserva la logística como champion por defecto."""

    def setUp(self) -> None:
        self.policy = BoostingComparisonPolicy.load(
            ROOT / "policies" / "boosting-comparison.v1.json"
        )
        self.training_policy = ConversionTrainingPolicy.load(
            ROOT / "policies" / "conversion-logistic-training.v1.json"
        )
        self.baseline_card = ConversionModelCard.load(
            ROOT / "models" / "conversion-logistic-baseline.v1.model-card.json"
        )
        self.candidate_card = BoostingModelCard.load(
            ROOT / "models" / "catboost-conversion-candidate.v1.model-card.json"
        )
        self.evaluator = CatBoostConversionEvaluator(
            self.policy, self.training_policy, self.baseline_card, self.candidate_card
        )

    def _dataset(self, production: bool = False) -> ConversionDataset:
        rows: list[dict[str, object]] = []
        starts = [
            datetime(2026, 4, 1, tzinfo=UTC),
            datetime(2026, 5, 1, tzinfo=UTC),
            datetime(2026, 6, 1, tzinfo=UTC),
        ]
        patterns = [(0.1, 0.1), (0.1, 0.9), (0.9, 0.1), (0.9, 0.9)]
        for start in starts:
            for index in range(40):
                affinity, availability = patterns[index % 4]
                converted = int((affinity > 0.5) != (availability > 0.5))
                rows.append(
                    {
                        "observationId": str(uuid4()),
                        "occurredAt": start + timedelta(hours=index),
                        "outcomeObservedAt": start + timedelta(hours=index + 1),
                        "evaluationSegment": "es" if (index // 4) % 2 == 0 else "en",
                        "features": {
                            "affinity": affinity,
                            "proximity": 0.5,
                            "availability": availability,
                            "normalizedPrice": 0.5,
                            "timeUrgency": 0.5,
                            "newVenue": index % 2,
                            "eveningSlot": (index // 2) % 2,
                            "weekendSlot": (index // 3) % 2,
                        },
                        "converted": converted,
                    }
                )
        return ConversionDataset.model_validate(
            {
                "datasetVersion": "boosting-xor-synthetic-v1",
                "extractedAt": datetime(2026, 7, 1, tzinfo=UTC),
                "productionEvidence": production,
                "containsPersonalData": False,
                "consentRevocationsApplied": True,
                "purpose": "analytics",
                "rows": rows,
            }
        )

    def test_real_catboost_beats_baseline_but_synthetic_data_blocks_promotion(self) -> None:
        result = self.evaluator.evaluate(self._dataset())
        self.assertTrue(result.qualityGatesPassed, result)
        self.assertGreaterEqual(result.rocAucGain, 0.02)
        self.assertLessEqual(result.stabilityMaximumDelta, 0.000001)
        self.assertLessEqual(result.maximumSegmentBrierGap, 0.05)
        self.assertFalse(result.promotionAllowed)
        self.assertEqual(
            ["productionEvidenceMissing", "cveReviewMissing"], result.blockingReasons
        )

    def test_promotion_requires_quality_production_and_approved_cve_review(self) -> None:
        result = self.evaluator.evaluate(
            self._dataset(production=True), cve_review_approved=True
        )
        self.assertTrue(result.qualityGatesPassed)
        self.assertTrue(result.promotionAllowed)
        self.assertEqual([], result.blockingReasons)
        self.assertEqual("1.2.10", result.libraryVersion)

    def test_small_audit_segment_fails_closed(self) -> None:
        raw = self._dataset().model_dump()
        for row in raw["rows"]:
            if row["occurredAt"] >= datetime(2026, 6, 1, tzinfo=UTC):
                row["evaluationSegment"] = "es"
        with self.assertRaisesRegex(ValueError, "AUDIT_SEGMENT_SAMPLE"):
            self.evaluator.evaluate(ConversionDataset.model_validate(raw))

    def test_dependency_and_model_versions_are_pinned(self) -> None:
        incompatible = self.candidate_card.model_copy(update={"libraryVersion": "1.2.9"})
        with self.assertRaisesRegex(ValueError, "DEPENDENCY_OR_MODEL_VERSION"):
            CatBoostConversionEvaluator(
                self.policy,
                self.training_policy,
                self.baseline_card,
                incompatible,
            )


if __name__ == "__main__":
    unittest.main()
