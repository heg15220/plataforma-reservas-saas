"""Registra la aprobación humana explícita de las 508 imágenes nuevas v3.

La autorización habilita exclusivamente development y evaluación offline. No
habilita entrenamiento productivo, promoción ni consume el holdout sellado.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .full_taxonomy_visual_multiview_v3_review import REQUIRED_APPROVAL_PHRASE


def _sha256(path: Path) -> str:
    """Devuelve el sello SHA-256 de un artefacto de autorización."""

    return hashlib.sha256(path.read_bytes()).hexdigest()


def _write_json_atomic(path: Path, payload: dict[str, Any]) -> None:
    """Publica JSON completo sin exponer un archivo parcialmente escrito."""

    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    temporary.replace(path)


def authorize(
    manifest_path: Path,
    qa_path: Path,
    checklist_path: Path,
    review_summary_path: Path,
    output_path: Path,
    approval_phrase: str,
    reviewed_at: str | None = None,
) -> dict[str, Any]:
    """Valida evidencia y registra una aprobación offline fail-closed."""

    if approval_phrase != REQUIRED_APPROVAL_PHRASE:
        raise ValueError("FULL_TAXONOMY_V3_APPROVAL_PHRASE_INVALID")

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    qa = json.loads(qa_path.read_text(encoding="utf-8"))
    checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
    review = json.loads(review_summary_path.read_text(encoding="utf-8"))
    source_hashes = {
        "manifestBeforeAuthorizationSha256": _sha256(manifest_path),
        "qaReportSha256": _sha256(qa_path),
        "checklistBeforeAuthorizationSha256": _sha256(checklist_path),
        "reviewSummaryBeforeAuthorizationSha256": _sha256(review_summary_path),
    }

    if (
        manifest.get("materialization", {}).get("complete") is not True
        or manifest.get("humanReviewComplete") is not False
        or qa.get("qaPassed") is not True
        or qa.get("ocrScanComplete") is not True
        or qa.get("evaluatedImageCount") != 508
        or qa.get("holdoutPredictionsComputed") is not False
        or qa.get("holdoutBudgetConsumed") != 0
        or review.get("reviewStatus") != "awaitingExplicitHumanApproval"
        or review.get("reviewImageCount") != 508
        or checklist.get("summary", {}).get("pendingCount") != 508
    ):
        raise ValueError("FULL_TAXONOMY_V3_APPROVAL_PREREQUISITES_INVALID")

    development_c = [
        row for row in manifest["developmentRows"] if row.get("developmentView") == "C"
    ]
    holdout = list(manifest["holdoutRows"])
    governed = {row["imageId"]: row for row in [*development_c, *holdout]}
    if len(development_c) != 254 or len(holdout) != 254 or len(governed) != 508:
        raise ValueError("FULL_TAXONOMY_V3_APPROVAL_MANIFEST_ROWS_INVALID")
    if len(checklist["rows"]) != 508:
        raise ValueError("FULL_TAXONOMY_V3_APPROVAL_CHECKLIST_ROWS_INVALID")

    for checklist_row in checklist["rows"]:
        manifest_row = governed.get(checklist_row["imageId"])
        if (
            manifest_row is None
            or checklist_row["humanReviewStatus"] != "pendingHumanReview"
            or checklist_row["relativePath"] != manifest_row["relativePath"]
            or checklist_row["imageSha256"] != manifest_row["generation"]["imageSha256"]
        ):
            raise ValueError("FULL_TAXONOMY_V3_APPROVAL_LINEAGE_MISMATCH")

    decided_at = reviewed_at or datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    reviewer = "projectOwnerExplicitConversationApproval"
    for row in development_c:
        row["generation"]["status"] = "materializedHumanApproved"
        row["humanReviewStatus"] = "approved"
        row["humanReviewer"] = reviewer
        row["humanReviewedAt"] = decided_at
        row["developmentTrainingAllowed"] = True
        row["productionTrainingAllowed"] = False
    for row in holdout:
        row["generation"]["status"] = "materializedHumanApproved"
        row["humanReviewStatus"] = "approved"
        row["humanReviewer"] = reviewer
        row["humanReviewedAt"] = decided_at
        row["testEvaluationAllowed"] = True
        row["productionTrainingAllowed"] = False

    for row in checklist["rows"]:
        row["reviewChecks"] = {key: True for key in row["reviewChecks"]}
        row["humanReviewStatus"] = "approved"
        row["humanReviewer"] = reviewer
        row["humanReviewedAt"] = decided_at
        row["humanReviewNotes"] = (
            "Aprobación explícita tras revisar hojas generales y alertas OCR anotadas."
        )
    checklist["summary"] = {
        "rowCount": 508,
        "pendingCount": 0,
        "approvedCount": 508,
        "rejectedCount": 0,
    }

    manifest.update(
        {
            "humanReviewComplete": True,
            "developmentTrainingAllowed": True,
            "holdoutEvaluationAllowed": True,
            "trainingAllowed": True,
            "productionTrainingAllowed": False,
            "promotionAllowed": False,
            "humanReviewedAt": decided_at,
            "authorizationRecord": output_path.name,
        }
    )
    review.update(
        {
            "reviewStatus": "approvedForOfflineDevelopmentAndSealedEvaluation",
            "humanReviewComplete": True,
            "developmentTrainingAllowed": True,
            "holdoutEvaluationAllowed": True,
            "productionTrainingAllowed": False,
            "promotionAllowed": False,
            "humanReviewedAt": decided_at,
            "authorizationRecord": output_path.name,
        }
    )

    record = {
        "schemaVersion": 1,
        "datasetVersion": manifest["datasetVersion"],
        "authorizedAt": decided_at,
        "authorizationSource": "explicitUserStatementInProjectConversation",
        "approvalPhraseSha256": hashlib.sha256(approval_phrase.encode("utf-8")).hexdigest(),
        "approvedImageCount": 508,
        "developmentCImageCount": 254,
        "holdoutV3ImageCount": 254,
        "ocrFlaggedImageCountReviewed": review["ocrFlaggedImageCount"],
        "ocrFindingCountReviewed": review["ocrFindingCount"],
        **source_hashes,
        "decision": "approvedForOfflineDevelopmentAndSealedEvaluation",
        "humanReviewComplete": True,
        "developmentTrainingAllowed": True,
        "holdoutEvaluationAllowed": True,
        "productionTrainingAllowed": False,
        "promotionAllowed": False,
        "clipLoaded": False,
        "embeddingsExtracted": False,
        "holdoutPredictionsComputed": False,
        "holdoutBudgetConsumed": 0,
    }

    _write_json_atomic(manifest_path, manifest)
    _write_json_atomic(checklist_path, checklist)
    _write_json_atomic(review_summary_path, review)
    _write_json_atomic(output_path, record)
    return record


def run() -> None:
    """CLI de autorización humana v3 con frase exacta obligatoria."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=root / "generation-manifest.v3.json")
    parser.add_argument("--qa", type=Path, default=root / "qa-report.v3.json")
    parser.add_argument("--checklist", type=Path, default=root / "human-review-checklist.v3.json")
    parser.add_argument("--review-summary", type=Path, default=root / "human-review-summary.v3.json")
    parser.add_argument("--output", type=Path, default=root / "human-review-authorization.v3.json")
    parser.add_argument("--approval", required=True)
    args = parser.parse_args()
    result = authorize(
        args.manifest,
        args.qa,
        args.checklist,
        args.review_summary,
        args.output,
        args.approval,
    )
    print(
        json.dumps(
            {
                "approvedImageCount": result["approvedImageCount"],
                "humanReviewComplete": result["humanReviewComplete"],
                "productionTrainingAllowed": result["productionTrainingAllowed"],
                "holdoutBudgetConsumed": result["holdoutBudgetConsumed"],
            }
        )
    )


if __name__ == "__main__":
    run()
