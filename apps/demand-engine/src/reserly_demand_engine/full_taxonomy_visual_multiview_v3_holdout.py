"""Apertura única y fail-closed del holdout visual taxonómico multivista v3.

Verifica el pretest lock antes de leer píxeles, extrae exactamente 254 embeddings
CLIP, aplica las dos cabezas congeladas y conserva las métricas aunque fallen las
puertas. No selecciona modelos ni modifica parámetros después de observar test.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder
from .full_taxonomy_visual_multiview_v3_training import _metrics, _resolve, _sha256, _write


RESULT_VERSION = "full-taxonomy-visual-multiview-holdout-v3"


def _fingerprint(rows: list[dict[str, Any]]) -> str:
    """Recalcula el fingerprint que se congeló antes de observar holdout."""

    return hashlib.sha256(
        json.dumps(
            [{
                "imageId": row["imageId"], "venueId": row["venueId"],
                "typeCode": row["typeCode"], "familyCode": row["familyCode"],
                "archetypeCode": row["visualArchetype"]["code"],
                "imageSha256": row["generation"]["imageSha256"],
            } for row in rows],
            sort_keys=True, separators=(",", ":"),
        ).encode("utf-8")
    ).hexdigest()


def _verify_preflight(
    manifest_path: Path, authorization_path: Path, model_manifest_path: Path,
    development_embeddings_path: Path, development_report_path: Path,
    model_path: Path, policy_path: Path, lock_path: Path,
    holdout_embeddings_path: Path, result_path: Path, opening_record_path: Path,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], dict[str, Any]]:
    """Rechaza reapertura, deriva hashes actuales y valida autorización/linaje."""

    if any(path.exists() for path in (holdout_embeddings_path, result_path, opening_record_path)):
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_ALREADY_OPENED")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    authorization = json.loads(authorization_path.read_text(encoding="utf-8"))
    model = json.loads(model_path.read_text(encoding="utf-8"))
    policy = json.loads(policy_path.read_text(encoding="utf-8"))
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    expected = {
        "manifestSha256": _sha256(manifest_path),
        "authorizationSha256": _sha256(authorization_path),
        "clipModelManifestSha256": _sha256(model_manifest_path),
        "developmentEmbeddingsSha256": _sha256(development_embeddings_path),
        "developmentReportSha256": _sha256(development_report_path),
        "modelSha256": _sha256(model_path),
        "policySha256": _sha256(policy_path),
    }
    if any(lock.get(key) != value for key, value in expected.items()):
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_PRETEST_HASH_MISMATCH")
    rows = manifest.get("holdoutRows", [])
    if (
        lock.get("budget") != 1 or lock.get("consumed") != 0
        or lock.get("reopenAllowed") is not False
        or policy.get("holdoutPredictionBudget") != 1
        or len(rows) != lock.get("holdoutImageCount") or len(rows) != 254
        or _fingerprint(rows) != lock.get("holdoutFingerprintSha256")
    ):
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_BUDGET_OR_FINGERPRINT_INVALID")
    if (
        manifest.get("humanReviewComplete") is not True
        or manifest.get("holdoutEvaluationAllowed") is not True
        or authorization.get("holdoutEvaluationAllowed") is not True
        or authorization.get("holdoutV3ImageCount") != 254
        or authorization.get("holdoutBudgetConsumed") != 0
        or any(row.get("humanReviewStatus") != "approved" or row.get("testEvaluationAllowed") is not True for row in rows)
        or len({row["imageId"] for row in rows}) != 254
        or len({row["venueId"] for row in rows}) != 254
    ):
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_NOT_AUTHORIZED")
    if model.get("inputFeatures") != ["clipImageEmbedding512"] or set(model.get("prohibitedInputFeatures", [])) != {"prompt", "typeCode", "familyCode", "archetypeCode"}:
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_MODEL_CONTRACT_INVALID")
    return manifest, model, policy, lock


def _family_scores(head: dict[str, Any], features: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Aplica el prototipo por tipo fusionado con arquetipo predicho."""

    if head.get("kind") != "archetypeFusion":
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_FAMILY_HEAD_UNSUPPORTED")
    classes = np.asarray(head["classes"])
    prototypes = np.asarray(head["prototypes"], dtype=np.float64)
    labels = np.asarray(head["prototypeLabels"])
    similarities = features @ prototypes.T
    base = np.column_stack([
        similarities[:, labels == family].max(axis=1) for family in classes
    ])
    weights = np.asarray(head["archetypeWeights"], dtype=np.float64)
    intercept = np.asarray(head["archetypeIntercept"], dtype=np.float64)
    logits = features @ weights.T + intercept
    logits -= logits.max(axis=1, keepdims=True)
    probabilities = np.exp(logits)
    probabilities /= probabilities.sum(axis=1, keepdims=True).clip(min=1e-12)
    auxiliary = probabilities @ np.asarray(head["archetypeToFamily"], dtype=np.float64)
    base = (base - base.mean(axis=1, keepdims=True)) / base.std(axis=1, keepdims=True).clip(min=1e-9)
    auxiliary = (auxiliary - auxiliary.mean(axis=1, keepdims=True)) / auxiliary.std(axis=1, keepdims=True).clip(min=1e-9)
    return base + float(head["alpha"]) * auxiliary, classes


def _archetype_scores(head: dict[str, Any], features: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Aplica la cabeza LDA auxiliar congelada."""

    if head.get("kind") != "lda":
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_ARCHETYPE_HEAD_UNSUPPORTED")
    scores = features @ np.asarray(head["weights"], dtype=np.float64).T
    scores += np.asarray(head["intercept"], dtype=np.float64)
    return scores, np.asarray(head["classes"])


def open_holdout(
    manifest_path: Path, authorization_path: Path, model_manifest_path: Path,
    development_embeddings_path: Path, development_report_path: Path,
    model_path: Path, policy_path: Path, lock_path: Path,
    holdout_embeddings_path: Path, result_path: Path, opening_record_path: Path,
    batch_size: int = 8,
) -> dict[str, Any]:
    """Abre una vez, conserva embeddings/métricas y consume el presupuesto."""

    manifest, model, policy, lock = _verify_preflight(
        manifest_path, authorization_path, model_manifest_path,
        development_embeddings_path, development_report_path, model_path,
        policy_path, lock_path, holdout_embeddings_path, result_path, opening_record_path,
    )
    clip_manifest = ClipVisualManifest.load(model_manifest_path)
    if clip_manifest.modelKey != model["clipModelKey"] or clip_manifest.revision != model["clipModelRevision"]:
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_CLIP_MODEL_MISMATCH")
    rows = manifest["holdoutRows"]
    dataset_root = manifest_path.parent
    evaluation_root = dataset_root.parent
    paths = []
    for row in rows:
        path = _resolve(evaluation_root, dataset_root, row["relativePath"])
        if _sha256(path) != row["generation"]["imageSha256"]:
            raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_IMAGE_HASH_MISMATCH")
        paths.append(path)

    embedder = HuggingFaceClipEmbedder(clip_manifest, local_files_only=True)
    vectors: list[list[float]] = []
    for start in range(0, len(paths), batch_size):
        vectors.extend(vector.values for vector in embedder.encode_images(paths[start:start + batch_size]))
    features = np.asarray(vectors, dtype=np.float64)
    if features.shape != (254, 512) or not np.all(np.isfinite(features)):
        raise ValueError("FULL_TAXONOMY_V3_HOLDOUT_EMBEDDINGS_INVALID")

    family_scores, family_classes = _family_scores(model["familyHead"], features)
    archetype_scores, archetype_classes = _archetype_scores(model["archetypeHead"], features)
    family_labels = np.asarray([row["familyCode"] for row in rows])
    archetype_labels = np.asarray([row["visualArchetype"]["code"] for row in rows])
    family_metrics = _metrics(family_labels, family_scores, family_classes)
    archetype_metrics = _metrics(archetype_labels, archetype_scores, archetype_classes)
    development = json.loads(development_report_path.read_text(encoding="utf-8"))
    development_metrics = development["selectedFamilyDevelopmentMetrics"]
    gap = round(abs(development_metrics["accuracy"] - family_metrics["accuracy"]), 8)
    gates = {
        "accuracy": family_metrics["accuracy"] >= policy["minimumHoldoutAccuracy"],
        "error": family_metrics["error"] <= policy["maximumHoldoutError"],
        "macroPrecision": family_metrics["macroPrecision"] >= policy["minimumMacroPrecision"],
        "macroRecall": family_metrics["macroRecall"] >= policy["minimumMacroRecall"],
        "macroF1": family_metrics["macroF1"] >= policy["minimumMacroF1"],
        "minimumClassRecall": family_metrics["minimumClassRecall"] >= policy["minimumPerClassRecall"],
        "generalizationGap": gap <= policy["maximumGeneralizationGap"],
    }
    passed = all(gates.values())
    embedding_artifact = {
        "schemaVersion": 1, "datasetVersion": manifest["datasetVersion"], "split": "sealedHoldoutV3-consumed",
        "modelKey": clip_manifest.modelKey, "modelRevision": clip_manifest.revision,
        "dimensions": 512, "imageCount": 254,
        "rows": [{
            "imageId": row["imageId"], "venueId": row["venueId"], "sourceId": row["sourceId"],
            "typeCode": row["typeCode"], "familyCode": row["familyCode"],
            "archetypeCode": row["visualArchetype"]["code"],
            "imageSha256": row["generation"]["imageSha256"], "embedding": vector,
        } for row, vector in zip(rows, vectors, strict=True)],
        "productionTrainingAllowed": False, "promotionAllowed": False,
    }
    _write(holdout_embeddings_path, embedding_artifact, compact=True)
    result = {
        "schemaVersion": 1, "reportVersion": RESULT_VERSION,
        "datasetVersion": manifest["datasetVersion"], "imageCount": 254,
        "typeCount": len({row["typeCode"] for row in rows}),
        "familyCount": len(family_classes), "archetypeCount": len(archetype_classes),
        "familyCandidate": model["familyHead"]["key"], "familyMetrics": family_metrics,
        "archetypeCandidate": model["archetypeHead"]["key"], "archetypeMetrics": archetype_metrics,
        "developmentFamilyMetrics": development_metrics, "generalizationAccuracyGap": gap,
        "gateResults": gates, "qualityGatesPassed": passed,
        "holdoutPredictionBudget": 1, "holdoutPredictionConsumed": 1,
        "selectionUsedHoldout": False, "productionEvidence": False,
        "promotionAllowed": False, "trainingAllowed": False,
        "fallback": "non-visual-taxonomy-and-contextual-ranking",
    }
    _write(result_path, result)
    opening_record = {
        "schemaVersion": 1, "status": "consumed", "budget": 1, "consumed": 1,
        "reopenAllowed": False, "selectionUsedHoldout": False,
        "pretestLockSha256": _sha256(lock_path),
        "holdoutEmbeddingsSha256": _sha256(holdout_embeddings_path),
        "resultSha256": _sha256(result_path),
        "familyQualityGatesPassed": passed,
    }
    _write(opening_record_path, opening_record)
    consumed_lock = dict(lock)
    consumed_lock.update({
        "consumed": 1, "status": "consumed",
        "holdoutEmbeddingsSha256": _sha256(holdout_embeddings_path),
        "holdoutResultSha256": _sha256(result_path),
        "openingRecordSha256": _sha256(opening_record_path),
        "qualityGatesPassed": passed,
    })
    _write(lock_path, consumed_lock)
    return result


def run() -> None:
    """CLI sin argumentos; las rutas versionadas son parte del protocolo."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    repo = Path(__file__).resolve().parents[4]
    dataset = repo / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    results = repo / "apps/demand-engine/evaluation/results"
    models = repo / "apps/demand-engine/models"
    policies = repo / "apps/demand-engine/policies"
    result = open_holdout(
        dataset / "generation-manifest.v3.json", dataset / "human-review-authorization.v3.json",
        models / "clip-vit-b32-visual-evidence.v1.json", dataset / "development-clip-embeddings.v3.json",
        results / "full-taxonomy-visual-multiview-development.v3.json",
        models / "full-taxonomy-visual-multiview-classifier.v3.json",
        policies / "full-taxonomy-visual-multiview-holdout.v3.json", dataset / "pretest-lock.v3.json",
        dataset / "holdout-clip-embeddings.v3.json",
        results / "full-taxonomy-visual-multiview-holdout.v3.json",
        dataset / "holdout-opening-record.v3.json",
    )
    print(json.dumps({"familyMetrics": result["familyMetrics"], "qualityGatesPassed": result["qualityGatesPassed"]}, ensure_ascii=False))


if __name__ == "__main__":
    run()
