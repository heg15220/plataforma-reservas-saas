"""Pruebas CP-SAT de capacidad, presupuesto, consentimiento, equidad y fallback."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.opportunity_optimization import (
    OpportunityOptimizationPolicy,
    OpportunityOptimizationRequest,
    OpportunityOptimizer,
)


ROOT = Path(__file__).resolve().parents[1]


class OpportunityOptimizationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = OpportunityOptimizationPolicy.load(ROOT / "policies" / "opportunity-optimization.v1.json")
        cls.optimizer = OpportunityOptimizer(cls.policy)
        cls.now = datetime(2026, 8, 20, 12, tzinfo=UTC)

    def _candidate(
        self,
        number: int,
        *,
        slot: int = 1,
        new: bool = False,
        value: int = 5_000,
        cost: int = 100,
        capacity: int = 2,
        requested: int = 1,
        consent: bool = True,
        distance: int = 1_000,
        reliable: bool = True,
    ) -> dict[str, object]:
        return {
            "opportunityId": str(UUID(int=number)),
            "contactSubjectId": str(UUID(int=number + 1_000)),
            "venueId": str(UUID(int=number + 2_000)),
            "timeSlotId": str(UUID(int=slot + 3_000)),
            "createdAt": (self.now + timedelta(seconds=number)).isoformat(),
            "exposureGroup": "newVenue" if new else "establishedVenue",
            "acceptanceProbability": 0.8,
            "attendanceProbability": 0.9,
            "allowedBookingValueCents": value,
            "contactCostCents": cost,
            "incentiveCostCents": 0,
            "projectedMarginCents": 2_000,
            "distanceMeters": distance,
            "maximumAcceptedDistanceMeters": 10_000,
            "contactsInWindow": 0,
            "maximumContactsInWindow": 2,
            "contactConsent": consent,
            "frequencyAllowed": True,
            "estimatesReliable": reliable,
            "upliftReliable": False,
            "constraints": {
                "venuePublished": True,
                "serviceBookable": True,
                "eligibilityAllowed": True,
                "permissionAllowed": True,
                "filtersMatched": True,
                "frequencyAllowed": True,
                "availableCapacity": capacity,
                "requestedCapacity": requested,
                "validUntil": (self.now + timedelta(minutes=5)).isoformat(),
            },
        }

    def _request(
        self,
        candidates: list[dict[str, object]],
        *,
        reliable: bool = True,
        budget: int = 1_000,
    ) -> OpportunityOptimizationRequest:
        return OpportunityOptimizationRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": self.policy.policyVersion,
                "budgetCents": budget,
                "estimatesReliable": reliable,
                "candidates": candidates,
            }
        )

    def test_cp_sat_maximizes_value_with_capacity_budget_and_fairness(self) -> None:
        candidates = [
            self._candidate(1, value=10_000),
            self._candidate(2, value=9_000),
            self._candidate(3, slot=2, new=True, value=2_000),
            self._candidate(4, slot=3, value=1_000),
        ]
        response = self.optimizer.optimize(self._request(candidates, budget=300))
        self.assertEqual("optimal", response.status)
        self.assertLessEqual(response.totalCostCents, 300)
        self.assertEqual(3, response.selectedCount)
        self.assertGreaterEqual(response.newVenueShare, 0.2)
        used_slot_one = sum(item.requestedCapacity for item in response.selections if item.timeSlotId == UUID(int=3001))
        self.assertLessEqual(used_slot_one, 2)
        self.assertFalse(response.automaticExecutionAllowed)

    def test_all_operational_constraints_filter_before_solver(self) -> None:
        invalid = [
            self._candidate(1, consent=False),
            self._candidate(2, distance=20_000),
            self._candidate(3),
        ]
        invalid[2]["contactsInWindow"] = 2
        response = self.optimizer.optimize(self._request(invalid))
        self.assertEqual(0, response.selectedCount)
        self.assertEqual(1, response.exclusionCounts["consentRequired"])
        self.assertEqual(1, response.exclusionCounts["distanceLimit"])
        self.assertEqual(1, response.exclusionCounts["frequencyLimit"])

    def test_same_subject_and_slot_cannot_be_overallocated(self) -> None:
        first = self._candidate(1, capacity=1)
        second = self._candidate(2, capacity=1)
        second["contactSubjectId"] = first["contactSubjectId"]
        response = self.optimizer.optimize(self._request([first, second]))
        self.assertEqual(1, response.selectedCount)

    def test_unreliable_estimates_use_fifo_deterministically(self) -> None:
        request = self._request(
            [self._candidate(1, new=True), self._candidate(2, slot=2), self._candidate(3, slot=3)],
            reliable=False,
        )
        first = self.optimizer.optimize(request)
        second = self.optimizer.optimize(request)
        self.assertEqual(first, second)
        self.assertEqual("deterministicFallback", first.status)
        self.assertTrue(first.fallbackRequired)
        self.assertEqual(UUID(int=1), first.selections[0].opportunityId)

    def test_incentive_requires_reliable_uplift_and_margin_floor(self) -> None:
        candidates = [self._candidate(1), self._candidate(2)]
        candidates[0]["incentiveCostCents"] = 50
        candidates[1]["projectedMarginCents"] = 99
        response = self.optimizer.optimize(self._request(candidates))
        self.assertEqual(0, response.selectedCount)
        self.assertEqual(1, response.exclusionCounts["upliftRequired"])
        self.assertEqual(1, response.exclusionCounts["marginFloor"])


if __name__ == "__main__":
    unittest.main()
