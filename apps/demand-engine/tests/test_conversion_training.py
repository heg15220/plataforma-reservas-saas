"""Pruebas de split temporal, leakage, calibración y model card de conversión."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.conversion_training import (
    ConversionDataset,
    ConversionLogisticTrainer,
    ConversionModelCard,
    ConversionTrainingPolicy,
)


ROOT = Path(__file__).parents[1]


class ConversionTrainingTests(unittest.TestCase):
    """Demuestra que cada parámetro usa solo la ventana que le corresponde."""

    def setUp(self) -> None:
        policy = ConversionTrainingPolicy.load(
            ROOT / "policies" / "conversion-logistic-training.v1.json"
        )
        card = ConversionModelCard.load(
            ROOT / "models" / "conversion-logistic-baseline.v1.model-card.json"
        )
        self.trainer = ConversionLogisticTrainer(policy, card)

    def _dataset(self, production: bool = False) -> ConversionDataset:
        rows: list[dict[str, object]] = []
        starts = [
            datetime(2026, 4, 1, tzinfo=UTC),
            datetime(2026, 5, 1, tzinfo=UTC),
            datetime(2026, 6, 1, tzinfo=UTC),
        ]
        for start in starts:
            for index in range(20):
                signal = index / 19
                rows.append(
                    {
                        "observationId": str(uuid4()),
                        "occurredAt": start + timedelta(days=index),
                        "outcomeObservedAt": start + timedelta(days=index, hours=2),
                        "features": {
                            "affinity": signal,
                            "proximity": signal,
                            "availability": 0.8,
                            "normalizedPrice": 1 - signal,
                            "timeUrgency": signal,
                            "newVenue": index % 2,
                            "eveningSlot": (index // 2) % 2,
                            "weekendSlot": (index // 3) % 2,
                        },
                        "converted": int(signal >= 0.5),
                    }
                )
        return ConversionDataset.model_validate(
            {
                "datasetVersion": "conversion-synthetic-contract-v1",
                "extractedAt": datetime(2026, 7, 1, tzinfo=UTC),
                "productionEvidence": production,
                "containsPersonalData": False,
                "consentRevocationsApplied": True,
                "purpose": "analytics",
                "rows": rows,
            }
        )

    def test_trains_calibrates_evaluates_and_is_deterministic(self) -> None:
        dataset = self._dataset()
        first = self.trainer.train(dataset)
        second = self.trainer.train(dataset)
        self.assertEqual(first, second)
        self.assertEqual(20, first.calibrationMetrics.rows)
        self.assertEqual(20, first.evaluationMetrics.rows)
        self.assertGreaterEqual(first.evaluationMetrics.rocAuc, 0.9)
        self.assertTrue(first.gatesPassed)
        self.assertFalse(first.promotionAllowed)
        self.assertEqual("candidate", first.modelCard.status)

    def test_prediction_uses_frozen_scaler_and_exact_feature_schema(self) -> None:
        artifact = self.trainer.train(self._dataset(production=True))
        low = {code: 0.2 for code in artifact.featureCodes}
        high = {code: 0.8 for code in artifact.featureCodes}
        low["normalizedPrice"] = 0.8
        high["normalizedPrice"] = 0.2
        self.assertLess(artifact.predict(low), artifact.predict(high))
        with self.assertRaises(ValueError):
            artifact.predict({**low, "bookingCompleted": 1})

    def test_rejects_outcome_and_identity_leakage(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"][0]["features"]["bookingCompleted"] = 1
        dataset = ConversionDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)
        raw = self._dataset().model_dump()
        raw["rows"][0]["features"]["customerEmail"] = 0
        dataset = ConversionDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)

    def test_rejects_label_not_mature_at_split_boundary(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"][0]["outcomeObservedAt"] = datetime(2026, 5, 1, tzinfo=UTC)
        dataset = ConversionDataset.model_validate(raw)
        with self.assertRaisesRegex(ValueError, "LABEL_NOT_MATURE"):
            self.trainer.train(dataset)

    def test_rejects_insufficient_temporal_split(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"] = raw["rows"][:39]
        dataset = ConversionDataset.model_validate(raw)
        with self.assertRaises(ValueError):
            self.trainer.train(dataset)


if __name__ == "__main__":
    unittest.main()
