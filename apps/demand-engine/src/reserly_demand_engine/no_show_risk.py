"""Entrenamiento calibrado de riesgo de no-show con salida incapaz de automatizar sanciones."""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version
from .conversion_training import ConversionMetrics, _fit_logistic, _metrics, _sigmoid


class NoShowTrainingPolicy(StrictContract):
    """Contrato de features pre-outcome, splits, calibración y auditoría permitida."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    featureCodes: list[Version] = Field(min_length=1)
    prohibitedFeatureCodes: list[str] = Field(min_length=1)
    trainingEndsBefore: datetime
    calibrationEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumRowsPerSplit: int = Field(ge=10)
    minimumPositivePerSplit: int = Field(ge=1)
    minimumNegativePerSplit: int = Field(ge=1)
    gradientEpochs: int = Field(ge=100)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0)
    calibrationEpochs: int = Field(ge=100)
    minimumRocAuc: float = Field(ge=0, le=1)
    maximumBrier: float = Field(ge=0, le=1)
    maximumExpectedCalibrationError: float = Field(ge=0, le=1)
    minimumRowsPerAuditSegment: int = Field(ge=5)
    maximumSegmentBrierGap: float = Field(ge=0, le=1)
    signalTtlMinutes: int = Field(ge=1, le=1440)

    @model_validator(mode="after")
    def validate_policy(self) -> "NoShowTrainingPolicy":
        boundaries = [self.trainingEndsBefore, self.calibrationEndsBefore, self.evaluationEndsBefore]
        if any(value.tzinfo is None or value.utcoffset() is None for value in boundaries):
            raise ValueError("NO_SHOW_SPLIT_TIMEZONE_REQUIRED")
        if boundaries != sorted(boundaries) or len(set(self.featureCodes)) != len(self.featureCodes):
            raise ValueError("NO_SHOW_POLICY_INVALID")
        if set(self.featureCodes) & set(self.prohibitedFeatureCodes):
            raise ValueError("NO_SHOW_FEATURE_LEAKAGE_POLICY")
        return self

    @classmethod
    def load(cls, path: Path) -> "NoShowTrainingPolicy":
        """Carga una política completa sin sustituir valores ausentes."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class NoShowModelCard(StrictContract):
    """Gobierna finalidad y usos prohibidos del artefacto de riesgo."""

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
    def load(cls, path: Path) -> "NoShowModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class NoShowTrainingRow(StrictContract):
    """Outcome maduro y features operativas conocidas antes de la cita."""

    observationId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    auditSegment: Literal["es", "en"]
    features: dict[str, float]
    noShow: Literal[0, 1]

    @model_validator(mode="after")
    def validate_times(self) -> "NoShowTrainingRow":
        if self.occurredAt.tzinfo is None or self.outcomeObservedAt.tzinfo is None:
            raise ValueError("NO_SHOW_ROW_TIMEZONE_REQUIRED")
        if self.outcomeObservedAt < self.occurredAt:
            raise ValueError("NO_SHOW_OUTCOME_PRECEDES_OBSERVATION")
        return self


class NoShowDataset(StrictContract):
    """Cohorte analítica minimizada, versionada y purgada de revocaciones."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    purpose: Literal["aggregateOperationsAnalytics"]
    rows: list[NoShowTrainingRow] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "NoShowDataset":
        ids = [row.observationId for row in self.rows]
        if self.extractedAt.tzinfo is None or len(ids) != len(set(ids)):
            raise ValueError("NO_SHOW_DATASET_INVALID")
        if any(row.outcomeObservedAt > self.extractedAt for row in self.rows):
            raise ValueError("NO_SHOW_DATASET_INVALID")
        return self


class AuditBrier(StrictContract):
    """Calibración agregada de una cohorte permitida, nunca una feature."""

    segment: Literal["es", "en"]
    rows: int
    brier: float


class NoShowRiskSignal(StrictContract):
    """Señal efímera que declara explícitamente la ausencia de autoridad decisoria."""

    probability: float = Field(ge=0, le=1)
    modelVersion: Version
    policyVersion: Version
    generatedAt: datetime
    validUntil: datetime
    allowedUse: Literal["aggregateCapacityPlanning"]
    automatedActionAllowed: Literal[False]
    penaltyAllowed: Literal[False]
    bookingDenialAllowed: Literal[False]
    priceChangeAllowed: Literal[False]


class NoShowArtifact(StrictContract):
    """Parámetros portables, calibración, auditoría y gates del candidato."""

    modelVersion: Version
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    datasetVersion: Version
    trainedAt: datetime
    featureCodes: list[Version]
    means: dict[str, float]
    scales: dict[str, float]
    intercept: float
    coefficients: dict[str, float]
    calibrationSlope: float
    calibrationIntercept: float
    calibrationMetrics: ConversionMetrics
    evaluationMetrics: ConversionMetrics
    auditSegments: list[AuditBrier]
    maximumSegmentBrierGap: float
    gatesPassed: bool
    productionEvidence: bool
    promotionReviewAllowed: bool
    modelCard: NoShowModelCard
    signalTtlMinutes: int

    def signal(self, features: dict[str, float], generated_at: datetime) -> NoShowRiskSignal:
        """Calcula probabilidad sin aceptar IDs ni devolver una acción individual."""
        if generated_at.tzinfo is None or set(features) != set(self.featureCodes):
            raise ValueError("NO_SHOW_SIGNAL_INPUT_INVALID")
        if any(not math.isfinite(value) or not 0 <= value <= 1 for value in features.values()):
            raise ValueError("NO_SHOW_SIGNAL_INPUT_INVALID")
        raw = self.intercept + sum(
            self.coefficients[code] * (features[code] - self.means[code]) / self.scales[code]
            for code in self.featureCodes
        )
        probability = _sigmoid(self.calibrationSlope * raw + self.calibrationIntercept)
        return NoShowRiskSignal(
            probability=round(probability, 8),
            modelVersion=self.modelVersion,
            policyVersion=self.policyVersion,
            generatedAt=generated_at,
            validUntil=generated_at + timedelta(minutes=self.signalTtlMinutes),
            allowedUse="aggregateCapacityPlanning",
            automatedActionAllowed=False,
            penaltyAllowed=False,
            bookingDenialAllowed=False,
            priceChangeAllowed=False,
        )


class NoShowRiskTrainer:
    """Entrena logística en train, Platt en calibration y audita evaluation futura."""

    def __init__(self, policy: NoShowTrainingPolicy, model_card: NoShowModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("NO_SHOW_MODEL_CARD_VERSION_MISMATCH")

    def train(self, dataset: NoShowDataset) -> NoShowArtifact:
        """Genera candidato; evidencia sintética jamás habilita revisión de promoción."""
        self._validate_features(dataset.rows)
        train, calibration, evaluation = self._split(dataset.rows)
        for name, rows in (("train", train), ("calibration", calibration), ("evaluation", evaluation)):
            self._validate_sample(name, rows)
        means, scales = self._scaler(train)
        intercept, weights = _fit_logistic(
            self._matrix(train, means, scales),
            [row.noShow for row in train],
            self.policy.gradientEpochs,
            self.policy.learningRate,
            self.policy.l2Penalty,
        )
        calibration_logits = self._logits(calibration, means, scales, intercept, weights)
        calibration_intercept, calibration_weights = _fit_logistic(
            [[value] for value in calibration_logits],
            [row.noShow for row in calibration],
            self.policy.calibrationEpochs,
            self.policy.learningRate,
            self.policy.l2Penalty,
        )
        calibration_probabilities = [
            _sigmoid(calibration_intercept + calibration_weights[0] * value)
            for value in calibration_logits
        ]
        evaluation_probabilities = [
            _sigmoid(calibration_intercept + calibration_weights[0] * value)
            for value in self._logits(evaluation, means, scales, intercept, weights)
        ]
        calibration_metrics = _metrics([row.noShow for row in calibration], calibration_probabilities)
        evaluation_metrics = _metrics([row.noShow for row in evaluation], evaluation_probabilities)
        audits = self._audit(evaluation, evaluation_probabilities)
        gap = max(item.brier for item in audits) - min(item.brier for item in audits)
        gates = (
            evaluation_metrics.rocAuc >= self.policy.minimumRocAuc
            and evaluation_metrics.brier <= self.policy.maximumBrier
            and evaluation_metrics.expectedCalibrationError <= self.policy.maximumExpectedCalibrationError
            and gap <= self.policy.maximumSegmentBrierGap
        )
        return NoShowArtifact(
            modelVersion=self.model_card.modelVersion,
            policyVersion=self.policy.policyVersion,
            algorithmVersion=self.policy.algorithmVersion,
            featureSetVersion=self.policy.featureSetVersion,
            datasetVersion=dataset.datasetVersion,
            trainedAt=dataset.extractedAt,
            featureCodes=self.policy.featureCodes,
            means=means,
            scales=scales,
            intercept=round(intercept, 12),
            coefficients={code: round(value, 12) for code, value in zip(self.policy.featureCodes, weights, strict=True)},
            calibrationSlope=round(calibration_weights[0], 12),
            calibrationIntercept=round(calibration_intercept, 12),
            calibrationMetrics=calibration_metrics,
            evaluationMetrics=evaluation_metrics,
            auditSegments=audits,
            maximumSegmentBrierGap=round(gap, 8),
            gatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            promotionReviewAllowed=gates and dataset.productionEvidence,
            modelCard=self.model_card,
            signalTtlMinutes=self.policy.signalTtlMinutes,
        )

    def _validate_features(self, rows: list[NoShowTrainingRow]) -> None:
        expected = set(self.policy.featureCodes)
        prohibited = set(self.policy.prohibitedFeatureCodes)
        for row in rows:
            if set(row.features) != expected or set(row.features) & prohibited:
                raise ValueError("NO_SHOW_FEATURE_SCHEMA_OR_LEAKAGE")
            if any(not math.isfinite(value) or not 0 <= value <= 1 for value in row.features.values()):
                raise ValueError("NO_SHOW_FEATURE_VALUE_INVALID")

    def _split(self, rows: list[NoShowTrainingRow]):
        train = [row for row in rows if row.occurredAt < self.policy.trainingEndsBefore]
        calibration = [row for row in rows if self.policy.trainingEndsBefore <= row.occurredAt < self.policy.calibrationEndsBefore]
        evaluation = [row for row in rows if self.policy.calibrationEndsBefore <= row.occurredAt < self.policy.evaluationEndsBefore]
        if len(train) + len(calibration) + len(evaluation) != len(rows):
            raise ValueError("NO_SHOW_ROW_OUTSIDE_SPLIT_WINDOWS")
        for split, boundary in ((train, self.policy.trainingEndsBefore), (calibration, self.policy.calibrationEndsBefore), (evaluation, self.policy.evaluationEndsBefore)):
            if any(row.outcomeObservedAt >= boundary for row in split):
                raise ValueError("NO_SHOW_LABEL_NOT_MATURE_AT_SPLIT")
        return train, calibration, evaluation

    def _validate_sample(self, name: str, rows: list[NoShowTrainingRow]) -> None:
        positives = sum(row.noShow for row in rows)
        if len(rows) < self.policy.minimumRowsPerSplit or positives < self.policy.minimumPositivePerSplit or len(rows) - positives < self.policy.minimumNegativePerSplit:
            raise ValueError(f"NO_SHOW_{name.upper()}_SAMPLE_INSUFFICIENT")

    def _scaler(self, rows: list[NoShowTrainingRow]):
        means, scales = {}, {}
        for code in self.policy.featureCodes:
            values = [row.features[code] for row in rows]
            means[code] = sum(values) / len(values)
            scales[code] = max(math.sqrt(sum((value - means[code]) ** 2 for value in values) / len(values)), 1e-9)
        return means, scales

    def _matrix(self, rows, means, scales):
        return [[(row.features[code] - means[code]) / scales[code] for code in self.policy.featureCodes] for row in rows]

    def _logits(self, rows, means, scales, intercept, weights):
        return [intercept + sum(weight * value for weight, value in zip(weights, vector, strict=True)) for vector in self._matrix(rows, means, scales)]

    def _audit(self, rows: list[NoShowTrainingRow], probabilities: list[float]) -> list[AuditBrier]:
        result = []
        for segment in ("es", "en"):
            indexes = [index for index, row in enumerate(rows) if row.auditSegment == segment]
            if len(indexes) < self.policy.minimumRowsPerAuditSegment:
                raise ValueError("NO_SHOW_AUDIT_SEGMENT_SAMPLE_INSUFFICIENT")
            brier = sum((probabilities[index] - rows[index].noShow) ** 2 for index in indexes) / len(indexes)
            result.append(AuditBrier(segment=segment, rows=len(indexes), brier=round(brier, 8)))
        return result
