"""Uplift Doubly Robust cross-fitted con overlap, intervalos y sensibilidad explícita."""

from __future__ import annotations

import hashlib
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
Segment = Literal["newCustomer", "returningCustomer"]


class UpliftPolicy(StrictContract):
    """Versiona AIPW, cross-fitting, overlap, intervalos, sensibilidad y acción segura."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    causalGatePolicyVersion: Version
    featureSetVersion: Version
    featureNames: list[Version] = Field(min_length=1, max_length=32)
    allowedSegments: list[Segment] = Field(min_length=1)
    crossFitFolds: Literal[2]
    ridgePenalty: float = Field(gt=0, le=100)
    minimumUnitsPerArm: int = Field(ge=30)
    minimumUnitsPerSegmentArm: int = Field(ge=10)
    overlapLowerBound: float = Field(gt=0, lt=0.5)
    overlapUpperBound: float = Field(gt=0.5, lt=1)
    minimumOverlapCoverage: float = Field(ge=0, le=1)
    maximumInversePropensityWeight: float = Field(ge=1, le=100)
    confidenceLevel: float = Field(gt=0.5, lt=1)
    sensitivityAbsoluteOutcomeBias: float = Field(ge=0, le=1)
    minimumActionableUplift: float = Field(ge=0, le=1)
    automaticActionAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "UpliftPolicy":
        if (
            len(self.featureNames) != len(set(self.featureNames))
            or len(self.allowedSegments) != len(set(self.allowedSegments))
            or self.overlapLowerBound >= self.overlapUpperBound
        ):
            raise ValueError("UPLIFT_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "UpliftPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class UpliftModelCard(StrictContract):
    """Gobierna uso agregado, límites causales, aprobación y rollback observacional."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20)
    intendedUse: list[Version] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    status: Literal["candidate"]
    trainingPolicyVersion: Version
    featureSetVersion: Version
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "UpliftModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class UpliftUnit(StrictContract):
    """Unidad experimental minimizada con propensity conocida, features pretratamiento y outcome."""

    unitId: UUID
    arm: Arm
    assignmentPropensity: float = Field(gt=0, lt=1)
    assignedAt: datetime
    outcomeObservedAt: datetime
    featureValues: list[float] = Field(min_length=1, max_length=32)
    segment: Segment
    completedBooking: Literal[0, 1]

    @model_validator(mode="after")
    def validate_unit(self) -> "UpliftUnit":
        if (
            self.assignedAt.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.assignedAt > self.outcomeObservedAt
            or not all(math.isfinite(value) for value in self.featureValues)
        ):
            raise ValueError("UPLIFT_UNIT_INVALID")
        return self


class UpliftDataset(StrictContract):
    """RCT validado por 22.5, con atribución observacional adjunta pero nunca usada por AIPW."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["doublyRobustUpliftEvaluation"]
    experimentDesign: Literal["randomizedControlledAb"]
    causalGatePolicyVersion: Version
    causalGateValidated: Literal[True]
    preRegistered: Literal[True]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    featureSetVersion: Version
    featureNames: list[Version] = Field(min_length=1, max_length=32)
    observationalAttributionVersion: Version
    observationalAttributedRateDifference: float = Field(ge=-1, le=1)
    units: list[UpliftUnit] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "UpliftDataset":
        ids = [unit.unitId for unit in self.units]
        if (
            self.extractedAt.tzinfo is None
            or len(ids) != len(set(ids))
            or len(self.featureNames) != len(set(self.featureNames))
            or any(unit.outcomeObservedAt > self.extractedAt for unit in self.units)
            or any(len(unit.featureValues) != len(self.featureNames) for unit in self.units)
        ):
            raise ValueError("UPLIFT_DATASET_INVALID")
        return self


class UpliftEstimate(StrictContract):
    """Efecto medio e intervalo, con tamaño por brazo para interpretar precisión."""

    scope: str = Field(min_length=1, max_length=64)
    controlUnits: int = Field(ge=0)
    treatmentUnits: int = Field(ge=0)
    estimate: float = Field(ge=-1, le=1)
    standardError: float = Field(ge=0)
    confidenceLower: float = Field(ge=-1, le=1)
    confidenceUpper: float = Field(ge=-1, le=1)


class UpliftReport(StrictContract):
    """Informe causal/observacional separado y sin autoridad de targeting o contacto."""

    modelVersion: Version
    policyVersion: Version
    algorithmVersion: Version
    causalGatePolicyVersion: Version
    datasetVersion: Version
    evaluatedAt: datetime
    overall: UpliftEstimate
    segments: list[UpliftEstimate]
    overlapCoverage: float = Field(ge=0, le=1)
    minimumObservedPropensity: float = Field(gt=0, lt=1)
    maximumObservedPropensity: float = Field(gt=0, lt=1)
    maximumObservedInversePropensityWeight: float = Field(ge=1)
    overlapGatesPassed: bool
    sensitivityAbsoluteOutcomeBias: float = Field(ge=0, le=1)
    sensitivityLower: float = Field(ge=-1, le=1)
    sensitivityUpper: float = Field(ge=-1, le=1)
    signStableUnderSensitivity: bool
    observationalAttributionVersion: Version
    observationalAttributedRateDifference: float = Field(ge=-1, le=1)
    observationalAttributionUsedForUplift: Literal[False]
    productionEvidence: bool
    causalInterpretationAllowed: bool
    upliftActionReviewAllowed: bool
    automaticActionAllowed: Literal[False]
    modelCard: UpliftModelCard


class DoublyRobustUpliftEvaluator:
    """Calcula AIPW cross-fitted y bloquea interpretación sin RCT productivo y overlap."""

    def __init__(self, policy: UpliftPolicy, model_card: UpliftModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("UPLIFT_MODEL_CARD_VERSION_MISMATCH")

    def evaluate(self, dataset: UpliftDataset) -> UpliftReport:
        """Estima uplift sin mezclar el número observacional adjunto con pseudo-outcomes AIPW."""
        if (
            dataset.causalGatePolicyVersion != self.policy.causalGatePolicyVersion
            or dataset.featureSetVersion != self.policy.featureSetVersion
            or dataset.featureNames != self.policy.featureNames
        ):
            raise ValueError("UPLIFT_VERSION_MISMATCH")
        control = [unit for unit in dataset.units if unit.arm == "control"]
        treatment = [unit for unit in dataset.units if unit.arm == "treatment"]
        self._require_arm_sample("CONTROL", control, self.policy.minimumUnitsPerArm)
        self._require_arm_sample("TREATMENT", treatment, self.policy.minimumUnitsPerArm)
        scores = self._cross_fitted_scores(dataset.units)
        propensities = [unit.assignmentPropensity for unit in dataset.units]
        overlap_count = sum(
            self.policy.overlapLowerBound <= propensity <= self.policy.overlapUpperBound
            for propensity in propensities
        )
        overlap_coverage = overlap_count / len(propensities)
        maximum_weight = max(
            1 / unit.assignmentPropensity if unit.arm == "treatment" else 1 / (1 - unit.assignmentPropensity)
            for unit in dataset.units
        )
        overlap_gates = (
            overlap_coverage >= self.policy.minimumOverlapCoverage
            and maximum_weight <= self.policy.maximumInversePropensityWeight
        )
        overall = _estimate("all", dataset.units, scores, self.policy.confidenceLevel)
        segments: list[UpliftEstimate] = []
        for segment in self.policy.allowedSegments:
            indexes = [index for index, unit in enumerate(dataset.units) if unit.segment == segment]
            segment_units = [dataset.units[index] for index in indexes]
            segment_scores = [scores[index] for index in indexes]
            segment_control = [unit for unit in segment_units if unit.arm == "control"]
            segment_treatment = [unit for unit in segment_units if unit.arm == "treatment"]
            self._require_arm_sample(f"{segment}_CONTROL", segment_control, self.policy.minimumUnitsPerSegmentArm)
            self._require_arm_sample(f"{segment}_TREATMENT", segment_treatment, self.policy.minimumUnitsPerSegmentArm)
            segments.append(_estimate(segment, segment_units, segment_scores, self.policy.confidenceLevel))
        sensitivity_lower = max(-1.0, overall.estimate - self.policy.sensitivityAbsoluteOutcomeBias)
        sensitivity_upper = min(1.0, overall.estimate + self.policy.sensitivityAbsoluteOutcomeBias)
        sign_stable = sensitivity_lower > 0 or sensitivity_upper < 0
        causal_allowed = overlap_gates and dataset.productionEvidence
        action_review = (
            causal_allowed
            and overall.estimate >= self.policy.minimumActionableUplift
            and overall.confidenceLower > 0
            and sign_stable
        )
        return UpliftReport(
            modelVersion=self.model_card.modelVersion,
            policyVersion=self.policy.policyVersion,
            algorithmVersion=self.policy.algorithmVersion,
            causalGatePolicyVersion=self.policy.causalGatePolicyVersion,
            datasetVersion=dataset.datasetVersion,
            evaluatedAt=dataset.extractedAt,
            overall=overall,
            segments=segments,
            overlapCoverage=round(overlap_coverage, 8),
            minimumObservedPropensity=round(min(propensities), 8),
            maximumObservedPropensity=round(max(propensities), 8),
            maximumObservedInversePropensityWeight=round(maximum_weight, 8),
            overlapGatesPassed=overlap_gates,
            sensitivityAbsoluteOutcomeBias=self.policy.sensitivityAbsoluteOutcomeBias,
            sensitivityLower=round(sensitivity_lower, 8),
            sensitivityUpper=round(sensitivity_upper, 8),
            signStableUnderSensitivity=sign_stable,
            observationalAttributionVersion=dataset.observationalAttributionVersion,
            observationalAttributedRateDifference=dataset.observationalAttributedRateDifference,
            observationalAttributionUsedForUplift=False,
            productionEvidence=dataset.productionEvidence,
            causalInterpretationAllowed=causal_allowed,
            upliftActionReviewAllowed=action_review,
            automaticActionAllowed=False,
            modelCard=self.model_card,
        )

    def _cross_fitted_scores(self, units: list[UpliftUnit]) -> list[float]:
        scores = [0.0] * len(units)
        for fold in range(self.policy.crossFitFolds):
            evaluation_indexes = [index for index, unit in enumerate(units) if _fold(unit.unitId) == fold]
            training = [unit for unit in units if _fold(unit.unitId) != fold]
            if not evaluation_indexes:
                raise ValueError("UPLIFT_CROSS_FIT_FOLD_EMPTY")
            control_model = self._fit_outcome([unit for unit in training if unit.arm == "control"])
            treatment_model = self._fit_outcome([unit for unit in training if unit.arm == "treatment"])
            for index in evaluation_indexes:
                unit = units[index]
                context = np.asarray([1.0, *unit.featureValues], dtype=np.float64)
                m0 = float(np.clip(context @ control_model, 0.001, 0.999))
                m1 = float(np.clip(context @ treatment_model, 0.001, 0.999))
                treatment = 1.0 if unit.arm == "treatment" else 0.0
                propensity = unit.assignmentPropensity
                scores[index] = (
                    m1
                    - m0
                    + treatment * (unit.completedBooking - m1) / propensity
                    - (1 - treatment) * (unit.completedBooking - m0) / (1 - propensity)
                )
        return scores

    def _fit_outcome(self, units: list[UpliftUnit]) -> np.ndarray:
        if len(units) < self.policy.minimumUnitsPerSegmentArm:
            raise ValueError("UPLIFT_NUISANCE_SAMPLE_INSUFFICIENT")
        features = np.asarray([[1.0, *unit.featureValues] for unit in units], dtype=np.float64)
        labels = np.asarray([unit.completedBooking for unit in units], dtype=np.float64)
        penalty = np.eye(features.shape[1], dtype=np.float64) * self.policy.ridgePenalty
        penalty[0, 0] = 0.0
        return np.linalg.solve(features.T @ features + penalty, features.T @ labels)

    @staticmethod
    def _require_arm_sample(name: str, units: list[UpliftUnit], minimum: int) -> None:
        outcomes = sum(unit.completedBooking for unit in units)
        if len(units) < minimum or outcomes == 0 or outcomes == len(units):
            raise ValueError(f"UPLIFT_{name}_SAMPLE_INSUFFICIENT")


def _fold(unit_id: UUID) -> int:
    return int.from_bytes(hashlib.sha256(unit_id.bytes).digest()[:8], "big") % 2


def _estimate(
    scope: str, units: list[UpliftUnit], scores: list[float], confidence: float
) -> UpliftEstimate:
    values = np.asarray(scores, dtype=np.float64)
    effect = float(np.mean(values))
    standard_error = float(np.std(values, ddof=1) / math.sqrt(len(values)))
    z_value = NormalDist().inv_cdf(0.5 + confidence / 2)
    return UpliftEstimate(
        scope=scope,
        controlUnits=sum(unit.arm == "control" for unit in units),
        treatmentUnits=sum(unit.arm == "treatment" for unit in units),
        estimate=round(min(max(effect, -1.0), 1.0), 8),
        standardError=round(standard_error, 8),
        confidenceLower=round(max(-1.0, effect - z_value * standard_error), 8),
        confidenceUpper=round(min(1.0, effect + z_value * standard_error), 8),
    )
