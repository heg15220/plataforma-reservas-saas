"""Pruebas de LinUCB, presupuesto de tráfico, idempotencia y replay IPS/SNIPS."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.contextual_exploration import (
    ContextualLinUCB,
    LinUCBArmState,
    LinUCBModelCard,
    LinUCBPolicy,
    LinUCBPolicyError,
    LinUCBSelectionRequest,
    LinUCBUpdateRequest,
    OfflineBanditDataset,
    OfflineLinUCBEvaluator,
)


ROOT = Path(__file__).resolve().parents[1]


class ContextualExplorationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = LinUCBPolicy.load(ROOT / "policies" / "linucb-contextual.v1.json")
        cls.card = LinUCBModelCard.load(ROOT / "models" / "linucb-contextual-candidate.v1.model-card.json")
        cls.bandit = ContextualLinUCB(cls.policy)
        cls.evaluator = OfflineLinUCBEvaluator(cls.policy, cls.card)
        cls.now = datetime(2026, 6, 10, 12, tzinfo=UTC)

    def _candidate(self, number: int, *, quality: float = 0.8, capacity: int = 1) -> dict[str, object]:
        venue_id = UUID(int=number)
        state = LinUCBArmState.prior(venue_id, None, self.policy)
        return {
            "venueId": str(venue_id),
            "quality": quality,
            "explorationAllowed": True,
            "contextValues": [0.8, 0.6, 0.4, number / 100],
            "constraints": {
                "venuePublished": True,
                "serviceBookable": True,
                "eligibilityAllowed": True,
                "permissionAllowed": True,
                "filtersMatched": True,
                "frequencyAllowed": True,
                "availableCapacity": capacity,
                "requestedCapacity": 1,
                "validUntil": (self.now + timedelta(minutes=5)).isoformat(),
            },
            "state": state.model_dump(mode="json"),
        }

    def _selection(self, candidates: list[dict[str, object]], explored: int = 9) -> LinUCBSelectionRequest:
        return LinUCBSelectionRequest.model_validate(
            {
                "requestId": "00000000-0000-0000-0000-000000000099",
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": self.policy.policyVersion,
                "requestedSlots": 10,
                "trafficWindowEligibleSelections": 90,
                "trafficWindowExplorationSelections": explored,
                "candidates": candidates,
            }
        )

    def _offline_dataset(self, *, production: bool = False) -> OfflineBanditDataset:
        events = []
        for index in range(60):
            success = index % 2 == 0
            events.append(
                {
                    "eventId": str(uuid4()),
                    "occurredAt": (self.now + timedelta(minutes=index)).isoformat(),
                    "outcomeObservedAt": (self.now + timedelta(minutes=index + 1)).isoformat(),
                    "contextValues": [0.8, 0.6, 0.4, (index % 5) / 10],
                    "loggingPropensity": 0.5,
                    "targetPolicyProbability": 0.9 if success else 0.1,
                    "reward": 1.0 if success else 0.0,
                    "quality": 0.8,
                    "exploratoryAction": not success,
                    "hardConstraintViolation": False,
                }
            )
        return OfflineBanditDataset.model_validate(
            {
                "datasetVersion": "linucb-replay-fixture-v1",
                "extractedAt": "2026-07-02T00:00:00Z",
                "productionEvidence": production,
                "purpose": "offlineContextualPolicyEvaluation",
                "containsPersonalData": False,
                "consentRevocationsApplied": True,
                "policyVersion": self.policy.policyVersion,
                "events": events,
            }
        )

    def test_selection_is_reproducible_and_respects_rolling_ten_percent_budget(self) -> None:
        request = self._selection([self._candidate(index) for index in range(1, 21)])
        first = self.bandit.select(request)
        second = self.bandit.select(request)
        self.assertEqual(first, second)
        self.assertEqual(1, first.maximumExplorationSlots)
        self.assertEqual(1, len(first.selections))
        self.assertEqual(0.1, first.projectedExplorationShare)

    def test_quality_and_hard_constraints_apply_before_ucb(self) -> None:
        candidates = [self._candidate(index) for index in range(1, 21)]
        candidates[0] = self._candidate(1, quality=0.59)
        candidates[1] = self._candidate(2, capacity=0)
        result = self.bandit.select(self._selection(candidates))
        selected = {item.venueId for item in result.selections}
        self.assertFalse({UUID(int=1), UUID(int=2)} & selected)
        self.assertEqual(18, result.guardedCandidateCount)

    def test_exhausted_traffic_budget_returns_no_exploration(self) -> None:
        result = self.bandit.select(
            self._selection([self._candidate(index) for index in range(1, 21)], explored=10)
        )
        self.assertEqual(0, result.maximumExplorationSlots)
        self.assertEqual([], result.selections)
        self.assertEqual(0.1, result.projectedExplorationShare)

    def test_contextual_update_is_idempotent_and_changes_sufficient_statistics(self) -> None:
        venue_id, event_id = uuid4(), uuid4()
        state = LinUCBArmState.prior(venue_id, None, self.policy)
        request = LinUCBUpdateRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": self.policy.policyVersion,
                "outcomeEventId": str(event_id),
                "reward": 1.0,
                "contextValues": [1.0, 0.0, 0.0, 0.0],
                "state": state.model_dump(mode="json"),
            }
        )
        first = self.bandit.update(request)
        replay = self.bandit.update(request.model_copy(update={"state": first.state}))
        self.assertTrue(first.applied)
        self.assertEqual(2.0, first.state.covariance[0][0])
        self.assertEqual(1.0, first.state.rewardVector[0])
        self.assertEqual(1, first.state.stateVersion)
        self.assertFalse(replay.applied)
        self.assertEqual(first.state, replay.state)

    def test_offline_ips_snips_pass_but_synthetic_blocks_promotion(self) -> None:
        report = self.evaluator.evaluate(self._offline_dataset())
        self.assertEqual(0.5, report.loggedMeanReward)
        self.assertEqual(0.9, report.ipsReward)
        self.assertEqual(0.9, report.snipsReward)
        self.assertEqual(0.1, report.targetExplorationShare)
        self.assertGreaterEqual(report.effectiveSampleSize, self.policy.minimumEffectiveSampleSize)
        self.assertTrue(report.qualityGatesPassed)
        self.assertFalse(report.promotionReviewAllowed)
        self.assertFalse(report.causalClaimAllowed)
        self.assertFalse(report.automaticDeploymentAllowed)

    def test_production_replay_allows_only_human_review(self) -> None:
        report = self.evaluator.evaluate(self._offline_dataset(production=True))
        self.assertTrue(report.promotionReviewAllowed)
        self.assertTrue(report.modelCard.humanApprovalRequired)
        self.assertFalse(report.automaticDeploymentAllowed)

    def test_offline_support_and_risk_violations_fail_closed(self) -> None:
        unsupported = self._offline_dataset().model_dump(mode="json")
        unsupported["events"][0]["loggingPropensity"] = 0.01
        with self.assertRaisesRegex(LinUCBPolicyError, "LINUCB_OFFLINE_SUPPORT_INSUFFICIENT"):
            self.evaluator.evaluate(OfflineBanditDataset.model_validate(unsupported))

        risky = self._offline_dataset().model_dump(mode="json")
        risky["events"][0]["hardConstraintViolation"] = True
        report = self.evaluator.evaluate(OfflineBanditDataset.model_validate(risky))
        self.assertFalse(report.qualityGatesPassed)
        self.assertGreater(report.constraintViolationRate, 0)


if __name__ == "__main__":
    unittest.main()
