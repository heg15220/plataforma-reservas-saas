"""Analítica de conversión aislada por local con Wilson y supresión de muestras pequeñas."""

from __future__ import annotations

import argparse
import json
import math
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from statistics import NormalDist
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


Dimension = Literal["service", "timeBand", "approximateZone", "permittedSegment", "attribute"]
Segment = Literal["anonymous", "newCustomer", "returningCustomer"]


class ConversionAnalyticsPolicy(StrictContract):
    """Define denominador, dimensiones permitidas, privacidad e intervalo versionados."""

    schemaVersion: Literal[1]
    policyVersion: Version
    definitionVersion: Version
    ontologyVersion: Version
    dimensions: list[Dimension] = Field(min_length=5, max_length=5)
    permittedSegments: list[Segment] = Field(min_length=3, max_length=3)
    minimumSample: int = Field(ge=10)
    minimumConverted: int = Field(ge=1)
    minimumNotConverted: int = Field(ge=1)
    confidenceLevel: float = Field(gt=0.5, lt=1)
    maximumGroups: int = Field(ge=10, le=100_000)
    maximumAttributesPerObservation: int = Field(ge=1, le=100)

    @model_validator(mode="after")
    def validate_policy(self) -> "ConversionAnalyticsPolicy":
        if set(self.dimensions) != {
            "service",
            "timeBand",
            "approximateZone",
            "permittedSegment",
            "attribute",
        } or set(self.permittedSegments) != {"anonymous", "newCustomer", "returningCustomer"}:
            raise ValueError("CONVERSION_ANALYTICS_POLICY_INVALID")
        if self.minimumConverted + self.minimumNotConverted > self.minimumSample:
            raise ValueError("CONVERSION_ANALYTICS_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "ConversionAnalyticsPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ConversionObservation(StrictContract):
    """Una exposición elegible ya minimizada y su outcome de reserva maduro."""

    observationId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    eligibleExposure: Literal[True]
    serviceId: UUID
    timeBand: Literal["morning", "afternoon", "evening"]
    approximateZoneCode: Version
    permittedSegment: Segment
    attributeCodes: list[Version] = Field(max_length=44)
    completedBooking: Literal[0, 1]

    @model_validator(mode="after")
    def validate_observation(self) -> "ConversionObservation":
        if (
            self.occurredAt.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.occurredAt
            or len(self.attributeCodes) != len(set(self.attributeCodes))
        ):
            raise ValueError("CONVERSION_ANALYTICS_OBSERVATION_INVALID")
        return self


class ConversionAnalyticsDataset(StrictContract):
    """Cohorte de un solo local y periodo; no admite identidad de cliente ni texto libre."""

    datasetVersion: Version
    venueId: UUID
    venueTimeZone: str = Field(min_length=3, max_length=64)
    periodStart: datetime
    periodEnd: datetime
    extractedAt: datetime
    purpose: Literal["venueConversionAnalytics"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    zoneGranularity: Literal["approximateNamedZone"]
    observations: list[ConversionObservation] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "ConversionAnalyticsDataset":
        times = (self.periodStart, self.periodEnd, self.extractedAt)
        ids = [item.observationId for item in self.observations]
        if any(value.tzinfo is None or value.utcoffset() is None for value in times):
            raise ValueError("CONVERSION_ANALYTICS_TIMEZONE_REQUIRED")
        if self.periodStart >= self.periodEnd or self.extractedAt < self.periodEnd:
            raise ValueError("CONVERSION_ANALYTICS_PERIOD_INVALID")
        if len(ids) != len(set(ids)):
            raise ValueError("CONVERSION_ANALYTICS_OBSERVATION_DUPLICATED")
        if any(
            not self.periodStart <= item.occurredAt < self.periodEnd
            or item.outcomeObservedAt > self.extractedAt
            for item in self.observations
        ):
            raise ValueError("CONVERSION_ANALYTICS_OBSERVATION_OUTSIDE_PERIOD")
        return self


class WilsonInterval(StrictContract):
    """Intervalo bilateral Wilson para una proporción binomial."""

    confidenceLevel: float
    lower: float = Field(ge=0, le=1)
    upper: float = Field(ge=0, le=1)


class ConversionGroup(StrictContract):
    """Bucket publicable o totalmente suprimido para impedir extrapolación/reidentificación."""

    dimension: Dimension
    value: str = Field(min_length=1, max_length=64)
    status: Literal["available", "insufficientSample"]
    sampleCount: int | None = Field(default=None, ge=0)
    convertedCount: int | None = Field(default=None, ge=0)
    conversionRate: float | None = Field(default=None, ge=0, le=1)
    interval: WilsonInterval | None
    suppressionReason: Literal["minimumSampleOrClassCount"] | None

    @model_validator(mode="after")
    def validate_suppression(self) -> "ConversionGroup":
        measurements = (self.sampleCount, self.convertedCount, self.conversionRate, self.interval)
        if self.status == "insufficientSample":
            if any(value is not None for value in measurements) or self.suppressionReason is None:
                raise ValueError("CONVERSION_ANALYTICS_SUPPRESSION_INVALID")
        elif any(value is None for value in measurements) or self.suppressionReason is not None:
            raise ValueError("CONVERSION_ANALYTICS_AVAILABLE_GROUP_INVALID")
        return self


class ConversionAnalyticsResult(StrictContract):
    """Informe local agregado con definición, cobertura y dimensiones cerradas."""

    policyVersion: Version
    definitionVersion: Version
    datasetVersion: Version
    ontologyVersion: Version
    venueId: UUID
    venueTimeZone: str
    periodStart: datetime
    periodEnd: datetime
    accessScope: Literal["singleAuthorizedVenue"]
    eligibleExposureCount: int
    availableGroupCount: int
    suppressedGroupCount: int
    groups: list[ConversionGroup]
    interpretation: Literal["observationalAssociationNotCausal"]


class ConversionAnalyticsCalculator:
    """Agrupa cinco dimensiones permitidas y falla cerrado ante acceso o ontología incoherentes."""

    def __init__(self, policy: ConversionAnalyticsPolicy, ontology_path: Path) -> None:
        self.policy = policy
        ontology = json.loads(ontology_path.read_text(encoding="utf-8"))
        if ontology.get("ontologyVersion") != policy.ontologyVersion:
            raise ValueError("CONVERSION_ANALYTICS_ONTOLOGY_VERSION_MISMATCH")
        self._attribute_codes = {item["code"] for item in ontology["attributes"]}

    def calculate(
        self, dataset: ConversionAnalyticsDataset, *, authorized_venue_id: UUID
    ) -> ConversionAnalyticsResult:
        """Calcula solo si el local autorizado coincide; nunca mezcla ni infiere segmentos."""
        if dataset.venueId != authorized_venue_id:
            raise PermissionError("CONVERSION_ANALYTICS_VENUE_FORBIDDEN")
        permitted = set(self.policy.permittedSegments)
        groups: dict[tuple[str, str], list[int]] = defaultdict(list)
        for observation in dataset.observations:
            if (
                observation.permittedSegment not in permitted
                or len(observation.attributeCodes) > self.policy.maximumAttributesPerObservation
                or not set(observation.attributeCodes) <= self._attribute_codes
            ):
                raise ValueError("CONVERSION_ANALYTICS_DIMENSION_INVALID")
            outcome = observation.completedBooking
            groups[("service", str(observation.serviceId))].append(outcome)
            groups[("timeBand", observation.timeBand)].append(outcome)
            groups[("approximateZone", observation.approximateZoneCode)].append(outcome)
            groups[("permittedSegment", observation.permittedSegment)].append(outcome)
            for attribute in observation.attributeCodes:
                groups[("attribute", attribute)].append(outcome)
        if len(groups) > self.policy.maximumGroups:
            raise ValueError("CONVERSION_ANALYTICS_GROUP_LIMIT_EXCEEDED")
        output = [self._group(dimension, value, outcomes) for (dimension, value), outcomes in groups.items()]
        output.sort(key=lambda item: (self.policy.dimensions.index(item.dimension), item.value))
        return ConversionAnalyticsResult(
            policyVersion=self.policy.policyVersion,
            definitionVersion=self.policy.definitionVersion,
            datasetVersion=dataset.datasetVersion,
            ontologyVersion=self.policy.ontologyVersion,
            venueId=dataset.venueId,
            venueTimeZone=dataset.venueTimeZone,
            periodStart=dataset.periodStart,
            periodEnd=dataset.periodEnd,
            accessScope="singleAuthorizedVenue",
            eligibleExposureCount=len(dataset.observations),
            availableGroupCount=sum(item.status == "available" for item in output),
            suppressedGroupCount=sum(item.status == "insufficientSample" for item in output),
            groups=output,
            interpretation="observationalAssociationNotCausal",
        )

    def _group(self, dimension: str, value: str, outcomes: list[int]) -> ConversionGroup:
        converted = sum(outcomes)
        sample = len(outcomes)
        if (
            sample < self.policy.minimumSample
            or converted < self.policy.minimumConverted
            or sample - converted < self.policy.minimumNotConverted
        ):
            return ConversionGroup(
                dimension=dimension,
                value=value,
                status="insufficientSample",
                sampleCount=None,
                convertedCount=None,
                conversionRate=None,
                interval=None,
                suppressionReason="minimumSampleOrClassCount",
            )
        rate = converted / sample
        lower, upper = _wilson(converted, sample, self.policy.confidenceLevel)
        return ConversionGroup(
            dimension=dimension,
            value=value,
            status="available",
            sampleCount=sample,
            convertedCount=converted,
            conversionRate=round(rate, 8),
            interval=WilsonInterval(
                confidenceLevel=self.policy.confidenceLevel,
                lower=round(lower, 8),
                upper=round(upper, 8),
            ),
            suppressionReason=None,
        )


def _wilson(successes: int, sample: int, confidence: float) -> tuple[float, float]:
    """Wilson score interval, acotado a [0,1], sin aproximar conteos suprimidos."""
    z = NormalDist().inv_cdf(1 - (1 - confidence) / 2)
    rate = successes / sample
    denominator = 1 + z * z / sample
    center = (rate + z * z / (2 * sample)) / denominator
    margin = z * math.sqrt(rate * (1 - rate) / sample + z * z / (4 * sample * sample)) / denominator
    return max(0.0, center - margin), min(1.0, center + margin)


def run() -> None:
    """CLI offline: valida alcance local y escribe únicamente agregados gobernados."""
    parser = argparse.ArgumentParser(description="Calculate governed venue conversion analytics")
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--ontology", type=Path, required=True)
    parser.add_argument("--authorized-venue-id", type=UUID, required=True)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    dataset = ConversionAnalyticsDataset.model_validate_json(
        arguments.dataset.read_text(encoding="utf-8")
    )
    result = ConversionAnalyticsCalculator(
        ConversionAnalyticsPolicy.load(arguments.policy), arguments.ontology
    ).calculate(dataset, authorized_venue_id=arguments.authorized_venue_id)
    arguments.output.write_text(result.model_dump_json(indent=2), encoding="utf-8")
