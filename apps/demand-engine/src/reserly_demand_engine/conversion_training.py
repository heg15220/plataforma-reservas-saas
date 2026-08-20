"""Entrenamiento reproducible de regresión logística y calibración Platt con split temporal."""

from __future__ import annotations

import argparse
import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


class ConversionEvaluationGates(StrictContract):
    """Calidad mínima futura exigida antes de considerar promoción."""

    minimumRocAuc: float = Field(ge=0, le=1)
    maximumBrier: float = Field(ge=0, le=1)
    maximumExpectedCalibrationError: float = Field(ge=0, le=1)


class ConversionTrainingPolicy(StrictContract):
    """Features, ventanas, optimización y gates inmutables del baseline."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    featureCodes: list[Version] = Field(min_length=1, max_length=64)
    prohibitedFeatureCodes: list[str] = Field(min_length=1, max_length=100)
    trainingEndsBefore: datetime
    calibrationEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumRowsPerSplit: int = Field(ge=10)
    minimumPositivePerSplit: int = Field(ge=1)
    minimumNegativePerSplit: int = Field(ge=1)
    gradientEpochs: int = Field(ge=100, le=100_000)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0, le=10)
    calibrationEpochs: int = Field(ge=100, le=100_000)
    evaluationGates: ConversionEvaluationGates

    @model_validator(mode="after")
    def validate_policy(self) -> "ConversionTrainingPolicy":
        boundaries = [self.trainingEndsBefore, self.calibrationEndsBefore, self.evaluationEndsBefore]
        if any(value.tzinfo is None or value.utcoffset() is None for value in boundaries):
            raise ValueError("CONVERSION_SPLIT_TIMEZONE_REQUIRED")
        if boundaries != sorted(boundaries) or len(set(self.featureCodes)) != len(self.featureCodes):
            raise ValueError("CONVERSION_TRAINING_POLICY_INVALID")
        if set(self.featureCodes) & set(self.prohibitedFeatureCodes):
            raise ValueError("CONVERSION_FEATURE_LEAKAGE_POLICY")
        return self

    @classmethod
    def load(cls, path: Path) -> "ConversionTrainingPolicy":
        """Carga la política sin defaults silenciosos."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ConversionModelCard(StrictContract):
    """Declaración estable de finalidad, usos prohibidos, rollback y aprobación."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20, max_length=500)
    intendedUse: list[Version] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    status: Literal["candidate"]
    trainingPolicyVersion: Version
    featureSetVersion: Version
    limitations: list[str] = Field(min_length=1)
    rollback: str = Field(min_length=20, max_length=500)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "ConversionModelCard":
        """Carga la tarjeta base que acompañará a todos los artefactos de esta versión."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ConversionTrainingRow(StrictContract):
    """Observación minimizada con features disponibles antes de conocer la conversión."""

    observationId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    evaluationSegment: Literal["es", "en"]
    features: dict[str, float]
    converted: Literal[0, 1]

    @model_validator(mode="after")
    def validate_times(self) -> "ConversionTrainingRow":
        if self.occurredAt.tzinfo is None or self.outcomeObservedAt.tzinfo is None:
            raise ValueError("CONVERSION_ROW_TIMEZONE_REQUIRED")
        if self.outcomeObservedAt < self.occurredAt:
            raise ValueError("CONVERSION_OUTCOME_PRECEDES_OBSERVATION")
        return self


class ConversionDataset(StrictContract):
    """Dataset versionado; declara procedencia, privacidad, revocación y corte de extracción."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    purpose: Literal["analytics"]
    rows: list[ConversionTrainingRow] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "ConversionDataset":
        if self.extractedAt.tzinfo is None or self.extractedAt.utcoffset() is None:
            raise ValueError("CONVERSION_DATASET_TIMEZONE_REQUIRED")
        ids = [row.observationId for row in self.rows]
        if len(ids) != len(set(ids)) or any(row.outcomeObservedAt > self.extractedAt for row in self.rows):
            raise ValueError("CONVERSION_DATASET_INVALID")
        return self


class ConversionMetrics(StrictContract):
    """Métricas probabilísticas de una partición no reutilizada para ajustar esos parámetros."""

    rows: int
    positives: int
    rocAuc: float
    brier: float
    logLoss: float
    expectedCalibrationError: float


class TrainedConversionArtifact(StrictContract):
    """Artefacto portable JSON: parámetros, escalado, calibración, métricas y model card."""

    modelVersion: Version
    algorithmVersion: Version
    policyVersion: Version
    datasetVersion: Version
    featureSetVersion: Version
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
    productionEvidence: bool
    gatesPassed: bool
    promotionAllowed: bool
    modelCard: ConversionModelCard

    def predict(self, features: dict[str, float]) -> float:
        """Predice con allowlist exacta, escalado de train y calibrador congelado."""
        if set(features) != set(self.featureCodes):
            raise ValueError("CONVERSION_FEATURE_SCHEMA_MISMATCH")
        raw = self.intercept
        for code in self.featureCodes:
            raw += self.coefficients[code] * (features[code] - self.means[code]) / self.scales[code]
        return _sigmoid(self.calibrationSlope * raw + self.calibrationIntercept)


class ConversionLogisticTrainer:
    """Ajusta train, calibra en una segunda ventana y evalúa una sola vez en el futuro."""

    def __init__(self, policy: ConversionTrainingPolicy, model_card: ConversionModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("CONVERSION_MODEL_CARD_VERSION_MISMATCH")

    def train(self, dataset: ConversionDataset) -> TrainedConversionArtifact:
        """Produce un candidato determinista y nunca lo promueve sin evidencia productiva."""
        self._validate_features(dataset)
        train, calibration, evaluation = self._split(dataset.rows)
        for name, rows in (("train", train), ("calibration", calibration), ("evaluation", evaluation)):
            self._validate_split(name, rows)
        means, scales = self._scaler(train)
        x_train = self._matrix(train, means, scales)
        y_train = [row.converted for row in train]
        intercept, weights = _fit_logistic(
            x_train,
            y_train,
            self.policy.gradientEpochs,
            self.policy.learningRate,
            self.policy.l2Penalty,
        )
        calibration_logits = [
            intercept + sum(weight * value for weight, value in zip(weights, vector, strict=True))
            for vector in self._matrix(calibration, means, scales)
        ]
        calibration_intercept, calibration_weights = _fit_logistic(
            [[value] for value in calibration_logits],
            [row.converted for row in calibration],
            self.policy.calibrationEpochs,
            self.policy.learningRate,
            self.policy.l2Penalty,
        )
        calibration_probabilities = [
            _sigmoid(calibration_weights[0] * value + calibration_intercept)
            for value in calibration_logits
        ]
        evaluation_logits = [
            intercept + sum(weight * value for weight, value in zip(weights, vector, strict=True))
            for vector in self._matrix(evaluation, means, scales)
        ]
        evaluation_probabilities = [
            _sigmoid(calibration_weights[0] * value + calibration_intercept)
            for value in evaluation_logits
        ]
        calibration_metrics = _metrics(
            [row.converted for row in calibration], calibration_probabilities
        )
        evaluation_metrics = _metrics(
            [row.converted for row in evaluation], evaluation_probabilities
        )
        gates = self.policy.evaluationGates
        gates_passed = (
            evaluation_metrics.rocAuc >= gates.minimumRocAuc
            and evaluation_metrics.brier <= gates.maximumBrier
            and evaluation_metrics.expectedCalibrationError <= gates.maximumExpectedCalibrationError
        )
        return TrainedConversionArtifact(
            modelVersion=self.model_card.modelVersion,
            algorithmVersion=self.policy.algorithmVersion,
            policyVersion=self.policy.policyVersion,
            datasetVersion=dataset.datasetVersion,
            featureSetVersion=self.policy.featureSetVersion,
            trainedAt=dataset.extractedAt,
            featureCodes=self.policy.featureCodes,
            means=means,
            scales=scales,
            intercept=round(intercept, 12),
            coefficients={
                code: round(value, 12)
                for code, value in zip(self.policy.featureCodes, weights, strict=True)
            },
            calibrationSlope=round(calibration_weights[0], 12),
            calibrationIntercept=round(calibration_intercept, 12),
            calibrationMetrics=calibration_metrics,
            evaluationMetrics=evaluation_metrics,
            productionEvidence=dataset.productionEvidence,
            gatesPassed=gates_passed,
            promotionAllowed=gates_passed and dataset.productionEvidence,
            modelCard=self.model_card,
        )

    def _validate_features(self, dataset: ConversionDataset) -> None:
        expected = set(self.policy.featureCodes)
        prohibited = set(self.policy.prohibitedFeatureCodes)
        for row in dataset.rows:
            if set(row.features) != expected or set(row.features) & prohibited:
                raise ValueError("CONVERSION_FEATURE_SCHEMA_OR_LEAKAGE")
            if any(not math.isfinite(value) or value < 0 or value > 1 for value in row.features.values()):
                raise ValueError("CONVERSION_FEATURE_VALUE_INVALID")

    def _split(
        self, rows: list[ConversionTrainingRow]
    ) -> tuple[list[ConversionTrainingRow], list[ConversionTrainingRow], list[ConversionTrainingRow]]:
        train = [row for row in rows if row.occurredAt < self.policy.trainingEndsBefore]
        calibration = [
            row
            for row in rows
            if self.policy.trainingEndsBefore <= row.occurredAt < self.policy.calibrationEndsBefore
        ]
        evaluation = [
            row
            for row in rows
            if self.policy.calibrationEndsBefore
            <= row.occurredAt
            < self.policy.evaluationEndsBefore
        ]
        if len(train) + len(calibration) + len(evaluation) != len(rows):
            raise ValueError("CONVERSION_ROW_OUTSIDE_SPLIT_WINDOWS")
        maturity_boundaries = (
            (train, self.policy.trainingEndsBefore),
            (calibration, self.policy.calibrationEndsBefore),
            (evaluation, self.policy.evaluationEndsBefore),
        )
        if any(row.outcomeObservedAt >= boundary for split, boundary in maturity_boundaries for row in split):
            raise ValueError("CONVERSION_LABEL_NOT_MATURE_AT_SPLIT")
        return train, calibration, evaluation

    def _validate_split(self, name: str, rows: list[ConversionTrainingRow]) -> None:
        positives = sum(row.converted for row in rows)
        negatives = len(rows) - positives
        if (
            len(rows) < self.policy.minimumRowsPerSplit
            or positives < self.policy.minimumPositivePerSplit
            or negatives < self.policy.minimumNegativePerSplit
        ):
            raise ValueError(f"CONVERSION_{name.upper()}_SAMPLE_INSUFFICIENT")

    def _scaler(self, rows: list[ConversionTrainingRow]) -> tuple[dict[str, float], dict[str, float]]:
        means: dict[str, float] = {}
        scales: dict[str, float] = {}
        for code in self.policy.featureCodes:
            values = [row.features[code] for row in rows]
            mean = sum(values) / len(values)
            variance = sum((value - mean) ** 2 for value in values) / len(values)
            means[code] = mean
            scales[code] = max(math.sqrt(variance), 1e-9)
        return means, scales

    def _matrix(
        self,
        rows: list[ConversionTrainingRow],
        means: dict[str, float],
        scales: dict[str, float],
    ) -> list[list[float]]:
        return [
            [(row.features[code] - means[code]) / scales[code] for code in self.policy.featureCodes]
            for row in rows
        ]


def _fit_logistic(
    features: list[list[float]],
    labels: list[int],
    epochs: int,
    learning_rate: float,
    l2_penalty: float,
) -> tuple[float, list[float]]:
    """Gradiente batch determinista; devuelve intercepto y pesos sin serialización ejecutable."""
    weights = [0.0] * len(features[0])
    intercept = 0.0
    count = len(features)
    for _ in range(epochs):
        errors = [
            _sigmoid(intercept + sum(weight * value for weight, value in zip(weights, row, strict=True)))
            - label
            for row, label in zip(features, labels, strict=True)
        ]
        intercept -= learning_rate * sum(errors) / count
        for index in range(len(weights)):
            gradient = sum(error * row[index] for error, row in zip(errors, features, strict=True)) / count
            weights[index] -= learning_rate * (gradient + l2_penalty * weights[index])
    return intercept, weights


def _sigmoid(value: float) -> float:
    if value >= 0:
        inverse = math.exp(-value)
        return 1 / (1 + inverse)
    exponential = math.exp(value)
    return exponential / (1 + exponential)


def _metrics(labels: list[int], probabilities: list[float]) -> ConversionMetrics:
    clipped = [min(max(value, 1e-12), 1 - 1e-12) for value in probabilities]
    brier = sum(
        (probability - label) ** 2
        for label, probability in zip(labels, clipped, strict=True)
    ) / len(labels)
    log_loss = -sum(
        label * math.log(probability) + (1 - label) * math.log(1 - probability)
        for label, probability in zip(labels, clipped, strict=True)
    ) / len(labels)
    ece = 0.0
    for bucket in range(10):
        indexes = [
            index
            for index, probability in enumerate(clipped)
            if bucket / 10 <= probability < (bucket + 1) / 10 or (bucket == 9 and probability == 1)
        ]
        if indexes:
            confidence = sum(clipped[index] for index in indexes) / len(indexes)
            frequency = sum(labels[index] for index in indexes) / len(indexes)
            ece += len(indexes) / len(labels) * abs(confidence - frequency)
    return ConversionMetrics(
        rows=len(labels),
        positives=sum(labels),
        rocAuc=round(_roc_auc(labels, clipped), 8),
        brier=round(brier, 8),
        logLoss=round(log_loss, 8),
        expectedCalibrationError=round(ece, 8),
    )


def _roc_auc(labels: list[int], probabilities: list[float]) -> float:
    positives = [value for label, value in zip(labels, probabilities, strict=True) if label == 1]
    negatives = [value for label, value in zip(labels, probabilities, strict=True) if label == 0]
    wins = sum(
        1 if positive > negative else 0.5 if positive == negative else 0
        for positive in positives
        for negative in negatives
    )
    return wins / (len(positives) * len(negatives))


def run() -> None:
    """CLI offline: valida entradas JSON y escribe un artefacto portable, nunca lo promueve."""
    parser = argparse.ArgumentParser(description="Train calibrated conversion logistic baseline")
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--model-card", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    dataset = ConversionDataset.model_validate_json(arguments.dataset.read_text(encoding="utf-8"))
    trainer = ConversionLogisticTrainer(
        ConversionTrainingPolicy.load(arguments.policy),
        ConversionModelCard.load(arguments.model_card),
    )
    artifact = trainer.train(dataset)
    arguments.output.write_text(artifact.model_dump_json(indent=2), encoding="utf-8")
