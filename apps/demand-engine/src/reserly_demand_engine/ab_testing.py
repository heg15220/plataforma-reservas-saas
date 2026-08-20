"""Análisis A/B prerregistrado del ranking con potencia, alpha spending y guardrails."""

from __future__ import annotations

import math
from pathlib import Path
from statistics import NormalDist
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


class AnalysisLook(StrictContract):
    """Única lectura autorizada con alpha acumulado declarado antes del experimento."""

    sequence: int = Field(ge=1)
    elapsedDays: int = Field(ge=1)
    twoSidedAlpha: float = Field(gt=0, lt=1)


class RankingAbPolicy(StrictContract):
    """Hipótesis, población, efecto, potencia, calendario y guardrails inmutables."""

    schemaVersion: Literal[1]
    protocolVersion: Version
    experimentKey: Version
    experimentDefinitionVersion: int = Field(ge=1)
    controlVariantKey: Literal["control"]
    treatmentVariantKey: Literal["treatment"]
    controlPolicyVersion: Version
    treatmentPolicyVersion: Version
    assignmentUnit: Literal["consentedPseudonymousSession"]
    allocationTreatmentBps: int = Field(gt=0, lt=10_000)
    primaryMetric: Literal["completedBookingRate"]
    baselineRate: float = Field(gt=0, lt=1)
    minimumDetectableAbsoluteEffect: float = Field(gt=0, lt=1)
    alpha: float = Field(gt=0, lt=1)
    targetPower: float = Field(gt=0.5, lt=1)
    plannedPeriodDays: int = Field(ge=7)
    maximumPeriodDays: int = Field(ge=7)
    analysisLooks: list[AnalysisLook] = Field(min_length=1)
    maximumTreatmentAllocationDeviation: float = Field(ge=0, lt=0.5)
    minimumExposureRate: float = Field(gt=0, le=1)
    maximumAttendanceRateDecrease: float = Field(ge=0, le=1)
    maximumCancellationRateIncrease: float = Field(ge=0, le=1)
    maximumOffPeakShareDecrease: float = Field(ge=0, le=1)
    minimumNewVenueExposureRatio: float = Field(ge=0, le=1)
    zeroToleranceHardConstraintViolations: Literal[True]
    zeroTolerancePrivacyViolations: Literal[True]
    zeroToleranceCrossOvers: Literal[True]

    @model_validator(mode="after")
    def validate_protocol(self) -> "RankingAbPolicy":
        sequences = [look.sequence for look in self.analysisLooks]
        days = [look.elapsedDays for look in self.analysisLooks]
        alphas = [look.twoSidedAlpha for look in self.analysisLooks]
        if (
            self.controlPolicyVersion == self.treatmentPolicyVersion
            or sequences != list(range(1, len(sequences) + 1))
            or days != sorted(days)
            or alphas != sorted(alphas)
            or alphas[-1] != self.alpha
            or self.plannedPeriodDays not in days
            or days[-1] != self.maximumPeriodDays
            or self.maximumPeriodDays < self.plannedPeriodDays
            or self.baselineRate + self.minimumDetectableAbsoluteEffect >= 1
        ):
            raise ValueError("RANKING_AB_PROTOCOL_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "RankingAbPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))

    def required_sample_per_arm(self) -> int:
        """Aproximación normal bilateral para dos proporciones con asignación 1:1."""
        p1 = self.baselineRate
        p2 = p1 + self.minimumDetectableAbsoluteEffect
        pooled = (p1 + p2) / 2
        z_alpha = NormalDist().inv_cdf(1 - self.alpha / 2)
        z_power = NormalDist().inv_cdf(self.targetPower)
        numerator = (
            z_alpha * math.sqrt(2 * pooled * (1 - pooled))
            + z_power * math.sqrt(p1 * (1 - p1) + p2 * (1 - p2))
        ) ** 2
        return math.ceil(numerator / (p2 - p1) ** 2)


class ExperimentArmAggregate(StrictContract):
    """Conteos agregados de una variante; no admite unidades, sesiones ni eventos individuales."""

    variantKey: Literal["control", "treatment"]
    policyVersion: Version
    assignedSessions: int = Field(ge=1)
    exposedSessions: int = Field(ge=0)
    completedBookings: int = Field(ge=0)
    maturedBookings: int = Field(ge=0)
    attendedBookings: int = Field(ge=0)
    cancelledBookings: int = Field(ge=0)
    totalImpressions: int = Field(ge=0)
    offPeakImpressions: int = Field(ge=0)
    newVenueImpressions: int = Field(ge=0)

    @model_validator(mode="after")
    def validate_counts(self) -> "ExperimentArmAggregate":
        if (
            self.exposedSessions > self.assignedSessions
            or self.completedBookings > self.exposedSessions
            or self.attendedBookings > self.maturedBookings
            or self.cancelledBookings > self.maturedBookings
            or self.offPeakImpressions > self.totalImpressions
            or self.newVenueImpressions > self.totalImpressions
        ):
            raise ValueError("RANKING_AB_AGGREGATE_INVALID")
        return self


class RankingAbSnapshot(StrictContract):
    """Lectura agregada de un look autorizado, con integridad y procedencia explícitas."""

    snapshotVersion: Literal[1]
    protocolVersion: Version
    experimentKey: Version
    experimentDefinitionVersion: int = Field(ge=1)
    analysisSequence: int = Field(ge=1)
    elapsedDays: int = Field(ge=1)
    productionEvidence: bool
    consentRevocationsApplied: Literal[True]
    containsPersonalData: Literal[False]
    control: ExperimentArmAggregate
    treatment: ExperimentArmAggregate
    crossOverCount: int = Field(ge=0)
    hardConstraintViolations: int = Field(ge=0)
    privacyViolations: int = Field(ge=0)


class GuardrailResult(StrictContract):
    """Resultado observable de una barrera operacional o de integridad."""

    metric: Version
    passed: bool
    observed: float
    required: float


class RankingAbResult(StrictContract):
    """Informe estadístico completo; solo éxito productivo permite afirmación causal."""

    protocolVersion: Version
    experimentKey: Version
    analysisSequence: int
    elapsedDays: int
    primaryMetric: Literal["completedBookingRate"]
    controlRate: float
    treatmentRate: float
    absoluteEffect: float
    relativeEffect: float
    confidenceLower: float
    confidenceUpper: float
    pValue: float
    alphaAtLook: float
    requiredSamplePerArm: int
    achievedPower: float
    powered: bool
    guardrails: list[GuardrailResult]
    decision: Literal["continue", "success", "futility", "safetyStop", "inconclusive", "simulationOnly"]
    stoppingCriterionMet: bool
    causalClaimAllowed: bool
    blockingReasons: list[Version]


class RankingAbAnalyzer:
    """Analiza solo looks prerregistrados y da prioridad absoluta a guardrails."""

    def __init__(self, policy: RankingAbPolicy) -> None:
        self.policy = policy

    def analyze(self, snapshot: RankingAbSnapshot) -> RankingAbResult:
        """Calcula efecto de dos proporciones sin leer ni aceptar datos individuales."""
        self._validate_snapshot(snapshot)
        look = self.policy.analysisLooks[snapshot.analysisSequence - 1]
        control = snapshot.control
        treatment = snapshot.treatment
        control_rate = control.completedBookings / control.exposedSessions
        treatment_rate = treatment.completedBookings / treatment.exposedSessions
        effect = treatment_rate - control_rate
        relative = effect / control_rate if control_rate else 0.0
        pooled = (control.completedBookings + treatment.completedBookings) / (
            control.exposedSessions + treatment.exposedSessions
        )
        standard_error_null = math.sqrt(
            pooled * (1 - pooled) * (1 / control.exposedSessions + 1 / treatment.exposedSessions)
        )
        z_score = effect / standard_error_null if standard_error_null else 0.0
        p_value = 2 * (1 - NormalDist().cdf(abs(z_score)))
        standard_error_effect = math.sqrt(
            control_rate * (1 - control_rate) / control.exposedSessions
            + treatment_rate * (1 - treatment_rate) / treatment.exposedSessions
        )
        critical = NormalDist().inv_cdf(1 - look.twoSidedAlpha / 2)
        lower = effect - critical * standard_error_effect
        upper = effect + critical * standard_error_effect
        required = self.policy.required_sample_per_arm()
        achieved_power = self._achieved_power(
            min(control.exposedSessions, treatment.exposedSessions)
        )
        powered = (
            control.exposedSessions >= required
            and treatment.exposedSessions >= required
            and achieved_power >= self.policy.targetPower
        )
        guardrails = self._guardrails(snapshot)
        all_guardrails = all(item.passed for item in guardrails)
        reasons: list[str] = []
        if not all_guardrails:
            reasons.append("guardrailFailed")
        if not powered:
            reasons.append("sampleOrPowerInsufficient")
        if not snapshot.productionEvidence:
            reasons.append("productionEvidenceMissing")
        significant = p_value <= look.twoSidedAlpha and lower > 0
        meaningful = effect >= self.policy.minimumDetectableAbsoluteEffect
        planned_period_reached = look.elapsedDays >= self.policy.plannedPeriodDays
        if not all_guardrails:
            decision = "safetyStop"
            stop = True
        elif not snapshot.productionEvidence:
            decision = "simulationOnly"
            stop = False
        elif powered and significant and meaningful:
            decision = "success"
            stop = True
        elif planned_period_reached and powered:
            decision = "futility"
            stop = True
        elif snapshot.elapsedDays >= self.policy.maximumPeriodDays:
            decision = "inconclusive"
            stop = True
        else:
            decision = "continue"
            stop = False
        return RankingAbResult(
            protocolVersion=self.policy.protocolVersion,
            experimentKey=self.policy.experimentKey,
            analysisSequence=look.sequence,
            elapsedDays=look.elapsedDays,
            primaryMetric=self.policy.primaryMetric,
            controlRate=round(control_rate, 8),
            treatmentRate=round(treatment_rate, 8),
            absoluteEffect=round(effect, 8),
            relativeEffect=round(relative, 8),
            confidenceLower=round(lower, 8),
            confidenceUpper=round(upper, 8),
            pValue=round(p_value, 10),
            alphaAtLook=look.twoSidedAlpha,
            requiredSamplePerArm=required,
            achievedPower=round(achieved_power, 8),
            powered=powered,
            guardrails=guardrails,
            decision=decision,
            stoppingCriterionMet=stop,
            causalClaimAllowed=decision == "success" and snapshot.productionEvidence,
            blockingReasons=reasons,
        )

    def _validate_snapshot(self, snapshot: RankingAbSnapshot) -> None:
        if (
            snapshot.protocolVersion != self.policy.protocolVersion
            or snapshot.experimentKey != self.policy.experimentKey
            or snapshot.experimentDefinitionVersion != self.policy.experimentDefinitionVersion
            or snapshot.analysisSequence > len(self.policy.analysisLooks)
        ):
            raise ValueError("RANKING_AB_VERSION_OR_LOOK_MISMATCH")
        look = self.policy.analysisLooks[snapshot.analysisSequence - 1]
        if snapshot.elapsedDays != look.elapsedDays:
            raise ValueError("RANKING_AB_UNPLANNED_PEEK")
        if (
            snapshot.control.variantKey != self.policy.controlVariantKey
            or snapshot.treatment.variantKey != self.policy.treatmentVariantKey
            or snapshot.control.policyVersion != self.policy.controlPolicyVersion
            or snapshot.treatment.policyVersion != self.policy.treatmentPolicyVersion
            or snapshot.control.exposedSessions == 0
            or snapshot.treatment.exposedSessions == 0
        ):
            raise ValueError("RANKING_AB_ARM_MISMATCH")

    def _achieved_power(self, sample_per_arm: int) -> float:
        p1 = self.policy.baselineRate
        p2 = p1 + self.policy.minimumDetectableAbsoluteEffect
        pooled = (p1 + p2) / 2
        null_se = math.sqrt(2 * pooled * (1 - pooled) / sample_per_arm)
        alternative_se = math.sqrt((p1 * (1 - p1) + p2 * (1 - p2)) / sample_per_arm)
        threshold = NormalDist().inv_cdf(1 - self.policy.alpha / 2) * null_se
        return NormalDist().cdf((-threshold - (p2 - p1)) / alternative_se) + 1 - NormalDist().cdf((threshold - (p2 - p1)) / alternative_se)

    def _guardrails(self, snapshot: RankingAbSnapshot) -> list[GuardrailResult]:
        control, treatment = snapshot.control, snapshot.treatment
        treatment_allocation = treatment.assignedSessions / (
            control.assignedSessions + treatment.assignedSessions
        )
        expected_allocation = self.policy.allocationTreatmentBps / 10_000
        control_attendance = control.attendedBookings / control.maturedBookings if control.maturedBookings else 0
        treatment_attendance = treatment.attendedBookings / treatment.maturedBookings if treatment.maturedBookings else 0
        control_cancellation = control.cancelledBookings / control.maturedBookings if control.maturedBookings else 0
        treatment_cancellation = treatment.cancelledBookings / treatment.maturedBookings if treatment.maturedBookings else 0
        control_off_peak = control.offPeakImpressions / control.totalImpressions if control.totalImpressions else 0
        treatment_off_peak = treatment.offPeakImpressions / treatment.totalImpressions if treatment.totalImpressions else 0
        control_new = control.newVenueImpressions / control.totalImpressions if control.totalImpressions else 0
        treatment_new = treatment.newVenueImpressions / treatment.totalImpressions if treatment.totalImpressions else 0
        # Sin impresiones nuevas en control no existe un denominador comparable; el guardrail
        # permanece neutral y debe complementarse con volumen mínimo en una política posterior.
        new_ratio = treatment_new / control_new if control_new else 1.0
        values = [
            ("treatmentAllocationDeviation", abs(treatment_allocation - expected_allocation), self.policy.maximumTreatmentAllocationDeviation, "maximum"),
            ("controlExposureRate", control.exposedSessions / control.assignedSessions, self.policy.minimumExposureRate, "minimum"),
            ("treatmentExposureRate", treatment.exposedSessions / treatment.assignedSessions, self.policy.minimumExposureRate, "minimum"),
            ("attendanceAbsoluteChange", treatment_attendance - control_attendance, -self.policy.maximumAttendanceRateDecrease, "minimum"),
            ("cancellationAbsoluteChange", treatment_cancellation - control_cancellation, self.policy.maximumCancellationRateIncrease, "maximum"),
            ("offPeakShareAbsoluteChange", treatment_off_peak - control_off_peak, -self.policy.maximumOffPeakShareDecrease, "minimum"),
            ("newVenueExposureRatio", new_ratio, self.policy.minimumNewVenueExposureRatio, "minimum"),
            ("crossOverCount", float(snapshot.crossOverCount), 0.0, "maximum"),
            ("hardConstraintViolations", float(snapshot.hardConstraintViolations), 0.0, "maximum"),
            ("privacyViolations", float(snapshot.privacyViolations), 0.0, "maximum"),
        ]
        return [
            GuardrailResult(metric=metric, observed=round(observed, 8), required=required, passed=observed >= required if direction == "minimum" else observed <= required)
            for metric, observed, required, direction in values
        ]
