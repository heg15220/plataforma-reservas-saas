"""Pruebas de calibración y límites no decisorios del riesgo de no-show."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.no_show_risk import (
    NoShowDataset,
    NoShowModelCard,
    NoShowRiskTrainer,
    NoShowTrainingPolicy,
)

ROOT = Path(__file__).parents[1]


class NoShowRiskTests(unittest.TestCase):
    """Valida entrenamiento temporal y que la salida no pueda autorizar acciones."""

    def setUp(self) -> None:
        self.policy = NoShowTrainingPolicy.load(ROOT / "policies" / "no-show-risk-training.v1.json")
        self.card = NoShowModelCard.load(ROOT / "models" / "no-show-risk-baseline.v1.model-card.json")
        self.trainer = NoShowRiskTrainer(self.policy, self.card)

    def _dataset(self, production: bool = False) -> NoShowDataset:
        rows = []
        for start in (datetime(2026, 4, 1, tzinfo=UTC), datetime(2026, 5, 1, tzinfo=UTC), datetime(2026, 6, 1, tzinfo=UTC)):
            for index in range(40):
                risk = 1 if index % 4 == 0 else 0
                rows.append({
                    "observationId": str(uuid4()),
                    "occurredAt": start + timedelta(hours=index),
                    "outcomeObservedAt": start + timedelta(hours=index + 2),
                    "auditSegment": "es" if (index // 4) % 2 == 0 else "en",
                    "features": {
                        "leadTimeNormalized": 0.9 if risk else 0.1,
                        "reminderDelivered": 0.0 if risk else 1.0,
                        "scheduleFlexibility": 0.1 if risk else 0.9,
                        "historicalAttendanceRate": 0.1 if risk else 0.9,
                        "rescheduleCountNormalized": 0.8 if risk else 0.2,
                        "eveningSlot": float(index % 2),
                        "weekendSlot": float((index // 2) % 2),
                    },
                    "noShow": risk,
                })
        return NoShowDataset.model_validate({
            "datasetVersion": "no-show-synthetic-v1",
            "extractedAt": datetime(2026, 7, 1, tzinfo=UTC),
            "productionEvidence": production,
            "containsPersonalData": False,
            "consentRevocationsApplied": True,
            "purpose": "aggregateOperationsAnalytics",
            "rows": rows,
        })

    def test_trains_calibrates_and_synthetic_evidence_blocks_promotion(self) -> None:
        artifact = self.trainer.train(self._dataset())
        self.assertTrue(artifact.gatesPassed)
        self.assertGreaterEqual(artifact.evaluationMetrics.rocAuc, 0.7)
        self.assertFalse(artifact.promotionReviewAllowed)
        self.assertLessEqual(artifact.maximumSegmentBrierGap, 0.05)

    def test_signal_explicitly_forbids_customer_actions(self) -> None:
        artifact = self.trainer.train(self._dataset(production=True))
        signal = artifact.signal(self._dataset().rows[0].features, datetime(2026, 8, 20, tzinfo=UTC))
        self.assertTrue(artifact.promotionReviewAllowed)
        self.assertEqual("aggregateCapacityPlanning", signal.allowedUse)
        self.assertFalse(signal.automatedActionAllowed)
        self.assertFalse(signal.penaltyAllowed)
        self.assertFalse(signal.bookingDenialAllowed)
        self.assertFalse(signal.priceChangeAllowed)
        self.assertNotIn("customer", signal.model_dump_json().lower())

    def test_rejects_sensitive_or_outcome_feature_leakage(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"][0]["features"]["gender"] = 1.0
        with self.assertRaisesRegex(ValueError, "FEATURE_SCHEMA_OR_LEAKAGE"):
            self.trainer.train(NoShowDataset.model_validate(raw))

    def test_rejects_immature_outcome(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"][0]["outcomeObservedAt"] = datetime(2026, 5, 1, tzinfo=UTC)
        with self.assertRaisesRegex(ValueError, "LABEL_NOT_MATURE"):
            self.trainer.train(NoShowDataset.model_validate(raw))

    def test_small_audit_cohort_fails_closed(self) -> None:
        raw = self._dataset().model_dump()
        for row in raw["rows"]:
            if row["occurredAt"] >= datetime(2026, 6, 1, tzinfo=UTC):
                row["auditSegment"] = "es"
        with self.assertRaisesRegex(ValueError, "AUDIT_SEGMENT_SAMPLE"):
            self.trainer.train(NoShowDataset.model_validate(raw))


if __name__ == "__main__":
    unittest.main()
