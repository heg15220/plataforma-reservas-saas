"""Desarrollo v4 con embeddings CLIP globales y de recorte central.

Las cuatro vistas A/B/C/D ya consumidas son exclusivamente desarrollo. Se
extrae una región central determinista sin crear imágenes nuevas y se selecciona
por leave-one-view-out. Ningún resultado se presenta como test independiente.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import numpy as np

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder
from .full_taxonomy_visual_multiview_v3_training import (
    _centers, _lda_parameters, _metrics, _mean_metrics, _resolve, _ridge_weights,
    _sha256, _write,
)


VERSION = "full-taxonomy-visual-multiregion-development-v4"
VIEWS = ("A", "B", "C", "D")


def _source_rows(dataset: Path) -> list[dict[str, Any]]:
    development = json.loads((dataset / "development-clip-embeddings.v3.json").read_text(encoding="utf-8"))["rows"]
    consumed = json.loads((dataset / "holdout-clip-embeddings.v3.json").read_text(encoding="utf-8"))["rows"]
    return development + [{**row, "developmentView": "D"} for row in consumed]


def _extract_center_embeddings(
    dataset: Path, manifest: dict[str, Any], rows: list[dict[str, Any]],
    clip_manifest: ClipVisualManifest, batch_size: int,
) -> list[list[float]]:
    """Codifica un recorte central del 80 % manteniendo aspecto 4:3."""

    from PIL import Image
    import torch

    lookup = {row["imageId"]: row for row in manifest["developmentRows"] + manifest["holdoutRows"]}
    embedder = HuggingFaceClipEmbedder(clip_manifest, local_files_only=True)
    model, processor = embedder._load()
    vectors: list[list[float]] = []
    for start in range(0, len(rows), batch_size):
        images = []
        try:
            for row in rows[start:start + batch_size]:
                source = lookup[row["imageId"]]
                path = _resolve(dataset.parent, dataset, source["relativePath"])
                if _sha256(path) != source["generation"]["imageSha256"]:
                    raise ValueError("FULL_TAXONOMY_V4_CENTER_IMAGE_HASH_MISMATCH")
                with Image.open(path) as image:
                    rgb = image.convert("RGB")
                    width, height = rgb.size
                    margin_x, margin_y = round(width * .10), round(height * .10)
                    images.append(rgb.crop((margin_x, margin_y, width - margin_x, height - margin_y)))
            inputs = processor(images=images, return_tensors="pt")
            with torch.inference_mode():
                features = model.get_image_features(**inputs)
            vectors.extend(vector.values for vector in embedder._vectors(features))
        finally:
            for image in images:
                image.close()
    return vectors


def _prototype_scores(
    train_x: np.ndarray, train_types: np.ndarray, train_families: np.ndarray,
    query_x: np.ndarray, classes: np.ndarray,
) -> np.ndarray:
    type_codes = np.unique(train_types)
    prototypes = _centers(train_x, train_types, type_codes)
    similarities = query_x @ prototypes.T
    labels = np.asarray([train_families[np.flatnonzero(train_types == code)[0]] for code in type_codes])
    return np.column_stack([similarities[:, labels == family].max(axis=1) for family in classes])


def _standardize(scores: np.ndarray) -> np.ndarray:
    return (scores - scores.mean(axis=1, keepdims=True)) / scores.std(axis=1, keepdims=True).clip(min=1e-9)


def _evaluate(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    global_x = np.asarray([row["globalEmbedding"] for row in rows], dtype=np.float64)
    center_x = np.asarray([row["centerEmbedding"] for row in rows], dtype=np.float64)
    combined_x = np.column_stack((global_x, center_x))
    combined_x /= np.linalg.norm(combined_x, axis=1, keepdims=True).clip(min=1e-12)
    families = np.asarray([row["familyCode"] for row in rows])
    types = np.asarray([row["typeCode"] for row in rows])
    views = np.asarray([row["developmentView"] for row in rows])
    classes = np.unique(families)
    specs = (
        [{"key": f"global-center-prototype-fusion-{alpha}", "kind": "fusion", "alpha": alpha} for alpha in (0, .1, .25, .5, .75, 1, 1.5, 2)]
        + [{"key": "combined-prototype", "kind": "prototype"},
           {"key": "combined-ridge-0.1", "kind": "ridge", "regularization": .1},
           {"key": "combined-ridge-1", "kind": "ridge", "regularization": 1},
           {"key": "combined-lda-0.1", "kind": "lda", "shrinkage": .1},
           {"key": "combined-lda-1", "kind": "lda", "shrinkage": 1}]
    )
    evaluations = []
    for spec in specs:
        folds = []
        for held in VIEWS:
            validation = views == held
            training = ~validation
            if spec["kind"] == "fusion":
                global_scores = _prototype_scores(global_x[training], types[training], families[training], global_x[validation], classes)
                center_scores = _prototype_scores(center_x[training], types[training], families[training], center_x[validation], classes)
                scores = _standardize(global_scores) + spec["alpha"] * _standardize(center_scores)
            elif spec["kind"] == "prototype":
                scores = _prototype_scores(combined_x[training], types[training], families[training], combined_x[validation], classes)
            elif spec["kind"] == "ridge":
                weights = _ridge_weights(combined_x[training], families[training], classes, spec["regularization"])
                scores = np.column_stack((combined_x[validation], np.ones(sum(validation)))) @ weights
            else:
                weights, intercept = _lda_parameters(combined_x[training], families[training], classes, spec["shrinkage"])
                scores = combined_x[validation] @ weights.T + intercept
            folds.append({"heldOutView": held, **_metrics(families[validation], scores, classes)})
        evaluations.append({**spec, "folds": folds, "mean": _mean_metrics(folds)})
    return evaluations


def _robust_evaluations(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Fusiona prototipos multirregión con LDA y prioriza el peor fold."""

    global_x = np.asarray([row["globalEmbedding"] for row in rows], dtype=np.float64)
    center_x = np.asarray([row["centerEmbedding"] for row in rows], dtype=np.float64)
    combined_x = np.column_stack((global_x, center_x))
    combined_x /= np.linalg.norm(combined_x, axis=1, keepdims=True).clip(min=1e-12)
    families = np.asarray([row["familyCode"] for row in rows])
    types = np.asarray([row["typeCode"] for row in rows])
    views = np.asarray([row["developmentView"] for row in rows])
    classes = np.unique(families)
    evaluations = []
    for alpha in (0, .1, .2, .3, .5, .75, 1, 1.5, 2, 3):
        folds = []
        for held in VIEWS:
            validation, training = views == held, views != held
            global_scores = _prototype_scores(global_x[training], types[training], families[training], global_x[validation], classes)
            center_scores = _prototype_scores(center_x[training], types[training], families[training], center_x[validation], classes)
            prototype = _standardize(global_scores) + _standardize(center_scores)
            weights, intercept = _lda_parameters(combined_x[training], families[training], classes, 1)
            lda = combined_x[validation] @ weights.T + intercept
            scores = _standardize(prototype) + alpha * _standardize(lda)
            folds.append({"heldOutView": held, **_metrics(families[validation], scores, classes)})
        evaluations.append({
            "key": f"global-center-prototype-lda-robust-{alpha}", "alpha": alpha,
            "folds": folds, "mean": _mean_metrics(folds),
            "worstFoldAccuracy": min(row["accuracy"] for row in folds),
            "worstFoldMacroF1": min(row["macroF1"] for row in folds),
        })
    return evaluations


def freeze_candidate(
    embeddings_path: Path, preliminary_report_path: Path, final_report_path: Path,
    model_path: Path, policy_path: Path,
) -> dict[str, Any]:
    """Congela el candidato robusto sin crear ni abrir un test nuevo."""

    if any(path.exists() for path in (final_report_path, model_path, policy_path)):
        raise ValueError("FULL_TAXONOMY_V4_CANDIDATE_ALREADY_FROZEN")
    artifact = json.loads(embeddings_path.read_text(encoding="utf-8"))
    rows = artifact["rows"]
    if artifact.get("imageCount") != 1016 or artifact.get("independentTestAvailable") is not False:
        raise ValueError("FULL_TAXONOMY_V4_EMBEDDING_ARTIFACT_INVALID")
    preliminary = json.loads(preliminary_report_path.read_text(encoding="utf-8"))
    robust = _robust_evaluations(rows)
    selected = max(
        robust,
        key=lambda row: (
            row["worstFoldMacroF1"], row["worstFoldAccuracy"],
            row["mean"]["macroF1"], row["mean"]["accuracy"], row["key"],
        ),
    )
    global_x = np.asarray([row["globalEmbedding"] for row in rows], dtype=np.float64)
    center_x = np.asarray([row["centerEmbedding"] for row in rows], dtype=np.float64)
    combined_x = np.column_stack((global_x, center_x))
    combined_x /= np.linalg.norm(combined_x, axis=1, keepdims=True).clip(min=1e-12)
    families = np.asarray([row["familyCode"] for row in rows])
    types = np.asarray([row["typeCode"] for row in rows])
    classes = np.unique(families)
    type_codes = np.unique(types)
    prototype_labels = [str(families[np.flatnonzero(types == code)[0]]) for code in type_codes]
    lda_weights, lda_intercept = _lda_parameters(combined_x, families, classes, 1)
    model = {
        "schemaVersion": 1, "modelKey": "full-taxonomy-visual-multiregion-classifier-v4",
        "candidateKey": selected["key"], "classes": classes.tolist(),
        "inputFeatures": ["clipGlobalEmbedding512", "clipCenter80Embedding512"],
        "prohibitedInputFeatures": ["prompt", "typeCode", "familyCode", "archetypeCode"],
        "typeCodes": type_codes.tolist(), "prototypeLabels": prototype_labels,
        "globalPrototypes": _centers(global_x, types, type_codes).tolist(),
        "centerPrototypes": _centers(center_x, types, type_codes).tolist(),
        "ldaShrinkage": 1, "ldaWeights": lda_weights.tolist(),
        "ldaIntercept": lda_intercept.tolist(), "fusionAlpha": selected["alpha"],
        "developmentEmbeddingsSha256": _sha256(embeddings_path),
        "independentTestEvaluated": False, "productionTrainingAllowed": False,
        "promotionAllowed": False,
    }
    _write(model_path, model, compact=True)
    policy = {
        "schemaVersion": 1, "policyVersion": "full-taxonomy-visual-multiregion-policy-v4",
        "selectionProtocol": "four-fold-leave-one-consumed-view-out",
        "selectionOrder": ["worstFoldMacroF1", "worstFoldAccuracy", "meanMacroF1", "meanAccuracy"],
        "minimumFutureTestAccuracy": .90, "maximumFutureTestError": .10,
        "minimumFutureMacroPrecision": .80, "minimumFutureMacroRecall": .80,
        "minimumFutureMacroF1": .80, "minimumFutureClassRecall": .70,
        "maximumFutureGeneralizationGap": .10, "freshHoldoutRequired": True,
        "holdoutPredictionBudget": 1, "automaticPromotionAllowed": False,
        "productionEvidence": False, "promotionAllowed": False,
    }
    _write(policy_path, policy)
    d_fold = next(row for row in selected["folds"] if row["heldOutView"] == "D")
    report = {
        "schemaVersion": 1, "reportVersion": "full-taxonomy-visual-multiregion-robust-development-v4",
        "imageCount": 1016, "viewCounts": {view: 254 for view in VIEWS},
        "regions": ["global", "center-80-percent"], "newImagesGenerated": 0,
        "validationProtocol": "four-fold-leave-one-consumed-view-out",
        "selectionOrder": policy["selectionOrder"], "candidateCount": len(robust),
        "candidates": robust, "selectedCandidate": selected["key"],
        "selectedDevelopmentMetrics": selected["mean"],
        "selectedWorstFoldAccuracy": selected["worstFoldAccuracy"],
        "selectedWorstFoldMacroF1": selected["worstFoldMacroF1"],
        "consumedViewDMetrics": d_fold,
        "v3FourViewBaselineAccuracy": .7992126,
        "developmentAccuracyUplift": round(selected["mean"]["accuracy"] - .7992126, 8),
        "v3ConsumedHoldoutAccuracy": .7480315,
        "viewDAccuracyUplift": round(d_fold["accuracy"] - .7480315, 8),
        "preliminaryReportSha256": _sha256(preliminary_report_path),
        "modelSha256": _sha256(model_path), "policySha256": _sha256(policy_path),
        "independentTestAvailable": False, "qualityConfirmed": False,
        "freshHoldoutRequired": True, "productionTrainingAllowed": False,
        "promotionAllowed": False,
    }
    _write(final_report_path, report)
    return report


def develop(
    dataset: Path, clip_manifest_path: Path, embeddings_path: Path,
    report_path: Path, batch_size: int = 16,
) -> dict[str, Any]:
    """Materializa representación y resultados development-only reproducibles."""

    if embeddings_path.exists() or report_path.exists():
        raise ValueError("FULL_TAXONOMY_V4_DEVELOPMENT_ALREADY_EXISTS")
    manifest = json.loads((dataset / "generation-manifest.v3.json").read_text(encoding="utf-8"))
    opening = json.loads((dataset / "holdout-opening-record.v3.json").read_text(encoding="utf-8"))
    if opening.get("consumed") != 1 or opening.get("reopenAllowed") is not False:
        raise ValueError("FULL_TAXONOMY_V4_REQUIRES_CONSUMED_V3")
    source = _source_rows(dataset)
    if len(source) != 1016 or {row["developmentView"] for row in source} != set(VIEWS):
        raise ValueError("FULL_TAXONOMY_V4_SOURCE_INVALID")
    clip_manifest = ClipVisualManifest.load(clip_manifest_path)
    center = _extract_center_embeddings(dataset, manifest, source, clip_manifest, batch_size)
    rows = [{
        "imageId": row["imageId"], "venueId": row["venueId"], "typeCode": row["typeCode"],
        "familyCode": row["familyCode"], "archetypeCode": row["archetypeCode"],
        "developmentView": row["developmentView"], "imageSha256": row["imageSha256"],
        "globalEmbedding": row["embedding"], "centerEmbedding": vector,
    } for row, vector in zip(source, center, strict=True)]
    artifact = {
        "schemaVersion": 1, "datasetVersion": VERSION, "split": "consumed-development-only",
        "imageCount": 1016, "viewCounts": {view: 254 for view in VIEWS},
        "modelKey": clip_manifest.modelKey, "modelRevision": clip_manifest.revision,
        "regions": ["global", "center-80-percent"], "dimensionsPerRegion": 512,
        "newImagesGenerated": 0, "independentTestAvailable": False,
        "productionTrainingAllowed": False, "promotionAllowed": False, "rows": rows,
    }
    _write(embeddings_path, artifact, compact=True)
    evaluations = _evaluate(rows)
    selected = max(evaluations, key=lambda row: (row["mean"]["macroF1"], row["mean"]["accuracy"], row["mean"]["recallAt3"], row["key"]))
    report = {
        "schemaVersion": 1, "reportVersion": VERSION, "imageCount": 1016,
        "protocol": "four-fold-leave-one-consumed-view-out", "folds": 4,
        "candidateCount": len(evaluations), "candidates": evaluations,
        "selectedCandidate": selected["key"], "selectedDevelopmentMetrics": selected["mean"],
        "v3FourViewBaselineAccuracy": .7992126,
        "developmentAccuracyUplift": round(selected["mean"]["accuracy"] - .7992126, 8),
        "independentTestOpened": False, "qualityConfirmed": False,
        "productionTrainingAllowed": False, "promotionAllowed": False,
    }
    _write(report_path, report)
    return report


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    repo = Path(__file__).resolve().parents[4]
    dataset = repo / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    embeddings = dataset / "development-multiregion-embeddings.v4.json"
    preliminary = repo / "apps/demand-engine/evaluation/results/full-taxonomy-visual-multiregion-development.v4.json"
    if not embeddings.exists() and not preliminary.exists():
        develop(dataset, repo / "apps/demand-engine/models/clip-vit-b32-visual-evidence.v1.json", embeddings, preliminary)
    report = freeze_candidate(
        embeddings, preliminary,
        repo / "apps/demand-engine/evaluation/results/full-taxonomy-visual-multiregion-robust-development.v4.json",
        repo / "apps/demand-engine/models/full-taxonomy-visual-multiregion-classifier.v4.json",
        repo / "apps/demand-engine/policies/full-taxonomy-visual-multiregion-holdout.v4.json",
    )
    print(json.dumps({"selected": report["selectedCandidate"], "metrics": report["selectedDevelopmentMetrics"]}, ensure_ascii=False))


if __name__ == "__main__":
    run()
