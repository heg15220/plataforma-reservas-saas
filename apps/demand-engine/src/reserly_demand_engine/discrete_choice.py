"""Modelo logit multinomial condicional sobre conjuntos completos de alternativas elegibles."""

from __future__ import annotations

import argparse
import math
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


Direction = Literal["positive", "negative"]


class ChoiceEvaluationGates(StrictContract):
    """Puertas offline para ajuste, discriminación e interpretación estable."""

    minimumTopOneAccuracy: float = Field(ge=0, le=1)
    minimumMcFaddenPseudoR2: float = Field(ge=0, le=1)
    maximumLogLoss: float = Field(ge=0)
    requireExpectedDirections: bool


class DiscreteChoicePolicy(StrictContract):
    """Contrato de features pre-choice, split temporal, optimización y signos esperados."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    featureSetVersion: Version
    featureCodes: list[Version] = Field(min_length=1, max_length=32)
    expectedDirections: dict[Version, Direction]
    prohibitedFeatureCodes: list[str] = Field(min_length=1)
    trainingEndsBefore: datetime
    evaluationEndsBefore: datetime
    minimumChoiceSetsPerSplit: int = Field(ge=10)
    minimumAlternativesPerSet: int = Field(ge=2, le=100)
    gradientEpochs: int = Field(ge=100, le=100_000)
    learningRate: float = Field(gt=0, le=1)
    l2Penalty: float = Field(ge=0, le=10)
    evaluationGates: ChoiceEvaluationGates

    @model_validator(mode="after")
    def validate_policy(self) -> "DiscreteChoicePolicy":
        if (
            self.trainingEndsBefore.tzinfo is None
            or self.evaluationEndsBefore.tzinfo is None
            or self.trainingEndsBefore >= self.evaluationEndsBefore
            or len(self.featureCodes) != len(set(self.featureCodes))
            or set(self.expectedDirections) != set(self.featureCodes)
            or set(self.featureCodes) & set(self.prohibitedFeatureCodes)
        ):
            raise ValueError("DISCRETE_CHOICE_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "DiscreteChoicePolicy":
        """Carga la política JSON estricta."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class DiscreteChoiceModelCard(StrictContract):
    """Finalidad, límites, prohibiciones y rollback del candidato interpretable."""

    schemaVersion: Literal[1]
    modelKey: Version
    modelVersion: Version
    owner: Version
    purpose: str = Field(min_length=20, max_length=500)
    status: Literal["candidate"]
    trainingPolicyVersion: Version
    featureSetVersion: Version
    limitations: list[str] = Field(min_length=1)
    prohibitedUse: list[Version] = Field(min_length=1)
    rollback: str = Field(min_length=20, max_length=500)
    humanApprovalRequired: Literal[True]

    @classmethod
    def load(cls, path: Path) -> "DiscreteChoiceModelCard":
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ChoiceAlternative(StrictContract):
    """Alternativa elegible observada antes de la elección; no incluye posición ni outcome."""

    alternativeId: UUID
    eligible: Literal[True]
    capacityAvailable: Literal[True]
    features: dict[str, float]


class ChoiceSet(StrictContract):
    """Conjunto completo con una alternativa elegida o la opción exterior."""

    choiceSetId: UUID
    occurredAt: datetime
    fullChoiceSetCaptured: Literal[True]
    candidateCount: int = Field(ge=2, le=100)
    alternatives: list[ChoiceAlternative] = Field(min_length=2, max_length=100)
    chosenAlternativeId: UUID | None = None
    outsideOptionChosen: bool

    @model_validator(mode="after")
    def validate_choice(self) -> "ChoiceSet":
        ids = [item.alternativeId for item in self.alternatives]
        if self.occurredAt.tzinfo is None or self.occurredAt.utcoffset() is None:
            raise ValueError("CHOICE_SET_TIMEZONE_REQUIRED")
        if len(ids) != len(set(ids)) or self.candidateCount != len(ids):
            raise ValueError("CHOICE_SET_CARDINALITY_INVALID")
        if self.outsideOptionChosen == (self.chosenAlternativeId is not None):
            raise ValueError("CHOICE_SET_OUTCOME_INVALID")
        if self.chosenAlternativeId is not None and self.chosenAlternativeId not in set(ids):
            raise ValueError("CHOICE_SET_CHOSEN_NOT_CAPTURED")
        return self


class ChoiceDataset(StrictContract):
    """Dataset minimizado, EUR y temporal, sin identidad ni alternativas ausentes."""

    datasetVersion: Version
    extractedAt: datetime
    currency: Literal["EUR"]
    productionEvidence: bool
    containsPersonalData: Literal[False]
    consentRevocationsApplied: Literal[True]
    purpose: Literal["analytics"]
    choiceSets: list[ChoiceSet] = Field(min_length=1, max_length=500_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "ChoiceDataset":
        ids = [item.choiceSetId for item in self.choiceSets]
        if self.extractedAt.tzinfo is None or self.extractedAt.utcoffset() is None:
            raise ValueError("CHOICE_DATASET_TIMEZONE_REQUIRED")
        if len(ids) != len(set(ids)) or any(item.occurredAt > self.extractedAt for item in self.choiceSets):
            raise ValueError("CHOICE_DATASET_INVALID")
        return self


class ChoiceMetrics(StrictContract):
    """Ajuste fuera de muestra frente al null uniforme con opción exterior."""

    choiceSets: int
    alternatives: int
    outsideChoices: int
    topOneAccuracy: float
    logLoss: float
    nullLogLoss: float
    mcfaddenPseudoR2: float


class CoefficientInterpretation(StrictContract):
    """Coeficiente en unidad original y razón de odds condicionada al mismo conjunto."""

    featureCode: Version
    coefficientPerUnit: float
    oddsRatioPerUnit: float
    expectedDirection: Direction
    directionMatches: bool


class ChoiceProbability(StrictContract):
    """Probabilidad condicional de una alternativa; UUID nulo representa no elegir."""

    alternativeId: UUID | None
    probability: float = Field(ge=0, le=1)


class DiscreteChoiceArtifact(StrictContract):
    """Parámetros JSON, interpretación y métricas del candidato logit condicional."""

    modelVersion: Version
    algorithmVersion: Version
    policyVersion: Version
    datasetVersion: Version
    featureSetVersion: Version
    trainedAt: datetime
    featureCodes: list[Version]
    intercept: float
    coefficients: dict[str, float]
    interpretations: list[CoefficientInterpretation]
    evaluationMetrics: ChoiceMetrics
    gatesPassed: bool
    productionEvidence: bool
    promotionAllowed: bool
    modelCard: DiscreteChoiceModelCard

    def probabilities(self, choice_set: ChoiceSet) -> list[ChoiceProbability]:
        """Calcula softmax incluyendo opción exterior y sin alterar el conjunto."""
        utilities = []
        for alternative in choice_set.alternatives:
            if set(alternative.features) != set(self.featureCodes):
                raise ValueError("CHOICE_FEATURE_SCHEMA_MISMATCH")
            utility = self.intercept + sum(
                self.coefficients[code] * alternative.features[code] for code in self.featureCodes
            )
            utilities.append(utility)
        alternative_probabilities, outside_probability = _softmax_with_outside(utilities)
        results = [
            ChoiceProbability(alternativeId=item.alternativeId, probability=round(probability, 12))
            for item, probability in zip(
                choice_set.alternatives, alternative_probabilities, strict=True
            )
        ]
        results.append(ChoiceProbability(alternativeId=None, probability=round(outside_probability, 12)))
        return results


class ConditionalLogitTrainer:
    """Ajusta utilidad relativa por máxima verosimilitud y evalúa en conjuntos futuros completos."""

    def __init__(self, policy: DiscreteChoicePolicy, model_card: DiscreteChoiceModelCard) -> None:
        self.policy = policy
        self.model_card = model_card
        if (
            model_card.trainingPolicyVersion != policy.policyVersion
            or model_card.featureSetVersion != policy.featureSetVersion
        ):
            raise ValueError("CHOICE_MODEL_CARD_VERSION_MISMATCH")

    def train(self, dataset: ChoiceDataset) -> DiscreteChoiceArtifact:
        """Entrena con el pasado, evalúa en futuro y mantiene promoción separada."""
        self._validate_features(dataset.choiceSets)
        training = [item for item in dataset.choiceSets if item.occurredAt < self.policy.trainingEndsBefore]
        evaluation = [
            item
            for item in dataset.choiceSets
            if self.policy.trainingEndsBefore <= item.occurredAt < self.policy.evaluationEndsBefore
        ]
        if len(training) + len(evaluation) != len(dataset.choiceSets):
            raise ValueError("CHOICE_SET_OUTSIDE_SPLIT_WINDOWS")
        if min(len(training), len(evaluation)) < self.policy.minimumChoiceSetsPerSplit:
            raise ValueError("CHOICE_SET_SAMPLE_INSUFFICIENT")
        means, scales = self._scaler(training)
        intercept, standardized = self._fit(training, means, scales)
        coefficients = {
            code: standardized[index] / scales[code]
            for index, code in enumerate(self.policy.featureCodes)
        }
        original_intercept = intercept - sum(
            standardized[index] * means[code] / scales[code]
            for index, code in enumerate(self.policy.featureCodes)
        )
        metrics = self._metrics(evaluation, original_intercept, coefficients)
        interpretations = [
            CoefficientInterpretation(
                featureCode=code,
                coefficientPerUnit=round(coefficients[code], 12),
                oddsRatioPerUnit=round(math.exp(max(min(coefficients[code], 50), -50)), 12),
                expectedDirection=self.policy.expectedDirections[code],
                directionMatches=(coefficients[code] > 0)
                == (self.policy.expectedDirections[code] == "positive"),
            )
            for code in self.policy.featureCodes
        ]
        gates = self.policy.evaluationGates
        directions_pass = all(item.directionMatches for item in interpretations)
        gates_passed = (
            metrics.topOneAccuracy >= gates.minimumTopOneAccuracy
            and metrics.mcfaddenPseudoR2 >= gates.minimumMcFaddenPseudoR2
            and metrics.logLoss <= gates.maximumLogLoss
            and (directions_pass or not gates.requireExpectedDirections)
        )
        return DiscreteChoiceArtifact(
            modelVersion=self.model_card.modelVersion,
            algorithmVersion=self.policy.algorithmVersion,
            policyVersion=self.policy.policyVersion,
            datasetVersion=dataset.datasetVersion,
            featureSetVersion=self.policy.featureSetVersion,
            trainedAt=dataset.extractedAt,
            featureCodes=self.policy.featureCodes,
            intercept=round(original_intercept, 12),
            coefficients={code: round(value, 12) for code, value in coefficients.items()},
            interpretations=interpretations,
            evaluationMetrics=metrics,
            gatesPassed=gates_passed,
            productionEvidence=dataset.productionEvidence,
            promotionAllowed=gates_passed and dataset.productionEvidence,
            modelCard=self.model_card,
        )

    def _validate_features(self, choice_sets: list[ChoiceSet]) -> None:
        expected = set(self.policy.featureCodes)
        prohibited = set(self.policy.prohibitedFeatureCodes)
        for choice_set in choice_sets:
            if len(choice_set.alternatives) < self.policy.minimumAlternativesPerSet:
                raise ValueError("CHOICE_SET_TOO_SMALL")
            for alternative in choice_set.alternatives:
                if set(alternative.features) != expected or set(alternative.features) & prohibited:
                    raise ValueError("CHOICE_FEATURE_SCHEMA_OR_LEAKAGE")
                if any(not math.isfinite(value) or value < 0 for value in alternative.features.values()):
                    raise ValueError("CHOICE_FEATURE_VALUE_INVALID")

    def _scaler(self, choice_sets: list[ChoiceSet]) -> tuple[dict[str, float], dict[str, float]]:
        means: dict[str, float] = {}
        scales: dict[str, float] = {}
        alternatives = [item for choice_set in choice_sets for item in choice_set.alternatives]
        for code in self.policy.featureCodes:
            values = [item.features[code] for item in alternatives]
            mean = sum(values) / len(values)
            variance = sum((value - mean) ** 2 for value in values) / len(values)
            means[code] = mean
            scales[code] = max(math.sqrt(variance), 1e-9)
        return means, scales

    def _fit(
        self, choice_sets: list[ChoiceSet], means: dict[str, float], scales: dict[str, float]
    ) -> tuple[float, list[float]]:
        weights = [0.0] * len(self.policy.featureCodes)
        intercept = 0.0
        count = len(choice_sets)
        for _ in range(self.policy.gradientEpochs):
            intercept_gradient = 0.0
            gradients = [0.0] * len(weights)
            for choice_set in choice_sets:
                vectors = [
                    [
                        (alternative.features[code] - means[code]) / scales[code]
                        for code in self.policy.featureCodes
                    ]
                    for alternative in choice_set.alternatives
                ]
                utilities = [
                    intercept + sum(weight * value for weight, value in zip(weights, vector, strict=True))
                    for vector in vectors
                ]
                probabilities, _ = _softmax_with_outside(utilities)
                chosen_index = next(
                    (
                        index
                        for index, alternative in enumerate(choice_set.alternatives)
                        if alternative.alternativeId == choice_set.chosenAlternativeId
                    ),
                    None,
                )
                intercept_gradient += (1.0 if chosen_index is not None else 0.0) - sum(probabilities)
                for feature_index in range(len(weights)):
                    observed = vectors[chosen_index][feature_index] if chosen_index is not None else 0.0
                    expected = sum(
                        probability * vector[feature_index]
                        for probability, vector in zip(probabilities, vectors, strict=True)
                    )
                    gradients[feature_index] += observed - expected
            intercept += self.policy.learningRate * intercept_gradient / count
            for index in range(len(weights)):
                weights[index] += self.policy.learningRate * (
                    gradients[index] / count - self.policy.l2Penalty * weights[index]
                )
        return intercept, weights

    def _metrics(
        self, choice_sets: list[ChoiceSet], intercept: float, coefficients: dict[str, float]
    ) -> ChoiceMetrics:
        losses: list[float] = []
        null_losses: list[float] = []
        correct = 0
        outside = 0
        alternatives_count = 0
        for choice_set in choice_sets:
            utilities = [
                intercept
                + sum(coefficients[code] * alternative.features[code] for code in self.policy.featureCodes)
                for alternative in choice_set.alternatives
            ]
            probabilities, outside_probability = _softmax_with_outside(utilities)
            alternatives_count += len(probabilities)
            if choice_set.outsideOptionChosen:
                chosen_probability = outside_probability
                outside += 1
                predicted_outside = outside_probability >= max(probabilities)
                correct += int(predicted_outside)
            else:
                chosen_index = next(
                    index
                    for index, item in enumerate(choice_set.alternatives)
                    if item.alternativeId == choice_set.chosenAlternativeId
                )
                chosen_probability = probabilities[chosen_index]
                correct += int(
                    chosen_probability >= outside_probability
                    and chosen_probability == max(probabilities)
                )
            losses.append(-math.log(max(chosen_probability, 1e-12)))
            null_losses.append(math.log(len(probabilities) + 1))
        log_loss = sum(losses) / len(losses)
        null_loss = sum(null_losses) / len(null_losses)
        return ChoiceMetrics(
            choiceSets=len(choice_sets),
            alternatives=alternatives_count,
            outsideChoices=outside,
            topOneAccuracy=round(correct / len(choice_sets), 8),
            logLoss=round(log_loss, 8),
            nullLogLoss=round(null_loss, 8),
            mcfaddenPseudoR2=round(1 - log_loss / null_loss, 8),
        )


def _softmax_with_outside(utilities: list[float]) -> tuple[list[float], float]:
    maximum = max([0.0, *utilities])
    exponentials = [math.exp(max(min(value - maximum, 50), -50)) for value in utilities]
    outside = math.exp(-maximum)
    denominator = outside + sum(exponentials)
    return [value / denominator for value in exponentials], outside / denominator


def run() -> None:
    """CLI offline que genera JSON candidato y nunca registra o promueve el modelo."""
    parser = argparse.ArgumentParser(description="Train conditional multinomial choice model")
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--model-card", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    dataset = ChoiceDataset.model_validate_json(arguments.dataset.read_text(encoding="utf-8"))
    artifact = ConditionalLogitTrainer(
        DiscreteChoicePolicy.load(arguments.policy),
        DiscreteChoiceModelCard.load(arguments.model_card),
    ).train(dataset)
    arguments.output.write_text(artifact.model_dump_json(indent=2), encoding="utf-8")
