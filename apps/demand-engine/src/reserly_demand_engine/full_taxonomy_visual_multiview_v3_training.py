"""Selección multivista development-only del clasificador visual v3.

Reutiliza por hash las vistas A/B ya consumidas como desarrollo, extrae CLIP
solo para la vista C y rota A/B/C como validación. El holdout v3 no se abre,
no se codifica y conserva su presupuesto 1/1.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any

import numpy as np

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder


SEED = 22023
DEVELOPMENT_VERSION = "full-taxonomy-visual-multiview-development-v3"


def _sha256(path: Path) -> str:
    """Calcula el sello de un artefacto o imagen."""

    return hashlib.sha256(path.read_bytes()).hexdigest()


def _write(path: Path, payload: dict[str, Any], compact: bool = False) -> None:
    """Escribe un JSON determinista, creando únicamente su directorio padre."""

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(
            payload,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":") if compact else None,
            indent=None if compact else 2,
        )
        + "\n",
        encoding="utf-8",
    )


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    """Resuelve una imagen development sin permitir escapes de evaluation."""

    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_V3_TRAINING_PATH_ESCAPE")
    return path


def _metrics(y_true: np.ndarray, scores: np.ndarray, classes: np.ndarray) -> dict[str, Any]:
    """Calcula top-1, métricas macro, Recall@3 y recall por clase."""

    prediction = classes[np.argmax(scores, axis=1)]
    precision_values: list[float] = []
    recall_values: list[float] = []
    f1_values: list[float] = []
    per_class_recall: dict[str, float] = {}
    for name in classes:
        tp = int(np.sum((y_true == name) & (prediction == name)))
        fp = int(np.sum((y_true != name) & (prediction == name)))
        fn = int(np.sum((y_true == name) & (prediction != name)))
        precision = tp / (tp + fp) if tp + fp else 0.0
        recall = tp / (tp + fn) if tp + fn else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        precision_values.append(precision)
        recall_values.append(recall)
        f1_values.append(f1)
        per_class_recall[str(name)] = round(recall, 8)
    top_count = min(3, len(classes))
    top = classes[np.argsort(scores, axis=1)[:, -top_count:]]
    accuracy = float(np.mean(y_true == prediction))
    return {
        "accuracy": round(accuracy, 8),
        "error": round(1.0 - accuracy, 8),
        "macroPrecision": round(float(np.mean(precision_values)), 8),
        "macroRecall": round(float(np.mean(recall_values)), 8),
        "macroF1": round(float(np.mean(f1_values)), 8),
        "recallAt3": round(
            float(np.mean([expected in row for expected, row in zip(y_true, top, strict=True)])),
            8,
        ),
        "minimumClassRecall": round(min(per_class_recall.values()), 8),
        "perClassRecall": per_class_recall,
    }


def _mean_metrics(folds: list[dict[str, Any]]) -> dict[str, float]:
    """Promedia solo escalares comparables entre folds."""

    keys = (
        "accuracy",
        "error",
        "macroPrecision",
        "macroRecall",
        "macroF1",
        "recallAt3",
        "minimumClassRecall",
    )
    return {key: round(float(np.mean([fold[key] for fold in folds])), 8) for key in keys}


def _centers(features: np.ndarray, labels: np.ndarray, classes: np.ndarray) -> np.ndarray:
    """Ajusta centroides L2 por clase."""

    values = np.vstack([features[labels == name].mean(axis=0) for name in classes])
    return values / np.linalg.norm(values, axis=1, keepdims=True).clip(min=1e-12)


def _ridge_weights(
    features: np.ndarray, labels: np.ndarray, classes: np.ndarray, regularization: float
) -> np.ndarray:
    """Ajusta ridge multiclase balanceado en forma dual."""

    x = np.column_stack((features, np.ones(len(features))))
    y = np.zeros((len(features), len(classes)), dtype=np.float64)
    class_index = {name: index for index, name in enumerate(classes)}
    counts = Counter(labels.tolist())
    weights = np.asarray([len(labels) / (len(classes) * counts[name]) for name in labels])
    for row, name in enumerate(labels):
        y[row, class_index[name]] = 1.0
    root = np.sqrt(weights)[:, None]
    xw, yw = x * root, y * root
    dual = np.linalg.solve(xw @ xw.T + regularization * np.eye(len(xw)), yw)
    return xw.T @ dual


def _knn_scores(
    train_x: np.ndarray,
    train_y: np.ndarray,
    query_x: np.ndarray,
    classes: np.ndarray,
    k: int,
) -> np.ndarray:
    """Vota vecinos CLIP sin usar metadatos de la consulta."""

    similarities = query_x @ train_x.T
    nearest = np.argsort(similarities, axis=1)[:, -k:]
    result = np.zeros((len(query_x), len(classes)), dtype=np.float64)
    class_index = {name: index for index, name in enumerate(classes)}
    for query_index, neighbours in enumerate(nearest):
        for neighbour in neighbours:
            result[query_index, class_index[train_y[neighbour]]] += max(
                0.0, float(similarities[query_index, neighbour])
            ) + 1e-9
    return result


def _prototype_scores(
    train_x: np.ndarray,
    train_groups: np.ndarray,
    train_labels: np.ndarray,
    query_x: np.ndarray,
    classes: np.ndarray,
    reduction: str,
) -> np.ndarray:
    """Compara píxeles con prototipos aprendidos y agrega por etiqueta objetivo."""

    groups = np.unique(train_groups)
    prototypes = _centers(train_x, train_groups, groups)
    similarity = query_x @ prototypes.T
    class_index = {name: index for index, name in enumerate(classes)}
    group_label = {group: train_labels[np.flatnonzero(train_groups == group)[0]] for group in groups}
    result = np.full((len(query_x), len(classes)), -1.0, dtype=np.float64)
    for class_name in classes:
        indices = [index for index, group in enumerate(groups) if group_label[group] == class_name]
        values = similarity[:, indices]
        if reduction == "max":
            result[:, class_index[class_name]] = values.max(axis=1)
        elif reduction == "top3Mean":
            keep = min(3, values.shape[1])
            result[:, class_index[class_name]] = np.sort(values, axis=1)[:, -keep:].mean(axis=1)
        else:
            temperature = float(reduction.removeprefix("logMeanExp"))
            scaled = temperature * values
            maximum = scaled.max(axis=1, keepdims=True)
            result[:, class_index[class_name]] = (
                maximum[:, 0]
                + np.log(np.exp(scaled - maximum).mean(axis=1))
            ) / temperature
    return result


def _kernel_ridge_scores(
    train_x: np.ndarray,
    train_y: np.ndarray,
    query_x: np.ndarray,
    classes: np.ndarray,
    gamma: float,
    regularization: float,
) -> np.ndarray:
    """Ajusta kernel ridge RBF balanceado sobre similitud coseno CLIP."""

    train_kernel = np.exp(gamma * (train_x @ train_x.T - 1.0))
    query_kernel = np.exp(gamma * (query_x @ train_x.T - 1.0))
    class_index = {name: index for index, name in enumerate(classes)}
    counts = Counter(train_y.tolist())
    targets = np.zeros((len(train_x), len(classes)), dtype=np.float64)
    for row, name in enumerate(train_y):
        targets[row, class_index[name]] = len(train_y) / (len(classes) * counts[name])
    dual = np.linalg.solve(
        train_kernel + regularization * np.eye(len(train_kernel)), targets
    )
    return query_kernel @ dual


def _pca_ridge_parameters(
    features: np.ndarray,
    labels: np.ndarray,
    classes: np.ndarray,
    dimensions: int,
    regularization: float,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Ajusta una proyección PCA development-only y una cabeza ridge."""

    mean = features.mean(axis=0)
    _, _, right = np.linalg.svd(features - mean, full_matrices=False)
    components = right[: min(dimensions, len(right))]
    projected = (features - mean) @ components.T
    weights = _ridge_weights(projected, labels, classes, regularization)
    return mean, components, weights


def _lda_parameters(
    features: np.ndarray,
    labels: np.ndarray,
    classes: np.ndarray,
    shrinkage: float,
) -> tuple[np.ndarray, np.ndarray]:
    """Ajusta discriminante lineal con covarianza intra-clase regularizada."""

    means = np.vstack([features[labels == name].mean(axis=0) for name in classes])
    residuals = np.vstack(
        [features[labels == name] - means[index] for index, name in enumerate(classes)]
    )
    covariance = residuals.T @ residuals / max(1, len(residuals) - len(classes))
    scale = float(np.trace(covariance) / covariance.shape[0])
    regularized = covariance + shrinkage * max(scale, 1e-9) * np.eye(covariance.shape[0])
    weights = np.linalg.solve(regularized, means.T).T
    intercept = -0.5 * np.sum(weights * means, axis=1)
    return weights, intercept


def _candidate_scores(
    candidate: dict[str, Any],
    train_x: np.ndarray,
    train_y: np.ndarray,
    train_types: np.ndarray,
    query_x: np.ndarray,
    classes: np.ndarray,
    train_archetypes: np.ndarray | None = None,
) -> np.ndarray:
    """Evalúa una arquitectura usando exclusivamente embeddings de píxeles."""

    if candidate["kind"] == "centroid":
        return query_x @ _centers(train_x, train_y, classes).T
    if candidate["kind"] == "knn":
        return _knn_scores(train_x, train_y, query_x, classes, candidate["k"])
    if candidate["kind"] == "ridge":
        weights = _ridge_weights(train_x, train_y, classes, candidate["regularization"])
        return np.column_stack((query_x, np.ones(len(query_x)))) @ weights
    if candidate["kind"] == "kernelRidge":
        return _kernel_ridge_scores(
            train_x,
            train_y,
            query_x,
            classes,
            candidate["gamma"],
            candidate["regularization"],
        )
    if candidate["kind"] == "pcaRidge":
        mean, components, weights = _pca_ridge_parameters(
            train_x,
            train_y,
            classes,
            candidate["dimensions"],
            candidate["regularization"],
        )
        projected = (query_x - mean) @ components.T
        return np.column_stack((projected, np.ones(len(projected)))) @ weights
    if candidate["kind"] == "lda":
        weights, intercept = _lda_parameters(
            train_x, train_y, classes, candidate["shrinkage"]
        )
        return query_x @ weights.T + intercept
    if candidate["kind"] == "archetypeFusion":
        if train_archetypes is None:
            raise ValueError("FULL_TAXONOMY_V3_AUXILIARY_LABELS_MISSING")
        base = _prototype_scores(
            train_x, train_types, train_y, query_x, classes, "max"
        )
        archetype_classes = np.unique(train_archetypes)
        aux_weights, aux_intercept = _lda_parameters(
            train_x, train_archetypes, archetype_classes, 1.0
        )
        aux_logits = query_x @ aux_weights.T + aux_intercept
        aux_logits -= aux_logits.max(axis=1, keepdims=True)
        aux_probabilities = np.exp(aux_logits)
        aux_probabilities /= aux_probabilities.sum(axis=1, keepdims=True).clip(min=1e-12)
        mapping = np.zeros((len(archetype_classes), len(classes)), dtype=np.float64)
        class_index = {name: index for index, name in enumerate(classes)}
        for archetype_index, archetype in enumerate(archetype_classes):
            family_counts = Counter(train_y[train_archetypes == archetype].tolist())
            total = sum(family_counts.values())
            for family, count in family_counts.items():
                mapping[archetype_index, class_index[family]] = count / total
        auxiliary = aux_probabilities @ mapping
        base = (base - base.mean(axis=1, keepdims=True)) / base.std(
            axis=1, keepdims=True
        ).clip(min=1e-9)
        auxiliary = (auxiliary - auxiliary.mean(axis=1, keepdims=True)) / auxiliary.std(
            axis=1, keepdims=True
        ).clip(min=1e-9)
        return base + candidate["alpha"] * auxiliary
    return _prototype_scores(
        train_x,
        train_types,
        train_y,
        query_x,
        classes,
        candidate["reduction"],
    )


def _candidates(include_auxiliary_fusion: bool = False) -> list[dict[str, Any]]:
    """Devuelve la búsqueda fijada antes de observar holdout v3."""

    candidates = (
        [{"key": "centroid", "kind": "centroid"}]
        + [{"key": f"knn-k{k}", "kind": "knn", "k": k} for k in (1, 3, 5, 7)]
        + [
            {"key": f"ridge-{value:g}", "kind": "ridge", "regularization": value}
            for value in (0.1, 1.0, 10.0, 100.0)
        ]
        + [
            {"key": f"type-prototype-{reduction}", "kind": "typePrototype", "reduction": reduction}
            for reduction in ("max", "top3Mean", "logMeanExp10", "logMeanExp20")
        ]
        + [
            {
                "key": f"rbf-g{gamma:g}-r{regularization:g}",
                "kind": "kernelRidge",
                "gamma": gamma,
                "regularization": regularization,
            }
            for gamma in (2.0, 5.0, 10.0)
            for regularization in (0.01, 0.1, 1.0)
        ]
        + [
            {
                "key": f"pca{dimensions}-ridge{regularization:g}",
                "kind": "pcaRidge",
                "dimensions": dimensions,
                "regularization": regularization,
            }
            for dimensions in (32, 64, 128)
            for regularization in (0.1, 1.0)
        ]
        + [
            {"key": f"lda-{shrinkage:g}", "kind": "lda", "shrinkage": shrinkage}
            for shrinkage in (0.01, 0.1, 1.0)
        ]
    )
    if include_auxiliary_fusion:
        candidates += [
            {
                "key": f"type-prototype-archetype-fusion-{alpha:g}",
                "kind": "archetypeFusion",
                "alpha": alpha,
            }
            for alpha in (0.25, 0.5, 1.0, 2.0)
        ]
    return candidates


def _reused_rows(path: Path) -> dict[str, dict[str, Any]]:
    """Carga embeddings v2 consumidos y los indexa por imageId."""

    artifact = json.loads(path.read_text(encoding="utf-8"))
    if artifact.get("modelKey") != "clip-vit-b32-visual-evidence-v1" or artifact.get("dimensions") != 512:
        raise ValueError("FULL_TAXONOMY_V3_REUSED_EMBEDDINGS_INVALID")
    return {row["imageId"]: row for row in artifact["rows"]}


def _development_embeddings(
    manifest: dict[str, Any],
    manifest_path: Path,
    model_manifest: ClipVisualManifest,
    reused_a_path: Path,
    reused_b_path: Path,
    output_path: Path,
    batch_size: int = 8,
) -> dict[str, Any]:
    """Reutiliza A/B por hash y extrae únicamente los 254 embeddings C."""

    if output_path.exists():
        cached = json.loads(output_path.read_text(encoding="utf-8"))
        if cached.get("datasetVersion") != DEVELOPMENT_VERSION or len(cached.get("rows", [])) != 762:
            raise ValueError("FULL_TAXONOMY_V3_DEVELOPMENT_EMBEDDINGS_INVALID")
        return cached

    reused = {**_reused_rows(reused_a_path), **_reused_rows(reused_b_path)}
    rows_by_view = {
        view: [row for row in manifest["developmentRows"] if row["developmentView"] == view]
        for view in ("A", "B", "C")
    }
    if any(len(rows) != 254 for rows in rows_by_view.values()):
        raise ValueError("FULL_TAXONOMY_V3_DEVELOPMENT_VIEW_COUNT_INVALID")

    vectors: dict[str, list[float]] = {}
    for view in ("A", "B"):
        for row in rows_by_view[view]:
            old = reused.get(row["imageId"])
            if (
                old is None
                or old["imageSha256"] != row["generation"]["imageSha256"]
                or old["typeCode"] != row["typeCode"]
                or old["familyCode"] != row["familyCode"]
            ):
                raise ValueError("FULL_TAXONOMY_V3_REUSED_EMBEDDING_LINEAGE_MISMATCH")
            vectors[row["imageId"]] = old["embedding"]

    embedder = HuggingFaceClipEmbedder(model_manifest, local_files_only=True)
    dataset_root = manifest_path.parent
    evaluation_root = dataset_root.parent
    c_rows = rows_by_view["C"]
    for start in range(0, len(c_rows), batch_size):
        batch = c_rows[start : start + batch_size]
        paths = [_resolve(evaluation_root, dataset_root, row["relativePath"]) for row in batch]
        for row, path in zip(batch, paths, strict=True):
            if _sha256(path) != row["generation"]["imageSha256"]:
                raise ValueError("FULL_TAXONOMY_V3_DEVELOPMENT_IMAGE_HASH_MISMATCH")
        encoded = embedder.encode_images(paths)
        for row, vector in zip(batch, encoded, strict=True):
            vectors[row["imageId"]] = vector.values

    artifact = {
        "schemaVersion": 1,
        "datasetVersion": DEVELOPMENT_VERSION,
        "split": "development-only",
        "modelKey": model_manifest.modelKey,
        "modelRevision": model_manifest.revision,
        "dimensions": 512,
        "imageCount": 762,
        "viewCounts": {"A": 254, "B": 254, "C": 254},
        "provenance": {
            "reusedAEmbeddingArtifactSha256": _sha256(reused_a_path),
            "reusedBEmbeddingArtifactSha256": _sha256(reused_b_path),
            "reusedEmbeddingCount": 508,
            "newlyExtractedEmbeddingCount": 254,
            "holdoutV3EmbeddingCount": 0,
        },
        "rows": [
            {
                "imageId": row["imageId"],
                "venueId": row["venueId"],
                "sourceId": row["sourceId"],
                "typeCode": row["typeCode"],
                "familyCode": row["familyCode"],
                "archetypeCode": row["visualArchetype"]["code"],
                "developmentView": row["developmentView"],
                "imageSha256": row["generation"]["imageSha256"],
                "embedding": vectors[row["imageId"]],
            }
            for row in manifest["developmentRows"]
        ],
    }
    _write(output_path, artifact, compact=True)
    return artifact


def _evaluate_candidates(
    features: np.ndarray,
    labels: np.ndarray,
    types: np.ndarray,
    views: np.ndarray,
    auxiliary_labels: np.ndarray | None = None,
) -> list[dict[str, Any]]:
    """Evalúa candidatos dejando fuera una vista completa en cada fold."""

    classes = np.unique(labels)
    evaluations: list[dict[str, Any]] = []
    for candidate in _candidates(include_auxiliary_fusion=auxiliary_labels is not None):
        fold_metrics: list[dict[str, Any]] = []
        for held_out_view in ("A", "B", "C"):
            validation = np.flatnonzero(views == held_out_view)
            training = np.flatnonzero(views != held_out_view)
            scores = _candidate_scores(
                candidate,
                features[training],
                labels[training],
                types[training],
                features[validation],
                classes,
                auxiliary_labels[training] if auxiliary_labels is not None else None,
            )
            fold_metrics.append(
                {"heldOutView": held_out_view, **_metrics(labels[validation], scores, classes)}
            )
        evaluations.append({**candidate, "folds": fold_metrics, "mean": _mean_metrics(fold_metrics)})
    return evaluations


def _selected(evaluations: list[dict[str, Any]]) -> dict[str, Any]:
    """Selecciona por F1 macro, accuracy, Recall@3 y clave determinista."""

    return max(
        evaluations,
        key=lambda row: (
            row["mean"]["macroF1"],
            row["mean"]["accuracy"],
            row["mean"]["recallAt3"],
            row["key"],
        ),
    )


def _fit_model(
    candidate: dict[str, Any],
    features: np.ndarray,
    labels: np.ndarray,
    types: np.ndarray,
    classes: np.ndarray,
    archetypes: np.ndarray | None = None,
) -> dict[str, Any]:
    """Ajusta el artefacto final de una cabeza ya seleccionada."""

    fitted = {
        key: candidate[key]
        for key in candidate
        if key
        in {
            "key",
            "kind",
            "k",
            "gamma",
            "dimensions",
            "regularization",
            "reduction",
            "shrinkage",
            "alpha",
        }
    }
    fitted["classes"] = classes.tolist()
    if candidate["kind"] == "centroid":
        fitted["centroids"] = _centers(features, labels, classes).tolist()
    elif candidate["kind"] == "ridge":
        fitted["weights"] = _ridge_weights(
            features, labels, classes, candidate["regularization"]
        ).tolist()
    elif candidate["kind"] == "kernelRidge":
        train_kernel = np.exp(candidate["gamma"] * (features @ features.T - 1.0))
        class_index = {name: index for index, name in enumerate(classes)}
        counts = Counter(labels.tolist())
        targets = np.zeros((len(features), len(classes)), dtype=np.float64)
        for row, name in enumerate(labels):
            targets[row, class_index[name]] = len(labels) / (len(classes) * counts[name])
        fitted["dualWeights"] = np.linalg.solve(
            train_kernel + candidate["regularization"] * np.eye(len(train_kernel)),
            targets,
        ).tolist()
    elif candidate["kind"] == "pcaRidge":
        mean, components, weights = _pca_ridge_parameters(
            features,
            labels,
            classes,
            candidate["dimensions"],
            candidate["regularization"],
        )
        fitted["featureMean"] = mean.tolist()
        fitted["components"] = components.tolist()
        fitted["weights"] = weights.tolist()
    elif candidate["kind"] == "lda":
        weights, intercept = _lda_parameters(
            features, labels, classes, candidate["shrinkage"]
        )
        fitted["weights"] = weights.tolist()
        fitted["intercept"] = intercept.tolist()
    elif candidate["kind"] == "archetypeFusion":
        if archetypes is None:
            raise ValueError("FULL_TAXONOMY_V3_AUXILIARY_LABELS_MISSING")
        groups = np.unique(types)
        fitted["prototypeTypeCodes"] = groups.tolist()
        fitted["prototypeLabels"] = [
            str(labels[np.flatnonzero(types == group)[0]]) for group in groups
        ]
        fitted["prototypes"] = _centers(features, types, groups).tolist()
        archetype_classes = np.unique(archetypes)
        aux_weights, aux_intercept = _lda_parameters(
            features, archetypes, archetype_classes, 1.0
        )
        mapping = np.zeros((len(archetype_classes), len(classes)), dtype=np.float64)
        class_index = {name: index for index, name in enumerate(classes)}
        for archetype_index, archetype in enumerate(archetype_classes):
            counts = Counter(labels[archetypes == archetype].tolist())
            total = sum(counts.values())
            for family, count in counts.items():
                mapping[archetype_index, class_index[family]] = count / total
        fitted["archetypeClasses"] = archetype_classes.tolist()
        fitted["archetypeWeights"] = aux_weights.tolist()
        fitted["archetypeIntercept"] = aux_intercept.tolist()
        fitted["archetypeToFamily"] = mapping.tolist()
    elif candidate["kind"] == "typePrototype":
        groups = np.unique(types)
        fitted["prototypeTypeCodes"] = groups.tolist()
        fitted["prototypeLabels"] = [
            str(labels[np.flatnonzero(types == group)[0]]) for group in groups
        ]
        fitted["prototypes"] = _centers(features, types, groups).tolist()
    return fitted


def develop(
    manifest_path: Path,
    authorization_path: Path,
    model_manifest_path: Path,
    reused_a_path: Path,
    reused_b_path: Path,
    embeddings_path: Path,
    report_path: Path,
    model_path: Path,
    policy_path: Path,
    lock_path: Path,
    holdout_embeddings_path: Path,
) -> dict[str, Any]:
    """Selecciona, ajusta y congela v3 sin cargar imágenes holdout."""

    if any(path.exists() for path in (report_path, model_path, policy_path, lock_path)):
        raise ValueError("FULL_TAXONOMY_V3_DEVELOPMENT_ALREADY_FROZEN")
    if holdout_embeddings_path.exists():
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_EMBEDDINGS_PREEXIST")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    authorization = json.loads(authorization_path.read_text(encoding="utf-8"))
    development_rows = manifest["developmentRows"]
    if (
        manifest.get("humanReviewComplete") is not True
        or manifest.get("developmentTrainingAllowed") is not True
        or authorization.get("approvedImageCount") != 508
        or authorization.get("holdoutBudgetConsumed") != 0
        or len(development_rows) != 762
        or any(row.get("developmentTrainingAllowed") is not True for row in development_rows)
    ):
        raise ValueError("FULL_TAXONOMY_V3_TRAINING_NOT_AUTHORIZED")

    model_manifest = ClipVisualManifest.load(model_manifest_path)
    artifact = _development_embeddings(
        manifest,
        manifest_path,
        model_manifest,
        reused_a_path,
        reused_b_path,
        embeddings_path,
    )
    rows = artifact["rows"]
    features = np.asarray([row["embedding"] for row in rows], dtype=np.float64)
    families = np.asarray([row["familyCode"] for row in rows])
    archetypes = np.asarray([row["archetypeCode"] for row in rows])
    types = np.asarray([row["typeCode"] for row in rows])
    views = np.asarray([row["developmentView"] for row in rows])

    family_evaluations = _evaluate_candidates(
        features, families, types, views, archetypes
    )
    archetype_evaluations = _evaluate_candidates(features, archetypes, types, views)
    selected_family = _selected(family_evaluations)
    selected_archetype = _selected(archetype_evaluations)
    family_classes = np.unique(families)
    archetype_classes = np.unique(archetypes)
    model = {
        "schemaVersion": 1,
        "modelKey": "full-taxonomy-visual-multiview-classifier-v3",
        "clipModelKey": model_manifest.modelKey,
        "clipModelRevision": model_manifest.revision,
        "inputFeatures": ["clipImageEmbedding512"],
        "prohibitedInputFeatures": ["prompt", "typeCode", "familyCode", "archetypeCode"],
        "familyHead": _fit_model(
            selected_family, features, families, types, family_classes, archetypes
        ),
        "archetypeHead": _fit_model(
            selected_archetype, features, archetypes, types, archetype_classes
        ),
        "developmentEmbeddingsSha256": _sha256(embeddings_path),
        "productionTrainingAllowed": False,
        "promotionAllowed": False,
    }
    _write(model_path, model, compact=True)

    policy = {
        "schemaVersion": 1,
        "policyVersion": "full-taxonomy-visual-multiview-policy-v3",
        "selectionProtocol": "three-fold-leave-one-view-out",
        "selectionMetricOrder": ["macroF1", "accuracy", "recallAt3"],
        "developmentViewCount": 3,
        "minimumHoldoutAccuracy": 0.90,
        "maximumHoldoutError": 0.10,
        "minimumMacroPrecision": 0.80,
        "minimumMacroRecall": 0.80,
        "minimumMacroF1": 0.80,
        "minimumPerClassRecall": 0.70,
        "maximumGeneralizationGap": 0.10,
        "holdoutPredictionBudget": 1,
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(policy_path, policy)

    report = {
        "schemaVersion": 1,
        "reportVersion": DEVELOPMENT_VERSION,
        "imageCount": 762,
        "typeCount": 254,
        "familyCount": 23,
        "archetypeCount": 38,
        "viewCounts": {"A": 254, "B": 254, "C": 254},
        "foldCount": 3,
        "validationProtocol": "train-two-views-validate-third-view",
        "familyCandidates": family_evaluations,
        "archetypeCandidates": archetype_evaluations,
        "selectedFamilyCandidate": selected_family["key"],
        "selectedFamilyDevelopmentMetrics": selected_family["mean"],
        "selectedArchetypeCandidate": selected_archetype["key"],
        "selectedArchetypeDevelopmentMetrics": selected_archetype["mean"],
        "reusedEmbeddingCount": 508,
        "newlyExtractedEmbeddingCount": 254,
        "holdoutV3ImagesRead": False,
        "holdoutV3EmbeddingsRead": False,
        "holdoutV3PredictionsComputed": False,
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(report_path, report)

    holdout_fingerprint = hashlib.sha256(
        json.dumps(
            [
                {
                    "imageId": row["imageId"],
                    "venueId": row["venueId"],
                    "typeCode": row["typeCode"],
                    "familyCode": row["familyCode"],
                    "archetypeCode": row["visualArchetype"]["code"],
                    "imageSha256": row["generation"]["imageSha256"],
                }
                for row in manifest["holdoutRows"]
            ],
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest()
    lock = {
        "schemaVersion": 1,
        "lockVersion": "full-taxonomy-visual-multiview-pretest-lock-v3",
        "manifestSha256": _sha256(manifest_path),
        "authorizationSha256": _sha256(authorization_path),
        "clipModelManifestSha256": _sha256(model_manifest_path),
        "developmentEmbeddingsSha256": _sha256(embeddings_path),
        "developmentReportSha256": _sha256(report_path),
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
        "holdoutFingerprintSha256": holdout_fingerprint,
        "holdoutImageCount": 254,
        "holdoutEmbeddingsCreatedBeforeLock": False,
        "holdoutPredictionsComputedBeforeLock": False,
        "budget": 1,
        "consumed": 0,
        "reopenAllowed": False,
    }
    _write(lock_path, lock)
    return report


def run() -> None:
    """CLI reproducible de selección development-only v3."""

    repo_root = Path(__file__).resolve().parents[4]
    dataset = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    v2 = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
    results = repo_root / "apps/demand-engine/evaluation/results"
    models = repo_root / "apps/demand-engine/models"
    policies = repo_root / "apps/demand-engine/policies"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    report = develop(
        manifest_path=dataset / "generation-manifest.v3.json",
        authorization_path=dataset / "human-review-authorization.v3.json",
        model_manifest_path=models / "clip-vit-b32-visual-evidence.v1.json",
        reused_a_path=v2 / "development-clip-embeddings.v2.json",
        reused_b_path=v2 / "holdout-clip-embeddings.v2.json",
        embeddings_path=dataset / "development-clip-embeddings.v3.json",
        report_path=results / "full-taxonomy-visual-multiview-development.v3.json",
        model_path=models / "full-taxonomy-visual-multiview-classifier.v3.json",
        policy_path=policies / "full-taxonomy-visual-multiview-holdout.v3.json",
        lock_path=dataset / "pretest-lock.v3.json",
        holdout_embeddings_path=dataset / "holdout-clip-embeddings.v3.json",
    )
    print(
        json.dumps(
            {
                "family": report["selectedFamilyCandidate"],
                "familyMetrics": report["selectedFamilyDevelopmentMetrics"],
                "archetype": report["selectedArchetypeCandidate"],
                "archetypeMetrics": report["selectedArchetypeDevelopmentMetrics"],
                "holdoutV3PredictionsComputed": False,
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    run()
