"""Pruebas de coseno, confianza, vigencia y puerta vectorial."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from uuid import uuid4

from reserly_demand_engine.affinity import AffinityRequest, ContentAffinityCalculator


class ContentAffinityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.now = datetime(2026, 8, 14, 12, tzinfo=UTC)

    def _request(self, vectors: bool = False) -> AffinityRequest:
        data: dict[str, object] = {
            "requestId": str(uuid4()), "schemaVersion": 1, "occurredAt": self.now,
            "locale": "es", "policyVersion": "content-affinity-v1", "venueId": str(uuid4()),
            "preferences": [
                {"attributeCode": "onlineBooking", "value": 1.0, "confidence": 0.8},
                {"attributeCode": "modernStyle", "value": 0.5, "confidence": 0.6},
            ],
            "candidateAttributes": [
                {"attributeCode": "onlineBooking", "value": 0.9, "confidence": 0.75},
                {"attributeCode": "modernStyle", "value": 0.4, "confidence": 0.5},
                {"attributeCode": "expired", "value": 1.0, "confidence": 1.0,
                 "validUntil": self.now - timedelta(seconds=1)},
            ],
        }
        if vectors:
            left = [1.0] + [0.0] * 383
            right = [0.8, 0.6] + [0.0] * 382
            data["sessionVector"] = {"modelVersion": "e5-v1", "values": left}
            data["candidateVector"] = {"modelVersion": "e5-v1", "values": right}
        return AffinityRequest.model_validate(data)

    def test_attribute_affinity_uses_both_confidences_and_real_contributions(self) -> None:
        result = ContentAffinityCalculator().calculate(self._request())
        self.assertFalse(result.vectorApplied)
        self.assertEqual(0.0, result.vectorAffinity)
        self.assertEqual(2, result.matchedAttributeCount)
        self.assertEqual("onlineBooking", result.contributions[0].attributeCode)
        self.assertAlmostEqual(0.6, result.contributions[0].combinedConfidence)
        self.assertAlmostEqual(result.attributeAffinity, result.affinity)

    def test_cosine_blends_only_when_promoted(self) -> None:
        snapshot = self._request(vectors=True)
        closed = ContentAffinityCalculator(False).calculate(snapshot)
        enabled = ContentAffinityCalculator(True).calculate(snapshot)
        self.assertFalse(closed.vectorApplied)
        self.assertEqual(0.0, closed.vectorAffinity)
        self.assertTrue(enabled.vectorApplied)
        self.assertAlmostEqual(0.8, enabled.vectorAffinity)
        self.assertAlmostEqual(
            0.6 * enabled.vectorAffinity + 0.4 * enabled.attributeAffinity, enabled.affinity
        )

    def test_expired_and_unmatched_attributes_do_not_contribute(self) -> None:
        raw = self._request().model_dump()
        raw["preferences"] = [
            {"attributeCode": "expired", "value": 1.0, "confidence": 1.0}
        ]
        validated = AffinityRequest.model_validate(raw)
        result = ContentAffinityCalculator().calculate(validated)
        self.assertEqual(0, result.matchedAttributeCount)
        self.assertEqual(0.0, result.affinity)


if __name__ == "__main__":
    unittest.main()
