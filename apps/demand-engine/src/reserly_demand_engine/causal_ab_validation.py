"""Puerta de diseño A/B previa a cualquier estimador causal o heterogéneo."""

from __future__ import annotations

import math
from datetime import datetime
from pathlib import Path
from statistics import NormalDist
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator

from .contracts import StrictContract, Version


Arm = Literal["control", "treatment"]


class CausalAbPolicy(StrictContract):
    """Fija protocolo, balance, muestra e inventario de estimadores aún no autorizados."""

    schemaVersion: Literal[1]
    policyVersion: Version
    experimentPolicyVersion: Version
    outcomeDefinitionVersion: Version
    analysisFeatureSetVersion: Version
    preTreatmentFeatureNames: list[Version] = Field(min_length=1, max_length=32)
    minimumUnitsPerArm: int = Field(ge=30)
    minimumOutcomesPerArm: int = Field(ge=5)
    maximumAssignmentRatioDeviation: float = Field(ge=0, le=0.5)
    maximumAbsoluteStandardizedMeanDifference: float = Field(gt=0, le=1)
    confidenceLevel: float = Field(gt=0.5, lt=1)
    permittedEstimatorReviewsAfterGate: list[Version] = Field(min_length=1)
    prohibitedFeatureFragments: list[str] = Field(min_length=1)
    automaticEstimatorUseAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "CausalAbPolicy":
        collections = (
            self.preTreatmentFeatureNames,
            self.permittedEstimatorReviewsAfterGate,
        )
        if any(len(values) != len(set(values)) for values in collections):
            raise ValueError("CAUSAL_AB_POLICY_DUPLICATED_VALUE")
        prohibited = [fragment.casefold() for fragment in self.prohibitedFeatureFragments]
        if any(
            fragment in feature.casefold()
            for feature in self.preTreatmentFeatureNames
            for fragment in prohibited
        ):
            raise ValueError("CAUSAL_AB_POLICY_PROHIBITED_FEATURE")
        return self

    @classmethod
    def load(cls, path: Path) -> "CausalAbPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ExperimentalUnit(StrictContract):
    """Unidad seudónima asignada antes de exposición, con covariables pretratamiento y outcome maduro."""

    unitId: UUID
    arm: Arm
    assignedAt: datetime
    exposedAt: datetime
    outcomeObservedAt: datetime
    preTreatmentFeatures: dict[Version, float] = Field(min_length=1, max_length=32)
    completedBooking: Literal[0, 1]

    @model_validator(mode="after")
    def validate_unit(self) -> "ExperimentalUnit":
        if (
            any(value.tzinfo is None for value in (self.assignedAt, self.exposedAt, self.outcomeObservedAt))
            or not (self.assignedAt <= self.exposedAt <= self.outcomeObservedAt)
            or not all(math.isfinite(value) for value in self.preTreatmentFeatures.values())
        ):
            raise ValueError("CAUSAL_AB_UNIT_INVALID")
        return self


class CausalAbDataset(StrictContract):
    """Extracto de experimento finalizado que excluye diseños observacionales por tipo literal."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["causalAbDesignValidation"]
    experimentDesign: Literal["randomizedControlledAb"]
    experimentPolicyVersion: Version
    outcomeDefinitionVersion: Version
    analysisFeatureSetVersion: Version
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    preRegistered: Literal[True]
    stableRandomAssignment: Literal[True]
    mutuallyExclusiveAssignment: Literal[True]
    assignmentLoggedBeforeExposure: Literal[True]
    experimentCompleted: Literal[True]
    guardrailsPassed: Literal[True]
    units: list[ExperimentalUnit] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "CausalAbDataset":
        ids = [unit.unitId for unit in self.units]
        if (
            self.extractedAt.tzinfo is None
            or len(ids) != len(set(ids))
            or any(unit.outcomeObservedAt > self.extractedAt for unit in self.units)
        ):
            raise ValueError("CAUSAL_AB_DATASET_INVALID")
        return self


class ArmSummary(StrictContract):
    """Conteos y tasa por brazo sin información de unidad."""

    units: int = Field(ge=0)
    outcomes: int = Field(ge=0)
    rate: float = Field(ge=0, le=1)


class CausalAbValidationReport(StrictContract):
    """Diagnóstico del diseño y ATE; solo producción válida abre revisión de estimadores."""

    policyVersion: Version
    experimentPolicyVersion: Version
    datasetVersion: Version
    evaluatedAt: datetime
    control: ArmSummary
    treatment: ArmSummary
    assignmentRatioDeviation: float = Field(ge=0)
    absoluteStandardizedMeanDifferences: dict[Version, float]
    maximumObservedAbsoluteSmd: float = Field(ge=0)
    averageTreatmentEffect: float = Field(ge=-1, le=1)
    confidenceLower: float = Field(ge=-1, le=1)
    confidenceUpper: float = Field(ge=-1, le=1)
    pValue: float = Field(ge=0, le=1)
    designGatesPassed: bool
    productionEvidence: bool
    causalEstimationAllowed: bool
    permittedEstimatorReviews: list[Version]
    observationalAttributionOnly: bool
    automaticEstimatorUseAllowed: Literal[False]


class CausalAbValidator:
    """Valida un RCT A/B y bloquea estimadores avanzados hasta cumplir todo el protocolo."""

    def __init__(self, policy: CausalAbPolicy) -> None:
        self.policy = policy

    def validate(self, dataset: CausalAbDataset) -> CausalAbValidationReport:
        """Calcula balance y diferencia de medias sin convertir evidencia sintética en causal."""
        if (
            dataset.experimentPolicyVersion != self.policy.experimentPolicyVersion
            or dataset.outcomeDefinitionVersion != self.policy.outcomeDefinitionVersion
            or dataset.analysisFeatureSetVersion != self.policy.analysisFeatureSetVersion
        ):
            raise ValueError("CAUSAL_AB_VERSION_MISMATCH")
        expected_features = set(self.policy.preTreatmentFeatureNames)
        if any(set(unit.preTreatmentFeatures) != expected_features for unit in dataset.units):
            raise ValueError("CAUSAL_AB_FEATURE_SET_MISMATCH")
        control = [unit for unit in dataset.units if unit.arm == "control"]
        treatment = [unit for unit in dataset.units if unit.arm == "treatment"]
        self._validate_sample("CONTROL", control)
        self._validate_sample("TREATMENT", treatment)
        ratio_deviation = abs(len(treatment) / len(dataset.units) - 0.5)
        smds = {
            feature: _absolute_smd(
                [unit.preTreatmentFeatures[feature] for unit in control],
                [unit.preTreatmentFeatures[feature] for unit in treatment],
            )
            for feature in self.policy.preTreatmentFeatureNames
        }
        maximum_smd = max(smds.values())
        control_summary = _summary(control)
        treatment_summary = _summary(treatment)
        effect = treatment_summary.rate - control_summary.rate
        standard_error = math.sqrt(
            treatment_summary.rate * (1 - treatment_summary.rate) / treatment_summary.units
            + control_summary.rate * (1 - control_summary.rate) / control_summary.units
        )
        z_value = NormalDist().inv_cdf(0.5 + self.policy.confidenceLevel / 2)
        lower = max(-1.0, effect - z_value * standard_error)
        upper = min(1.0, effect + z_value * standard_error)
        z_score = effect / standard_error if standard_error > 0 else 0.0
        p_value = 2 * (1 - NormalDist().cdf(abs(z_score))) if standard_error > 0 else 1.0
        gates = (
            ratio_deviation <= self.policy.maximumAssignmentRatioDeviation
            and maximum_smd <= self.policy.maximumAbsoluteStandardizedMeanDifference
        )
        allowed = gates and dataset.productionEvidence
        return CausalAbValidationReport(
            policyVersion=self.policy.policyVersion,
            experimentPolicyVersion=self.policy.experimentPolicyVersion,
            datasetVersion=dataset.datasetVersion,
            evaluatedAt=dataset.extractedAt,
            control=control_summary,
            treatment=treatment_summary,
            assignmentRatioDeviation=round(ratio_deviation, 8),
            absoluteStandardizedMeanDifferences={key: round(value, 8) for key, value in smds.items()},
            maximumObservedAbsoluteSmd=round(maximum_smd, 8),
            averageTreatmentEffect=round(effect, 8),
            confidenceLower=round(lower, 8),
            confidenceUpper=round(upper, 8),
            pValue=round(p_value, 8),
            designGatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            causalEstimationAllowed=allowed,
            permittedEstimatorReviews=(self.policy.permittedEstimatorReviewsAfterGate if allowed else []),
            observationalAttributionOnly=not allowed,
            automaticEstimatorUseAllowed=False,
        )

    def _validate_sample(self, arm: str, units: list[ExperimentalUnit]) -> None:
        outcomes = sum(unit.completedBooking for unit in units)
        if (
            len(units) < self.policy.minimumUnitsPerArm
            or outcomes < self.policy.minimumOutcomesPerArm
            or len(units) - outcomes < self.policy.minimumOutcomesPerArm
        ):
            raise ValueError(f"CAUSAL_AB_{arm}_SAMPLE_INSUFFICIENT")


def _summary(units: list[ExperimentalUnit]) -> ArmSummary:
    outcomes = sum(unit.completedBooking for unit in units)
    return ArmSummary(units=len(units), outcomes=outcomes, rate=round(outcomes / len(units), 8))


def _absolute_smd(control: list[float], treatment: list[float]) -> float:
    control_values = np.asarray(control, dtype=np.float64)
    treatment_values = np.asarray(treatment, dtype=np.float64)
    pooled_variance = (float(np.var(control_values)) + float(np.var(treatment_values))) / 2
    mean_difference = abs(float(np.mean(treatment_values) - np.mean(control_values)))
    if pooled_variance <= 1e-15:
        return 0.0 if mean_difference <= 1e-15 else math.inf
    return mean_difference / math.sqrt(pooled_variance)
