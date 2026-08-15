"""Pruebas de prior, determinismo, cuota, guardrails e idempotencia Thompson."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.exploration import (
    BasicThompsonSampler,
    ThompsonPolicy,
    ThompsonSelectionRequest,
    ThompsonUpdateRequest,
)


ROOT = Path(__file__).resolve().parents[1]


class BasicThompsonSamplerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.sampler = BasicThompsonSampler(
            ThompsonPolicy.load(ROOT / "policies" / "thompson-basic.v1.json")
        )
        self.now = datetime(2026, 8, 15, 12, tzinfo=UTC)

    def _candidate(self, number: int, *, quality: float = 0.8, allowed: bool = True) -> dict[str, object]:
        venue_id = UUID(int=number)
        return {
            "venueId": str(venue_id), "quality": quality,
            "explorationAllowed": allowed,
            "constraints": {
                "venuePublished": True, "serviceBookable": True,
                "eligibilityAllowed": True, "permissionAllowed": True,
                "filtersMatched": True, "frequencyAllowed": True,
                "availableCapacity": 1, "requestedCapacity": 1,
                "validUntil": (self.now + timedelta(minutes=5)).isoformat(),
            },
            "posterior": {
                "venueId": str(venue_id), "alpha": 1, "beta": 1,
                "posteriorVersion": 0, "appliedOutcomeIds": [],
            },
        }

    def _selection(self, candidates: list[dict[str, object]]) -> ThompsonSelectionRequest:
        return ThompsonSelectionRequest.model_validate({
            "requestId": "00000000-0000-0000-0000-000000000099",
            "schemaVersion": 1, "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "thompson-basic-v1", "requestedSlots": 10,
            "candidates": candidates,
        })

    def test_selection_is_reproducible_and_never_exceeds_ten_percent(self) -> None:
        request = self._selection([self._candidate(index) for index in range(1, 21)])
        first = self.sampler.select(request)
        second = self.sampler.select(request)
        self.assertEqual(first, second)
        self.assertEqual(2, first.maximumExplorationSlots)
        self.assertEqual(2, len(first.selections))

    def test_quality_permission_and_hard_constraints_filter_before_sampling(self) -> None:
        candidates = [self._candidate(index) for index in range(1, 21)]
        candidates[0] = self._candidate(1, quality=0.59)
        candidates[1] = self._candidate(2, allowed=False)
        candidates[2]["constraints"] = {
            **candidates[2]["constraints"], "availableCapacity": 0,
        }
        result = self.sampler.select(self._selection(candidates))
        selected = {item.venueId for item in result.selections}
        self.assertFalse({UUID(int=1), UUID(int=2), UUID(int=3)} & selected)
        self.assertEqual(17, result.guardedCandidateCount)
        self.assertEqual(1, result.maximumExplorationSlots)
        self.assertEqual(1, len(result.selections))

    def test_small_guarded_pool_cannot_consume_an_exploration_slot(self) -> None:
        result = self.sampler.select(
            self._selection([self._candidate(index) for index in range(1, 10)])
        )
        self.assertEqual(0, result.maximumExplorationSlots)
        self.assertEqual([], result.selections)

    def test_update_is_idempotent_for_same_outcome_event(self) -> None:
        event_id, venue_id = uuid4(), uuid4()
        request = ThompsonUpdateRequest.model_validate({
            "requestId": str(uuid4()), "schemaVersion": 1,
            "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "thompson-basic-v1", "outcomeEventId": str(event_id),
            "reward": "success", "state": {
                "venueId": str(venue_id), "alpha": 1, "beta": 1,
                "posteriorVersion": 0, "appliedOutcomeIds": [],
            },
        })
        first = self.sampler.update(request)
        replay = self.sampler.update(request.model_copy(update={"state": first.state}))
        self.assertTrue(first.applied)
        self.assertEqual((2, 1, 1), (first.state.alpha, first.state.beta, first.state.posteriorVersion))
        self.assertFalse(replay.applied)
        self.assertEqual(first.state, replay.state)

    def test_failure_increments_only_beta(self) -> None:
        request = ThompsonUpdateRequest.model_validate({
            "requestId": str(uuid4()), "schemaVersion": 1,
            "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "thompson-basic-v1", "outcomeEventId": str(uuid4()),
            "reward": "failure", "state": {
                "venueId": str(uuid4()), "alpha": 1, "beta": 1,
                "posteriorVersion": 0, "appliedOutcomeIds": [],
            },
        })
        result = self.sampler.update(request)
        self.assertEqual((1, 2, 1), (
            result.state.alpha, result.state.beta, result.state.posteriorVersion,
        ))


if __name__ == "__main__":
    unittest.main()
