"""Aceptación transversal del ranking MVP: seguridad, calidad y presupuesto de latencia."""

from __future__ import annotations

import math
import time
import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import UUID

from pydantic import ValidationError

from reserly_demand_engine.explanations import ExplanationBuilder, ExplanationPolicy
from reserly_demand_engine.fallback import DeterministicFallback, FallbackPolicy
from reserly_demand_engine.scoring import ScoreMvp, ScoreMvpRequest, ScorePolicy


ROOT = Path(__file__).resolve().parents[1]
OCCURRED_AT = datetime(2026, 8, 20, 12, tzinfo=UTC)


class DemandMvpAcceptanceTests(unittest.TestCase):
    """Ejerce invariantes compartidas sin red, reloj real, modelo externo o datos productivos."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.scorer = ScoreMvp(
            ScorePolicy.load(ROOT / "policies/score-mvp.v1.json"),
            DeterministicFallback(FallbackPolicy.load(ROOT / "policies/fallback-mvp.v1.json")),
            ExplanationBuilder(
                ExplanationPolicy.load(ROOT / "policies/explanation-mvp.v1.json")
            ),
        )

    def test_relevance_and_replay_are_deterministic(self) -> None:
        """Una afinidad mayor gana con el resto constante y el replay es byte-equivalente."""
        request = self._request(
            [
                self._candidate(3, affinity=0.2),
                self._candidate(2, affinity=0.6),
                self._candidate(1, affinity=1.0),
            ]
        )

        first = self.scorer.rank(request)
        replay = self.scorer.rank(request)

        self.assertEqual(UUID(int=1), first.items[0].venueId)
        self.assertEqual([1, 2, 3], [item.position for item in first.items])
        self.assertEqual(first.model_dump(mode="json"), replay.model_dump(mode="json"))

    def test_every_hard_filter_runs_before_fallback_and_cannot_reintroduce_candidate(self) -> None:
        """Una alternativa con ocho fallos queda excluida incluso al degradar el modelo."""
        blocked = self._candidate(1)
        blocked["constraints"] = {
            "venuePublished": False,
            "serviceBookable": False,
            "eligibilityAllowed": False,
            "permissionAllowed": False,
            "filtersMatched": False,
            "frequencyAllowed": False,
            "availableCapacity": 0,
            "requestedCapacity": 1,
            "validUntil": "2026-08-20T11:59:59+00:00",
        }
        request = self._request([blocked]).model_copy(
            update={"fallbackReason": "model_timeout"}
        )

        result = self.scorer.rank(request)

        self.assertEqual("no_eligible_candidates", result.status)
        self.assertEqual([], result.items)
        self.assertFalse(result.fallbackApplied)
        self.assertEqual(
            [
                "CONSTRAINT_SNAPSHOT_EXPIRED",
                "VENUE_NOT_PUBLISHED",
                "SERVICE_NOT_BOOKABLE",
                "NOT_ELIGIBLE",
                "PERMISSION_DENIED",
                "FILTER_MISMATCH",
                "FREQUENCY_LIMIT_REACHED",
                "INSUFFICIENT_CAPACITY",
            ],
            result.excluded[0].reasonCodes,
        )

    def test_fallback_and_explanations_only_use_applied_permitted_evidence(self) -> None:
        """El fallback es estable, sin score ficticio ni explicación de señales no permitidas."""
        denied = self._candidate(2, affinity=1.0)
        denied["explanationPermissions"] = {
            "personalization": False,
            "availability": True,
            "location": False,
            "popularity": False,
            "rating": False,
            "novelty": False,
        }
        request = self._request([denied, self._candidate(1, affinity=0.1)]).model_copy(
            update={"fallbackReason": "dependency_unavailable"}
        )

        first = self.scorer.rank(request)
        replay = self.scorer.rank(request)

        self.assertEqual(first.model_dump(mode="json"), replay.model_dump(mode="json"))
        self.assertTrue(first.fallbackApplied)
        self.assertTrue(all(item.score is None for item in first.items))
        denied_result = next(item for item in first.items if item.venueId == UUID(int=2))
        self.assertEqual(["GOOD_AVAILABILITY"], [item.code for item in denied_result.explanations])
        self.assertTrue(all(item.sourceContribution is None for item in first.items[0].explanations))

    def test_contract_isolation_rejects_identity_and_output_contains_no_personal_fields(self) -> None:
        """El límite interno falla cerrado ante identidad y la respuesta queda minimizada."""
        payload = self._request([self._candidate(1)]).model_dump(mode="json")
        payload["email"] = "persona@example.test"
        with self.assertRaises(ValidationError):
            ScoreMvpRequest.model_validate(payload)

        response = self.scorer.rank(self._request([self._candidate(1)])).model_dump_json()
        self.assertNotIn("email", response.lower())
        self.assertNotIn("customer", response.lower())
        self.assertNotIn("session", response.lower())

    def test_one_hundred_candidates_stay_within_the_online_p95_budget(self) -> None:
        """El máximo contractual se ordena repetidamente por debajo del gate local de 150 ms."""
        request = self._request(
            [self._candidate(index, affinity=(index % 10) / 10) for index in range(1, 101)]
        )
        self.scorer.rank(request)
        elapsed_ms: list[float] = []
        for _ in range(20):
            started = time.perf_counter()
            result = self.scorer.rank(request)
            elapsed_ms.append((time.perf_counter() - started) * 1_000)
            self.assertEqual(100, len(result.items))
        ordered = sorted(elapsed_ms)
        p95 = ordered[min(len(ordered) - 1, math.ceil(len(ordered) * 0.95) - 1)]
        self.assertLessEqual(p95, 150.0, f"ranking local p95={p95:.3f} ms")

    def _request(self, candidates: list[dict[str, object]]) -> ScoreMvpRequest:
        return ScoreMvpRequest.model_validate(
            {
                "requestId": "00000000-0000-0000-0000-000000009999",
                "schemaVersion": 1,
                "occurredAt": OCCURRED_AT,
                "locale": "es",
                "policyVersion": "score-mvp-v1",
                "candidates": candidates,
            }
        )

    def _candidate(self, suffix: int, affinity: float = 0.5) -> dict[str, object]:
        return {
            "venueId": str(UUID(int=suffix)),
            "constraints": {
                "venuePublished": True,
                "serviceBookable": True,
                "eligibilityAllowed": True,
                "permissionAllowed": True,
                "filtersMatched": True,
                "frequencyAllowed": True,
                "availableCapacity": 2,
                "requestedCapacity": 1,
                "validUntil": "2026-08-20T12:05:00+00:00",
            },
            "affinity": affinity,
            "conversion": 0.5,
            "proximity": 0.5,
            "availability": 0.8,
            "capacityNeed": 0.4,
            "quality": 0.8,
            "exploration": 0.0,
            "fallback": {
                "contextualPopularity": 0.5,
                "popularitySampleCount": 20,
                "rating": 0.8,
                "ratingSampleCount": 10,
                "proximity": 0.5,
                "locationPermissionGranted": True,
                "availability": 0.8,
                "novelty": 0.0,
                "isNewVenue": False,
            },
            "explanationPermissions": {
                "personalization": True,
                "availability": True,
                "location": True,
                "popularity": True,
                "rating": True,
                "novelty": True,
            },
        }


if __name__ == "__main__":
    unittest.main()
