"""Pruebas de EMA, incertidumbre, zona IANA y fiabilidad del baseline horario."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.occupancy import (
    HourlyOccupancyBaseline,
    OccupancyBaselineRequest,
    OccupancyPolicy,
)


ROOT = Path(__file__).resolve().parents[1]


class HourlyOccupancyBaselineTests(unittest.TestCase):
    def setUp(self) -> None:
        self.baseline = HourlyOccupancyBaseline(
            OccupancyPolicy.load(ROOT / "policies" / "occupancy-baseline.v1.json")
        )
        self.now = datetime(2026, 8, 15, 12, tzinfo=UTC)

    def _request(self, ratios: list[float], *, zone: str = "Europe/Madrid") -> OccupancyBaselineRequest:
        observations = []
        for index, ratio in enumerate(ratios, 1):
            # Sábados a las 10:00 UTC equivalen a las 12:00 locales en agosto.
            observed = self.now - timedelta(weeks=index)
            observations.append({
                "observationId": str(uuid4()), "observedAt": observed.isoformat(),
                "occupiedCapacity": round(ratio * 100), "offeredCapacity": 100,
            })
        return OccupancyBaselineRequest.model_validate({
            "requestId": str(uuid4()), "schemaVersion": 1,
            "occurredAt": self.now.isoformat(), "locale": "es",
            "policyVersion": "occupancy-baseline-v1", "venueId": str(uuid4()),
            "targetAt": self.now.isoformat(), "timeZone": zone,
            "observations": observations,
        })

    def test_ema_uses_only_same_local_day_and_hour_and_is_order_independent(self) -> None:
        request = self._request([0.2, 0.4, 0.8, 1.0, 0.6, 0.5, 0.7, 0.9])
        result = self.baseline.calculate(request)
        shuffled = request.model_copy(update={"observations": list(reversed(request.observations))})
        again = self.baseline.calculate(shuffled)
        self.assertEqual(result.expectedOccupancy, again.expectedOccupancy)
        self.assertEqual(8, result.observationCount)
        self.assertTrue(result.reliable)
        self.assertEqual("reliable", result.status)
        self.assertEqual((6, 14), (result.localDayOfWeek, result.localHour))

    def test_insufficient_history_publishes_wide_interval_and_never_claims_reliability(self) -> None:
        result = self.baseline.calculate(self._request([0.9, 0.8]))
        self.assertFalse(result.reliable)
        self.assertEqual("insufficient_history", result.status)
        self.assertLessEqual(result.lowerBound, result.expectedOccupancy)
        self.assertGreaterEqual(result.upperBound, result.expectedOccupancy)
        self.assertGreater(result.uncertainty, 0)

    def test_invalid_zone_duplicate_observation_and_overcapacity_fail_closed(self) -> None:
        raw = self._request([0.5]).model_dump(mode="json")
        raw["timeZone"] = "Mars/Olympus"
        with self.assertRaises(ValidationError):
            OccupancyBaselineRequest.model_validate(raw)
        raw = self._request([0.5]).model_dump(mode="json")
        raw["observations"][0]["occupiedCapacity"] = 101
        with self.assertRaises(ValidationError):
            OccupancyBaselineRequest.model_validate(raw)


if __name__ == "__main__":
    unittest.main()
