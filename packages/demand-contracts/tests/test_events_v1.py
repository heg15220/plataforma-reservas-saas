"""Pruebas de catálogo, compatibilidad JSON/Pydantic y minimización del contrato v1."""

import json
import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_contracts.events_v1 import (
    EVENT_TYPES_V1,
    BehaviorEventV1,
    DiscoveryContextV1,
    EventPurpose,
)


ROOT = Path(__file__).parents[1]


def base_event() -> dict[str, object]:
    """Crea un evento mínimo válido sin identidad persistente."""
    return {
        "eventId": uuid4(),
        "schemaVersion": 1,
        "eventType": "searchPerformed",
        "occurredAt": datetime.now(UTC),
        "requestId": uuid4(),
        "purpose": EventPurpose.ANALYTICS,
        "sessionId": uuid4(),
        "context": DiscoveryContextV1(queryLength=12, resultCount=8),
    }


class EventContractsV1Tests(unittest.TestCase):
    """Contrato ejecutable del catálogo y el sobre v1."""

    def test_catalog_and_pydantic_expose_exactly_the_same_event_types(self) -> None:
        """Evita que JSON y Python evolucionen con catálogos divergentes."""
        catalog = json.loads((ROOT / "catalog/event-catalog.v1.json").read_text(encoding="utf-8"))
        catalog_types = tuple(event["type"] for event in catalog["events"])
        family_types = tuple(
            event_type
            for family in catalog["families"].values()
            for event_type in family["events"]
        )
        self.assertEqual(catalog_types, EVENT_TYPES_V1)
        self.assertEqual(set(family_types), set(EVENT_TYPES_V1))
        self.assertEqual(len(EVENT_TYPES_V1), len(set(EVENT_TYPES_V1)))
        self.assertEqual(len(EVENT_TYPES_V1), 22)

    def test_accepts_minimal_event_and_preserves_late_received_time(self) -> None:
        """La recepción puede ser posterior, pero nunca sustituye la ocurrencia."""
        payload = base_event()
        payload["receivedAt"] = payload["occurredAt"] + timedelta(minutes=10)
        event = BehaviorEventV1.model_validate(payload)
        self.assertEqual(event.schemaVersion, 1)
        self.assertGreater(event.receivedAt, event.occurredAt)

    def test_rejects_unknown_fields_pii_wrong_context_and_unconsented_identity(self) -> None:
        """Las extensiones ad hoc y la identidad sin consentimiento fallan cerradas."""
        payload = base_event()
        payload["email"] = "forbidden@example.invalid"
        with self.assertRaises(ValidationError):
            BehaviorEventV1.model_validate(payload)

        payload = base_event()
        payload["anonymousId"] = uuid4()
        with self.assertRaisesRegex(ValidationError, "consentVersion"):
            BehaviorEventV1.model_validate(payload)

        payload = base_event()
        payload["eventType"] = "bookingCompleted"
        with self.assertRaisesRegex(ValidationError, "ConversionContextV1"):
            BehaviorEventV1.model_validate(payload)

    def test_checked_in_json_schema_matches_pydantic_envelope_contract(self) -> None:
        """Audita campos requeridos, versión y enum sin generador externo."""
        checked_in = json.loads(
            (ROOT / "schemas/behavior-event.v1.schema.json").read_text(encoding="utf-8")
        )
        generated = BehaviorEventV1.model_json_schema()
        self.assertEqual(set(checked_in["required"]), set(generated["required"]))
        self.assertEqual(checked_in["properties"]["schemaVersion"]["const"], 1)
        self.assertEqual(
            tuple(checked_in["properties"]["eventType"]["enum"]), EVENT_TYPES_V1
        )
        self.assertFalse(checked_in["additionalProperties"])
