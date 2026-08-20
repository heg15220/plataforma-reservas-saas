"""Factorization Machine binaria para evaluar interacciones dispersas frente a content-based."""

from __future__ import annotations

import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator

from .contracts import StrictContract, Version
from .conversion_training import ConversionMetrics, _metrics, _sigmoid


class FactorizationMachinePolicy(StrictContract):
    """Versiona splits, optimización, mejora incremental y fragmentos prohibidos."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    trainingEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumRowsPerSplit: int = Field(ge=20)
    minimumPositivePerSplit: int = Field(ge=1)
    minimumNegativePerSplit: int = Field(ge=1)
    latentDimensions: int = Field(ge=2, le=64)
    epochs: int = Field(ge=100, le=100_000)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0, le=10)
    randomSeed: int
    minimumRocAucGain: float = Field(ge=0, le=1)
    maximumLogLossRegression: float = Field(ge=0, le=1)
    maximumStabilityDelta: float = Field(ge=0, le=1)
    prohibitedFeatureFragments: list[str] = Field(min_length=1)
    automaticDeploymentAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "FactorizationMachinePolicy":
        if (
            self.trainingEndsBefore.tzinfo is None
            or self.evaluationEndsBefore.tzinfo is None
            or self.trainingEndsBefore >= self.evaluationEndsBefore
        ):
            raise ValueError("FM_POLICY_TIME_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "FactorizationMachinePolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class FactorizationMachineModelCard(StrictContract):
    """Gobierna finalidad, limitaciones, usos prohibidos y rollback del challenger."""

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
    def load(cls, path: Path) -> "FactorizationMachineModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class SparseInteractionRow(StrictContract):
    """Exposición minimizada con features binarias pre-outcome y baseline congelado."""

    observationId: UUID
    occurredAt: datetime
    outcomeObservedAt: datetime
    activeFeatureCodes: list[Version] = Field(min_length=2, max_length=64)
    contentBaselineProbability: float = Field(ge=0, le=1)
    converted: Literal[0, 1]

    @model_validator(mode="after")
    def validate_row(self) -> "SparseInteractionRow":
        if (
            self.occurredAt.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.occurredAt
            or len(self.activeFeatureCodes) != len(set(self.activeFeatureCodes))
        ):
            raise ValueError("FM_ROW_INVALID")
        return self


class SparseInteractionDataset(StrictContract):
    """Dataset temporal versionado sin PII y purgado de revocaciones."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["sparseInteractionEvaluation"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    featureVocabulary: list[Version] = Field(min_length=4, max_length=100_000)
    rows: list[SparseInteractionRow] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "SparseInteractionDataset":
        ids = [row.observationId for row in self.rows]
        vocabulary = set(self.featureVocabulary)
        if self.extractedAt.tzinfo is None or len(ids) != len(set(ids)):
            raise ValueError("FM_DATASET_INVALID")
        if len(vocabulary) != len(self.featureVocabulary):
            raise ValueError("FM_VOCABULARY_DUPLICATED")
        if any(not set(row.activeFeatureCodes) <= vocabulary for row in self.rows):
            raise ValueError("FM_FEATURE_OUTSIDE_VOCABULARY")
        if any(row.outcomeObservedAt > self.extractedAt for row in self.rows):
            raise ValueError("FM_DATASET_INVALID")
        return self


class FactorizationMachineArtifact(StrictContract):
    """Parámetros portables y decisión comparativa; nunca despliega automáticamente."""

    modelVersion: Version
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    datasetVersion: Version
    trainedAt: datetime
    featureVocabulary: list[Version]
    bias: float
    linearWeights: dict[Version, float]
    latentFactors: dict[Version, list[float]]
    baselineMetrics: ConversionMetrics
    candidateMetrics: ConversionMetrics
    rocAucGain: float
    logLossRegression: float
    stabilityMaximumDelta: float
    qualityGatesPassed: bool
    productionEvidence: bool
    promotionReviewAllowed: bool
    automaticDeploymentAllowed: Literal[False]
    modelCard: FactorizationMachineModelCard

    def predict(self, active_feature_codes: list[str]) -> float:
        """Evalúa solo un subconjunto conocido; no imputa ni deriva identidad latente."""
        if len(active_feature_codes) != len(set(active_feature_codes)):
            raise ValueError("FM_PREDICTION_FEATURE_DUPLICATED")
        if not set(active_feature_codes) <= set(self.featureVocabulary):
            raise ValueError("FM_PREDICTION_UNKNOWN_FEATURE")
        return _sigmoid(
            _raw_score(
                active_feature_codes,
                self.bias,
                self.linearWeights,
                self.latentFactors,
            )
        )


class FactorizationMachineEvaluator:
    """Entrena dos veces para estabilidad y compara en evaluación futura común."""

    def __init__(
        self,
        policy: FactorizationMachinePolicy,
        model_card: FactorizationMachineModelCard,
    ) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("FM_MODEL_CARD_VERSION_MISMATCH")

    def evaluate(self, dataset: SparseInteractionDataset) -> FactorizationMachineArtifact:
        """Devuelve candidato comparable; mejora sintética solo habilita revisión, nunca despliegue."""
        self._validate_features(dataset)
        train, evaluation = self._split(dataset.rows)
        self._validate_sample("TRAIN", train)
        self._validate_sample("EVALUATION", evaluation)
        first = self._fit(dataset.featureVocabulary, train)
        second = self._fit(dataset.featureVocabulary, train)
        vocabulary_map = {code: index for index, code in enumerate(dataset.featureVocabulary)}
        candidate_probabilities = [
            self._predict_parameters(row.activeFeatureCodes, first, vocabulary_map)
            for row in evaluation
        ]
        repeated_probabilities = [
            self._predict_parameters(row.activeFeatureCodes, second, vocabulary_map)
            for row in evaluation
        ]
        baseline_probabilities = [row.contentBaselineProbability for row in evaluation]
        labels = [row.converted for row in evaluation]
        candidate_metrics = _metrics(labels, candidate_probabilities)
        baseline_metrics = _metrics(labels, baseline_probabilities)
        auc_gain = candidate_metrics.rocAuc - baseline_metrics.rocAuc
        log_loss_regression = candidate_metrics.logLoss - baseline_metrics.logLoss
        stability = max(
            abs(first_value - second_value)
            for first_value, second_value in zip(candidate_probabilities, repeated_probabilities, strict=True)
        )
        gates = (
            auc_gain >= self.policy.minimumRocAucGain
            and log_loss_regression <= self.policy.maximumLogLossRegression
            and stability <= self.policy.maximumStabilityDelta
        )
        bias, linear, factors = first
        return FactorizationMachineArtifact(
            modelVersion=self.model_card.modelVersion,
            policyVersion=self.policy.policyVersion,
            algorithmVersion=self.policy.algorithmVersion,
            featureSetVersion=self.policy.featureSetVersion,
            datasetVersion=dataset.datasetVersion,
            trainedAt=dataset.extractedAt,
            featureVocabulary=dataset.featureVocabulary,
            bias=round(bias, 12),
            linearWeights={code: round(linear[index], 12) for index, code in enumerate(dataset.featureVocabulary)},
            latentFactors={code: [round(value, 12) for value in factors[index]] for index, code in enumerate(dataset.featureVocabulary)},
            baselineMetrics=baseline_metrics,
            candidateMetrics=candidate_metrics,
            rocAucGain=round(auc_gain, 8),
            logLossRegression=round(log_loss_regression, 8),
            stabilityMaximumDelta=round(stability, 12),
            qualityGatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            promotionReviewAllowed=gates and dataset.productionEvidence,
            automaticDeploymentAllowed=False,
            modelCard=self.model_card,
        )

    def _validate_features(self, dataset: SparseInteractionDataset) -> None:
        prohibited = [fragment.casefold() for fragment in self.policy.prohibitedFeatureFragments]
        if any(
            fragment in code.casefold()
            for code in dataset.featureVocabulary
            for fragment in prohibited
        ):
            raise ValueError("FM_PROHIBITED_FEATURE")

    def _split(self, rows: list[SparseInteractionRow]):
        train = [row for row in rows if row.occurredAt < self.policy.trainingEndsBefore]
        evaluation = [
            row
            for row in rows
            if self.policy.trainingEndsBefore <= row.occurredAt < self.policy.evaluationEndsBefore
        ]
        if len(train) + len(evaluation) != len(rows):
            raise ValueError("FM_ROW_OUTSIDE_SPLIT")
        if any(row.outcomeObservedAt >= self.policy.trainingEndsBefore for row in train):
            raise ValueError("FM_TRAIN_LABEL_NOT_MATURE")
        if any(row.outcomeObservedAt >= self.policy.evaluationEndsBefore for row in evaluation):
            raise ValueError("FM_EVALUATION_LABEL_NOT_MATURE")
        return train, evaluation

    def _validate_sample(self, split: str, rows: list[SparseInteractionRow]) -> None:
        positives = sum(row.converted for row in rows)
        if (
            len(rows) < self.policy.minimumRowsPerSplit
            or positives < self.policy.minimumPositivePerSplit
            or len(rows) - positives < self.policy.minimumNegativePerSplit
        ):
            raise ValueError(f"FM_{split}_SAMPLE_INSUFFICIENT")

    def _fit(self, vocabulary: list[str], rows: list[SparseInteractionRow]):
        indexes = {code: index for index, code in enumerate(vocabulary)}
        rng = np.random.default_rng(self.policy.randomSeed)
        linear = np.zeros(len(vocabulary), dtype=np.float64)
        factors = rng.normal(0, 0.05, (len(vocabulary), self.policy.latentDimensions))
        bias = 0.0
        ordered = sorted(rows, key=lambda row: (row.occurredAt, str(row.observationId)))
        for _ in range(self.policy.epochs):
            for row in ordered:
                active = [indexes[code] for code in row.activeFeatureCodes]
                raw = _raw_score_indexes(active, bias, linear, factors)
                error = _sigmoid(raw) - row.converted
                sums = factors[active].sum(axis=0)
                bias -= self.policy.learningRate * error
                for index in active:
                    old_factor = factors[index].copy()
                    linear[index] -= self.policy.learningRate * (
                        error + self.policy.l2Penalty * linear[index]
                    )
                    factors[index] -= self.policy.learningRate * (
                        error * (sums - old_factor) + self.policy.l2Penalty * old_factor
                    )
        return bias, linear, factors

    def _predict_parameters(self, codes: list[str], parameters, vocabulary_map) -> float:
        bias, linear, factors = parameters
        return _sigmoid(
            _raw_score_indexes_from_codes(codes, vocabulary_map, bias, linear, factors)
        )


def _raw_score_indexes(active, bias, linear, factors) -> float:
    score = bias + float(linear[active].sum())
    selected = factors[active]
    score += 0.5 * float(((selected.sum(axis=0) ** 2) - (selected**2).sum(axis=0)).sum())
    return score


def _raw_score_indexes_from_codes(codes, vocabulary_map, bias, linear, factors) -> float:
    return _raw_score_indexes([vocabulary_map[code] for code in codes], bias, linear, factors)


def _raw_score(codes, bias, linear_weights, latent_factors) -> float:
    score = bias + sum(linear_weights[code] for code in codes)
    dimensions = len(next(iter(latent_factors.values())))
    for factor in range(dimensions):
        values = [latent_factors[code][factor] for code in codes]
        score += 0.5 * (sum(values) ** 2 - sum(value * value for value in values))
    return score
