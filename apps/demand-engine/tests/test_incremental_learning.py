"""Pruebas del challenger River, drift, checkpoints y rollback seguro."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.incremental_learning import (
    IncrementalLearningMonitor, IncrementalLearningPolicy, IncrementalLearningRequest,
    IncrementalModelCard,
)


POLICY = Path(__file__).resolve().parents[1] / "policies/incremental-learning.v1.json"
MODEL_CARD = Path(__file__).resolve().parents[1] / "models/incremental-logistic-shadow.v1.json"


class IncrementalLearningTests(unittest.TestCase):
    """Acredita prequentialidad, reproducibilidad y cierre ante degradación."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 21, 18, 0, tzinfo=UTC)
        self.monitor = IncrementalLearningMonitor(
            IncrementalLearningPolicy.load(POLICY), IncrementalModelCard.load(MODEL_CARD)
        )

    def _observations(self, count: int, shift: float = 0.0) -> list[dict[str, object]]:
        return [{
            "observationId": str(uuid4()), "sequence": index + 1,
            "occurredAt": (self.now - timedelta(days=2)).isoformat(),
            "outcomeObservedAt": (self.now - timedelta(days=1)).isoformat(),
            "features": {
                "availability": (index % 2) * 0.1 + shift,
                "contentAffinity": ((index + 1) % 2) * 0.1 + shift,
                "quality": (index % 3) * 0.05 + shift,
            },
            "completedBooking": index % 2,
        } for index in range(count)]

    def _request(self, observations, **changes) -> IncrementalLearningRequest:
        body = {
            "requestId": str(uuid4()), "schemaVersion": 1, "occurredAt": self.now.isoformat(),
            "locale": "es", "policyVersion": "incremental-learning-v1",
            "modelVersion": "incremental-logistic-shadow-v1",
            "featureSetVersion": "incremental-ranking-features-v1",
            "productionEvidence": True, "purpose": "shadowIncrementalEvaluation",
            "containsPersonalData": False, "consentRevocationsApplied": True,
            "priorState": self.monitor.empty_state().model_dump(mode="json"),
            "referenceAbsoluteErrors": [0.5] * 64,
            "referenceFeatureValues": {
                "availability": [0.0, 0.1] * 32,
                "contentAffinity": [0.1, 0.0] * 32,
                "quality": [0.0, 0.05, 0.1, 0.05] * 16,
            },
            "observations": observations,
        }
        body.update(changes)
        return IncrementalLearningRequest.model_validate(body)

    def test_stable_production_batch_builds_reviewable_checkpoint(self) -> None:
        result = self.monitor.evaluate(self._request(self._observations(32)))
        self.assertEqual("candidateUpdated", result.status)
        self.assertFalse(result.driftDetected)
        self.assertTrue(result.humanReviewAllowed)
        self.assertEqual(32, result.candidateState.trainingCount)
        self.assertFalse(result.automaticPromotionAllowed)
        self.assertFalse(result.onlineDeploymentAllowed)

    def test_error_degradation_blocks_checkpoint_and_requires_rollback(self) -> None:
        observations = self._observations(64)
        result = self.monitor.evaluate(self._request(
            observations, referenceAbsoluteErrors=[0.01] * 64
        ))
        self.assertEqual("driftBlocked", result.status)
        self.assertTrue(result.rollbackRequired)
        self.assertIsNone(result.candidateState)
        self.assertEqual("fallback-mvp-v1", result.fallbackPolicyVersion)

    def test_page_hinkley_detects_abrupt_feature_shift(self) -> None:
        result = self.monitor.evaluate(self._request(self._observations(64, shift=20.0)))
        self.assertTrue(result.driftDetected)
        self.assertTrue(any(signal.detector == "PageHinkley" and signal.detected
                            for signal in result.driftSignals))

    def test_small_or_synthetic_batch_never_opens_human_review(self) -> None:
        small = self.monitor.evaluate(self._request(self._observations(8)))
        synthetic = self.monitor.evaluate(self._request(
            self._observations(32), productionEvidence=False
        ))
        self.assertEqual("insufficientBatch", small.status)
        self.assertFalse(small.humanReviewAllowed)
        self.assertFalse(synthetic.humanReviewAllowed)

    def test_replay_from_same_checkpoint_is_byte_equivalent_except_request_id(self) -> None:
        request = self._request(self._observations(32))
        first = self.monitor.evaluate(request)
        second = self.monitor.evaluate(request)
        self.assertEqual(first.candidateState, second.candidateState)
        self.assertEqual(first.driftSignals, second.driftSignals)

    def test_sequence_feature_policy_and_checksum_drift_fail_closed(self) -> None:
        observations = self._observations(32)
        observations[0]["sequence"] = 2
        with self.assertRaisesRegex(ValueError, "REQUEST_REJECTED"):
            self.monitor.evaluate(self._request(observations))
        request = self._request(self._observations(32))
        request = request.model_copy(update={
            "priorState": request.priorState.model_copy(update={"stateChecksum": "0" * 64})
        })
        with self.assertRaisesRegex(ValueError, "REQUEST_REJECTED"):
            self.monitor.evaluate(request)


if __name__ == "__main__":
    unittest.main()
