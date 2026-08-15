"""Pruebas de necesidad de capacidad, gap y supresión irreversible de conteos pequeños."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.demand_aggregation import (
    DemandAggregationPolicy,
    DemandAggregationRequest,
    DemandCapacityCalculator,
)


ROOT = Path(__file__).resolve().parents[1]


class DemandCapacityCalculatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.calculator = DemandCapacityCalculator(
            DemandAggregationPolicy.load(ROOT / "policies" / "demand-aggregation.v1.json")
        )
        self.now = datetime(2026, 8, 15, 12, tzinfo=UTC)

    def _request(self, **overrides: object) -> DemandAggregationRequest:
        bucket: dict[str, object] = {
            "bucketId": str(uuid4()), "zoneCode": "ES-santiago-centro",
            "category": "peluqueria",
            "periodStart": (self.now - timedelta(hours=24)).isoformat(),
            "periodEnd": self.now.isoformat(), "eligibleSearchCount": 20,
            "distinctSessionCount": 15, "completedBookingCount": 5,
            "offeredCapacity": 40, "expectedOccupancy": 0.65,
            "occupancyReliable": True,
        }
        bucket.update(overrides)
        return DemandAggregationRequest.model_validate({
            "requestId": str(uuid4()), "schemaVersion": 1,
            "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "demand-aggregation-v1", "buckets": [bucket],
        })

    def test_publishes_capacity_need_and_unsatisfied_demand_for_private_bucket(self) -> None:
        result = self.calculator.calculate(self._request()).results[0]
        self.assertEqual("published", result.status)
        self.assertEqual(15, result.unsatisfiedDemand)
        self.assertEqual(0.75, result.unsatisfiedDemandRatio)
        self.assertEqual(0.35, result.capacityNeed)

    def test_suppresses_all_small_counts_but_can_keep_reliable_capacity_need(self) -> None:
        result = self.calculator.calculate(
            self._request(eligibleSearchCount=9, distinctSessionCount=8, completedBookingCount=1)
        ).results[0]
        self.assertEqual("partial", result.status)
        self.assertEqual(
            ["INSUFFICIENT_ELIGIBLE_SEARCHES", "INSUFFICIENT_DISTINCT_SESSIONS", "SMALL_NON_ZERO_BOOKING_COUNT"],
            result.suppressionReasons,
        )
        self.assertIsNone(result.eligibleSearchCount)
        self.assertIsNone(result.completedBookingCount)
        self.assertIsNone(result.unsatisfiedDemand)
        self.assertEqual(0.35, result.capacityNeed)

    def test_unreliable_occupancy_never_produces_capacity_need(self) -> None:
        result = self.calculator.calculate(
            self._request(expectedOccupancy=None, occupancyReliable=False)
        ).results[0]
        self.assertEqual("partial", result.status)
        self.assertIsNone(result.capacityNeed)
        self.assertEqual(15, result.unsatisfiedDemand)

    def test_rejects_exact_location_duplicate_subjects_and_invalid_period(self) -> None:
        with self.assertRaises(ValidationError):
            self._request(zoneCode="40.4168,-3.7038")
        with self.assertRaises(ValidationError):
            self._request(distinctSessionCount=21)


if __name__ == "__main__":
    unittest.main()
