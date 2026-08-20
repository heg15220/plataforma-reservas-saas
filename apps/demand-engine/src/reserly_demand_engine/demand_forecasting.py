"""Forecast agregado de demanda con XGBoost Poisson y calibración conformal temporal."""

from __future__ import annotations

import hashlib
import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

import numpy as np
from pydantic import Field, model_validator
from xgboost import XGBRegressor

from .contracts import StrictContract, Version


class DemandForecastPolicy(StrictContract):
    """Congela features, cortes, hiperparámetros, incertidumbre y gates incrementales."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    featureNames: list[Version] = Field(min_length=1, max_length=32)
    trainingEndsBefore: datetime
    calibrationEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumRowsPerSplit: int = Field(ge=30)
    nEstimators: int = Field(ge=10, le=2_000)
    maximumDepth: int = Field(ge=1, le=16)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0, le=100)
    randomSeed: int
    intervalConfidence: float = Field(gt=0.5, lt=1)
    minimumMaeImprovement: float = Field(ge=0, le=1)
    maximumWapeRegression: float = Field(ge=0, le=1)
    minimumIntervalCoverage: float = Field(ge=0, le=1)
    maximumIntervalWidthRatioToBaseline: float = Field(gt=0, le=10)
    maximumStabilityDelta: float = Field(ge=0, le=1)
    automaticDeploymentAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "DemandForecastPolicy":
        if (
            len(self.featureNames) != len(set(self.featureNames))
            or any(value.tzinfo is None for value in (
                self.trainingEndsBefore,
                self.calibrationEndsBefore,
                self.evaluationEndsBefore,
            ))
            or not (
                self.trainingEndsBefore
                < self.calibrationEndsBefore
                < self.evaluationEndsBefore
            )
        ):
            raise ValueError("DEMAND_FORECAST_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "DemandForecastPolicy":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DemandForecastModelCard(StrictContract):
    """Limita el forecast a planificación agregada y exige rollback/aprobación humana."""

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
    def load(cls, path: Path) -> "DemandForecastModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DemandForecastRow(StrictContract):
    """Bucket agregado con features conocidas al corte y baseline auditable congelado."""

    bucketId: UUID
    venueId: UUID
    categoryCode: Version
    bucketStart: datetime
    outcomeObservedAt: datetime
    featureValues: list[float] = Field(min_length=1, max_length=32)
    baselineForecast: float = Field(ge=0)
    baselineLower: float = Field(ge=0)
    baselineUpper: float = Field(ge=0)
    observedDemand: int = Field(ge=0, le=1_000_000)

    @model_validator(mode="after")
    def validate_row(self) -> "DemandForecastRow":
        if (
            self.bucketStart.tzinfo is None
            or self.outcomeObservedAt.tzinfo is None
            or self.outcomeObservedAt < self.bucketStart
            or self.baselineLower > self.baselineForecast
            or self.baselineForecast > self.baselineUpper
            or not all(math.isfinite(value) for value in self.featureValues)
        ):
            raise ValueError("DEMAND_FORECAST_ROW_INVALID")
        return self


class DemandForecastDataset(StrictContract):
    """Extracto agregado, temporal, sin PII y purgado de revocaciones."""

    datasetVersion: Version
    extractedAt: datetime
    productionEvidence: bool
    purpose: Literal["aggregateDemandForecastEvaluation"]
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    sourceTimezoneValidated: Literal[True]
    sourceQualityValidated: Literal[True]
    featureNames: list[Version] = Field(min_length=1, max_length=32)
    rows: list[DemandForecastRow] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "DemandForecastDataset":
        ids = [row.bucketId for row in self.rows]
        if (
            self.extractedAt.tzinfo is None
            or len(ids) != len(set(ids))
            or len(self.featureNames) != len(set(self.featureNames))
            or any(row.outcomeObservedAt > self.extractedAt for row in self.rows)
            or any(len(row.featureValues) != len(self.featureNames) for row in self.rows)
        ):
            raise ValueError("DEMAND_FORECAST_DATASET_INVALID")
        return self


class ForecastMetrics(StrictContract):
    """Error puntual y calidad del intervalo para una misma ventana futura."""

    mae: float = Field(ge=0)
    rmse: float = Field(ge=0)
    wape: float = Field(ge=0)
    intervalCoverage: float = Field(ge=0, le=1)
    meanIntervalWidth: float = Field(ge=0)


class DemandForecastReport(StrictContract):
    """Comparación champion/challenger portable sin autoridad de acción automática."""

    modelVersion: Version
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    datasetVersion: Version
    evaluatedAt: datetime
    baselineMetrics: ForecastMetrics
    candidateMetrics: ForecastMetrics
    maeImprovement: float
    wapeRegression: float
    intervalWidthRatioToBaseline: float = Field(ge=0)
    conformalAbsoluteResidual: float = Field(ge=0)
    stabilityMaximumDelta: float = Field(ge=0)
    modelSha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    qualityGatesPassed: bool
    productionEvidence: bool
    reliableForecast: bool
    promotionReviewAllowed: bool
    automaticActionAllowed: Literal[False]
    automaticDeploymentAllowed: Literal[False]
    modelCard: DemandForecastModelCard


class DemandForecastEvaluator:
    """Entrena dos boostings Poisson y calibra incertidumbre solo en ventana intermedia."""

    def __init__(self, policy: DemandForecastPolicy, model_card: DemandForecastModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("DEMAND_FORECAST_MODEL_CARD_VERSION_MISMATCH")

    def evaluate(self, dataset: DemandForecastDataset) -> DemandForecastReport:
        """Compara sobre futuro común; gates sintéticos nunca habilitan promoción ni acciones."""
        if dataset.featureNames != self.policy.featureNames:
            raise ValueError("DEMAND_FORECAST_FEATURE_VERSION_MISMATCH")
        train, calibration, evaluation = self._split(dataset.rows)
        for name, rows in (("TRAIN", train), ("CALIBRATION", calibration), ("EVALUATION", evaluation)):
            if len(rows) < self.policy.minimumRowsPerSplit:
                raise ValueError(f"DEMAND_FORECAST_{name}_SAMPLE_INSUFFICIENT")
        first = self._fit(train)
        second = self._fit(train)
        calibration_prediction = self._predict(first, calibration)
        residual = _conformal_residual(
            [row.observedDemand for row in calibration],
            calibration_prediction,
            self.policy.intervalConfidence,
        )
        candidate_prediction = self._predict(first, evaluation)
        repeated_prediction = self._predict(second, evaluation)
        candidate_lower = [max(0.0, prediction - residual) for prediction in candidate_prediction]
        candidate_upper = [prediction + residual for prediction in candidate_prediction]
        labels = [row.observedDemand for row in evaluation]
        baseline = [row.baselineForecast for row in evaluation]
        baseline_lower = [row.baselineLower for row in evaluation]
        baseline_upper = [row.baselineUpper for row in evaluation]
        baseline_metrics = _forecast_metrics(labels, baseline, baseline_lower, baseline_upper)
        candidate_metrics = _forecast_metrics(labels, candidate_prediction, candidate_lower, candidate_upper)
        mae_improvement = (
            (baseline_metrics.mae - candidate_metrics.mae) / baseline_metrics.mae
            if baseline_metrics.mae > 0
            else 0.0
        )
        wape_regression = candidate_metrics.wape - baseline_metrics.wape
        width_ratio = (
            candidate_metrics.meanIntervalWidth / baseline_metrics.meanIntervalWidth
            if baseline_metrics.meanIntervalWidth > 0
            else math.inf
        )
        stability = max(
            abs(first_value - second_value)
            for first_value, second_value in zip(candidate_prediction, repeated_prediction, strict=True)
        )
        gates = (
            mae_improvement >= self.policy.minimumMaeImprovement
            and wape_regression <= self.policy.maximumWapeRegression
            and candidate_metrics.intervalCoverage >= self.policy.minimumIntervalCoverage
            and width_ratio <= self.policy.maximumIntervalWidthRatioToBaseline
            and stability <= self.policy.maximumStabilityDelta
        )
        model_bytes = bytes(first.get_booster().save_raw(raw_format="json"))
        return DemandForecastReport(
            modelVersion=self.model_card.modelVersion,
            policyVersion=self.policy.policyVersion,
            algorithmVersion=self.policy.algorithmVersion,
            featureSetVersion=self.policy.featureSetVersion,
            datasetVersion=dataset.datasetVersion,
            evaluatedAt=dataset.extractedAt,
            baselineMetrics=baseline_metrics,
            candidateMetrics=candidate_metrics,
            maeImprovement=round(mae_improvement, 8),
            wapeRegression=round(wape_regression, 8),
            intervalWidthRatioToBaseline=round(width_ratio, 8),
            conformalAbsoluteResidual=round(residual, 8),
            stabilityMaximumDelta=round(stability, 12),
            modelSha256=hashlib.sha256(model_bytes).hexdigest(),
            qualityGatesPassed=gates,
            productionEvidence=dataset.productionEvidence,
            reliableForecast=gates and dataset.productionEvidence,
            promotionReviewAllowed=gates and dataset.productionEvidence,
            automaticActionAllowed=False,
            automaticDeploymentAllowed=False,
            modelCard=self.model_card,
        )

    def _split(self, rows: list[DemandForecastRow]):
        train = [row for row in rows if row.bucketStart < self.policy.trainingEndsBefore]
        calibration = [
            row
            for row in rows
            if self.policy.trainingEndsBefore <= row.bucketStart < self.policy.calibrationEndsBefore
        ]
        evaluation = [
            row
            for row in rows
            if self.policy.calibrationEndsBefore <= row.bucketStart < self.policy.evaluationEndsBefore
        ]
        if len(train) + len(calibration) + len(evaluation) != len(rows):
            raise ValueError("DEMAND_FORECAST_ROW_OUTSIDE_SPLIT")
        boundaries = (
            (train, self.policy.trainingEndsBefore, "TRAIN"),
            (calibration, self.policy.calibrationEndsBefore, "CALIBRATION"),
            (evaluation, self.policy.evaluationEndsBefore, "EVALUATION"),
        )
        for split, boundary, name in boundaries:
            if any(row.outcomeObservedAt >= boundary for row in split):
                raise ValueError(f"DEMAND_FORECAST_{name}_OUTCOME_NOT_MATURE")
        return train, calibration, evaluation

    def _fit(self, rows: list[DemandForecastRow]) -> XGBRegressor:
        ordered = sorted(rows, key=lambda row: (row.bucketStart, str(row.bucketId)))
        features = np.asarray([row.featureValues for row in ordered], dtype=np.float64)
        labels = np.asarray([row.observedDemand for row in ordered], dtype=np.float64)
        model = XGBRegressor(
            objective="count:poisson",
            eval_metric="poisson-nloglik",
            n_estimators=self.policy.nEstimators,
            max_depth=self.policy.maximumDepth,
            learning_rate=self.policy.learningRate,
            reg_lambda=self.policy.l2Penalty,
            subsample=1.0,
            colsample_bytree=1.0,
            tree_method="hist",
            random_state=self.policy.randomSeed,
            n_jobs=1,
            verbosity=0,
        )
        model.fit(features, labels, verbose=False)
        return model

    @staticmethod
    def _predict(model: XGBRegressor, rows: list[DemandForecastRow]) -> list[float]:
        features = np.asarray([row.featureValues for row in rows], dtype=np.float64)
        return [max(0.0, float(value)) for value in model.predict(features)]


def _conformal_residual(labels: list[int], predictions: list[float], confidence: float) -> float:
    residuals = np.abs(np.asarray(labels, dtype=np.float64) - np.asarray(predictions, dtype=np.float64))
    quantile = min(1.0, math.ceil((len(residuals) + 1) * confidence) / len(residuals))
    return float(np.quantile(residuals, quantile, method="higher"))


def _forecast_metrics(
    labels: list[int], predictions: list[float], lower: list[float], upper: list[float]
) -> ForecastMetrics:
    observed = np.asarray(labels, dtype=np.float64)
    forecast = np.asarray(predictions, dtype=np.float64)
    errors = observed - forecast
    denominator = max(float(observed.sum()), 1.0)
    coverage = np.mean(
        [low <= value <= high for value, low, high in zip(labels, lower, upper, strict=True)]
    )
    return ForecastMetrics(
        mae=round(float(np.mean(np.abs(errors))), 8),
        rmse=round(float(np.sqrt(np.mean(np.square(errors)))), 8),
        wape=round(float(np.abs(errors).sum() / denominator), 8),
        intervalCoverage=round(float(coverage), 8),
        meanIntervalWidth=round(float(np.mean(np.asarray(upper) - np.asarray(lower))), 8),
    )
