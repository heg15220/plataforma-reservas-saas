"""Pruebas del perímetro HTTP interno v1 y sus límites de confianza."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime
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
        }
        self.assertTrue(expected <= set(document["paths"]))
        self.assertFalse(any(path.startswith("/api/") for path in document["paths"]))

        response = self.client.get(f"/internal/demand/v1/demand/{uuid4()}")
        self.assertEqual(401, response.status_code)
        self.assertEqual("SERVICE_AUTH_INVALID", response.json()["code"])

    def test_recommendation_and_ranking_defer_without_mutating_candidates(self) -> None:
        for path in ("recommendations", "ranking"):
            body = self._envelope()
            body["candidates"] = [self._candidate(), self._candidate()]
            response = self.client.post(
                f"/internal/demand/v1/{path}", json=body, headers=HEADERS
            )
            self.assertEqual(200, response.status_code, response.text)
            self.assertEqual("deferred", response.json()["status"])
            self.assertTrue(response.json()["fallbackRequired"])
            self.assertEqual(2, response.json()["candidateCount"])
            self.assertNotIn("items", response.json())

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


if __name__ == "__main__":
    unittest.main()
