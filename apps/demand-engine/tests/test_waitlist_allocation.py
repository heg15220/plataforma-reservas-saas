"""Pruebas de prioridad, escalonado, consentimiento e idempotencia de listas de espera."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.waitlist_allocation import (
    WaitlistAllocationPolicy,
    WaitlistAllocationRequest,
    WaitlistAllocator,
)


POLICY_PATH = (
    Path(__file__).resolve().parents[1] / "policies" / "waitlist-allocation.v1.json"
)


class WaitlistAllocationTests(unittest.TestCase):
    """Protege invariantes operativas antes de que Spring materialice una propuesta."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 20, 10, 0, tzinfo=UTC)
        self.venue_id = uuid4()
        self.slot_id = uuid4()
        self.allocator = WaitlistAllocator(WaitlistAllocationPolicy.load(POLICY_PATH))

    def _candidate(
        self,
        *,
        created_minutes: int,
        value: int = 10_000,
        capacity: int = 1,
        requested: int = 1,
        consent: bool = True,
        contacts: int = 0,
        reliable: bool = True,
        subject_id: UUID | None = None,
    ) -> dict[str, object]:
        return {
            "entryId": str(uuid4()),
            "contactSubjectId": str(subject_id or uuid4()),
            "venueId": str(self.venue_id),
            "timeSlotId": str(self.slot_id),
            "createdAt": (self.now + timedelta(minutes=created_minutes)).isoformat(),
            "contactConsent": consent,
            "contactsInWindow": contacts,
            "acceptanceProbability": 0.8,
            "attendanceProbability": 0.9,
            "allowedBookingValueCents": value,
            "estimatesReliable": reliable,
            "constraints": {
                "venuePublished": True,
                "serviceBookable": True,
                "eligibilityAllowed": True,
                "permissionAllowed": True,
                "filtersMatched": True,
                "frequencyAllowed": True,
                "availableCapacity": capacity,
                "requestedCapacity": requested,
                "validUntil": (self.now + timedelta(hours=1)).isoformat(),
            },
        }

    def _request(
        self, candidates: list[dict[str, object]], reliable: bool = True
    ) -> WaitlistAllocationRequest:
        return WaitlistAllocationRequest.model_validate(
            {
                "requestId": "df928db0-ee51-4076-9b04-62dd8b1bd988",
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": "waitlist-allocation-v1",
                "estimatesReliable": reliable,
                "candidates": candidates,
            }
        )

    def test_reliable_priority_is_staggered_without_exceeding_capacity(self) -> None:
        low = self._candidate(created_minutes=0, value=1_000)
        high = self._candidate(created_minutes=1, value=20_000)
        result = self.allocator.allocate(self._request([low, high]))
        self.assertEqual("ranked", result.status)
        self.assertEqual(2, result.waveCount)
        self.assertEqual(UUID(high["entryId"]), result.offers[0].entryId)
        self.assertEqual(1, result.offers[0].waveNumber)
        self.assertEqual(2, result.offers[1].waveNumber)
        self.assertEqual(self.now + timedelta(minutes=10), result.offers[1].availableAt)
        self.assertEqual(timedelta(minutes=10), result.offers[0].expiresAt - self.now)

    def test_unreliable_estimates_use_fifo(self) -> None:
        first = self._candidate(created_minutes=0, value=1_000)
        second = self._candidate(created_minutes=1, value=20_000)
        result = self.allocator.allocate(self._request([second, first], reliable=False))
        self.assertEqual("deterministicFallback", result.status)
        self.assertTrue(result.fallbackRequired)
        self.assertEqual(UUID(first["entryId"]), result.offers[0].entryId)

    def test_consent_frequency_and_hard_constraints_are_excluded(self) -> None:
        no_consent = self._candidate(created_minutes=0, consent=False)
        contacted = self._candidate(created_minutes=1, contacts=3)
        invalid = self._candidate(created_minutes=2)
        invalid["constraints"]["serviceBookable"] = False
        result = self.allocator.allocate(self._request([no_consent, contacted, invalid]))
        self.assertEqual("empty", result.status)
        self.assertEqual(
            {"consentRequired": 1, "frequencyLimit": 1, "hardConstraint": 1},
            result.exclusionCounts,
        )

    def test_same_request_is_byte_equivalent_and_offer_ids_are_stable(self) -> None:
        request = self._request([self._candidate(created_minutes=0)])
        first = self.allocator.allocate(request).model_dump_json()
        second = self.allocator.allocate(request).model_dump_json()
        self.assertEqual(first, second)

    def test_same_subject_receives_only_oldest_offer(self) -> None:
        subject_id = uuid4()
        oldest = self._candidate(created_minutes=0, subject_id=subject_id)
        duplicate = self._candidate(created_minutes=1, subject_id=subject_id)
        result = self.allocator.allocate(self._request([duplicate, oldest]))
        self.assertEqual(1, result.offerCount)
        self.assertEqual(UUID(oldest["entryId"]), result.offers[0].entryId)
        self.assertEqual(1, result.exclusionCounts["duplicateContact"])


if __name__ == "__main__":
    unittest.main()
