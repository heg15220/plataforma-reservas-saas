"""Pruebas de jerarquía, decaimiento, contradicción y corrección del perfil implícito."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.implicit_profiles import (
    ImplicitProfileBuilder,
    ImplicitProfilePolicy,
    ImplicitProfileRequest,
)


POLICY = Path(__file__).parents[1] / "policies" / "implicit-profile.v1.json"


class ImplicitProfileTests(unittest.TestCase):
    """Valida que el perfil sea determinista, consentido, interpretable y corregible."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 20, 12, tzinfo=UTC)
        self.builder = ImplicitProfileBuilder(ImplicitProfilePolicy.load(POLICY))

    def _evidence(
        self,
        source: str,
        polarity: str = "positive",
        days: int = 0,
        attribute: str = "onlineBooking",
    ) -> dict[str, object]:
        return {
            "evidenceId": str(uuid4()),
            "attributeCode": attribute,
            "source": source,
            "polarity": polarity,
            "strength": 1,
            "confidence": 1,
            "occurredAt": self.now - timedelta(days=days),
        }

    def _request(
        self,
        evidence: list[dict[str, object]],
        corrections: list[dict[str, object]] | None = None,
    ) -> ImplicitProfileRequest:
        return ImplicitProfileRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now,
                "locale": "es",
                "policyVersion": "implicit-profile-v1",
                "customerIdentityId": str(uuid4()),
                "personalizationConsent": True,
                "consentVersion": "personalization-v1",
                "evidence": evidence,
                "corrections": corrections or [],
            }
        )

    def test_recent_attendance_outweighs_old_negative_click(self) -> None:
        result = self.builder.build(
            self._request(
                [
                    self._evidence("click", "negative", days=90),
                    self._evidence("attendance", "positive"),
                ]
            )
        )
        preference = result.preferences[0]
        self.assertGreater(preference.value, 0.95)
        self.assertEqual(["attendance", "click"], preference.sourceCodes)
        self.assertEqual(2, preference.evidenceCount)

    def test_contradiction_reduces_confidence(self) -> None:
        agreeing = self.builder.build(
            self._request([self._evidence("booking"), self._evidence("attendance")])
        )
        contradictory = self.builder.build(
            self._request(
                [self._evidence("booking"), self._evidence("attendance", "negative")]
            )
        )
        self.assertGreater(
            agreeing.preferences[0].confidence, contradictory.preferences[0].confidence
        )

    def test_correction_dominates_without_erasing_evidence(self) -> None:
        correction_id = uuid4()
        corrected_at = self.now - timedelta(hours=1)
        request = self._request(
            [self._evidence("attendance")],
            [
                {
                    "correctionId": str(correction_id),
                    "attributeCode": "onlineBooking",
                    "correctedValue": 0,
                    "correctedAt": corrected_at,
                }
            ],
        )
        result = self.builder.build(request)
        preference = result.preferences[0]
        self.assertEqual(0, preference.value)
        self.assertEqual(1, preference.confidence)
        self.assertEqual(1, preference.evidenceCount)
        self.assertEqual(["attendance"], preference.sourceCodes)
        self.assertEqual(correction_id, preference.correctionId)

    def test_old_evidence_is_ignored_and_replay_is_deterministic(self) -> None:
        request = self._request(
            [self._evidence("review"), self._evidence("click", days=366)]
        )
        first = self.builder.build(request)
        second = self.builder.build(request)
        self.assertEqual(first, second)
        self.assertEqual(1, first.usedEvidenceCount)
        self.assertEqual(1, first.ignoredEvidenceCount)

    def test_requires_consent_and_rejects_direct_identifiers(self) -> None:
        raw = self._request([self._evidence("filter")]).model_dump()
        raw["personalizationConsent"] = False
        with self.assertRaises(ValidationError):
            ImplicitProfileRequest.model_validate(raw)
        raw = self._request([self._evidence("filter")]).model_dump()
        raw["email"] = "persona@example.test"
        with self.assertRaises(ValidationError):
            ImplicitProfileRequest.model_validate(raw)


if __name__ == "__main__":
    unittest.main()
