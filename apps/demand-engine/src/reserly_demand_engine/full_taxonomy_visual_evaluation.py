"""Evalúa el corpus visual parcial de taxonomía completa con píxeles reales.

El job verifica integridad técnica, extrae embeddings CLIP congelados y ejecuta
validación cruzada estratificada sobre familias. También calcula un control de
etiquetas permutadas: una mejora clara frente a ese control acredita señal
visual, aunque el corpus siga siendo sintético y no esté aprobado para producción.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageStat
from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder


REPORT_VERSION = "full-taxonomy-visual-evaluation-v1"
SEED = 8147


def _dhash(image: Image.Image) -> int:
    """Calcula un dHash de 64 bits para detectar copias visuales evidentes."""

    gray = image.convert("L").resize((9, 8), Image.Resampling.LANCZOS)
    values = np.asarray(gray, dtype=np.int16)
    bits = values[:, 1:] > values[:, :-1]
    result = 0
    for bit in bits.ravel():
        result = (result << 1) | int(bit)
    return result


def _quality(path: Path) -> dict[str, Any]:
    """Inspecciona decodificación, dimensiones, variación y metadatos del PNG."""

    payload = path.read_bytes()
    with Image.open(path) as source:
        source.verify()
    with Image.open(path) as source:
        rgb = source.convert("RGB")
        stat = ImageStat.Stat(rgb.resize((128, 128)))
        return {
            "sha256": hashlib.sha256(payload).hexdigest(),
            "width": rgb.width,
            "height": rgb.height,
            "format": source.format,
            "mode": rgb.mode,
            "channelStdMean": round(float(np.mean(stat.stddev)), 6),
            "dhash": f"{_dhash(rgb):016x}",
            "exifEntryCount": len(source.getexif()),
        }


def _metric(y_true: np.ndarray, y_pred: np.ndarray, classes: np.ndarray) -> dict[str, float]:
    """Devuelve métricas macro y error con redondeo estable."""

    precisions: list[float] = []
    recalls: list[float] = []
    f1s: list[float] = []
    for class_name in classes:
        true_positive = int(np.sum((y_true == class_name) & (y_pred == class_name)))
        false_positive = int(np.sum((y_true != class_name) & (y_pred == class_name)))
        false_negative = int(np.sum((y_true == class_name) & (y_pred != class_name)))
        precision = true_positive / (true_positive + false_positive) if true_positive + false_positive else 0.0
        recall = true_positive / (true_positive + false_negative) if true_positive + false_negative else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        precisions.append(precision)
        recalls.append(recall)
        f1s.append(f1)
    accuracy = float(np.mean(y_true == y_pred))
    return {
        "accuracy": round(float(accuracy), 8),
        "error": round(float(1.0 - accuracy), 8),
        "macroPrecision": round(float(np.mean(precisions)), 8),
        "macroRecall": round(float(np.mean(recalls)), 8),
        "macroF1": round(float(np.mean(f1s)), 8),
    }


def _cross_validate(features: np.ndarray, labels: np.ndarray, folds: int) -> dict[str, Any]:
    """Evalúa centroides coseno sin ajustar sobre el fold de test."""

    rng = np.random.default_rng(SEED)
    classes = np.unique(labels)
    fold_indices: list[list[int]] = [[] for _ in range(folds)]
    for class_name in classes:
        indices = np.flatnonzero(labels == class_name)
        rng.shuffle(indices)
        for offset, index in enumerate(indices):
            fold_indices[offset % folds].append(int(index))
    train_metrics: list[dict[str, float]] = []
    test_metrics: list[dict[str, float]] = []
    fold_rows: list[dict[str, Any]] = []
    out_of_fold_true: list[str] = []
    out_of_fold_prediction: list[str] = []
    top_three_hits = 0
    all_indices = np.arange(len(labels))
    for fold, test_values in enumerate(fold_indices, start=1):
        test_index = np.asarray(sorted(test_values), dtype=np.int64)
        train_index = np.setdiff1d(all_indices, test_index)
        centroids = np.vstack(
            [features[train_index][labels[train_index] == class_name].mean(axis=0) for class_name in classes]
        )
        centroids /= np.linalg.norm(centroids, axis=1, keepdims=True).clip(min=1e-12)
        train_prediction = classes[np.argmax(features[train_index] @ centroids.T, axis=1)]
        test_scores = features[test_index] @ centroids.T
        test_prediction = classes[np.argmax(test_scores, axis=1)]
        top_three = classes[np.argsort(test_scores, axis=1)[:, -3:]]
        top_three_hits += sum(
            expected in candidates
            for expected, candidates in zip(labels[test_index], top_three, strict=True)
        )
        out_of_fold_true.extend(labels[test_index].tolist())
        out_of_fold_prediction.extend(test_prediction.tolist())
        train_metric = _metric(labels[train_index], train_prediction, classes)
        test_metric = _metric(labels[test_index], test_prediction, classes)
        train_metrics.append(train_metric)
        test_metrics.append(test_metric)
        fold_rows.append({"fold": fold, "train": train_metric, "test": test_metric})

    def mean(key: str, rows: list[dict[str, float]]) -> float:
        return round(float(np.mean([row[key] for row in rows])), 8)

    confusion_counts = Counter(
        (expected, predicted)
        for expected, predicted in zip(out_of_fold_true, out_of_fold_prediction, strict=True)
        if expected != predicted
    )
    return {
        "foldCount": folds,
        "classifier": "nearest-family-centroid-cosine",
        "trainableParameterCount": 0,
        "folds": fold_rows,
        "train": {key: mean(key, train_metrics) for key in train_metrics[0]},
        "test": {key: mean(key, test_metrics) for key in test_metrics[0]},
        "outOfFold": _metric(
            np.asarray(out_of_fold_true), np.asarray(out_of_fold_prediction), classes
        ),
        "familyRecallAt3": round(top_three_hits / len(labels), 8),
        "mostCommonConfusions": [
            {"expected": pair[0], "predicted": pair[1], "count": count}
            for pair, count in confusion_counts.most_common(12)
        ],
    }


def evaluate(
    manifest_path: Path,
    model_manifest_path: Path,
    output_path: Path,
    embeddings_path: Path,
    batch_size: int = 8,
) -> dict[str, Any]:
    """Ejecuta QA y validación cruzada sobre todos los PNG materializados."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    root = manifest_path.parent
    rows = [row for row in manifest["rows"] if (root / row["relativePath"]).is_file()]
    if len(rows) < 100:
        raise ValueError("FULL_TAXONOMY_VISUAL_SAMPLE_TOO_SMALL")

    quality_rows: list[dict[str, Any]] = []
    for row in rows:
        quality_rows.append({**{key: row[key] for key in ("imageId", "typeCode", "familyCode", "relativePath")}, **_quality(root / row["relativePath"])})
    hashes = [row["sha256"] for row in quality_rows]
    dhashes = [int(row["dhash"], 16) for row in quality_rows]
    near_pairs = sum(
        1
        for left in range(len(dhashes))
        for right in range(left + 1, len(dhashes))
        if (dhashes[left] ^ dhashes[right]).bit_count() <= 4
    )

    model_manifest = ClipVisualManifest.load(model_manifest_path)
    cached = json.loads(embeddings_path.read_text(encoding="utf-8")) if embeddings_path.is_file() else None
    expected_hashes = [quality["sha256"] for quality in quality_rows]
    cache_valid = bool(
        cached
        and cached.get("modelRevision") == model_manifest.revision
        and [row.get("imageId") for row in cached.get("rows", [])]
        == [row["imageId"] for row in rows]
        and [row.get("imageSha256") for row in cached.get("rows", [])] == expected_hashes
    )
    vectors: list[list[float]] = []
    if cache_valid:
        vectors = [row["embedding"] for row in cached["rows"]]
    else:
        embedder = HuggingFaceClipEmbedder(model_manifest, local_files_only=True)
        for start in range(0, len(rows), batch_size):
            paths = [root / row["relativePath"] for row in rows[start : start + batch_size]]
            vectors.extend(vector.values for vector in embedder.encode_images(paths))
    features = np.asarray(vectors, dtype=np.float64)
    norms = np.linalg.norm(features, axis=1)
    labels = np.asarray([row["familyCode"] for row in rows])
    minimum_family_count = min(Counter(labels).values())
    folds = min(5, minimum_family_count)
    cv = _cross_validate(features, labels, folds)

    rng = np.random.default_rng(SEED)
    permuted_labels = labels.copy()
    rng.shuffle(permuted_labels)
    permutation_cv = _cross_validate(features, permuted_labels, folds)
    uplift = round(cv["test"]["accuracy"] - permutation_cv["test"]["accuracy"], 8)

    embeddings = {
        "schemaVersion": 1,
        "datasetVersion": manifest["datasetVersion"],
        "modelKey": model_manifest.modelKey,
        "modelRevision": model_manifest.revision,
        "dimensions": model_manifest.dimensions,
        "pixelBytesHashedBeforeEmbedding": True,
        "rows": [
            {
                "imageId": row["imageId"],
                "typeCode": row["typeCode"],
                "familyCode": row["familyCode"],
                "imageSha256": quality["sha256"],
                "embedding": vector,
            }
            for row, quality, vector in zip(rows, quality_rows, vectors, strict=True)
        ],
    }
    embeddings_path.write_text(
        json.dumps(embeddings, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )

    present_families = sorted(set(labels))
    all_families = sorted({row["familyCode"] for row in manifest["rows"]})
    report = {
        "schemaVersion": 1,
        "reportVersion": REPORT_VERSION,
        "datasetVersion": manifest["datasetVersion"],
        "synthetic": True,
        "productionEvidence": False,
        "humanReviewComplete": False,
        "coverage": {
            "materializedImages": len(rows),
            "materializedTypes": len({row["typeCode"] for row in rows}),
            "presentFamilies": len(present_families),
            "expectedTypes": manifest["coverage"]["typeCount"],
            "expectedFamilies": manifest["coverage"]["familyCount"],
            "missingTypes": manifest["coverage"]["typeCount"] - len(rows),
            "missingFamilies": sorted(set(all_families) - set(present_families)),
        },
        "technicalQa": {
            "decodablePngCount": sum(row["format"] == "PNG" for row in quality_rows),
            "minimumWidth": min(row["width"] for row in quality_rows),
            "minimumHeight": min(row["height"] for row in quality_rows),
            "minimumChannelStdMean": min(row["channelStdMean"] for row in quality_rows),
            "exactDuplicatePairs": len(hashes) - len(set(hashes)),
            "nearDuplicatePairsDhashDistanceLe4": near_pairs,
            "imagesWithExif": sum(row["exifEntryCount"] > 0 for row in quality_rows),
            "embeddingNormMin": round(float(norms.min()), 8),
            "embeddingNormMax": round(float(norms.max()), 8),
        },
        "familyClassification": cv,
        "permutedLabelControl": permutation_cv,
        "pixelSignalAccuracyUplift": uplift,
        "qualityGates": {
            "minimumTestAccuracy": 0.90,
            "maximumTestError": 0.15,
            "minimumPixelSignalUplift": 0.25,
            "accuracyPassed": cv["test"]["accuracy"] >= 0.90,
            "errorPassed": cv["test"]["error"] < 0.15,
            "pixelSignalPassed": uplift >= 0.25,
            "minimumFamilyRecallAt3": 0.90,
            "familyRecallAt3Passed": cv["familyRecallAt3"] >= 0.90,
        },
        "promotionAllowed": False,
        "trainingAllowed": False,
        "limitations": [
            "El corpus contiene una sola imagen por tipo y no mide generalización intratipo.",
            "Dos familias y 34 tipos no están materializados.",
            "Las imágenes son sintéticas y permanecen pendientes de revisión humana.",
        ],
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def run() -> None:
    """CLI de evaluación reproducible del corpus parcial."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v1"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=root / "generation-manifest.json")
    parser.add_argument("--model-manifest", type=Path, default=repo_root / "apps/demand-engine/models/clip-vit-b32-visual-evidence.v1.json")
    parser.add_argument("--output", type=Path, default=repo_root / "apps/demand-engine/evaluation/results/full-taxonomy-visual-evaluation.v1.json")
    parser.add_argument("--embeddings", type=Path, default=root / "clip-embeddings.json")
    parser.add_argument("--batch-size", type=int, default=8)
    args = parser.parse_args()
    result = evaluate(args.manifest, args.model_manifest, args.output, args.embeddings, args.batch_size)
    print(json.dumps({"coverage": result["coverage"], "test": result["familyClassification"]["test"], "gates": result["qualityGates"]}, ensure_ascii=False))


if __name__ == "__main__":
    run()
