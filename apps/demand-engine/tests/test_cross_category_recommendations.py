"""Pruebas de compatibilidad, filtros, diversidad y fallback entre categorías."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.cross_category_recommendations import (
    CrossCategoryPolicy, CrossCategoryRecommender, CrossCategoryRequest,
)


POLICY = Path(__file__).resolve().parents[1] / "policies/cross-category-recommendation.v1.json"


class CrossCategoryRecommendationTests(unittest.TestCase):
    """Demuestra que relevancia nunca omite compatibilidad ni diversidad obligatorias."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 21, 12, 0, tzinfo=UTC)
        self.recommender = CrossCategoryRecommender(CrossCategoryPolicy.load(POLICY))

    def _candidate(self, category: str, **changes: object) -> dict[str, object]:
        candidate: dict[str, object] = {
            "candidateId": str(uuid4()), "venueId": str(uuid4()), "serviceId": str(uuid4()),
            "categoryCode": category, "contentAffinity": 0.8,
            "conversionProbability": 0.7, "quality": 0.8, "isNewVenue": False,
            "constraints": {
                "venuePublished": True, "serviceBookable": True, "eligibilityAllowed": True,
                "permissionAllowed": True, "filtersMatched": True, "frequencyAllowed": True,
                "availableCapacity": 1, "requestedCapacity": 1,
                "validUntil": (self.now + timedelta(minutes=5)).isoformat(),
            },
        }
        for key, value in changes.items():
            if key.startswith("constraint_"):
                candidate["constraints"][key.removeprefix("constraint_")] = value
            else:
                candidate[key] = value
        return candidate

    def _request(self, candidates, **changes) -> CrossCategoryRequest:
        body = {
            "requestId": str(uuid4()), "schemaVersion": 1,
            "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "cross-category-recommendation-v1",
            "intentCode": "active-day", "intentSource": "explicitFilter",
            "sourceCategoryCode": "centro-deportivo", "estimatesReliable": True,
            "requestedMaximum": 4, "candidates": candidates,
            "persistentPersonalizationUsed": False, "sensitiveFeaturesUsed": False,
        }
        body.update(changes)
        return CrossCategoryRequest.model_validate(body)

    def test_ranked_result_excludes_source_and_enforces_category_diversity(self) -> None:
        candidates = [self._candidate("pista-de-padel") for _ in range(4)] + [
            self._candidate("campo-de-futbol"), self._candidate("restaurante")
        ] + [self._candidate("centro-deportivo", contentAffinity=1.0)]
        result = self.recommender.recommend(self._request(candidates))
        self.assertEqual("ranked", result.status)
        self.assertGreaterEqual(result.distinctCategoryCount, 2)
        self.assertLessEqual(max(list(item.categoryCode for item in result.items).count(code)
                                 for code in {item.categoryCode for item in result.items}), 2)
        self.assertNotIn("centro-deportivo", {item.categoryCode for item in result.items})
        self.assertEqual(1, result.exclusionCounts["sourceCategoryExcluded"])
        self.assertTrue(all(len(item.contributions) == 5 for item in result.items))

    def test_incompatible_and_hard_constraint_candidates_never_reenter(self) -> None:
        candidates = [self._candidate("centro-de-estetica", contentAffinity=1.0),
                      self._candidate("pista-de-padel", constraint_serviceBookable=False)]
        result = self.recommender.recommend(self._request(candidates))
        self.assertEqual("empty", result.status)
        self.assertEqual(1, result.exclusionCounts["intentIncompatible"])
        self.assertEqual(1, result.exclusionCounts["hardConstraint"])

    def test_unreliable_estimates_use_explainable_round_robin_fallback(self) -> None:
        candidates = [self._candidate("pista-de-padel") for _ in range(3)] + [
            self._candidate("campo-de-futbol"), self._candidate("restaurante")]
        first = self.recommender.recommend(self._request(candidates, estimatesReliable=False))
        second = self.recommender.recommend(self._request(candidates, estimatesReliable=False))
        self.assertEqual("deterministicFallback", first.status)
        self.assertEqual([item.candidateId for item in first.items],
                         [item.candidateId for item in second.items])
        self.assertGreaterEqual(first.distinctCategoryCount, 2)
        self.assertTrue(all(item.score is None and not item.contributions for item in first.items))

    def test_new_venue_receives_bounded_exposure_when_enough_results_exist(self) -> None:
        candidates = [self._candidate("pista-de-padel", quality=1.0),
                      self._candidate("pista-de-padel", quality=0.9),
                      self._candidate("campo-de-futbol", quality=0.9),
                      self._candidate("restaurante", quality=0.1, isNewVenue=True)]
        result = self.recommender.recommend(self._request(candidates, requestedMaximum=3))
        self.assertTrue(any(item.isNewVenue for item in result.items))

    def test_unknown_intent_and_policy_drift_are_rejected(self) -> None:
        candidate = self._candidate("pista-de-padel")
        with self.assertRaisesRegex(ValueError, "INTENT_UNKNOWN"):
            self.recommender.recommend(self._request([candidate], intentCode="private-inference"))
        with self.assertRaisesRegex(ValueError, "POLICY_VERSION"):
            self.recommender.recommend(self._request([candidate], policyVersion="other-v1"))

    def test_contract_forbids_persistent_or_sensitive_personalization(self) -> None:
        candidate = self._candidate("pista-de-padel")
        with self.assertRaises(ValueError):
            self._request([candidate], persistentPersonalizationUsed=True)
        with self.assertRaises(ValueError):
            self._request([candidate], sensitiveFeaturesUsed=True)


if __name__ == "__main__":
    unittest.main()
