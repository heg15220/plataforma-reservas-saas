"""Pruebas de autorización, leakage, selección y generalización del entrenamiento visual."""

from __future__ import annotations

import hashlib
import math
import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from pydantic import ValidationError

from reserly_demand_engine.visual_training import (
    ClipLinearCategoryTrainer,
    VisualTrainingDataset,
    VisualTrainingPolicy,
)


ROOT = Path(__file__).parents[1]
CATEGORIES = [
    "restaurante",
    "peluqueria",
    "campo-de-futbol",
    "pista-de-padel",
    "instalacion-municipal",
    "centro-deportivo",
    "centro-de-estetica",
    "otros",
]
REVISION = "f" * 40


class VisualTrainingTests(unittest.TestCase):
    """El test permanece fuera de la selección y todos los activos exigen revisión humana."""

    def setUp(self) -> None:
        self.policy = VisualTrainingPolicy.model_validate(
            {
                "schemaVersion": 1,
                "policyVersion": "visual-training-fixture-v1",
                "algorithmVersion": "softmax-linear-head-sgd-v1",
                "baseModelKey": "clip-fixture-v1",
                "baseModelRevision": REVISION,
                "categories": CATEGORIES,
                "embeddingDimensions": 8,
                "minimumTrainPerCategory": 4,
                "minimumValidationPerCategory": 2,
                "minimumTestPerCategory": 3,
                "learningRate": 0.2,
                "maximumEpochs": 500,
                "earlyStoppingPatience": 40,
                "l2Candidates": [0.0001, 0.01, 0.1],
                "seed": 1729,
                "gates": {
                    "minimumTestAccuracy": 0.9,
                    "maximumTestError": 0.1,
                    "minimumMacroPrecision": 0.8,
                    "minimumMacroRecall": 0.8,
                    "minimumMacroF1": 0.8,
                    "minimumPerCategoryRecall": 0.6,
                    "maximumTrainTestAccuracyGap": 0.1,
                    "syntheticPerfectAccuracyReviewThreshold": 0.98,
                },
                "humanReviewRequired": True,
                "automaticPromotionAllowed": False,
            }
        )

    @staticmethod
    def _vector(category_index: int, sample_index: int) -> list[float]:
        values = [0.01 * ((sample_index + offset) % 3) for offset in range(8)]
        values[category_index] += 1.0
        norm = math.sqrt(sum(value * value for value in values))
        return [value / norm for value in values]

    def _dataset(
        self,
        *,
        swapped_test_pair: bool = True,
        rotate_all_test: bool = False,
    ) -> VisualTrainingDataset:
        rows: list[dict[str, object]] = []
        split_counts = {"train": 4, "validation": 2, "test": 3}
        for split, count in split_counts.items():
            for category_index, category in enumerate(CATEGORIES):
                for sample_index in range(count):
                    vector_category = category_index
                    if split == "test" and rotate_all_test:
                        vector_category = (category_index + 1) % len(CATEGORIES)
                    elif split == "test" and swapped_test_pair and sample_index == 0:
                        if category_index == 0:
                            vector_category = 1
                        elif category_index == 1:
                            vector_category = 0
                    key = f"{split}:{category}:{sample_index}"
                    rows.append(
                        {
                            "imageId": str(uuid5(NAMESPACE_URL, f"image:{key}")),
                            "imageSha256": hashlib.sha256(key.encode()).hexdigest(),
                            "venueId": str(uuid5(NAMESPACE_URL, f"venue:{key}")),
                            "categoryCode": category,
                            "split": split,
                            "embedding": self._vector(vector_category, sample_index),
                            "humanReviewStatus": "approved",
                            "developmentTrainingAllowed": True,
                        }
                    )
        return VisualTrainingDataset.model_validate(
            {
                "schemaVersion": 1,
                "datasetVersion": "visual-training-fixture-v1",
                "frozenAt": datetime(2026, 8, 28, tzinfo=UTC),
                "baseModelKey": "clip-fixture-v1",
                "baseModelRevision": REVISION,
                "synthetic": True,
                "containsPersonalData": False,
                "rows": rows,
            }
        )

    def test_trains_deterministically_and_test_accuracy_exceeds_ninety_percent(self) -> None:
        dataset = self._dataset()
        trainer = ClipLinearCategoryTrainer(self.policy)
        first = trainer.train(dataset)
        second = trainer.train(dataset)
        self.assertEqual(first, second)
        self.assertGreaterEqual(first.testMetrics.accuracy, 0.9)
        self.assertLessEqual(first.testMetrics.errorRate, 0.1)
        self.assertGreaterEqual(first.testMetrics.macroF1, 0.8)
        self.assertLessEqual(first.trainTestAccuracyGap, 0.1)
        self.assertTrue(first.gatesPassed)
        self.assertFalse(first.promotionAllowed)
        self.assertTrue(first.testOpenedExactlyOnce)

    def test_unreviewed_or_unauthorized_image_is_rejected_before_training(self) -> None:
        raw = self._dataset().model_dump(mode="json")
        raw["rows"][0]["humanReviewStatus"] = "pending"
        raw["rows"][0]["developmentTrainingAllowed"] = False
        with self.assertRaises(ValidationError):
            VisualTrainingDataset.model_validate(raw)

    def test_same_venue_cannot_cross_splits(self) -> None:
        raw = self._dataset().model_dump(mode="json")
        train_venue = next(row["venueId"] for row in raw["rows"] if row["split"] == "train")
        next(row for row in raw["rows"] if row["split"] == "test")["venueId"] = train_venue
        with self.assertRaisesRegex(ValueError, "VISUAL_TRAINING_VENUE_LEAKAGE"):
            VisualTrainingDataset.model_validate(raw)

    def test_low_test_accuracy_fails_closed(self) -> None:
        artifact = ClipLinearCategoryTrainer(self.policy).train(
            self._dataset(swapped_test_pair=False, rotate_all_test=True)
        )
        self.assertLess(artifact.testMetrics.accuracy, 0.9)
        self.assertFalse(artifact.metricQualityPassed)
        self.assertFalse(artifact.gatesPassed)

    def test_test_metrics_are_computed_only_after_hyperparameter_selection(self) -> None:
        observed_splits: list[str] = []

        class AuditedTrainer(ClipLinearCategoryTrainer):
            def _metrics(self, rows, weights, biases):  # type: ignore[no-untyped-def]
                observed_splits.append(rows[0].split)
                return super()._metrics(rows, weights, biases)

        AuditedTrainer(self.policy).train(self._dataset())
        self.assertEqual(["validation"] * len(self.policy.l2Candidates), observed_splits[:-3])
        self.assertEqual(["train", "validation", "test"], observed_splits[-3:])

    def test_perfect_synthetic_test_requires_benchmark_review(self) -> None:
        artifact = ClipLinearCategoryTrainer(self.policy).train(
            self._dataset(swapped_test_pair=False)
        )
        self.assertEqual(1.0, artifact.testMetrics.accuracy)
        self.assertTrue(artifact.metricQualityPassed)
        self.assertTrue(artifact.benchmarkDifficultyReviewRequired)
        self.assertFalse(artifact.benchmarkAdequacyPassed)
        self.assertFalse(artifact.gatesPassed)

    def test_repository_policy_requires_real_scale_and_ninety_percent_test(self) -> None:
        policy = VisualTrainingPolicy.load(
            ROOT / "policies" / "clip-linear-category-training.v1.json"
        )
        self.assertEqual(0.9, policy.gates.minimumTestAccuracy)
        self.assertEqual(0.1, policy.gates.maximumTestError)
        self.assertEqual(0.7, policy.gates.minimumPerCategoryRecall)
        self.assertEqual(10, policy.minimumTestPerCategory)
        self.assertEqual(8, len(policy.categories))
        self.assertIn('"torch==2.8.0"', (ROOT / "pyproject.toml").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
