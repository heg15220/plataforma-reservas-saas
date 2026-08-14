"""Pruebas de configuración, contribuciones y orden estable de ScoreMvp."""

from __future__ import annotations

import json
import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import UUID, uuid4

from pydantic import ValidationError

from reserly_demand_engine.scoring import ScoreMvp, ScoreMvpRequest, ScorePolicy
from reserly_demand_engine.fallback import DeterministicFallback, FallbackPolicy
from reserly_demand_engine.explanations import ExplanationBuilder, ExplanationPolicy


ROOT = Path(__file__).resolve().parents[1]


class ScoreMvpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = ScorePolicy.load(ROOT / "policies" / "score-mvp.v1.json")
        self.fallback = DeterministicFallback(
            FallbackPolicy.load(ROOT / "policies" / "fallback-mvp.v1.json")
        )
        self.scorer = ScoreMvp(
            self.policy,
            self.fallback,
            ExplanationBuilder(ExplanationPolicy.load(ROOT / "policies" / "explanation-mvp.v1.json")),
        )

    def _candidate(self, venue_id: UUID, affinity: float) -> dict[str, object]:
        return {
            "venueId": str(venue_id),
            "constraints": self._constraints(),
            "affinity": affinity, "conversion": 0.5, "proximity": 0.5,
            "availability": 0.8, "capacityNeed": 0.4, "quality": 0.6,
            "exploration": 1.0,
            "fallback": self._fallback(),
            "explanationPermissions": {
                "personalization": True, "availability": True, "location": True,
                "popularity": True, "rating": True, "novelty": True,
            },
        }

    def _fallback(self, **overrides: object) -> dict[str, object]:
        values: dict[str, object] = {
            "contextualPopularity": 0.5, "popularitySampleCount": 20,
            "rating": 0.8, "ratingSampleCount": 10,
            "proximity": 0.5, "locationPermissionGranted": True,
            "availability": 0.8, "novelty": 0.0, "isNewVenue": False,
        }
        values.update(overrides)
        return values

    def _constraints(self, **overrides: object) -> dict[str, object]:
        values: dict[str, object] = {
            "venuePublished": True, "serviceBookable": True,
            "eligibilityAllowed": True, "permissionAllowed": True,
            "filtersMatched": True, "frequencyAllowed": True,
            "availableCapacity": 2, "requestedCapacity": 1,
            "validUntil": datetime(2026, 8, 14, 12, 5, tzinfo=UTC).isoformat(),
        }
        values.update(overrides)
        return values

    def _request(self, candidates: list[dict[str, object]]) -> ScoreMvpRequest:
        return ScoreMvpRequest.model_validate(
            {
                "requestId": str(uuid4()), "schemaVersion": 1,
                "occurredAt": datetime(2026, 8, 14, 12, tzinfo=UTC), "locale": "es",
                "policyVersion": "score-mvp-v1", "candidates": candidates,
            }
        )

    def test_policy_is_complete_normalized_and_caps_exploration(self) -> None:
        self.assertAlmostEqual(1.0, sum(self.policy.weights.as_dict().values()))
        self.assertEqual(0.05, self.policy.maximumExplorationContribution)
        raw = json.loads((ROOT / "policies" / "score-mvp.v1.json").read_text("utf-8"))
        raw["weights"]["affinity"] = 0.31
        with self.assertRaises(ValidationError):
            ScorePolicy.model_validate(raw)

    def test_ranks_by_weighted_score_and_returns_exact_contributions(self) -> None:
        low, high = uuid4(), uuid4()
        result = self.scorer.rank(
            self._request([self._candidate(low, 0.1), self._candidate(high, 0.9)])
        )
        self.assertEqual([high, low], [item.venueId for item in result.items])
        self.assertEqual([1, 2], [item.position for item in result.items])
        winner = result.items[0]
        self.assertAlmostEqual(0.27, winner.contributions[0].contribution)
        self.assertAlmostEqual(sum(item.contribution for item in winner.contributions), winner.score)
        self.assertEqual("weighted-baseline-v1", result.modelVersion)

    def test_tie_breaks_by_uuid_and_policy_mismatch_fails_closed(self) -> None:
        first = UUID("00000000-0000-0000-0000-000000000001")
        second = UUID("00000000-0000-0000-0000-000000000002")
        request = self._request([self._candidate(second, 0.5), self._candidate(first, 0.5)])
        self.assertEqual(first, self.scorer.rank(request).items[0].venueId)
        with self.assertRaisesRegex(ValueError, "SCORE_POLICY_VERSION_MISMATCH"):
            self.scorer.rank(request.model_copy(update={"policyVersion": "wrong-v1"}))

    def test_excludes_hard_constraint_failures_before_scoring(self) -> None:
        accepted, rejected = uuid4(), uuid4()
        blocked = self._candidate(rejected, 1.0)
        blocked["constraints"] = self._constraints(
            permissionAllowed=False, filtersMatched=False,
            availableCapacity=0, frequencyAllowed=False,
        )
        result = self.scorer.rank(
            self._request([blocked, self._candidate(accepted, 0.1)])
        )
        self.assertEqual([accepted], [item.venueId for item in result.items])
        self.assertEqual(1, result.eligibleCount)
        self.assertEqual(
            ["PERMISSION_DENIED", "FILTER_MISMATCH", "FREQUENCY_LIMIT_REACHED", "INSUFFICIENT_CAPACITY"],
            result.excluded[0].reasonCodes,
        )

    def test_expired_snapshot_cannot_be_ranked_or_reused_as_fallback(self) -> None:
        candidate = self._candidate(uuid4(), 1.0)
        candidate["constraints"] = self._constraints(
            validUntil=datetime(2026, 8, 14, 11, 59, tzinfo=UTC).isoformat()
        )
        result = self.scorer.rank(self._request([candidate]))
        self.assertEqual("no_eligible_candidates", result.status)
        self.assertTrue(result.fallbackRequired)
        self.assertEqual([], result.items)
        self.assertEqual(["CONSTRAINT_SNAPSHOT_EXPIRED"], result.excluded[0].reasonCodes)


if __name__ == "__main__":
    unittest.main()
