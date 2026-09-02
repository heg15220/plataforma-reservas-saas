"""Selección development-only y apertura única del holdout visual taxonómico v2."""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any

import numpy as np

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder


SEED = 9329
DEVELOPMENT_VERSION = "full-taxonomy-visual-development-v2"
RESULT_VERSION = "full-taxonomy-visual-holdout-result-v2"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_TRAINING_PATH_ESCAPE")
    return path


def _write(path: Path, value: dict[str, Any], compact: bool = False) -> None:
    payload = json.dumps(value, ensure_ascii=False, separators=(",", ":") if compact else None, indent=None if compact else 2) + "\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(payload, encoding="utf-8")


def _metric(y_true: np.ndarray, scores: np.ndarray, classes: np.ndarray) -> dict[str, float]:
    prediction = classes[np.argmax(scores, axis=1)]
    precision_values: list[float] = []
    recall_values: list[float] = []
    f1_values: list[float] = []
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
    accuracy = float(np.mean(y_true == prediction))
    top_three = classes[np.argsort(scores, axis=1)[:, -3:]]
    recall_at_three = float(np.mean([expected in row for expected, row in zip(y_true, top_three, strict=True)]))
    return {
        "accuracy": round(accuracy, 8),
        "error": round(1.0 - accuracy, 8),
        "macroPrecision": round(float(np.mean(precision_values)), 8),
        "macroRecall": round(float(np.mean(recall_values)), 8),
        "macroF1": round(float(np.mean(f1_values)), 8),
        "familyRecallAt3": round(recall_at_three, 8),
    }


def _folds(labels: np.ndarray, count: int = 4) -> list[np.ndarray]:
    rng = np.random.default_rng(SEED)
    result: list[list[int]] = [[] for _ in range(count)]
    for name in np.unique(labels):
        indices = np.flatnonzero(labels == name)
        if len(indices) < count:
            raise ValueError("FULL_TAXONOMY_TRAINING_FOLD_CLASS_TOO_SMALL")
        rng.shuffle(indices)
        for offset, index in enumerate(indices):
            result[offset % count].append(int(index))
    return [np.asarray(sorted(row), dtype=np.int64) for row in result]


def _centroid_fit(features: np.ndarray, labels: np.ndarray, classes: np.ndarray) -> np.ndarray:
    centers = np.vstack([features[labels == name].mean(axis=0) for name in classes])
    return centers / np.linalg.norm(centers, axis=1, keepdims=True).clip(min=1e-12)


def _knn_scores(train_x: np.ndarray, train_y: np.ndarray, query_x: np.ndarray, classes: np.ndarray, k: int) -> np.ndarray:
    similarities = query_x @ train_x.T
    nearest = np.argsort(similarities, axis=1)[:, -k:]
    scores = np.zeros((len(query_x), len(classes)), dtype=np.float64)
    class_index = {name: index for index, name in enumerate(classes)}
    for query_index, neighbours in enumerate(nearest):
        for neighbour in neighbours:
            scores[query_index, class_index[train_y[neighbour]]] += max(0.0, float(similarities[query_index, neighbour])) + 1e-9
    return scores


def _ridge_fit(features: np.ndarray, labels: np.ndarray, classes: np.ndarray, regularization: float) -> np.ndarray:
    x = np.column_stack((features, np.ones(len(features))))
    y = np.zeros((len(features), len(classes)), dtype=np.float64)
    class_index = {name: index for index, name in enumerate(classes)}
    counts = Counter(labels.tolist())
    weights = np.asarray([len(labels) / (len(classes) * counts[name]) for name in labels])
    for row, name in enumerate(labels):
        y[row, class_index[name]] = 1.0
    root_weights = np.sqrt(weights)[:, None]
    xw, yw = x * root_weights, y * root_weights
    dual = np.linalg.solve(xw @ xw.T + regularization * np.eye(len(xw)), yw)
    return xw.T @ dual


def _candidate_scores(
    candidate: dict[str, Any],
    train_x: np.ndarray,
    train_y: np.ndarray,
    query_x: np.ndarray,
    classes: np.ndarray,
) -> np.ndarray:
    if candidate["kind"] == "centroid":
        return query_x @ _centroid_fit(train_x, train_y, classes).T
    if candidate["kind"] == "knn":
        return _knn_scores(train_x, train_y, query_x, classes, candidate["k"])
    weights = _ridge_fit(train_x, train_y, classes, candidate["regularization"])
    return np.column_stack((query_x, np.ones(len(query_x)))) @ weights


def _extract(
    rows: list[dict[str, Any]],
    dataset_root: Path,
    evaluation_root: Path,
    model_manifest: ClipVisualManifest,
    output_path: Path,
    split: str,
    batch_size: int = 8,
) -> dict[str, Any]:
    embedder = HuggingFaceClipEmbedder(model_manifest, local_files_only=True)
    vectors: list[list[float]] = []
    for start in range(0, len(rows), batch_size):
        batch = rows[start : start + batch_size]
        paths = [_resolve(evaluation_root, dataset_root, row["relativePath"]) for row in batch]
        for row, path in zip(batch, paths, strict=True):
            if _sha256(path) != row["generation"]["imageSha256"]:
                raise ValueError("FULL_TAXONOMY_TRAINING_IMAGE_HASH_MISMATCH")
        vectors.extend(vector.values for vector in embedder.encode_images(paths))
    artifact = {
        "schemaVersion": 1,
        "datasetVersion": DEVELOPMENT_VERSION if split == "development" else RESULT_VERSION,
        "split": split,
        "modelKey": model_manifest.modelKey,
        "modelRevision": model_manifest.revision,
        "dimensions": 512,
        "rows": [
            {
                "imageId": row["imageId"],
                "venueId": row["venueId"],
                "typeCode": row["typeCode"],
                "familyCode": row["familyCode"],
                "imageSha256": row["generation"]["imageSha256"],
                "embedding": vector,
            }
            for row, vector in zip(rows, vectors, strict=True)
        ],
    }
    _write(output_path, artifact, compact=True)
    return artifact


def develop(
    manifest_path: Path,
    authorization_path: Path,
    model_manifest_path: Path,
    embeddings_path: Path,
    report_path: Path,
    model_path: Path,
    policy_path: Path,
    lock_path: Path,
) -> dict[str, Any]:
    """Selecciona y congela candidato sin extraer ni leer embeddings holdout."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    authorization = json.loads(authorization_path.read_text(encoding="utf-8"))
    if (
        manifest.get("humanReviewComplete") is not True
        or manifest.get("developmentTrainingAllowed") is not True
        or authorization.get("approvedImageCount") != 508
        or authorization.get("holdoutBudgetConsumed") != 0
        or any(row.get("developmentTrainingAllowed") is not True for row in manifest["developmentRows"])
    ):
        raise ValueError("FULL_TAXONOMY_TRAINING_NOT_AUTHORIZED")
    if lock_path.exists() or report_path.exists() or model_path.exists():
        raise ValueError("FULL_TAXONOMY_DEVELOPMENT_ALREADY_FROZEN")

    model_manifest = ClipVisualManifest.load(model_manifest_path)
    dataset_root = manifest_path.parent
    development = _extract(manifest["developmentRows"], dataset_root, dataset_root.parent, model_manifest, embeddings_path, "development")
    features = np.asarray([row["embedding"] for row in development["rows"]], dtype=np.float64)
    labels = np.asarray([row["familyCode"] for row in development["rows"]])
    classes = np.unique(labels)
    candidates = (
        [{"key": "centroid", "kind": "centroid"}]
        + [{"key": f"knn-k{k}", "kind": "knn", "k": k} for k in (1, 3, 5, 7)]
        + [{"key": f"ridge-{value}", "kind": "ridge", "regularization": value} for value in (0.01, 0.1, 1.0, 10.0)]
    )
    folds = _folds(labels, 4)
    all_indices = np.arange(len(labels))
    evaluations: list[dict[str, Any]] = []
    for candidate in candidates:
        fold_metrics: list[dict[str, float]] = []
        for test_index in folds:
            train_index = np.setdiff1d(all_indices, test_index)
            scores = _candidate_scores(candidate, features[train_index], labels[train_index], features[test_index], classes)
            fold_metrics.append(_metric(labels[test_index], scores, classes))
        mean = {key: round(float(np.mean([row[key] for row in fold_metrics])), 8) for key in fold_metrics[0]}
        evaluations.append({**candidate, "folds": fold_metrics, "mean": mean})
    selected = max(evaluations, key=lambda row: (row["mean"]["macroF1"], row["mean"]["accuracy"], row["mean"]["familyRecallAt3"], row["key"]))

    model: dict[str, Any] = {
        "schemaVersion": 1,
        "modelKey": "full-taxonomy-visual-family-classifier-v2",
        "selectedCandidate": {key: selected[key] for key in selected if key not in {"folds", "mean"}},
        "classes": classes.tolist(),
        "developmentEmbeddingsSha256": _sha256(embeddings_path),
        "clipModelKey": model_manifest.modelKey,
        "clipModelRevision": model_manifest.revision,
    }
    if selected["kind"] == "centroid":
        model["centroids"] = _centroid_fit(features, labels, classes).tolist()
    elif selected["kind"] == "ridge":
        model["weights"] = _ridge_fit(features, labels, classes, selected["regularization"]).tolist()
    _write(model_path, model, compact=True)

    policy = {
        "schemaVersion": 1,
        "policyVersion": "full-taxonomy-visual-policy-v2",
        "selectionMetricOrder": ["macroF1", "accuracy", "familyRecallAt3"],
        "foldCount": 4,
        "minimumHoldoutAccuracy": 0.90,
        "maximumHoldoutError": 0.10,
        "minimumMacroPrecision": 0.80,
        "minimumMacroRecall": 0.80,
        "minimumMacroF1": 0.80,
        "maximumGeneralizationGap": 0.10,
        "holdoutPredictionBudget": 1,
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(policy_path, policy)
    report = {
        "schemaVersion": 1,
        "reportVersion": DEVELOPMENT_VERSION,
        "imageCount": 254,
        "typeCount": 254,
        "familyCount": 23,
        "foldCount": 4,
        "holdoutEmbeddingsRead": False,
        "holdoutPredictionsComputed": False,
        "candidates": evaluations,
        "selectedCandidate": selected["key"],
        "selectedDevelopmentMetrics": selected["mean"],
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(report_path, report)
    holdout_fingerprint = hashlib.sha256(
        json.dumps(
            [
                {key: row[key] if key != "imageSha256" else row["generation"]["imageSha256"] for key in ("imageId", "venueId", "typeCode", "familyCode", "imageSha256")}
                for row in manifest["holdoutRows"]
            ],
            sort_keys=True,
            separators=(",", ":"),
        ).encode()
    ).hexdigest()
    lock = {
        "schemaVersion": 1,
        "lockVersion": "full-taxonomy-visual-pretest-lock-v2",
        "manifestSha256": _sha256(manifest_path),
        "authorizationSha256": _sha256(authorization_path),
        "developmentEmbeddingsSha256": _sha256(embeddings_path),
        "developmentReportSha256": _sha256(report_path),
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
        "holdoutFingerprintSha256": holdout_fingerprint,
        "holdoutImageCount": 254,
        "holdoutPredictionsComputedBeforeLock": False,
        "budget": 1,
        "consumed": 0,
    }
    _write(lock_path, lock)
    return report


def _predict(model: dict[str, Any], development: dict[str, Any], test_features: np.ndarray) -> np.ndarray:
    classes = np.asarray(model["classes"])
    candidate = model["selectedCandidate"]
    if candidate["kind"] == "centroid":
        return test_features @ np.asarray(model["centroids"]).T
    if candidate["kind"] == "ridge":
        return np.column_stack((test_features, np.ones(len(test_features)))) @ np.asarray(model["weights"])
    train_x = np.asarray([row["embedding"] for row in development["rows"]], dtype=np.float64)
    train_y = np.asarray([row["familyCode"] for row in development["rows"]])
    return _knn_scores(train_x, train_y, test_features, classes, candidate["k"])


def open_test(
    manifest_path: Path,
    authorization_path: Path,
    model_manifest_path: Path,
    development_embeddings_path: Path,
    development_report_path: Path,
    model_path: Path,
    policy_path: Path,
    lock_path: Path,
    holdout_embeddings_path: Path,
    result_path: Path,
    opening_record_path: Path,
) -> dict[str, Any]:
    """Consume apertura 1/1, extrae holdout y conserva el resultado sin reajuste."""

    if opening_record_path.exists() or result_path.exists():
        raise ValueError("FULL_TAXONOMY_HOLDOUT_ALREADY_OPENED")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    if (
        lock.get("consumed") != 0
        or lock.get("budget") != 1
        or lock["manifestSha256"] != _sha256(manifest_path)
        or lock["authorizationSha256"] != _sha256(authorization_path)
        or lock["developmentEmbeddingsSha256"] != _sha256(development_embeddings_path)
        or lock["developmentReportSha256"] != _sha256(development_report_path)
        or lock["modelSha256"] != _sha256(model_path)
        or lock["policySha256"] != _sha256(policy_path)
        or any(row.get("testEvaluationAllowed") is not True for row in manifest["holdoutRows"])
    ):
        raise ValueError("FULL_TAXONOMY_HOLDOUT_LOCK_INVALID")

    model_manifest = ClipVisualManifest.load(model_manifest_path)
    holdout = _extract(manifest["holdoutRows"], manifest_path.parent, manifest_path.parent.parent, model_manifest, holdout_embeddings_path, "holdout")
    development = json.loads(development_embeddings_path.read_text(encoding="utf-8"))
    model = json.loads(model_path.read_text(encoding="utf-8"))
    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    features = np.asarray([row["embedding"] for row in holdout["rows"]], dtype=np.float64)
    labels = np.asarray([row["familyCode"] for row in holdout["rows"]])
    classes = np.asarray(model["classes"])
    scores = _predict(model, development, features)
    metrics = _metric(labels, scores, classes)
    prediction = classes[np.argmax(scores, axis=1)]
    family_recall = {
        name: round(float(np.mean(prediction[labels == name] == name)), 8) for name in classes
    }
    development_report = json.loads(development_report_path.read_text(encoding="utf-8"))
    gap = round(abs(development_report["selectedDevelopmentMetrics"]["accuracy"] - metrics["accuracy"]), 8)
    gates = {
        "accuracyPassed": metrics["accuracy"] >= policy["minimumHoldoutAccuracy"],
        "errorPassed": metrics["error"] <= policy["maximumHoldoutError"],
        "macroPrecisionPassed": metrics["macroPrecision"] >= policy["minimumMacroPrecision"],
        "macroRecallPassed": metrics["macroRecall"] >= policy["minimumMacroRecall"],
        "macroF1Passed": metrics["macroF1"] >= policy["minimumMacroF1"],
        "generalizationGapPassed": gap <= policy["maximumGeneralizationGap"],
    }
    result = {
        "schemaVersion": 1,
        "resultVersion": RESULT_VERSION,
        "holdoutImageCount": 254,
        "typeCount": 254,
        "familyCount": 23,
        "opening": "1/1",
        "selectedCandidate": development_report["selectedCandidate"],
        "developmentMetrics": development_report["selectedDevelopmentMetrics"],
        "holdoutMetrics": metrics,
        "absoluteDevelopmentHoldoutAccuracyGap": gap,
        "familyRecall": family_recall,
        "qualityGates": gates,
        "qualityGatesPassed": all(gates.values()),
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(result_path, result)
    record = {
        "schemaVersion": 1,
        "lockSha256": _sha256(lock_path),
        "holdoutEmbeddingsSha256": _sha256(holdout_embeddings_path),
        "resultSha256": _sha256(result_path),
        "budget": 1,
        "consumed": 1,
        "reopenAllowed": False,
    }
    _write(opening_record_path, record)
    return result


def run() -> None:
    repo_root = Path(__file__).resolve().parents[4]
    dataset = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
    results = repo_root / "apps/demand-engine/evaluation/results"
    models = repo_root / "apps/demand-engine/models"
    policies = repo_root / "apps/demand-engine/policies"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("develop", "open-test"))
    args = parser.parse_args()
    common = {
        "manifest_path": dataset / "generation-manifest.v2.json",
        "authorization_path": dataset / "human-review-authorization.v2.json",
        "model_manifest_path": models / "clip-vit-b32-visual-evidence.v1.json",
    }
    if args.command == "develop":
        report = develop(
            **common,
            embeddings_path=dataset / "development-clip-embeddings.v2.json",
            report_path=results / "full-taxonomy-visual-development.v2.json",
            model_path=models / "full-taxonomy-visual-family-classifier.v2.json",
            policy_path=policies / "full-taxonomy-visual-holdout.v2.json",
            lock_path=dataset / "pretest-lock.v2.json",
        )
        print(json.dumps({"selected": report["selectedCandidate"], "metrics": report["selectedDevelopmentMetrics"]}, ensure_ascii=False))
    else:
        result = open_test(
            **common,
            development_embeddings_path=dataset / "development-clip-embeddings.v2.json",
            development_report_path=results / "full-taxonomy-visual-development.v2.json",
            model_path=models / "full-taxonomy-visual-family-classifier.v2.json",
            policy_path=policies / "full-taxonomy-visual-holdout.v2.json",
            lock_path=dataset / "pretest-lock.v2.json",
            holdout_embeddings_path=dataset / "holdout-clip-embeddings.v2.json",
            result_path=results / "full-taxonomy-visual-holdout.v2.json",
            opening_record_path=dataset / "test-opening-record.v2.json",
        )
        print(json.dumps({"metrics": result["holdoutMetrics"], "gates": result["qualityGates"]}, ensure_ascii=False))


if __name__ == "__main__":
    run()
