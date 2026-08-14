"""Pruebas de reglas, gobierno y acceso HTTP del perfil inicial de local."""

from __future__ import annotations

import json
import unittest
from datetime import UTC, datetime
from pathlib import Path
from uuid import uuid4

from fastapi.testclient import TestClient
from pydantic import ValidationError

from reserly_demand_engine.application import create_app
from reserly_demand_engine.config import DemandEngineSettings
from reserly_demand_engine.profiles import VenueProfileBuilder, VenueProfileRequest


TOKEN = "profile-test-service-token-with-at-least-32-characters"
HEADERS = {
    "X-Reserly-Service-Id": "spring-api",
    "X-Reserly-Service-Token": TOKEN,
}


class VenueProfileTests(unittest.TestCase):
    """Asegura clasificación determinista, interpretable, minimizada y limitada al piloto."""

    def _payload(self) -> dict[str, object]:
        return {
            "requestId": str(uuid4()),
            "schemaVersion": 1,
            "occurredAt": datetime.now(UTC).isoformat(),
            "locale": "es",
            "policyVersion": "venue-profile-rules-v1",
            "venueId": str(uuid4()),
            "verticalCode": "personalCareIndividualAppointment",
            "categoryCode": "peluqueria",
            "declaredAttributeCodes": ["stepFreeAccess", "lowNoiseAppointments"],
            "localizedText": {
                "es": "Espacio moderno con luz natural y atención bilingüe.",
                "en": "Modern salon with natural light and bilingual service.",
            },
            "services": [
                {
                    "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                    "nameEs": "Corte de cabello", "nameEn": "Hair cut", "durationMinutes": 45,
                },
                {
                    "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                    "nameEs": "Color y mechas", "durationMinutes": 120,
                },
                {
                    "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                    "nameEs": "Peinado", "durationMinutes": 60,
                },
            ],
            "operational": {
                "sampledAt": datetime.now(UTC).isoformat(),
                "appointmentSampleCount": 20,
                "exactTimeAppointmentRatio": 0.95,
                "sameDayAvailableSlots": 2,
                "eveningAvailableSlots": 3,
                "weekendAvailableSlots": 4,
                "lowDemandAvailableSlots": 1,
                "averageAppointmentMinutes": 75,
                "onlineBookingEnabled": True,
                "flexibleCancellationEnabled": True,
                "professionalSelectionEnabled": True,
            },
        }

    def test_sources_generate_versioned_interpretable_attributes(self) -> None:
        request = VenueProfileRequest.model_validate_json(json.dumps(self._payload()))
        profile = VenueProfileBuilder().build(request)
        by_code = {attribute.code: attribute for attribute in profile.attributes}
        expected = {
            "modernStyle", "naturalLight", "multilingualService", "stepFreeAccess",
            "lowNoiseAppointments", "hairServices", "hairCutService", "hairColorService",
            "hairStylingService", "transparentServiceInformation",
            "averageAppointmentDuration", "exactTimeAppointments", "sameDayAvailability",
            "eveningAvailability", "weekendAvailability", "lowDemandTimeAvailability",
            "onlineBooking", "flexibleCancellationPolicy", "professionalChoice",
        }
        self.assertTrue(expected <= set(by_code))
        self.assertEqual(sorted(by_code), [item.code for item in profile.attributes])
        self.assertIsNotNone(by_code["sameDayAvailability"].validUntil)
        self.assertIsNone(by_code["onlineBooking"].validUntil)
        self.assertEqual("venue-profile-rules-v1", profile.profileVersion)
        self.assertNotIn("calmAtmosphere", by_code)
        self.assertNotIn("privateExperience", by_code)
        serialized = profile.model_dump_json()
        self.assertNotIn("Espacio moderno", serialized)
        self.assertNotIn("Corte de cabello", serialized)
        self.assertNotIn("serviceId", serialized)

    def test_unsupported_vertical_and_non_unit_capacity_are_rejected(self) -> None:
        payload = self._payload()
        payload["verticalCode"] = "health"
        with self.assertRaises(ValidationError):
            VenueProfileRequest.model_validate_json(json.dumps(payload))

        payload = self._payload()
        payload["services"][0]["simultaneousCapacity"] = 2
        with self.assertRaises(ValidationError):
            VenueProfileRequest.model_validate_json(json.dumps(payload))

        payload = self._payload()
        payload["services"][0]["nameEs"] = "Tratamiento médico facial"
        with self.assertRaises(ValidationError):
            VenueProfileRequest.model_validate_json(json.dumps(payload))

    def test_beauty_services_are_classified_without_subjective_inference(self) -> None:
        payload = self._payload()
        payload["categoryCode"] = "centro-de-estetica"
        payload["services"] = [
            {
                "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                "nameEs": "Tratamiento facial", "durationMinutes": 50,
            },
            {
                "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                "nameEs": "Manicura", "durationMinutes": 40,
            },
            {
                "serviceId": str(uuid4()), "active": True, "simultaneousCapacity": 1,
                "nameEs": "Maquillaje", "durationMinutes": 60,
            },
        ]
        request = VenueProfileRequest.model_validate_json(json.dumps(payload))
        codes = {item.code for item in VenueProfileBuilder().build(request).attributes}
        self.assertTrue(
            {"skinCareServices", "facialTreatmentService", "nailService", "makeupService"}
            <= codes
        )
        self.assertFalse({"calmAtmosphere", "staffAttentiveness", "privateExperience"} & codes)

    def test_emitted_sources_and_validity_conform_to_governed_ontology(self) -> None:
        root = Path(__file__).resolve().parents[3]
        ontology = json.loads(
            (root / "packages/demand-contracts/ontology/personal-care.v1.json").read_text(
                encoding="utf-8"
            )
        )
        definitions = {item["code"]: item for item in ontology["attributes"]}
        request = VenueProfileRequest.model_validate_json(json.dumps(self._payload()))
        profile = VenueProfileBuilder().build(request)
        for attribute in profile.attributes:
            definition = definitions[attribute.code]
            self.assertTrue(set(attribute.sourceCodes) <= set(definition["allowedSources"]))
            ttl_days = definition["validity"]["ttlDays"]
            if ttl_days is None:
                self.assertIsNone(attribute.validUntil)
            else:
                self.assertIsNotNone(attribute.validUntil)
                delta = attribute.validUntil - profile.generatedAt
                self.assertEqual(ttl_days, delta.days)
    def test_internal_evaluation_is_readable_with_fresh_correlation(self) -> None:
        settings = DemandEngineSettings(
            environment="test", service_id="spring-api", service_token=TOKEN,
        )
        client = TestClient(create_app(settings), raise_server_exceptions=False)
        payload = self._payload()
        venue_id = payload["venueId"]
        evaluated = client.post(
            f"/internal/demand/v1/venues/{venue_id}/attributes/evaluate",
            json=payload, headers=HEADERS,
        )
        self.assertEqual(200, evaluated.status_code, evaluated.text)
        correlation = str(uuid4())
        read = client.get(
            f"/internal/demand/v1/venues/{venue_id}/attributes",
            headers=HEADERS | {"X-Reserly-Correlation-Id": correlation},
        )
        self.assertEqual(200, read.status_code, read.text)
        self.assertEqual(correlation, read.json()["requestId"])
        self.assertEqual(evaluated.json()["attributes"], read.json()["attributes"])
        mismatch = self._payload()
        response = client.post(
            f"/internal/demand/v1/venues/{venue_id}/attributes/evaluate",
            json=mismatch, headers=HEADERS,
        )
        self.assertEqual(409, response.status_code)
        self.assertEqual("VENUE_ID_MISMATCH", response.json()["code"])


if __name__ == "__main__":
    unittest.main()
