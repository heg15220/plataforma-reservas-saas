"""Pruebas de consentimiento, recencia e idempotencia del contexto de sesión."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.session_context import SessionContextBuilder, SessionContextRequest


class SessionContextTests(unittest.TestCase):
    def setUp(self) -> None:
        self.now = datetime(2026, 8, 14, 12, tzinfo=UTC)

    def _request(self, consent: bool, signals: list[dict[str, object]]) -> SessionContextRequest:
        return SessionContextRequest.model_validate(
            {
                "requestId": str(uuid4()), "schemaVersion": 1, "occurredAt": self.now,
                "locale": "es", "policyVersion": "session-context-v1",
                "sessionId": str(uuid4()), "personalizationConsent": consent,
                "consentVersion": "personalizationV1" if consent else None, "signals": signals,
            }
        )

    def _signal(self, kind: str, minutes: int = 0, current: bool = False) -> dict[str, object]:
        return {
            "signalId": str(uuid4()), "signalType": kind,
            "occurredAt": self.now - timedelta(minutes=minutes),
            "attributeCodes": ["onlineBooking"], "currentContext": current,
        }

    def test_without_consent_uses_only_current_filter_and_ignores_history(self) -> None:
        request = self._request(
            False,
            [self._signal("filter", current=True), self._signal("click"),
             self._signal("comparison"), self._signal("availability")],
        )
        result = SessionContextBuilder().build(request)
        self.assertFalse(result.personalizationApplied)
        self.assertIsNone(result.consentVersion)
        self.assertEqual(1, result.usedSignalCount)
        self.assertEqual(3, result.ignoredSignalCount)
        self.assertEqual(["filter"], result.attributePreferences[0].sourceCodes)

    def test_with_consent_aggregates_sources_and_decays_old_evidence(self) -> None:
        request = self._request(
            True,
            [self._signal("click", minutes=30), self._signal("comparison"),
             self._signal("availability")],
        )
        result = SessionContextBuilder().build(request)
        preference = result.attributePreferences[0]
        self.assertEqual(3, preference.evidenceCount)
        self.assertEqual(["availability", "click", "comparison"], preference.sourceCodes)
        self.assertGreater(preference.confidence, 0.5)
        self.assertLess(preference.value, 1.0)
        self.assertEqual(self.now + timedelta(minutes=15), result.validUntil)

    def test_rejects_duplicate_signal_future_event_and_consent_mismatch(self) -> None:
        duplicate = self._signal("click")
        with self.assertRaises(ValidationError):
            self._request(True, [duplicate, duplicate])
        future = self._signal("click")
        future["occurredAt"] = self.now + timedelta(minutes=1)
        with self.assertRaises(ValidationError):
            self._request(True, [future])
        raw = self._request(False, []).model_dump()
        raw["consentVersion"] = "personalizationV1"
        with self.assertRaises(ValidationError):
            SessionContextRequest.model_validate(raw)


if __name__ == "__main__":
    unittest.main()
