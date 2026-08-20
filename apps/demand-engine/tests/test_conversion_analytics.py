"""Pruebas de dimensiones, Wilson, muestra mínima, permisos y privacidad de conversión."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from pydantic import ValidationError

from reserly_demand_engine.conversion_analytics import (
    ConversionAnalyticsCalculator,
    ConversionAnalyticsDataset,
    ConversionAnalyticsPolicy,
)

ROOT = Path(__file__).parents[1]


class ConversionAnalyticsTests(unittest.TestCase):
    """Asegura aislamiento de local y ausencia de cifras para cohorts insuficientes."""

    def setUp(self) -> None:
        self.policy = ConversionAnalyticsPolicy.load(ROOT / "policies" / "conversion-analytics.v1.json")
        self.calculator = ConversionAnalyticsCalculator(
            self.policy,
            ROOT.parents[1] / "packages" / "demand-contracts" / "ontology" / "personal-care.v1.json",
        )
        self.venue_id = uuid4()
        self.service_id = uuid4()

    def _dataset(self):
        start = datetime(2026, 7, 1, tzinfo=UTC)
        observations = []
        for index in range(40):
            observations.append({
                "observationId": str(uuid4()),
                "occurredAt": start + timedelta(hours=index),
                "outcomeObservedAt": start + timedelta(hours=index + 2),
                "eligibleExposure": True,
                "serviceId": str(self.service_id if index < 35 else uuid4()),
                "timeBand": "morning" if index < 35 else "evening",
                "approximateZoneCode": "zone-centro" if index < 35 else "zone-norte",
                "permittedSegment": "anonymous" if index < 35 else "newCustomer",
                "attributeCodes": ["calmAtmosphere"] if index < 35 else ["modernStyle"],
                "completedBooking": index % 2,
            })
        return ConversionAnalyticsDataset.model_validate({
            "datasetVersion": "conversion-analytics-synthetic-v1",
            "venueId": str(self.venue_id),
            "venueTimeZone": "Europe/Madrid",
            "periodStart": start,
            "periodEnd": start + timedelta(days=2),
            "extractedAt": start + timedelta(days=3),
            "purpose": "venueConversionAnalytics",
            "containsPersonalData": False,
            "consentRevocationsApplied": True,
            "zoneGranularity": "approximateNamedZone",
            "observations": observations,
        })

    def test_five_dimensions_publish_wilson_intervals_for_sufficient_groups(self) -> None:
        result = self.calculator.calculate(self._dataset(), authorized_venue_id=self.venue_id)
        available = [group for group in result.groups if group.status == "available"]
        self.assertEqual(
            {"service", "timeBand", "approximateZone", "permittedSegment", "attribute"},
            {group.dimension for group in available},
        )
        for group in available:
            self.assertLess(group.interval.lower, group.conversionRate)
            self.assertGreater(group.interval.upper, group.conversionRate)
            self.assertEqual(0.95, group.interval.confidenceLevel)
        self.assertEqual("observationalAssociationNotCausal", result.interpretation)

    def test_small_groups_hide_all_counts_rates_and_intervals(self) -> None:
        result = self.calculator.calculate(self._dataset(), authorized_venue_id=self.venue_id)
        suppressed = [group for group in result.groups if group.status == "insufficientSample"]
        self.assertGreater(len(suppressed), 0)
        for group in suppressed:
            self.assertIsNone(group.sampleCount)
            self.assertIsNone(group.convertedCount)
            self.assertIsNone(group.conversionRate)
            self.assertIsNone(group.interval)

    def test_other_venue_is_forbidden_before_aggregation(self) -> None:
        with self.assertRaisesRegex(PermissionError, "VENUE_FORBIDDEN"):
            self.calculator.calculate(self._dataset(), authorized_venue_id=uuid4())

    def test_unknown_attribute_and_direct_personal_fields_fail_closed(self) -> None:
        raw = self._dataset().model_dump()
        raw["observations"][0]["attributeCodes"] = ["sensitiveUnknownAttribute"]
        with self.assertRaisesRegex(ValueError, "DIMENSION_INVALID"):
            self.calculator.calculate(
                ConversionAnalyticsDataset.model_validate(raw), authorized_venue_id=self.venue_id
            )
        raw = self._dataset().model_dump()
        raw["observations"][0]["customerEmail"] = "person@example.com"
        with self.assertRaises(ValidationError):
            ConversionAnalyticsDataset.model_validate(raw)

    def test_immature_or_out_of_period_observation_is_rejected(self) -> None:
        raw = self._dataset().model_dump()
        raw["observations"][0]["outcomeObservedAt"] = raw["extractedAt"] + timedelta(seconds=1)
        with self.assertRaisesRegex(ValueError, "OUTSIDE_PERIOD"):
            ConversionAnalyticsDataset.model_validate(raw)


if __name__ == "__main__":
    unittest.main()
