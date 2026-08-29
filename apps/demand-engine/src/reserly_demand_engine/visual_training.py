"""Entrenamiento gobernado de una cabeza lineal sobre embeddings CLIP congelados.

El módulo nunca ajusta CLIP ni abre el test durante la selección. Requiere etiquetas y autorización
humanas por activo, selecciona regularización únicamente con validación y evalúa test una sola vez.
"""

from __future__ import annotations

import argparse
import math
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Literal
from uuid import UUID

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


class VisualTrainingGates(StrictContract):
    """Umbrales de generalización fijados antes de abrir el test."""

    minimumTestAccuracy: float = Field(ge=0.9, le=1)
    maximumTestError: float = Field(ge=0, le=0.1)
    minimumMacroPrecision: float = Field(ge=0.8, le=1)
    minimumMacroRecall: float = Field(ge=0.8, le=1)
    minimumMacroF1: float = Field(ge=0.8, le=1)
    minimumPerCategoryRecall: float = Field(ge=0.5, le=1)
    maximumTrainTestAccuracyGap: float = Field(ge=0, le=0.1)
    syntheticPerfectAccuracyReviewThreshold: float = Field(gt=0.9, le=1)


class VisualTrainingPolicy(StrictContract):
    """Contrato inmutable de datos, optimización y aceptación de la cabeza visual."""

    schemaVersion: Literal[1]
    policyVersion: Version
    algorithmVersion: Version
    baseModelKey: Version
    baseModelRevision: str = Field(pattern=r"^[0-9a-f]{40}$")
    categories: list[Version] = Field(min_length=2, max_length=32)
    embeddingDimensions: int = Field(ge=2, le=4096)
    minimumTrainPerCategory: int = Field(ge=2, le=10_000)
    minimumValidationPerCategory: int = Field(ge=2, le=10_000)
    minimumTestPerCategory: int = Field(ge=2, le=10_000)
    learningRate: float = Field(gt=0, le=1)
    maximumEpochs: int = Field(ge=20, le=100_000)
    earlyStoppingPatience: int = Field(ge=5, le=10_000)
    l2Candidates: list[float] = Field(min_length=2, max_length=20)
    seed: int = Field(ge=0, le=2_147_483_647)
    gates: VisualTrainingGates
    humanReviewRequired: Literal[True]
    automaticPromotionAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_policy(self) -> "VisualTrainingPolicy":
        if (
            len(self.categories) != len(set(self.categories))
            or len(self.l2Candidates) != len(set(self.l2Candidates))
            or any(value < 0 or value > 10 for value in self.l2Candidates)
            or self.earlyStoppingPatience >= self.maximumEpochs
        ):
            raise ValueError("VISUAL_TRAINING_POLICY_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "VisualTrainingPolicy":
        """Carga la política sin defaults silenciosos."""

        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class VisualTrainingRow(StrictContract):
    """Embedding etiquetado y aprobado; no contiene píxeles ni texto libre."""

    imageId: UUID
    imageSha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    venueId: UUID
    categoryCode: Version
    split: Literal["train", "validation", "test"]
    embedding: list[float] = Field(min_length=2, max_length=4096)
    humanReviewStatus: Literal["approved"]
    developmentTrainingAllowed: Literal[True]

    @model_validator(mode="after")
    def validate_embedding(self) -> "VisualTrainingRow":
        if not all(math.isfinite(value) for value in self.embedding):
            raise ValueError("VISUAL_TRAINING_EMBEDDING_NON_FINITE")
        return self


class VisualTrainingDataset(StrictContract):
    """Dataset congelado con entidades disjuntas y procedencia CLIP exacta."""

    schemaVersion: Literal[1]
    datasetVersion: Version
    frozenAt: datetime
    baseModelKey: Version
    baseModelRevision: str = Field(pattern=r"^[0-9a-f]{40}$")
    synthetic: bool
    containsPersonalData: Literal[False]
    testPredictionsObservedDuringEmbedding: Literal[False] = False
    rows: list[VisualTrainingRow] = Field(min_length=1, max_length=1_000_000)

    @model_validator(mode="after")
    def validate_dataset(self) -> "VisualTrainingDataset":
        if self.frozenAt.tzinfo is None or self.frozenAt.utcoffset() is None:
            raise ValueError("VISUAL_TRAINING_DATASET_TIMEZONE_REQUIRED")
        image_ids = [row.imageId for row in self.rows]
        hashes = [row.imageSha256 for row in self.rows]
        if len(image_ids) != len(set(image_ids)) or len(hashes) != len(set(hashes)):
            raise ValueError("VISUAL_TRAINING_DATASET_DUPLICATE")
        venue_splits: dict[UUID, str] = {}
        for row in self.rows:
            previous = venue_splits.setdefault(row.venueId, row.split)
            if previous != row.split:
                raise ValueError("VISUAL_TRAINING_VENUE_LEAKAGE")
        return self

    @classmethod
    def load(cls, path: Path) -> "VisualTrainingDataset":
        """Carga embeddings gobernados desde JSON."""

        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class VisualCategoryMetric(StrictContract):
    """Métrica one-vs-rest por categoría."""

    categoryCode: Version
    precision: float = Field(ge=0, le=1)
    recall: float = Field(ge=0, le=1)
    f1: float = Field(ge=0, le=1)
    support: int = Field(ge=0)


class VisualSplitMetrics(StrictContract):
    """Métricas multiclase de un split inmutable."""

    rows: int = Field(ge=1)
    correct: int = Field(ge=0)
    accuracy: float = Field(ge=0, le=1)
    errorRate: float = Field(ge=0, le=1)
    macroPrecision: float = Field(ge=0, le=1)
    macroRecall: float = Field(ge=0, le=1)
    macroF1: float = Field(ge=0, le=1)
    perCategory: list[VisualCategoryMetric]
    confusionMatrix: dict[str, dict[str, int]]


class TrainedVisualArtifact(StrictContract):
    """Cabeza lineal portable con evidencia completa y promoción bloqueada."""

    schemaVersion: Literal[1]
    modelVersion: Version
    algorithmVersion: Version
    policyVersion: Version
    datasetVersion: Version
    baseModelKey: Version
    baseModelRevision: str
    trainedAt: datetime
    categories: list[Version]
    selectedL2: float
    completedEpochs: int
    weights: list[list[float]]
    biases: list[float]
    trainMetrics: VisualSplitMetrics
    validationMetrics: VisualSplitMetrics
    testMetrics: VisualSplitMetrics
    trainTestAccuracyGap: float
    testOpenedExactlyOnce: Literal[True]
    metricQualityPassed: bool
    generalizationPassed: bool
    benchmarkDifficultyReviewRequired: bool
    benchmarkAdequacyPassed: bool
    gatesPassed: bool
    syntheticEvidence: bool
    promotionAllowed: Literal[False]
    humanPromotionRequired: Literal[True]


class ClipLinearCategoryTrainer:
    """Selecciona L2 con validación y abre test solo tras congelar el candidato."""

    def __init__(self, policy: VisualTrainingPolicy) -> None:
        self.policy = policy

    def train(self, dataset: VisualTrainingDataset) -> TrainedVisualArtifact:
        """Entrena una cabeza softmax reproducible sobre embeddings CLIP congelados."""

        self._validate_readiness(dataset)
        splits = {
            name: [row for row in dataset.rows if row.split == name]
            for name in ("train", "validation", "test")
        }
        candidates: list[
            tuple[tuple[float, float, float], float, list[list[float]], list[float], int]
        ] = []
        for l2_penalty in sorted(self.policy.l2Candidates):
            weights, biases, epochs = self._fit(
                splits["train"], splits["validation"], l2_penalty
            )
            validation_metrics = self._metrics(splits["validation"], weights, biases)
            selection_key = (
                validation_metrics.macroF1,
                validation_metrics.accuracy,
                -l2_penalty,
            )
            candidates.append((selection_key, l2_penalty, weights, biases, epochs))
        _, selected_l2, weights, biases, epochs = max(candidates, key=lambda item: item[0])
        train_metrics = self._metrics(splits["train"], weights, biases)
        validation_metrics = self._metrics(splits["validation"], weights, biases)
        # Esta es la única llamada que calcula métricas de test: ocurre tras seleccionar L2.
        test_metrics = self._metrics(splits["test"], weights, biases)
        gates = self.policy.gates
        gap = round(abs(train_metrics.accuracy - test_metrics.accuracy), 8)
        metric_quality = (
            test_metrics.accuracy >= gates.minimumTestAccuracy
            and test_metrics.errorRate <= gates.maximumTestError
            and test_metrics.macroPrecision >= gates.minimumMacroPrecision
            and test_metrics.macroRecall >= gates.minimumMacroRecall
            and test_metrics.macroF1 >= gates.minimumMacroF1
            and min(metric.recall for metric in test_metrics.perCategory)
            >= gates.minimumPerCategoryRecall
        )
        generalization = gap <= gates.maximumTrainTestAccuracyGap
        difficulty_review = bool(
            dataset.synthetic
            and test_metrics.accuracy >= gates.syntheticPerfectAccuracyReviewThreshold
        )
        benchmark_adequacy = not difficulty_review
        return TrainedVisualArtifact(
            schemaVersion=1,
            modelVersion="clip-linear-category-head-v1",
            algorithmVersion=self.policy.algorithmVersion,
            policyVersion=self.policy.policyVersion,
            datasetVersion=dataset.datasetVersion,
            baseModelKey=dataset.baseModelKey,
            baseModelRevision=dataset.baseModelRevision,
            trainedAt=dataset.frozenAt,
            categories=self.policy.categories,
            selectedL2=selected_l2,
            completedEpochs=epochs,
            weights=[[round(value, 10) for value in row] for row in weights],
            biases=[round(value, 10) for value in biases],
            trainMetrics=train_metrics,
            validationMetrics=validation_metrics,
            testMetrics=test_metrics,
            trainTestAccuracyGap=gap,
            testOpenedExactlyOnce=True,
            metricQualityPassed=metric_quality,
            generalizationPassed=generalization,
            benchmarkDifficultyReviewRequired=difficulty_review,
            benchmarkAdequacyPassed=benchmark_adequacy,
            gatesPassed=metric_quality and generalization and benchmark_adequacy,
            syntheticEvidence=dataset.synthetic,
            promotionAllowed=False,
            humanPromotionRequired=True,
        )

    def _validate_readiness(self, dataset: VisualTrainingDataset) -> None:
        if (
            dataset.baseModelKey != self.policy.baseModelKey
            or dataset.baseModelRevision != self.policy.baseModelRevision
        ):
            raise ValueError("VISUAL_TRAINING_BASE_MODEL_MISMATCH")
        category_set = set(self.policy.categories)
        counts: dict[str, Counter[str]] = {
            split: Counter(row.categoryCode for row in dataset.rows if row.split == split)
            for split in ("train", "validation", "test")
        }
        minimums = {
            "train": self.policy.minimumTrainPerCategory,
            "validation": self.policy.minimumValidationPerCategory,
            "test": self.policy.minimumTestPerCategory,
        }
        for split, split_counts in counts.items():
            if set(split_counts) != category_set or any(
                split_counts[category] < minimums[split] for category in category_set
            ):
                raise ValueError("VISUAL_TRAINING_SPLIT_INSUFFICIENT")
        for row in dataset.rows:
            if (
                row.categoryCode not in category_set
                or len(row.embedding) != self.policy.embeddingDimensions
            ):
                raise ValueError("VISUAL_TRAINING_SCHEMA_MISMATCH")
            norm = math.sqrt(sum(value * value for value in row.embedding))
            if not 0.999 <= norm <= 1.001:
                raise ValueError("VISUAL_TRAINING_EMBEDDING_NOT_NORMALIZED")

    def _fit(
        self,
        train: list[VisualTrainingRow],
        validation: list[VisualTrainingRow],
        l2_penalty: float,
    ) -> tuple[list[list[float]], list[float], int]:
        import torch

        torch.manual_seed(self.policy.seed)
        torch.set_num_threads(1)
        category_index = {code: index for index, code in enumerate(self.policy.categories)}
        x_train = torch.tensor([row.embedding for row in train], dtype=torch.float32)
        y_train = torch.tensor(
            [category_index[row.categoryCode] for row in train], dtype=torch.long
        )
        x_validation = torch.tensor(
            [row.embedding for row in validation], dtype=torch.float32
        )
        y_validation = torch.tensor(
            [category_index[row.categoryCode] for row in validation], dtype=torch.long
        )
        weights = torch.zeros(
            (len(self.policy.categories), self.policy.embeddingDimensions), requires_grad=True
        )
        biases = torch.zeros(len(self.policy.categories), requires_grad=True)
        optimizer = torch.optim.SGD(
            [weights, biases], lr=self.policy.learningRate, weight_decay=l2_penalty
        )
        best_loss = math.inf
        best_weights = weights.detach().clone()
        best_biases = biases.detach().clone()
        patience = self.policy.earlyStoppingPatience
        completed_epochs = 0
        for epoch in range(1, self.policy.maximumEpochs + 1):
            optimizer.zero_grad()
            loss = torch.nn.functional.cross_entropy(x_train @ weights.T + biases, y_train)
            loss.backward()
            optimizer.step()
            with torch.inference_mode():
                validation_loss = float(
                    torch.nn.functional.cross_entropy(
                        x_validation @ weights.T + biases, y_validation
                    ).item()
                )
            completed_epochs = epoch
            if validation_loss < best_loss - 1e-7:
                best_loss = validation_loss
                best_weights = weights.detach().clone()
                best_biases = biases.detach().clone()
                patience = self.policy.earlyStoppingPatience
            else:
                patience -= 1
                if patience == 0:
                    break
        return best_weights.tolist(), best_biases.tolist(), completed_epochs

    def _metrics(
        self,
        rows: list[VisualTrainingRow],
        weights: list[list[float]],
        biases: list[float],
    ) -> VisualSplitMetrics:
        category_index = {code: index for index, code in enumerate(self.policy.categories)}
        predictions: list[int] = []
        actual: list[int] = []
        for row in rows:
            scores = [
                bias
                + sum(
                    weight * value
                    for weight, value in zip(class_weights, row.embedding, strict=True)
                )
                for class_weights, bias in zip(weights, biases, strict=True)
            ]
            predictions.append(max(range(len(scores)), key=scores.__getitem__))
            actual.append(category_index[row.categoryCode])
        correct = sum(
            expected == predicted
            for expected, predicted in zip(actual, predictions, strict=True)
        )
        confusion = [Counter() for _ in self.policy.categories]
        for expected, predicted in zip(actual, predictions, strict=True):
            confusion[expected][predicted] += 1
        category_metrics: list[VisualCategoryMetric] = []
        for index, code in enumerate(self.policy.categories):
            true_positive = confusion[index][index]
            false_positive = sum(
                confusion[other][index]
                for other in range(len(self.policy.categories))
                if other != index
            )
            false_negative = sum(
                count for predicted, count in confusion[index].items() if predicted != index
            )
            precision = (
                true_positive / (true_positive + false_positive)
                if true_positive + false_positive
                else 0.0
            )
            recall = (
                true_positive / (true_positive + false_negative)
                if true_positive + false_negative
                else 0.0
            )
            f1 = (
                2 * precision * recall / (precision + recall)
                if precision + recall
                else 0.0
            )
            category_metrics.append(
                VisualCategoryMetric(
                    categoryCode=code,
                    precision=round(precision, 8),
                    recall=round(recall, 8),
                    f1=round(f1, 8),
                    support=sum(confusion[index].values()),
                )
            )
        accuracy = correct / len(rows)
        return VisualSplitMetrics(
            rows=len(rows),
            correct=correct,
            accuracy=round(accuracy, 8),
            errorRate=round(1 - accuracy, 8),
            macroPrecision=round(
                sum(metric.precision for metric in category_metrics) / len(category_metrics), 8
            ),
            macroRecall=round(
                sum(metric.recall for metric in category_metrics) / len(category_metrics), 8
            ),
            macroF1=round(
                sum(metric.f1 for metric in category_metrics) / len(category_metrics), 8
            ),
            perCategory=category_metrics,
            confusionMatrix={
                actual_code: {
                    self.policy.categories[predicted]: count
                    for predicted, count in sorted(confusion[index].items())
                }
                for index, actual_code in enumerate(self.policy.categories)
            },
        )


def run() -> None:
    """Entrena desde un dataset aprobado y escribe un artefacto JSON candidato."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    policy = VisualTrainingPolicy.load(args.policy)
    dataset = VisualTrainingDataset.load(args.dataset)
    artifact = ClipLinearCategoryTrainer(policy).train(dataset)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(artifact.model_dump_json(indent=2) + "\n", encoding="utf-8")
    print(
        artifact.model_dump_json(
            include={
                "modelVersion",
                "datasetVersion",
                "testMetrics",
                "trainTestAccuracyGap",
                "gatesPassed",
                "promotionAllowed",
            }
        )
    )


if __name__ == "__main__":
    run()
