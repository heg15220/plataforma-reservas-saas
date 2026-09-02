"""Candidato visual v5: CLIP multirregión y píxel clásico auxiliar.

Las vistas A/B/C/D ya consumidas se tratan exclusivamente como desarrollo. El
pipeline extrae color, disposición y textura desde los PNG aprobados, los
fusiona con CLIP global+centro y selecciona por el peor fold leave-one-view-out.
No genera imágenes ni abre un test independiente.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np

from .full_taxonomy_visual_multiregion_v4 import _prototype_scores, _standardize
from .full_taxonomy_visual_multiview_v3_training import (
    _lda_parameters,
    _mean_metrics,
    _metrics,
    _ridge_weights,
    _sha256,
    _write,
)


VERSION = "full-taxonomy-visual-hybrid-development-v5"
VIEWS = ("A", "B", "C", "D")
CLASSIC_DIMENSIONS = 336


def classic_pixel_features(path: Path) -> np.ndarray:
    """Extrae 336 señales no sensibles de color, espacio y gradiente.

    La imagen se decodifica a 64x64 en memoria. No se infieren personas,
    identidad, emoción, salud, seguridad ni otros atributos sensibles.
    """

    from PIL import Image

    with Image.open(path) as image:
        rgb = np.asarray(
            image.convert("RGB").resize((64, 64), Image.Resampling.LANCZOS),
            dtype=np.float64,
        ) / 255.0
        hsv = np.asarray(
            image.convert("HSV").resize((64, 64), Image.Resampling.LANCZOS),
            dtype=np.float64,
        ) / 255.0
    values: list[float] = []
    for source in (rgb, hsv):
        for channel in range(3):
            histogram = np.histogram(
                source[:, :, channel], bins=16, range=(0, 1), density=True
            )[0] / 16.0
            values.extend(histogram.tolist())
        for grid_y in range(4):
            for grid_x in range(4):
                block = source[
                    grid_y * 16 : (grid_y + 1) * 16,
                    grid_x * 16 : (grid_x + 1) * 16,
                ]
                values.extend(block.mean(axis=(0, 1)).tolist())
                values.extend(block.std(axis=(0, 1)).tolist())
    grey = rgb.mean(axis=2)
    gradient_y, gradient_x = np.gradient(grey)
    magnitude = np.sqrt(gradient_x * gradient_x + gradient_y * gradient_y)
    angle = (np.arctan2(gradient_y, gradient_x) + np.pi) / (2 * np.pi)
    orientation = np.histogram(
        angle, bins=16, range=(0, 1), weights=magnitude
    )[0] / (magnitude.sum() + 1e-9)
    values.extend(orientation.tolist())
    for grid_y in range(4):
        for grid_x in range(4):
            block = magnitude[
                grid_y * 16 : (grid_y + 1) * 16,
                grid_x * 16 : (grid_x + 1) * 16,
            ]
            values.extend((float(block.mean()), float(block.std())))
    result = np.asarray(values, dtype=np.float64)
    if result.shape != (CLASSIC_DIMENSIONS,) or not np.isfinite(result).all():
        raise ValueError("FULL_TAXONOMY_V5_CLASSIC_FEATURES_INVALID")
    return result


def _resolve_image(dataset: Path, relative_path: str) -> Path:
    """Resuelve una ruta sellada sin permitir salir del árbol evaluation."""

    evaluation = dataset.parent.resolve()
    resolved = (dataset / relative_path).resolve()
    if evaluation not in resolved.parents:
        raise ValueError("FULL_TAXONOMY_V5_IMAGE_PATH_OUTSIDE_EVALUATION")
    return resolved


def extract_classic_artifact(
    dataset: Path, embeddings_path: Path, output_path: Path
) -> dict[str, Any]:
    """Verifica hashes y materializa features clásicas alineadas por imageId."""

    if output_path.exists():
        raise ValueError("FULL_TAXONOMY_V5_CLASSIC_ARTIFACT_ALREADY_EXISTS")
    embeddings = json.loads(embeddings_path.read_text(encoding="utf-8"))
    rows = embeddings["rows"]
    manifest = json.loads(
        (dataset / "generation-manifest.v3.json").read_text(encoding="utf-8")
    )
    lookup = {
        row["imageId"]: row
        for row in manifest["developmentRows"] + manifest["holdoutRows"]
    }
    features: list[np.ndarray] = []
    for row in rows:
        source = lookup[row["imageId"]]
        image_path = _resolve_image(dataset, source["relativePath"])
        if hashlib.sha256(image_path.read_bytes()).hexdigest() != row["imageSha256"]:
            raise ValueError("FULL_TAXONOMY_V5_IMAGE_HASH_MISMATCH")
        features.append(classic_pixel_features(image_path))
    matrix = np.vstack(features).astype(np.float32)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(
        output_path,
        schema_version=np.asarray([1], dtype=np.int16),
        image_ids=np.asarray([row["imageId"] for row in rows]),
        features=matrix,
    )
    return {
        "imageCount": len(rows),
        "dimensions": CLASSIC_DIMENSIONS,
        "sha256": _sha256(output_path),
    }


def _load_classic(path: Path, rows: list[dict[str, Any]]) -> np.ndarray:
    with np.load(path, allow_pickle=False) as artifact:
        image_ids = artifact["image_ids"].tolist()
        features = np.asarray(artifact["features"], dtype=np.float64)
    if image_ids != [row["imageId"] for row in rows]:
        raise ValueError("FULL_TAXONOMY_V5_CLASSIC_ALIGNMENT_INVALID")
    if features.shape != (len(rows), CLASSIC_DIMENSIONS):
        raise ValueError("FULL_TAXONOMY_V5_CLASSIC_SHAPE_INVALID")
    return features


def _arrays(rows: list[dict[str, Any]]) -> tuple[np.ndarray, ...]:
    global_x = np.asarray([row["globalEmbedding"] for row in rows], dtype=np.float64)
    center_x = np.asarray([row["centerEmbedding"] for row in rows], dtype=np.float64)
    combined = np.column_stack((global_x, center_x))
    combined /= np.linalg.norm(combined, axis=1, keepdims=True).clip(min=1e-12)
    return (
        global_x,
        center_x,
        combined,
        np.asarray([row["familyCode"] for row in rows]),
        np.asarray([row["typeCode"] for row in rows]),
        np.asarray([row["developmentView"] for row in rows]),
    )


def evaluate_candidates(
    rows: list[dict[str, Any]], classic: np.ndarray
) -> list[dict[str, Any]]:
    """Evalúa fusiones fijadas; cada fold ajusta todas las cabezas desde cero."""

    global_x, center_x, combined, families, types, views = _arrays(rows)
    classes = np.unique(families)
    branches: dict[str, dict[str, Any]] = {}
    for held_out in VIEWS:
        training, validation = views != held_out, views == held_out
        prototype = _standardize(
            _prototype_scores(
                global_x[training], types[training], families[training],
                global_x[validation], classes,
            )
        ) + _standardize(
            _prototype_scores(
                center_x[training], types[training], families[training],
                center_x[validation], classes,
            )
        )
        lda_weights, lda_intercept = _lda_parameters(
            combined[training], families[training], classes, 1.0
        )
        clip_scores = _standardize(
            combined[validation] @ lda_weights.T + lda_intercept
        )
        mean = classic[training].mean(axis=0)
        std = classic[training].std(axis=0).clip(min=1e-6)
        train_classic = (classic[training] - mean) / std
        validation_classic = (classic[validation] - mean) / std
        classic_scores = {}
        for regularization in (0.1, 1.0, 10.0):
            weights = _ridge_weights(
                train_classic, families[training], classes, regularization
            )
            values = np.column_stack(
                (validation_classic, np.ones(sum(validation)))
            ) @ weights
            classic_scores[regularization] = _standardize(values)
        branches[held_out] = {
            "validation": validation,
            "prototype": _standardize(prototype),
            "clip": clip_scores,
            "classic": classic_scores,
        }
    evaluations = []
    for clip_alpha in (0.6, 0.75, 0.9):
        for classic_alpha in (0.0, 0.025, 0.05, 0.075, 0.1, 0.15):
            for regularization in (0.1, 1.0, 10.0):
                folds = []
                for held_out in VIEWS:
                    branch = branches[held_out]
                    scores = (
                        branch["prototype"]
                        + clip_alpha * branch["clip"]
                        + classic_alpha * branch["classic"][regularization]
                    )
                    folds.append(
                        {
                            "heldOutView": held_out,
                            **_metrics(
                                families[branch["validation"]], scores, classes
                            ),
                        }
                    )
                evaluations.append(
                    {
                        "key": (
                            f"global-center-lda-classic-c{clip_alpha:g}"
                            f"-p{classic_alpha:g}-r{regularization:g}"
                        ),
                        "clipAlpha": clip_alpha,
                        "classicAlpha": classic_alpha,
                        "classicRegularization": regularization,
                        "folds": folds,
                        "mean": _mean_metrics(folds),
                        "worstFoldAccuracy": min(row["accuracy"] for row in folds),
                        "worstFoldMacroF1": min(row["macroF1"] for row in folds),
                    }
                )
    return evaluations


def _fit_model(
    rows: list[dict[str, Any]], classic: np.ndarray, selected: dict[str, Any]
) -> dict[str, Any]:
    global_x, center_x, combined, families, types, _ = _arrays(rows)
    classes, type_codes = np.unique(families), np.unique(types)
    prototype_labels = [
        str(families[np.flatnonzero(types == code)[0]]) for code in type_codes
    ]
    lda_weights, lda_intercept = _lda_parameters(combined, families, classes, 1.0)
    classic_mean = classic.mean(axis=0)
    classic_std = classic.std(axis=0).clip(min=1e-6)
    normalized_classic = (classic - classic_mean) / classic_std
    classic_weights = _ridge_weights(
        normalized_classic,
        families,
        classes,
        selected["classicRegularization"],
    )
    from .full_taxonomy_visual_multiview_v3_training import _centers

    return {
        "schemaVersion": 1,
        "modelKey": "full-taxonomy-visual-hybrid-classifier-v5",
        "candidateKey": selected["key"],
        "classes": classes.tolist(),
        "inputFeatures": [
            "clipGlobalEmbedding512",
            "clipCenter80Embedding512",
            "classicPixelFeatures336",
        ],
        "prohibitedInputFeatures": [
            "prompt", "typeCode", "familyCode", "archetypeCode",
            "identity", "age", "gender", "ethnicity", "health", "emotion",
        ],
        "typeCodes": type_codes.tolist(),
        "prototypeLabels": prototype_labels,
        "globalPrototypes": _centers(global_x, types, type_codes).tolist(),
        "centerPrototypes": _centers(center_x, types, type_codes).tolist(),
        "ldaWeights": lda_weights.tolist(),
        "ldaIntercept": lda_intercept.tolist(),
        "classicMean": classic_mean.tolist(),
        "classicStd": classic_std.tolist(),
        "classicWeights": classic_weights.tolist(),
        "clipAlpha": selected["clipAlpha"],
        "classicAlpha": selected["classicAlpha"],
        "classicRegularization": selected["classicRegularization"],
        "independentTestEvaluated": False,
        "productionTrainingAllowed": False,
        "promotionAllowed": False,
    }


def predict_scores(
    model: dict[str, Any], global_x: np.ndarray, center_x: np.ndarray,
    classic: np.ndarray,
) -> np.ndarray:
    """Aplica el candidato congelado sin etiquetas privilegiadas."""

    global_values = np.atleast_2d(np.asarray(global_x, dtype=np.float64))
    center_values = np.atleast_2d(np.asarray(center_x, dtype=np.float64))
    classic_values = np.atleast_2d(np.asarray(classic, dtype=np.float64))
    classes = np.asarray(model["classes"])
    prototype_labels = np.asarray(model["prototypeLabels"])
    global_similarity = global_values @ np.asarray(model["globalPrototypes"]).T
    center_similarity = center_values @ np.asarray(model["centerPrototypes"]).T
    global_scores = np.column_stack(
        [global_similarity[:, prototype_labels == family].max(axis=1) for family in classes]
    )
    center_scores = np.column_stack(
        [center_similarity[:, prototype_labels == family].max(axis=1) for family in classes]
    )
    prototype = _standardize(global_scores) + _standardize(center_scores)
    combined = np.column_stack((global_values, center_values))
    combined /= np.linalg.norm(combined, axis=1, keepdims=True).clip(min=1e-12)
    clip = _standardize(
        combined @ np.asarray(model["ldaWeights"]).T
        + np.asarray(model["ldaIntercept"])
    )
    normalized_classic = (
        classic_values - np.asarray(model["classicMean"])
    ) / np.asarray(model["classicStd"])
    classic_with_bias = np.column_stack(
        (normalized_classic, np.ones(len(normalized_classic)))
    )
    classic_scores = _standardize(
        classic_with_bias @ np.asarray(model["classicWeights"])
    )
    return (
        _standardize(prototype)
        + float(model["clipAlpha"]) * clip
        + float(model["classicAlpha"]) * classic_scores
    )


def develop_and_freeze(
    embeddings_path: Path, classic_path: Path, report_path: Path,
    model_path: Path, policy_path: Path,
) -> dict[str, Any]:
    """Selecciona y congela v5 exclusivamente sobre development consumido."""

    if any(path.exists() for path in (report_path, model_path, policy_path)):
        raise ValueError("FULL_TAXONOMY_V5_CANDIDATE_ALREADY_FROZEN")
    artifact = json.loads(embeddings_path.read_text(encoding="utf-8"))
    if artifact.get("imageCount") != 1016 or artifact.get("independentTestAvailable") is not False:
        raise ValueError("FULL_TAXONOMY_V5_SOURCE_INVALID")
    rows = artifact["rows"]
    classic = _load_classic(classic_path, rows)
    candidates = evaluate_candidates(rows, classic)
    selected = max(
        candidates,
        key=lambda row: (
            row["worstFoldMacroF1"], row["worstFoldAccuracy"],
            row["mean"]["macroF1"], row["mean"]["accuracy"], row["key"],
        ),
    )
    model = _fit_model(rows, classic, selected)
    model["developmentEmbeddingsSha256"] = _sha256(embeddings_path)
    model["classicArtifactSha256"] = _sha256(classic_path)
    _write(model_path, model, compact=True)
    policy = {
        "schemaVersion": 1,
        "policyVersion": "full-taxonomy-visual-hybrid-policy-v5",
        "selectionProtocol": "four-fold-leave-one-consumed-view-out",
        "selectionOrder": [
            "worstFoldMacroF1", "worstFoldAccuracy", "meanMacroF1", "meanAccuracy"
        ],
        "freshHoldoutRequired": True,
        "holdoutPredictionBudget": 1,
        "minimumFutureTestAccuracy": 0.90,
        "maximumFutureTestError": 0.10,
        "minimumFutureMacroF1": 0.80,
        "minimumFutureClassRecall": 0.70,
        "automaticPromotionAllowed": False,
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write(policy_path, policy)
    report = {
        "schemaVersion": 1,
        "reportVersion": VERSION,
        "imageCount": 1016,
        "views": list(VIEWS),
        "classicDimensions": CLASSIC_DIMENSIONS,
        "candidateCount": len(candidates),
        "selectionProtocol": policy["selectionProtocol"],
        "selectionOrder": policy["selectionOrder"],
        "selectedCandidate": selected["key"],
        "selectedDevelopmentMetrics": selected["mean"],
        "selectedWorstFoldAccuracy": selected["worstFoldAccuracy"],
        "selectedWorstFoldMacroF1": selected["worstFoldMacroF1"],
        "v4DevelopmentAccuracy": 0.83267717,
        "v4DevelopmentMacroF1": 0.81591205,
        "v4WorstFoldAccuracy": 0.78346457,
        "v4WorstFoldMacroF1": 0.76688298,
        "meanAccuracyUplift": round(selected["mean"]["accuracy"] - 0.83267717, 8),
        "meanMacroF1Uplift": round(selected["mean"]["macroF1"] - 0.81591205, 8),
        "worstFoldAccuracyUplift": round(selected["worstFoldAccuracy"] - 0.78346457, 8),
        "worstFoldMacroF1Uplift": round(selected["worstFoldMacroF1"] - 0.76688298, 8),
        "candidates": candidates,
        "embeddingsSha256": _sha256(embeddings_path),
        "classicArtifactSha256": _sha256(classic_path),
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
        "newImagesGenerated": 0,
        "independentTestAvailable": False,
        "qualityConfirmed": False,
        "productionTrainingAllowed": False,
        "promotionAllowed": False,
    }
    _write(report_path, report)
    return report


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    repo = Path(__file__).resolve().parents[4]
    dataset = repo / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    embeddings = dataset / "development-multiregion-embeddings.v4.json"
    classic = dataset / "development-classic-pixel-features.v5.npz"
    if not classic.exists():
        extract_classic_artifact(dataset, embeddings, classic)
    report = develop_and_freeze(
        embeddings,
        classic,
        repo / "apps/demand-engine/evaluation/results/full-taxonomy-visual-hybrid-development.v5.json",
        repo / "apps/demand-engine/models/full-taxonomy-visual-hybrid-classifier.v5.json",
        repo / "apps/demand-engine/policies/full-taxonomy-visual-hybrid-holdout.v5.json",
    )
    print(json.dumps({"selected": report["selectedCandidate"], "metrics": report["selectedDevelopmentMetrics"]}))


if __name__ == "__main__":
    run()
