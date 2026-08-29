"""Entrenamiento de desarrollo robusto tras consumir el test visual v2.

El test v2 deja de ser evidencia de aceptación y sus 200 filas pasan a desarrollo.
La selección usa validación cruzada estratificada y leave-one-source-out para evitar
premiar únicamente el estilo de un generador. El artefacto resultante no contiene
métricas de test y exige un holdout nuevo antes de cualquier afirmación de calidad.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np


def _metrics(actual: np.ndarray, predicted: np.ndarray, categories: list[str]) -> dict[str, Any]:
    per_category: list[dict[str, Any]] = []
    confusion: dict[str, dict[str, int]] = {category: {} for category in categories}
    for expected, observed in zip(actual, predicted, strict=True):
        expected_code = categories[int(expected)]
        observed_code = categories[int(observed)]
        confusion[expected_code][observed_code] = (
            confusion[expected_code].get(observed_code, 0) + 1
        )
    for index, category in enumerate(categories):
        true_positive = int(np.sum((actual == index) & (predicted == index)))
        false_positive = int(np.sum((actual != index) & (predicted == index)))
        false_negative = int(np.sum((actual == index) & (predicted != index)))
        precision = true_positive / (true_positive + false_positive) if true_positive + false_positive else 0.0
        recall = true_positive / (true_positive + false_negative) if true_positive + false_negative else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        per_category.append(
            {
                "categoryCode": category,
                "precision": round(precision, 8),
                "recall": round(recall, 8),
                "f1": round(f1, 8),
                "support": int(np.sum(actual == index)),
            }
        )
    accuracy = float(np.mean(actual == predicted))
    return {
        "rows": int(actual.size),
        "correct": int(np.sum(actual == predicted)),
        "accuracy": round(accuracy, 8),
        "errorRate": round(1 - accuracy, 8),
        "macroPrecision": round(float(np.mean([row["precision"] for row in per_category])), 8),
        "macroRecall": round(float(np.mean([row["recall"] for row in per_category])), 8),
        "macroF1": round(float(np.mean([row["f1"] for row in per_category])), 8),
        "perCategory": per_category,
        "confusionMatrix": confusion,
    }


def _fit_projection(features: np.ndarray, components: int) -> tuple[np.ndarray, np.ndarray]:
    """Ajusta PCA solo con el fold de entrenamiento para impedir leakage."""

    mean = np.mean(features, axis=0)
    _, _, right_vectors = np.linalg.svd(features - mean, full_matrices=False)
    return mean, right_vectors[:components].T


def _project(features: np.ndarray, mean: np.ndarray, projection: np.ndarray) -> np.ndarray:
    return (features - mean) @ projection


def _fit_ridge(features: np.ndarray, labels: np.ndarray, classes: int, penalty: float) -> np.ndarray:
    """Ajusta ridge multisalida en forma dual sobre las componentes reducidas."""

    augmented = np.column_stack([features, np.ones(features.shape[0])])
    targets = np.eye(classes, dtype=np.float64)[labels]
    kernel = augmented @ augmented.T + penalty * np.eye(augmented.shape[0])
    return augmented.T @ np.linalg.solve(kernel, targets)


def _predict(features: np.ndarray, coefficients: np.ndarray) -> np.ndarray:
    augmented = np.column_stack([features, np.ones(features.shape[0])])
    return np.argmax(augmented @ coefficients, axis=1)


def _stratified_folds(labels: np.ndarray, image_ids: list[str], count: int) -> np.ndarray:
    folds = np.empty(labels.size, dtype=np.int64)
    for category in sorted(set(labels.tolist())):
        indices = [index for index, value in enumerate(labels) if value == category]
        indices.sort(key=lambda index: image_ids[index])
        for ordinal, index in enumerate(indices):
            folds[index] = ordinal % count
    return folds


def train_robust_development_candidate(
    policy_path: Path,
    definition_path: Path,
    embeddings_path: Path,
    opening_record_path: Path,
    output_path: Path,
) -> dict[str, Any]:
    """Selecciona capacidad sin fingir que el dataset consumido sigue siendo test."""

    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    embeddings = json.loads(embeddings_path.read_text(encoding="utf-8"))
    opening = json.loads(opening_record_path.read_text(encoding="utf-8"))
    categories = policy.get("categories", [])
    penalties = policy.get("ridgeCandidates", [])
    component_candidates = policy.get("pcaComponentsCandidates", [])
    if (
        policy.get("schemaVersion") != 1
        or policy.get("independentTestRequired") is not True
        or policy.get("automaticPromotionAllowed") is not False
        or len(categories) < 2
        or len(categories) != len(set(categories))
        or policy.get("stratifiedFolds", 0) < 3
        or len(penalties) < 2
        or len(penalties) != len(set(penalties))
        or any(not isinstance(value, (int, float)) or value <= 0 for value in penalties)
        or len(component_candidates) < 2
        or len(component_candidates) != len(set(component_candidates))
        or any(
            not isinstance(value, int)
            or value < 2
            or value >= policy.get("embeddingDimensions", 0)
            for value in component_candidates
        )
        or policy.get("embeddingDimensions", 0) < 2
    ):
        raise ValueError("VISUAL_ROBUST_POLICY_INVALID")
    if (
        opening.get("datasetVersion") != policy.get("consumedDatasetVersion")
        or opening.get("openingNumber") != 1
        or opening.get("openingBudget") != 1
        or opening.get("remainingOpenings") != 0
        or definition.get("datasetVersion") != policy.get("consumedDatasetVersion")
        or embeddings.get("datasetVersion") != policy.get("consumedDatasetVersion")
        or embeddings.get("baseModelKey") != policy.get("baseModelKey")
        or embeddings.get("baseModelRevision") != policy.get("baseModelRevision")
        or embeddings.get("testPredictionsObservedDuringEmbedding") is not False
        or opening.get("approvedDefinitionSha256")
        != hashlib.sha256(definition_path.read_bytes()).hexdigest()
        or opening.get("embeddingDatasetSha256")
        != hashlib.sha256(embeddings_path.read_bytes()).hexdigest()
    ):
        raise ValueError("VISUAL_ROBUST_CONSUMED_DATASET_CONTRACT_INVALID")
    rows = embeddings.get("rows", [])
    metadata = {row["imageId"]: row for row in definition.get("rows", [])}
    if (
        len(rows) != 200
        or len(metadata) != 200
        or {row["imageId"] for row in rows} != set(metadata)
        or any(row.get("humanReviewStatus") != "approved" for row in rows)
    ):
        raise ValueError("VISUAL_ROBUST_DEVELOPMENT_DATASET_INVALID")
    category_index = {category: index for index, category in enumerate(categories)}
    if any(row["categoryCode"] not in category_index for row in rows):
        raise ValueError("VISUAL_ROBUST_CATEGORY_INVALID")
    features = np.asarray([row["embedding"] for row in rows], dtype=np.float64)
    if features.shape != (200, policy["embeddingDimensions"]):
        raise ValueError("VISUAL_ROBUST_DIMENSION_INVALID")
    norms = np.linalg.norm(features, axis=1)
    if not np.all(np.isfinite(features)) or np.any((norms < 0.999) | (norms > 1.001)):
        raise ValueError("VISUAL_ROBUST_EMBEDDING_INVALID")
    labels = np.asarray([category_index[row["categoryCode"]] for row in rows], dtype=np.int64)
    image_ids = [row["imageId"] for row in rows]
    sources = np.asarray([metadata[image_id]["source"] for image_id in image_ids])
    unique_sources = sorted(set(sources.tolist()))
    if len(unique_sources) < 3:
        raise ValueError("VISUAL_ROBUST_SOURCE_COVERAGE_INSUFFICIENT")

    folds = _stratified_folds(labels, image_ids, policy["stratifiedFolds"])
    candidates: list[dict[str, Any]] = []
    fitted_candidates: dict[tuple[int, float], tuple[np.ndarray, np.ndarray, np.ndarray]] = {}
    for components in policy["pcaComponentsCandidates"]:
        for penalty in policy["ridgeCandidates"]:
            oof_predictions = np.empty(labels.size, dtype=np.int64)
            fold_train_accuracies: list[float] = []
            for fold in range(policy["stratifiedFolds"]):
                train_mask = folds != fold
                validation_mask = ~train_mask
                mean, projection = _fit_projection(features[train_mask], components)
                projected_train = _project(features[train_mask], mean, projection)
                projected_validation = _project(features[validation_mask], mean, projection)
                coefficients = _fit_ridge(
                    projected_train, labels[train_mask], len(categories), penalty
                )
                oof_predictions[validation_mask] = _predict(
                    projected_validation, coefficients
                )
                fold_train_accuracies.append(
                    float(
                        np.mean(
                            _predict(projected_train, coefficients)
                            == labels[train_mask]
                        )
                    )
                )
            stratified = _metrics(labels, oof_predictions, categories)
            mean_train_accuracy = float(np.mean(fold_train_accuracies))

            source_predictions = np.empty(labels.size, dtype=np.int64)
            per_source: list[dict[str, Any]] = []
            for source in unique_sources:
                train_mask = sources != source
                held_out_mask = ~train_mask
                mean, projection = _fit_projection(features[train_mask], components)
                projected_train = _project(features[train_mask], mean, projection)
                projected_held_out = _project(features[held_out_mask], mean, projection)
                coefficients = _fit_ridge(
                    projected_train, labels[train_mask], len(categories), penalty
                )
                predicted = _predict(projected_held_out, coefficients)
                source_predictions[held_out_mask] = predicted
                per_source.append(
                    {
                        "source": source,
                        "metrics": _metrics(
                            labels[held_out_mask], predicted, categories
                        ),
                    }
                )
            source_held_out = _metrics(labels, source_predictions, categories)
            gap = round(abs(mean_train_accuracy - stratified["accuracy"]), 8)
            candidates.append(
                {
                    "pcaComponents": components,
                    "ridgePenalty": penalty,
                    "effectiveLinearParameters": len(categories) * (components + 1),
                    "meanFoldTrainAccuracy": round(mean_train_accuracy, 8),
                    "trainOofAccuracyGap": gap,
                    "stratifiedOofMetrics": stratified,
                    "sourceHeldOutMetrics": source_held_out,
                    "perSource": per_source,
                }
            )
            full_mean, full_projection = _fit_projection(features, components)
            full_coefficients = _fit_ridge(
                _project(features, full_mean, full_projection),
                labels,
                len(categories),
                penalty,
            )
            fitted_candidates[(components, penalty)] = (
                full_mean,
                full_projection,
                full_coefficients,
            )

    selected = max(
        candidates,
        key=lambda candidate: (
            candidate["sourceHeldOutMetrics"]["macroF1"],
            candidate["sourceHeldOutMetrics"]["accuracy"],
            candidate["stratifiedOofMetrics"]["macroF1"],
            -candidate["trainOofAccuracyGap"],
            -candidate["pcaComponents"],
            candidate["ridgePenalty"],
        ),
    )
    selected_penalty = selected["ridgePenalty"]
    selected_components = selected["pcaComponents"]
    feature_mean, projection, coefficients = fitted_candidates[
        (selected_components, selected_penalty)
    ]
    projected_features = _project(features, feature_mean, projection)
    training_metrics = _metrics(
        labels, _predict(projected_features, coefficients), categories
    )
    gates = policy["developmentGates"]
    development_gates_passed = (
        selected["stratifiedOofMetrics"]["accuracy"]
        >= gates["minimumStratifiedOofAccuracy"]
        and selected["stratifiedOofMetrics"]["macroF1"]
        >= gates["minimumStratifiedOofMacroF1"]
        and selected["sourceHeldOutMetrics"]["accuracy"]
        >= gates["minimumSourceHeldOutAccuracy"]
        and selected["trainOofAccuracyGap"] <= gates["maximumTrainOofAccuracyGap"]
    )
    artifact = {
        "schemaVersion": 1,
        "modelVersion": "clip-ridge-category-head-v2-development",
        "policyVersion": policy["policyVersion"],
        "algorithmVersion": policy["algorithmVersion"],
        "developmentDatasetVersion": policy["consumedDatasetVersion"],
        "consumedTestReclassifiedAsDevelopment": True,
        "developmentRows": len(rows),
        "sources": unique_sources,
        "categories": categories,
        "selectedPcaComponents": selected_components,
        "selectedRidgePenalty": selected_penalty,
        "selectionInputs": ["stratified-oof", "leave-one-source-out"],
        "candidateDiagnostics": candidates,
        "trainingMetrics": training_metrics,
        "stratifiedOofMetrics": selected["stratifiedOofMetrics"],
        "sourceHeldOutMetrics": selected["sourceHeldOutMetrics"],
        "trainOofAccuracyGap": selected["trainOofAccuracyGap"],
        "developmentGatesPassed": development_gates_passed,
        "featureMean": feature_mean.round(10).tolist(),
        "pcaProjection": projection.round(10).tolist(),
        "weights": coefficients[:-1].T.round(10).tolist(),
        "biases": coefficients[-1].round(10).tolist(),
        "testMetrics": None,
        "independentTestStatus": "required",
        "independentTestPredictionsObserved": False,
        "promotionAllowed": False,
    }
    output_path.write_text(
        json.dumps(artifact, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return artifact


def run() -> None:
    """CLI del entrenador robusto de desarrollo."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--definition", type=Path, required=True)
    parser.add_argument("--embeddings", type=Path, required=True)
    parser.add_argument("--opening-record", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    artifact = train_robust_development_candidate(
        args.policy,
        args.definition,
        args.embeddings,
        args.opening_record,
        args.output,
    )
    print(
        json.dumps(
            {
                "selectedRidgePenalty": artifact["selectedRidgePenalty"],
                "trainingAccuracy": artifact["trainingMetrics"]["accuracy"],
                "stratifiedOofAccuracy": artifact["stratifiedOofMetrics"]["accuracy"],
                "sourceHeldOutAccuracy": artifact["sourceHeldOutMetrics"]["accuracy"],
                "developmentGatesPassed": artifact["developmentGatesPassed"],
                "independentTestStatus": artifact["independentTestStatus"],
            }
        )
    )


if __name__ == "__main__":
    run()
