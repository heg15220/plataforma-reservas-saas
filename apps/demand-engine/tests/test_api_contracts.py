"""Pruebas del perímetro HTTP interno v1 y sus límites de confianza."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from uuid import UUID, uuid4

from fastapi.testclient import TestClient

from reserly_demand_engine.application import create_app
from reserly_demand_engine.config import DemandEngineSettings


TOKEN = "contract-test-service-token-at-least-32-characters"
HEADERS = {
    "X-Reserly-Service-Id": "spring-api",
    "X-Reserly-Service-Token": TOKEN,
}


class InternalApiContractTests(unittest.TestCase):
    """Protege namespace, auth, contratos estrictos y fallbacks bootstrap."""

    def setUp(self) -> None:
        settings = DemandEngineSettings(
            environment="test",
            service_id="spring-api",
            service_token=TOKEN,
            docs_enabled=True,
            request_timeout_seconds=1,
        )
        self.client = TestClient(create_app(settings), raise_server_exceptions=False)

    def _envelope(self) -> dict[str, object]:
        return {
            "requestId": str(uuid4()),
            "schemaVersion": 1,
            "occurredAt": datetime.now(UTC).isoformat(),
            "locale": "es",
            "policyVersion": "contract-v1",
        }

    def _candidate(self) -> dict[str, object]:
        return {
            "venueId": str(uuid4()),
            "distanceMeters": 1200,
            "availableCapacity": 1,
            "eligible": True,
            "attributeCodes": ["onlineBooking"],
        }

    def test_all_functional_paths_are_internal_and_authenticated(self) -> None:
        document = self.client.get("/internal/demand/openapi.json").json()
        expected = {
            "/internal/demand/v1/events",
            "/internal/demand/v1/recommendations",
            "/internal/demand/v1/ranking",
            "/internal/demand/v1/venues/{venue_id}/attributes",
            "/internal/demand/v1/conversion/predict",
            "/internal/demand/v1/demand/{venue_id}",
            "/internal/demand/v1/session/context",
            "/internal/demand/v1/affinity/evaluate",
            "/internal/demand/v1/occupancy/baseline",
            "/internal/demand/v1/demand/aggregate",
            "/internal/demand/v1/exploration/select",
            "/internal/demand/v1/exploration/update",
        }
        self.assertTrue(expected <= set(document["paths"]))
        self.assertFalse(any(path.startswith("/api/") for path in document["paths"]))

        response = self.client.get(f"/internal/demand/v1/demand/{uuid4()}")
        self.assertEqual(401, response.status_code)
        self.assertEqual("SERVICE_AUTH_INVALID", response.json()["code"])

    def test_recommendation_defers_without_mutating_candidates(self) -> None:
        body = self._envelope()
        body["candidates"] = [self._candidate(), self._candidate()]
        response = self.client.post(
            "/internal/demand/v1/recommendations", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertEqual("deferred", response.json()["status"])
        self.assertTrue(response.json()["fallbackRequired"])
        self.assertEqual(2, response.json()["candidateCount"])
        self.assertNotIn("items", response.json())

    def test_ranking_uses_versioned_score_and_preserves_candidate_set(self) -> None:
        body = self._envelope()
        body["policyVersion"] = "score-mvp-v1"
        first, second = uuid4(), uuid4()
        body["candidates"] = [
            self._score_candidate(first, 0.2), self._score_candidate(second, 0.9)
        ]
        response = self.client.post(
            "/internal/demand/v1/ranking", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertEqual("ranked", response.json()["status"])
        self.assertFalse(response.json()["fallbackRequired"])
        self.assertEqual(
            {str(first), str(second)}, {item["venueId"] for item in response.json()["items"]}
        )
        self.assertEqual(str(second), response.json()["items"][0]["venueId"])
        self.assertEqual(7, len(response.json()["items"][0]["contributions"]))
        self.assertEqual(2, len(response.json()["items"][0]["explanations"]))
        self.assertTrue(
            all(item["locale"] == "es" for item in response.json()["items"][0]["explanations"])
        )

        body["policyVersion"] = "unknown-v1"
        mismatch = self.client.post(
            "/internal/demand/v1/ranking", json=body, headers=HEADERS
        )
        self.assertEqual(409, mismatch.status_code)
        self.assertEqual("SCORE_POLICY_VERSION_MISMATCH", mismatch.json()["code"])

    def test_ranking_applies_deterministic_fallback_and_reports_actual_policy(self) -> None:
        body = self._envelope()
        body["policyVersion"] = "score-mvp-v1"
        body["fallbackReason"] = "dependency_unavailable"
        low, high = uuid4(), uuid4()
        low_candidate = self._score_candidate(low, 1.0)
        high_candidate = self._score_candidate(high, 0.0)
        low_candidate["fallback"]["contextualPopularity"] = 0.1
        high_candidate["fallback"]["contextualPopularity"] = 0.9
        body["candidates"] = [low_candidate, high_candidate]
        response = self.client.post(
            "/internal/demand/v1/ranking", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        payload = response.json()
        self.assertEqual("fallback_ranked", payload["status"])
        self.assertEqual("fallback-mvp-v1", payload["policyVersion"])
        self.assertEqual("deterministic-rules-v1", payload["modelVersion"])
        self.assertEqual(str(high), payload["items"][0]["venueId"])
        self.assertIsNone(payload["items"][0]["score"])
        self.assertEqual(5, len(payload["items"][0]["fallbackEvidence"]))
        self.assertEqual(
            ["POPULAR_IN_CONTEXT", "GOOD_AVAILABILITY"],
            [item["code"] for item in payload["items"][0]["explanations"]],
        )

    def _score_candidate(self, venue_id: UUID, affinity: float) -> dict[str, object]:
        return {
            "venueId": str(venue_id),
            "constraints": {
                "venuePublished": True, "serviceBookable": True,
                "eligibilityAllowed": True, "permissionAllowed": True,
                "filtersMatched": True, "frequencyAllowed": True,
                "availableCapacity": 1, "requestedCapacity": 1,
                "validUntil": (datetime.now(UTC) + timedelta(minutes=5)).isoformat(),
            },
            "affinity": affinity, "conversion": 0.5, "proximity": 0.5,
            "availability": 0.8, "capacityNeed": 0.4, "quality": 0.6,
            "exploration": 0.0,
            "fallback": {
                "contextualPopularity": 0.5, "popularitySampleCount": 20,
                "rating": 0.8, "ratingSampleCount": 10,
                "proximity": 0.5, "locationPermissionGranted": True,
                "availability": 0.8, "novelty": 0.0, "isNewVenue": False,
            },
            "explanationPermissions": {
                "personalization": True, "availability": True, "location": True,
                "popularity": True, "rating": True, "novelty": True,
            },
        }

    def test_candidate_limit_and_unknown_fields_are_rejected_opaquely(self) -> None:
        too_many = self._envelope()
        too_many["candidates"] = [self._candidate() for _ in range(101)]
        response = self.client.post(
            "/internal/demand/v1/ranking", json=too_many, headers=HEADERS
        )
        self.assertEqual(422, response.status_code)
        self.assertEqual({"code", "requestId"}, set(response.json()))

        unknown = self._envelope()
        unknown["candidates"] = [self._candidate()]
        unknown["customerEmail"] = "not-allowed@example.test"
        response = self.client.post(
            "/internal/demand/v1/recommendations", json=unknown, headers=HEADERS
        )
        self.assertEqual(422, response.status_code)
        self.assertNotIn("customerEmail", response.text)

    def test_model_endpoints_report_unavailable_and_profile_is_not_fabricated(self) -> None:
        body = self._envelope()
        body.update(
            {
                "venueId": str(uuid4()),
                "distanceMeters": 500,
                "availableSlotCount": 3,
                "hourOfDay": 17,
                "dayOfWeek": 5,
            }
        )
        response = self.client.post(
            "/internal/demand/v1/conversion/predict", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertFalse(response.json()["available"])
        self.assertIsNone(response.json()["probability"])

        venue_id = uuid4()
        demand = self.client.get(
            f"/internal/demand/v1/demand/{venue_id}", headers=HEADERS
        )
        self.assertEqual(200, demand.status_code)
        self.assertEqual(str(venue_id), demand.json()["venueId"])
        self.assertFalse(demand.json()["available"])

        profile = self.client.get(
            f"/internal/demand/v1/venues/{venue_id}/attributes", headers=HEADERS
        )
        self.assertEqual(404, profile.status_code)
        self.assertEqual("VENUE_PROFILE_NOT_FOUND", profile.json()["code"])

    def test_events_only_validate_and_correlate_the_batch(self) -> None:
        body = self._envelope()
        event_id = uuid4()
        body["events"] = [
            {
                "eventId": str(event_id),
                "schemaVersion": 1,
                "eventType": "searchPerformed",
                "occurredAt": body["occurredAt"],
                "requestId": body["requestId"],
                "purpose": "analytics",
                "countryCode": "ES",
                "context": {"queryLength": 12, "resultCount": 4},
            }
        ]
        response = self.client.post(
            "/internal/demand/v1/events", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertEqual(1, response.json()["validatedCount"])
        self.assertEqual(0, response.json()["persistedCount"])

        body["events"][0]["requestId"] = str(UUID(int=0))
        response = self.client.post(
            "/internal/demand/v1/events", json=body, headers=HEADERS
        )
        self.assertEqual(422, response.status_code)

    def test_session_context_endpoint_does_not_personalize_without_consent(self) -> None:
        body = self._envelope()
        body.update(
            {
                "sessionId": str(uuid4()),
                "personalizationConsent": False,
                "signals": [
                    {
                        "signalId": str(uuid4()),
                        "signalType": "filter",
                        "occurredAt": body["occurredAt"],
                        "attributeCodes": ["onlineBooking"],
                        "currentContext": True,
                    },
                    {
                        "signalId": str(uuid4()),
                        "signalType": "click",
                        "occurredAt": body["occurredAt"],
                        "attributeCodes": ["modernStyle"],
                    },
                ],
            }
        )
        response = self.client.post(
            "/internal/demand/v1/session/context", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertFalse(response.json()["personalizationApplied"])
        self.assertEqual(1, response.json()["usedSignalCount"])
        self.assertEqual(1, response.json()["ignoredSignalCount"])
        self.assertEqual("onlineBooking", response.json()["attributePreferences"][0]["attributeCode"])

    def test_affinity_endpoint_exposes_real_attribute_contribution(self) -> None:
        body = self._envelope()
        body.update(
            {
                "venueId": str(uuid4()),
                "preferences": [
                    {"attributeCode": "onlineBooking", "value": 1.0, "confidence": 0.8}
                ],
                "candidateAttributes": [
                    {"attributeCode": "onlineBooking", "value": 0.75, "confidence": 0.5}
                ],
            }
        )
        response = self.client.post(
            "/internal/demand/v1/affinity/evaluate", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        self.assertEqual(0.75, response.json()["affinity"])
        self.assertFalse(response.json()["vectorApplied"])
        self.assertEqual(0.4, response.json()["contributions"][0]["combinedConfidence"])

    def test_occupancy_baseline_reports_timezone_uncertainty_and_reliability(self) -> None:
        body = self._envelope()
        body["policyVersion"] = "occupancy-baseline-v1"
        body.update({
            "venueId": str(uuid4()), "targetAt": body["occurredAt"],
            "timeZone": "Europe/Madrid", "observations": [],
        })
        current = datetime.fromisoformat(str(body["occurredAt"]))
        for index in range(1, 9):
            body["observations"].append({
                "observationId": str(uuid4()),
                "observedAt": (current - timedelta(weeks=index)).isoformat(),
                "occupiedCapacity": 7, "offeredCapacity": 10,
            })
        response = self.client.post(
            "/internal/demand/v1/occupancy/baseline", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        payload = response.json()
        self.assertEqual("hourly-ema-v1", payload["modelVersion"])
        self.assertEqual("Europe/Madrid", payload["timeZone"])
        self.assertTrue(payload["reliable"])
        self.assertLessEqual(payload["lowerBound"], payload["expectedOccupancy"])
        self.assertGreaterEqual(payload["upperBound"], payload["expectedOccupancy"])

    def test_demand_aggregation_suppresses_small_counts_over_http(self) -> None:
        body = self._envelope()
        body["policyVersion"] = "demand-aggregation-v1"
        current = datetime.fromisoformat(str(body["occurredAt"]))
        body["buckets"] = [{
            "bucketId": str(uuid4()), "zoneCode": "ES-madrid-centro",
            "category": "centro-de-estetica",
            "periodStart": (current - timedelta(hours=24)).isoformat(),
            "periodEnd": current.isoformat(), "eligibleSearchCount": 8,
            "distinctSessionCount": 7, "completedBookingCount": 1,
            "offeredCapacity": 20, "expectedOccupancy": None,
            "occupancyReliable": False,
        }]
        response = self.client.post(
            "/internal/demand/v1/demand/aggregate", json=body, headers=HEADERS
        )
        self.assertEqual(200, response.status_code, response.text)
        result = response.json()["results"][0]
        self.assertEqual("suppressed", result["status"])
        self.assertIsNone(result["eligibleSearchCount"])
        self.assertIsNone(result["completedBookingCount"])
        self.assertIsNone(result["unsatisfiedDemand"])

    def test_thompson_selection_and_update_are_bounded_and_idempotent(self) -> None:
        body = self._envelope()
        body["policyVersion"] = "thompson-basic-v1"
        body["requestedSlots"] = 10
        candidates = []
        for _ in range(10):
            venue_id = uuid4()
            candidates.append({
                "venueId": str(venue_id), "quality": 0.8,
                "explorationAllowed": True,
                "constraints": {
                    "venuePublished": True, "serviceBookable": True,
                    "eligibilityAllowed": True, "permissionAllowed": True,
                    "filtersMatched": True, "frequencyAllowed": True,
                    "availableCapacity": 1, "requestedCapacity": 1,
                    "validUntil": (datetime.now(UTC) + timedelta(minutes=5)).isoformat(),
                },
                "posterior": {
                    "venueId": str(venue_id), "alpha": 1, "beta": 1,
                    "posteriorVersion": 0, "appliedOutcomeIds": [],
                },
            })
        body["candidates"] = candidates
        selection = self.client.post(
            "/internal/demand/v1/exploration/select", json=body, headers=HEADERS
        )
        self.assertEqual(200, selection.status_code, selection.text)
        self.assertEqual(1, selection.json()["maximumExplorationSlots"])
        self.assertEqual(1, len(selection.json()["selections"]))

        outcome_id = uuid4()
        update_body = self._envelope()
        update_body.update({
            "policyVersion": "thompson-basic-v1",
            "outcomeEventId": str(outcome_id), "reward": "success",
            "state": candidates[0]["posterior"],
        })
        applied = self.client.post(
            "/internal/demand/v1/exploration/update", json=update_body, headers=HEADERS
        )
        self.assertEqual(200, applied.status_code, applied.text)
        self.assertTrue(applied.json()["applied"])
        update_body["state"] = applied.json()["state"]
        replay = self.client.post(
            "/internal/demand/v1/exploration/update", json=update_body, headers=HEADERS
        )
        self.assertEqual(200, replay.status_code, replay.text)
        self.assertFalse(replay.json()["applied"])
        self.assertEqual(applied.json()["state"], replay.json()["state"])


if __name__ == "__main__":
    unittest.main()
