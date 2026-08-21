"""Medición comercial con atribución observacional y causalidad experimental separadas."""

from __future__ import annotations

from collections import Counter
from datetime import datetime, timedelta
import math
from pathlib import Path
from statistics import NormalDist
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import RequestEnvelope, StrictContract, Version


Arm = Literal["control", "treatment"]
AttributionClass = Literal["direct", "assisted", "generated", "recovered"]
OutcomeStatus = Literal["attended", "cancelled", "noShow"]


class IncrementalityMeasurementPolicy(StrictContract):
    """Versiona ventanas, muestra, confianza y protocolo causal requerido."""

    schemaVersion: Literal[1]
    policyVersion: Version
    requiredExperimentPolicyVersion: Version
    requiredCausalGatePolicyVersion: Version
    attributionWindowHours: int = Field(ge=1, le=24 * 30)
    outcomeMaturityHours: int = Field(ge=1, le=24 * 30)
    maximumPeriodDays: int = Field(ge=1, le=366)
    minimumUnitsPerArm: int = Field(ge=30)
    minimumMaturedBookingsPerArm: int = Field(ge=1)
    maximumAssignmentRatioDeviation: float = Field(ge=0, lt=0.5)
    confidenceLevel: float = Field(gt=0.5, lt=1)
    currency: Literal["EUR"]
    automaticCommercialClaimAllowed: Literal[False]

    @classmethod
    def load(cls, path: Path) -> "IncrementalityMeasurementPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class CommercialOutcomeUnit(StrictContract):
    """Unidad experimental minimizada; una reserva opcional solo puede clasificarse una vez."""

    unitId: UUID
    arm: Arm
    assignedAt: datetime
    exposedAt: datetime
    bookingId: UUID | None = None
    bookingAt: datetime | None = None
    attributionClass: AttributionClass | None = None
    customerStatus: Literal["new", "returning"] | None = None
    outcomeStatus: OutcomeStatus | None = None
    outcomeObservedAt: datetime | None = None
    realizedNetRevenueCents: int = Field(ge=-10_000_000, le=100_000_000)
    activationCostCents: int = Field(ge=0, le=10_000_000)
    offPeakBooking: bool

    @model_validator(mode="after")
    def validate_unit(self) -> "CommercialOutcomeUnit":
        datetimes = [self.assignedAt, self.exposedAt]
        datetimes.extend(value for value in (self.bookingAt, self.outcomeObservedAt) if value)
        has_booking = self.bookingId is not None
        booking_fields = (
            self.bookingAt, self.attributionClass, self.customerStatus,
        )
        if (
            any(value.tzinfo is None for value in datetimes)
            or self.assignedAt > self.exposedAt
            or has_booking != all(value is not None for value in booking_fields)
            or (not has_booking and (
                self.outcomeStatus is not None or self.outcomeObservedAt is not None
                or self.realizedNetRevenueCents != 0 or self.offPeakBooking
            ))
            or ((self.outcomeStatus is None) != (self.outcomeObservedAt is None))
            or (self.bookingAt is not None and self.bookingAt < self.exposedAt)
            or (self.outcomeObservedAt is not None and self.outcomeObservedAt < self.bookingAt)
        ):
            raise ValueError("COMMERCIAL_OUTCOME_UNIT_INVALID")
        return self


class IncrementalityMeasurementRequest(RequestEnvelope):
    """Cohorte aislada por local/periodo con protocolo y calidad declarados."""

    venueId: UUID
    periodStart: datetime
    periodEnd: datetime
    experimentPolicyVersion: Version
    causalGatePolicyVersion: Version
    experimentDesign: Literal["randomizedControlledAb", "observational"]
    productionEvidence: bool
    preRegistered: bool
    assignmentPersistedBeforeExposure: bool
    stableMutuallyExclusiveAssignment: bool
    causalGateValidated: bool
    consentRevocationsApplied: Literal[True]
    containsPersonalData: Literal[False]
    crossOverCount: int = Field(ge=0)
    hardConstraintViolations: int = Field(ge=0)
    privacyViolations: int = Field(ge=0)
    units: list[CommercialOutcomeUnit] = Field(min_length=1, max_length=200_000)

    @model_validator(mode="after")
    def validate_cohort(self) -> "IncrementalityMeasurementRequest":
        unit_ids = [unit.unitId for unit in self.units]
        booking_ids = [unit.bookingId for unit in self.units if unit.bookingId]
        if (
            self.periodStart.tzinfo is None or self.periodEnd.tzinfo is None
            or self.periodStart >= self.periodEnd
            or self.occurredAt < self.periodEnd
            or len(unit_ids) != len(set(unit_ids))
            or len(booking_ids) != len(set(booking_ids))
            or any(not self.periodStart <= unit.exposedAt < self.periodEnd for unit in self.units)
            or any(unit.outcomeObservedAt and unit.outcomeObservedAt > self.occurredAt
                   for unit in self.units)
        ):
            raise ValueError("INCREMENTALITY_COHORT_INVALID")
        return self


class ConfidenceInterval(StrictContract):
    """Estimación puntual e intervalo bilateral para una diferencia tratamiento-control."""

    estimate: float
    lower: float
    upper: float


class ArmCommercialMetrics(StrictContract):
    """Conteos y valor observado de un brazo, sin lenguaje incremental."""

    units: int = Field(ge=0)
    bookings: int = Field(ge=0)
    maturedBookings: int = Field(ge=0)
    attendedBookings: int = Field(ge=0)
    cancelledBookings: int = Field(ge=0)
    noShowBookings: int = Field(ge=0)
    directBookings: int = Field(ge=0)
    assistedBookings: int = Field(ge=0)
    generatedBookings: int = Field(ge=0)
    recoveredBookings: int = Field(ge=0)
    newCustomerBookings: int = Field(ge=0)
    returningCustomerBookings: int = Field(ge=0)
    offPeakBookings: int = Field(ge=0)
    realizedNetRevenueCents: int
    activationCostCents: int = Field(ge=0)


class IncrementalityMeasurementResponse(StrictContract):
    """Informe que omite campos causales cuando controles o muestra son insuficientes."""

    requestId: UUID
    schemaVersion: Literal[1] = 1
    venueId: UUID
    policyVersion: Version
    experimentPolicyVersion: Version
    causalGatePolicyVersion: Version
    periodStart: datetime
    periodEnd: datetime
    attributionWindowHours: int
    outcomeMaturityHours: int
    currency: Literal["EUR"]
    status: Literal["causal", "observational", "insufficient"]
    terminology: Literal["incrementalDemonstrated", "attributedEstimated"]
    control: ArmCommercialMetrics
    treatment: ArmCommercialMetrics
    excludedOutsideAttributionWindow: int = Field(ge=0)
    immatureBookingCount: int = Field(ge=0)
    coverage: float = Field(ge=0, le=1)
    causalGateFailures: list[Version]
    bookingRateEffect: ConfidenceInterval | None
    attendedBookingRateEffect: ConfidenceInterval | None
    recoveredBookingRateEffect: ConfidenceInterval | None
    netRevenuePerUnitEffectCents: ConfidenceInterval | None
    incrementalBookingsEstimate: float | None
    incrementalAttendedBookingsEstimate: float | None
    incrementalRecoveredBookingsEstimate: float | None
    incrementalNetRevenueEstimateCents: float | None
    incrementalNewCustomersEstimate: float | None
    costPerIncrementalCustomerCents: float | None
    returnOnActivationCost: float | None
    causalInterpretationAllowed: bool
    automaticCommercialClaimAllowed: Literal[False]


class IncrementalityMeasurementService:
    """Agrega valor observado y abre métricas causales solo tras todos los controles."""

    def __init__(self, policy: IncrementalityMeasurementPolicy) -> None:
        self.policy = policy

    def measure(self, request: IncrementalityMeasurementRequest) -> IncrementalityMeasurementResponse:
        """Calcula clasificación, cobertura, efectos e intervalos sin doble conteo."""
        if request.policyVersion != self.policy.policyVersion:
            raise ValueError("INCREMENTALITY_POLICY_VERSION_MISMATCH")
        if request.periodEnd - request.periodStart > timedelta(days=self.policy.maximumPeriodDays):
            raise ValueError("INCREMENTALITY_PERIOD_TOO_LONG")
        included, outside = self._attributed_units(request)
        control_units = [unit for unit in included if unit.arm == "control"]
        treatment_units = [unit for unit in included if unit.arm == "treatment"]
        control = self._arm_metrics(control_units, request.occurredAt)
        treatment = self._arm_metrics(treatment_units, request.occurredAt)
        failures = self._causal_failures(request, control, treatment)
        causal = not failures
        enough_any = min(control.units, treatment.units) >= self.policy.minimumUnitsPerArm
        status = "causal" if causal else ("observational" if enough_any else "insufficient")
        effects = self._effects(control_units, treatment_units, request.occurredAt) if causal else {}
        treatment_count = treatment.units
        incremental_new = effects.get("new", (None,))[0] * treatment_count if causal else None
        incremental_revenue = effects.get("revenue", (None,))[0] * treatment_count if causal else None
        total_cost = treatment.activationCostCents
        cost_per_customer = (
            total_cost / incremental_new if incremental_new is not None and incremental_new > 0 else None
        )
        roi = (
            (incremental_revenue - total_cost) / total_cost
            if incremental_revenue is not None and total_cost > 0 else None
        )
        immature = sum(
            1 for unit in included if unit.bookingId and not self._matured(unit, request.occurredAt)
        )
        matured = control.maturedBookings + treatment.maturedBookings
        bookings = control.bookings + treatment.bookings
        return IncrementalityMeasurementResponse(
            requestId=request.requestId, venueId=request.venueId,
            policyVersion=self.policy.policyVersion,
            experimentPolicyVersion=request.experimentPolicyVersion,
            causalGatePolicyVersion=request.causalGatePolicyVersion,
            periodStart=request.periodStart, periodEnd=request.periodEnd,
            attributionWindowHours=self.policy.attributionWindowHours,
            outcomeMaturityHours=self.policy.outcomeMaturityHours, currency=self.policy.currency,
            status=status, terminology="incrementalDemonstrated" if causal else "attributedEstimated",
            control=control, treatment=treatment, excludedOutsideAttributionWindow=outside,
            immatureBookingCount=immature, coverage=round(matured / bookings, 8) if bookings else 0.0,
            causalGateFailures=failures,
            bookingRateEffect=self._interval_contract(effects.get("booking")),
            attendedBookingRateEffect=self._interval_contract(effects.get("attended")),
            recoveredBookingRateEffect=self._interval_contract(effects.get("recovered")),
            netRevenuePerUnitEffectCents=self._interval_contract(effects.get("revenue")),
            incrementalBookingsEstimate=self._scaled(effects.get("booking"), treatment_count),
            incrementalAttendedBookingsEstimate=self._scaled(effects.get("attended"), treatment_count),
            incrementalRecoveredBookingsEstimate=self._scaled(effects.get("recovered"), treatment_count),
            incrementalNetRevenueEstimateCents=round(incremental_revenue, 2) if incremental_revenue is not None else None,
            incrementalNewCustomersEstimate=round(incremental_new, 4) if incremental_new is not None else None,
            costPerIncrementalCustomerCents=round(cost_per_customer, 2) if cost_per_customer is not None else None,
            returnOnActivationCost=round(roi, 8) if roi is not None else None,
            causalInterpretationAllowed=causal, automaticCommercialClaimAllowed=False,
        )

    def _attributed_units(self, request):
        result = []
        outside = 0
        window = timedelta(hours=self.policy.attributionWindowHours)
        for unit in request.units:
            if unit.bookingAt is not None and unit.bookingAt > unit.exposedAt + window:
                outside += 1
                result.append(unit.model_copy(update={
                    "bookingId": None, "bookingAt": None, "attributionClass": None,
                    "customerStatus": None, "outcomeStatus": None, "outcomeObservedAt": None,
                    "realizedNetRevenueCents": 0, "offPeakBooking": False,
                }))
            else:
                result.append(unit)
        return result, outside

    def _matured(self, unit, evaluated_at):
        return bool(
            unit.bookingAt and unit.outcomeObservedAt and unit.outcomeStatus
            and unit.outcomeObservedAt >= unit.bookingAt + timedelta(hours=self.policy.outcomeMaturityHours)
            and unit.outcomeObservedAt <= evaluated_at
        )

    def _arm_metrics(self, units, evaluated_at):
        bookings = [unit for unit in units if unit.bookingId]
        matured = [unit for unit in bookings if self._matured(unit, evaluated_at)]
        classes = Counter(unit.attributionClass for unit in bookings)
        outcomes = Counter(unit.outcomeStatus for unit in matured)
        customers = Counter(unit.customerStatus for unit in bookings)
        return ArmCommercialMetrics(
            units=len(units), bookings=len(bookings), maturedBookings=len(matured),
            attendedBookings=outcomes["attended"], cancelledBookings=outcomes["cancelled"],
            noShowBookings=outcomes["noShow"], directBookings=classes["direct"],
            assistedBookings=classes["assisted"], generatedBookings=classes["generated"],
            recoveredBookings=classes["recovered"], newCustomerBookings=customers["new"],
            returningCustomerBookings=customers["returning"],
            offPeakBookings=sum(unit.offPeakBooking for unit in bookings),
            realizedNetRevenueCents=sum(unit.realizedNetRevenueCents for unit in matured),
            activationCostCents=sum(unit.activationCostCents for unit in units),
        )

    def _causal_failures(self, request, control, treatment):
        failures = []
        checks = {
            "productionEvidenceRequired": request.productionEvidence,
            "randomizedControlRequired": request.experimentDesign == "randomizedControlledAb",
            "experimentPolicyMismatch": request.experimentPolicyVersion == self.policy.requiredExperimentPolicyVersion,
            "causalGatePolicyMismatch": request.causalGatePolicyVersion == self.policy.requiredCausalGatePolicyVersion,
            "preRegistrationRequired": request.preRegistered,
            "assignmentBeforeExposureRequired": request.assignmentPersistedBeforeExposure,
            "stableExclusiveAssignmentRequired": request.stableMutuallyExclusiveAssignment,
            "causalGateValidationRequired": request.causalGateValidated,
            "crossOverDetected": request.crossOverCount == 0,
            "hardConstraintViolation": request.hardConstraintViolations == 0,
            "privacyViolation": request.privacyViolations == 0,
            "controlSampleInsufficient": control.units >= self.policy.minimumUnitsPerArm,
            "treatmentSampleInsufficient": treatment.units >= self.policy.minimumUnitsPerArm,
            "controlMaturityInsufficient": control.maturedBookings >= self.policy.minimumMaturedBookingsPerArm,
            "treatmentMaturityInsufficient": treatment.maturedBookings >= self.policy.minimumMaturedBookingsPerArm,
        }
        failures.extend(name for name, passed in checks.items() if not passed)
        total = control.units + treatment.units
        if total == 0 or abs(treatment.units / total - 0.5) > self.policy.maximumAssignmentRatioDeviation:
            failures.append("assignmentRatioInvalid")
        return sorted(failures)

    def _effects(self, control, treatment, evaluated_at):
        return {
            "booking": self._difference([float(bool(u.bookingId)) for u in control],
                                        [float(bool(u.bookingId)) for u in treatment]),
            "attended": self._difference([
                float(self._matured(u, evaluated_at) and u.outcomeStatus == "attended") for u in control
            ], [float(self._matured(u, evaluated_at) and u.outcomeStatus == "attended") for u in treatment]),
            "recovered": self._difference([
                float(u.attributionClass == "recovered") for u in control
            ], [float(u.attributionClass == "recovered") for u in treatment]),
            "new": self._difference([float(u.customerStatus == "new") for u in control],
                                    [float(u.customerStatus == "new") for u in treatment]),
            "revenue": self._difference([
                float(u.realizedNetRevenueCents if self._matured(u, evaluated_at) else 0) for u in control
            ], [float(u.realizedNetRevenueCents if self._matured(u, evaluated_at) else 0) for u in treatment]),
        }

    def _difference(self, control, treatment):
        control_mean = sum(control) / len(control)
        treatment_mean = sum(treatment) / len(treatment)
        control_var = self._variance(control, control_mean)
        treatment_var = self._variance(treatment, treatment_mean)
        standard_error = math.sqrt(control_var / len(control) + treatment_var / len(treatment))
        z = NormalDist().inv_cdf(0.5 + self.policy.confidenceLevel / 2)
        effect = treatment_mean - control_mean
        return effect, effect - z * standard_error, effect + z * standard_error

    @staticmethod
    def _variance(values, mean):
        return sum((value - mean) ** 2 for value in values) / (len(values) - 1) if len(values) > 1 else 0.0

    @staticmethod
    def _interval_contract(value):
        return ConfidenceInterval(estimate=round(value[0], 8), lower=round(value[1], 8),
                                  upper=round(value[2], 8)) if value else None

    @staticmethod
    def _scaled(value, count):
        return round(value[0] * count, 4) if value else None
