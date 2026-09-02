"""Registra la aprobación humana explícita del corpus taxonómico visual v2."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


APPROVAL_PHRASE = "Apruebo las 508 imágenes del dataset visual taxonómico v2"
AUTHORIZED_AT = "2026-08-31T00:30:00Z"


def authorize(
    manifest_path: Path,
    qa_path: Path,
    output_path: Path,
    approval_phrase: str,
) -> dict[str, Any]:
    """Aprueba las 508 filas si materialización y QA están completas.

    La autorización permite desarrollo y evaluación offline; nunca habilita
    entrenamiento productivo, promoción o inferencias sensibles.
    """

    if approval_phrase != APPROVAL_PHRASE:
        raise ValueError("FULL_TAXONOMY_VISUAL_APPROVAL_PHRASE_INVALID")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    qa = json.loads(qa_path.read_text(encoding="utf-8"))
    if (
        manifest.get("materialization", {}).get("complete") is not True
        or qa.get("qaPassed") is not True
        or qa.get("evaluatedImageCount") != 508
        or qa.get("holdoutBudgetConsumed") != 0
        or qa.get("holdoutPredictionsComputed") is not False
    ):
        raise ValueError("FULL_TAXONOMY_VISUAL_APPROVAL_PREREQUISITES_INVALID")

    for row in manifest["developmentRows"]:
        row["humanReviewStatus"] = "approved"
        row["developmentTrainingAllowed"] = True
        row["productionTrainingAllowed"] = False
    for row in manifest["holdoutRows"]:
        row["humanReviewStatus"] = "approved"
        row["testEvaluationAllowed"] = True
        row["productionTrainingAllowed"] = False
    manifest["humanReviewComplete"] = True
    manifest["developmentTrainingAllowed"] = True
    manifest["holdoutEvaluationAllowed"] = True
    manifest["trainingAllowed"] = True
    manifest["productionTrainingAllowed"] = False
    manifest["promotionAllowed"] = False
    manifest["authorizationRecord"] = output_path.name
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    record = {
        "schemaVersion": 1,
        "datasetVersion": manifest["datasetVersion"],
        "authorizedAt": AUTHORIZED_AT,
        "authorizationSource": "explicitUserStatementInProjectConversation",
        "approvalPhraseSha256": hashlib.sha256(approval_phrase.encode("utf-8")).hexdigest(),
        "approvedImageCount": 508,
        "developmentImageCount": 254,
        "holdoutImageCount": 254,
        "qaReportSha256": hashlib.sha256(qa_path.read_bytes()).hexdigest(),
        "decision": "approvedForOfflineDevelopmentAndSealedEvaluation",
        "humanReviewComplete": True,
        "developmentTrainingAllowed": True,
        "holdoutEvaluationAllowed": True,
        "productionTrainingAllowed": False,
        "promotionAllowed": False,
        "holdoutBudgetConsumed": 0,
    }
    output_path.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return record


def run() -> None:
    """CLI de autorización fail-closed."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=root / "generation-manifest.v2.json")
    parser.add_argument("--qa", type=Path, default=root / "qa-report.v2.json")
    parser.add_argument("--output", type=Path, default=root / "human-review-authorization.v2.json")
    parser.add_argument("--approval", required=True)
    args = parser.parse_args()
    result = authorize(args.manifest, args.qa, args.output, args.approval)
    print(json.dumps({"approvedImageCount": result["approvedImageCount"], "productionTrainingAllowed": False}))


if __name__ == "__main__":
    run()
