"""Pruebas de orden, umbrales, permisos y cuota del fallback determinista."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import UUID

from reserly_demand_engine.fallback import DeterministicFallback, FallbackPolicy
from reserly_demand_engine.scoring import ScoreMvp, ScoreMvpRequest, ScorePolicy


ROOT = Path(__file__).resolve().parents[1]


class DeterministicFallbackTests(unittest.TestCase):
    def setUp(self) -> None:
        fallback = DeterministicFallback(
            FallbackPolicy.load(ROOT / "policies" / "fallback-mvp.v1.json")
        )
        self.ranker = ScoreMvp(
            ScorePolicy.load(ROOT / "policies" / "score-mvp.v1.json"), fallback
        )

    def _candidate(self, suffix: int, **signals: object) -> dict[str, object]:
        fallback: dict[str, object] = {
            "contextualPopularity": 0.5, "popularitySampleCount": 20,
            "rating": 0.8, "ratingSampleCount": 10,
            "proximity": 0.5, "locationPermissionGranted": True,
            "availability": 0.8, "novelty": 0.0, "isNewVenue": False,
        }
        fallback.update(signals)
        return {
            "venueId": str(UUID(int=suffix)),
            "constraints": {
                "venuePublished": True, "serviceBookable": True,
                "eligibilityAllowed": True, "permissionAllowed": True,
                "filtersMatched": True, "frequencyAllowed": True,
                "availableCapacity": 1, "requestedCapacity": 1,
                "validUntil": "2026-08-14T12:05:00+00:00",
            },
            "affinity": 0.5, "conversion": 0.5, "proximity": 0.5,
            "availability": 0.5, "capacityNeed": 0.5, "quality": 0.8,
            "exploration": 0.0, "fallback": fallback,
        }

    def _request(self, candidates: list[dict[str, object]]) -> ScoreMvpRequest:
        return ScoreMvpRequest.model_validate({
            "requestId": "00000000-0000-0000-0000-000000000099",
            "schemaVersion": 1, "occurredAt": datetime(2026, 8, 14, 12, tzinfo=UTC),
            "locale": "es", "policyVersion": "score-mvp-v1",
            "fallbackReason": "model_not_available", "candidates": candidates,
        })

    def test_uses_contextual_popularity_then_availability_and_stable_uuid(self) -> None:
        low = self._candidate(3, contextualPopularity=0.2)
        tied_second = self._candidate(2, contextualPopularity=0.9, availability=0.7)
        tied_first = self._candidate(1, contextualPopularity=0.9, availability=0.7)
        result = self.ranker.rank(self._request([low, tied_second, tied_first]))
        self.assertEqual([UUID(int=1), UUID(int=2), UUID(int=3)], [x.venueId for x in result.items])
        self.assertEqual("fallback-mvp-v1", result.policyVersion)
        self.assertEqual("fallback_ranked", result.status)
        self.assertTrue(result.fallbackApplied)
        self.assertTrue(all(item.score is None for item in result.items))

    def test_ignores_rating_without_sample_and_proximity_without_permission(self) -> None:
        unsupported = self._candidate(
            2, contextualPopularity=0, popularitySampleCount=0,
            rating=1.0, ratingSampleCount=1, proximity=1.0,
            locationPermissionGranted=False,
        )
        supported = self._candidate(
            1, contextualPopularity=0, popularitySampleCount=0,
            rating=0.5, ratingSampleCount=10, proximity=0,
            locationPermissionGranted=False,
        )
        result = self.ranker.rank(self._request([unsupported, supported]))
        self.assertEqual(UUID(int=1), result.items[0].venueId)
        evidence = {item.component: item for item in result.items[1].fallbackEvidence}
        self.assertFalse(evidence["rating"].applied)
        self.assertFalse(evidence["proximity"].applied)

    def test_promotes_only_best_guarded_new_venue_to_third_position(self) -> None:
        candidates = [self._candidate(index, contextualPopularity=1 - index / 10) for index in range(1, 6)]
        candidates[3]["fallback"] = {**candidates[3]["fallback"], "isNewVenue": True, "novelty": 0.8}
        candidates[4]["fallback"] = {**candidates[4]["fallback"], "isNewVenue": True, "novelty": 0.9}
        result = self.ranker.rank(self._request(candidates))
        self.assertEqual(UUID(int=5), result.items[2].venueId)
        self.assertEqual(5, len(result.items[2].fallbackEvidence))


if __name__ == "__main__":
    unittest.main()
