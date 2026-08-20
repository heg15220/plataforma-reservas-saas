"""Pruebas de Factorization Machine frente a baseline content-based congelado."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.factorization_machine import (
    FactorizationMachineEvaluator,
    FactorizationMachineModelCard,
    FactorizationMachinePolicy,
    SparseInteractionDataset,
)

ROOT = Path(__file__).parents[1]


class FactorizationMachineTests(unittest.TestCase):
    """Valida mejora por interacción, estabilidad, leakage y bloqueo de despliegue."""

    def setUp(self) -> None:
        self.policy = FactorizationMachinePolicy.load(
            ROOT / "policies" / "factorization-machine-evaluation.v1.json"
        )
        self.card = FactorizationMachineModelCard.load(
            ROOT / "models" / "factorization-machine-candidate.v1.model-card.json"
        )
        self.evaluator = FactorizationMachineEvaluator(self.policy, self.card)

    def _dataset(self, production: bool = False) -> SparseInteractionDataset:
        vocabulary = [
            "user.styleA",
            "user.styleB",
            "venue.styleA",
            "venue.styleB",
            "context.morning",
        ]
        rows = []
        for start in (datetime(2026, 5, 1, tzinfo=UTC), datetime(2026, 6, 1, tzinfo=UTC)):
            for index in range(80):
                user_a = index % 2 == 0
                venue_a = (index // 2) % 2 == 0
                converted = int(user_a == venue_a)
                rows.append(
                    {
                        "observationId": str(uuid4()),
                        "occurredAt": start + timedelta(hours=index),
                        "outcomeObservedAt": start + timedelta(hours=index + 1),
                        "activeFeatureCodes": [
                            "user.styleA" if user_a else "user.styleB",
                            "venue.styleA" if venue_a else "venue.styleB",
                            "context.morning",
                        ],
                        "contentBaselineProbability": 0.5,
                        "converted": converted,
                    }
                )
        return SparseInteractionDataset.model_validate(
            {
                "datasetVersion": "fm-interactions-synthetic-v1",
                "extractedAt": datetime(2026, 7, 1, tzinfo=UTC),
                "productionEvidence": production,
                "purpose": "sparseInteractionEvaluation",
                "containsPersonalData": False,
                "consentRevocationsApplied": True,
                "featureVocabulary": vocabulary,
                "rows": rows,
            }
        )

    def test_real_fm_improves_content_baseline_but_synthetic_blocks_review(self) -> None:
        artifact = self.evaluator.evaluate(self._dataset())
        self.assertTrue(artifact.qualityGatesPassed, artifact)
        self.assertGreaterEqual(artifact.rocAucGain, 0.03)
        self.assertLessEqual(artifact.logLossRegression, 0)
        self.assertLessEqual(artifact.stabilityMaximumDelta, 1e-8)
        self.assertFalse(artifact.promotionReviewAllowed)
        self.assertFalse(artifact.automaticDeploymentAllowed)

    def test_production_improvement_only_allows_human_promotion_review(self) -> None:
        artifact = self.evaluator.evaluate(self._dataset(production=True))
        self.assertTrue(artifact.promotionReviewAllowed)
        self.assertFalse(artifact.automaticDeploymentAllowed)
        probability = artifact.predict(["user.styleA", "venue.styleA", "context.morning"])
        self.assertGreater(probability, 0.5)

    def test_sensitive_feature_and_unknown_prediction_fail_closed(self) -> None:
        raw = self._dataset().model_dump()
        raw["featureVocabulary"].append("user.health")
        with self.assertRaisesRegex(ValueError, "PROHIBITED_FEATURE"):
            self.evaluator.evaluate(SparseInteractionDataset.model_validate(raw))
        artifact = self.evaluator.evaluate(self._dataset())
        with self.assertRaisesRegex(ValueError, "UNKNOWN_FEATURE"):
            artifact.predict(["user.unknown", "venue.styleA"])

    def test_temporal_label_and_sample_are_enforced(self) -> None:
        raw = self._dataset().model_dump()
        raw["rows"][0]["outcomeObservedAt"] = datetime(2026, 6, 1, tzinfo=UTC)
        with self.assertRaisesRegex(ValueError, "TRAIN_LABEL_NOT_MATURE"):
            self.evaluator.evaluate(SparseInteractionDataset.model_validate(raw))
        raw = self._dataset().model_dump()
        raw["rows"] = raw["rows"][:30]
        with self.assertRaisesRegex(ValueError, "SAMPLE_INSUFFICIENT"):
            self.evaluator.evaluate(SparseInteractionDataset.model_validate(raw))


if __name__ == "__main__":
    unittest.main()
