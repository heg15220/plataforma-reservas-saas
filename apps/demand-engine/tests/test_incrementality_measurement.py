"""Pruebas de atribución, causalidad, recuperación, coste y retorno."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.incrementality_measurement import (
    IncrementalityMeasurementPolicy, IncrementalityMeasurementRequest,
    IncrementalityMeasurementService,
)


POLICY = Path(__file__).resolve().parents[1] / "policies/incrementality-measurement.v1.json"


class IncrementalityMeasurementTests(unittest.TestCase):
    """Acredita que las métricas causales desaparecen cuando falla cualquier control."""

    def setUp(self) -> None:
        self.end = datetime(2026, 8, 20, tzinfo=UTC)
        self.now = self.end + timedelta(days=4)
        self.service = IncrementalityMeasurementService(
            IncrementalityMeasurementPolicy.load(POLICY)
        )

    def _units(self, arm: str, count: int, bookings: int, recovered: int = 0):
        result = []
        for index in range(count):
            booked = index < bookings
            result.append({
                "unitId": str(uuid4()), "arm": arm,
                "assignedAt": (self.end - timedelta(days=10, minutes=2)).isoformat(),
                "exposedAt": (self.end - timedelta(days=10)).isoformat(),
                "bookingId": str(uuid4()) if booked else None,
                "bookingAt": (self.end - timedelta(days=9, hours=23)).isoformat() if booked else None,
                "attributionClass": (
                    "recovered" if booked and index < recovered else "generated" if booked else None
                ),
                "customerStatus": "new" if booked else None,
                "outcomeStatus": "attended" if booked else None,
                "outcomeObservedAt": (self.end - timedelta(days=7)).isoformat() if booked else None,
                "realizedNetRevenueCents": 5000 if booked else 0,
                "activationCostCents": 100 if arm == "treatment" else 0,
                "offPeakBooking": bool(booked and index % 2 == 0),
            })
        return result

    def _request(self, units, **changes):
        body = {
            "requestId": str(uuid4()), "schemaVersion": 1, "occurredAt": self.now.isoformat(),
            "locale": "es", "policyVersion": "incrementality-measurement-v1",
            "venueId": str(uuid4()), "periodStart": (self.end - timedelta(days=28)).isoformat(),
            "periodEnd": self.end.isoformat(), "experimentPolicyVersion": "ranking-ab-test-v1",
            "causalGatePolicyVersion": "causal-ab-validation-v1",
            "experimentDesign": "randomizedControlledAb", "productionEvidence": True,
            "preRegistered": True, "assignmentPersistedBeforeExposure": True,
            "stableMutuallyExclusiveAssignment": True, "causalGateValidated": True,
            "consentRevocationsApplied": True, "containsPersonalData": False,
            "crossOverCount": 0, "hardConstraintViolations": 0, "privacyViolations": 0,
            "units": units,
        }
        body.update(changes)
        return IncrementalityMeasurementRequest.model_validate(body)

    def test_valid_rct_reports_incremental_value_recovery_cost_and_return(self) -> None:
        units = self._units("control", 100, 20) + self._units("treatment", 100, 40, 10)
        result = self.service.measure(self._request(units))
        self.assertEqual("causal", result.status)
        self.assertEqual(20.0, result.incrementalBookingsEstimate)
        self.assertEqual(10.0, result.incrementalRecoveredBookingsEstimate)
        self.assertEqual(100000.0, result.incrementalNetRevenueEstimateCents)
        self.assertEqual(500.0, result.costPerIncrementalCustomerCents)
        self.assertEqual(9.0, result.returnOnActivationCost)

    def test_observational_data_never_emits_incremental_fields(self) -> None:
        units = self._units("control", 100, 20) + self._units("treatment", 100, 40)
        result = self.service.measure(self._request(
            units, experimentDesign="observational", causalGateValidated=False
        ))
        self.assertEqual("observational", result.status)
        self.assertEqual("attributedEstimated", result.terminology)
        self.assertIsNone(result.incrementalBookingsEstimate)
        self.assertFalse(result.causalInterpretationAllowed)

    def test_small_or_immature_sample_fails_closed(self) -> None:
        units = self._units("control", 20, 5) + self._units("treatment", 20, 8)
        result = self.service.measure(self._request(units))
        self.assertEqual("insufficient", result.status)
        self.assertIn("controlSampleInsufficient", result.causalGateFailures)

    def test_booking_outside_window_is_not_attributed_or_valued(self) -> None:
        units = self._units("control", 100, 20) + self._units("treatment", 100, 40)
        units[100]["bookingAt"] = (self.end - timedelta(days=5)).isoformat()
        units[100]["outcomeObservedAt"] = (self.end - timedelta(days=2)).isoformat()
        result = self.service.measure(self._request(units, causalGateValidated=False))
        self.assertEqual(1, result.excludedOutsideAttributionWindow)
        self.assertEqual(39, result.treatment.bookings)

    def test_duplicate_booking_or_unit_is_rejected_to_prevent_double_counting(self) -> None:
        units = self._units("control", 2, 2)
        units[1]["bookingId"] = units[0]["bookingId"]
        with self.assertRaises(ValueError):
            self._request(units)

    def test_zero_or_negative_incremental_customer_denominator_hides_cost(self) -> None:
        units = self._units("control", 100, 30) + self._units("treatment", 100, 20)
        result = self.service.measure(self._request(units))
        self.assertIsNone(result.costPerIncrementalCustomerCents)
        self.assertIsNotNone(result.returnOnActivationCost)


if __name__ == "__main__":
    unittest.main()
