"""Aplica una decisión humana explícita sin mutar la definición congelada original.

La autorización produce copias aprobadas del manifiesto y de la definición. Falla
si el alcance, cardinalidad o identidad del dataset no coinciden exactamente.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path
from typing import Any


def apply_human_approval(
    definition_path: Path,
    manifest_path: Path,
    approval_path: Path,
    output_definition_path: Path,
    output_manifest_path: Path,
) -> dict[str, Any]:
    """Autoriza todas las filas tras verificar una aprobación humana cerrada."""

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    manifest = [
        json.loads(line)
        for line in manifest_path.read_text(encoding="utf-8").splitlines()
    ]
    approval = json.loads(approval_path.read_text(encoding="utf-8"))
    decided_at = datetime.fromisoformat(approval.get("decidedAt", ""))
    if decided_at.tzinfo is None or decided_at.utcoffset() is None:
        raise ValueError("VISUAL_APPROVAL_TIMEZONE_REQUIRED")
    pending_definition = [
        row for row in definition["rows"] if row["humanReviewStatus"] == "pending"
    ]
    pending_manifest = [
        row for row in manifest if row["humanReviewStatus"] == "pending"
    ]
    accepted_scopes = {"all-provisional-images", "all-pending-images"}
    if (
        approval.get("schemaVersion") != 1
        or approval.get("decision") != "approved"
        or approval.get("scope") not in accepted_scopes
        or approval.get("datasetVersion") != definition["datasetVersion"]
        or approval.get("approvedImageCount") != len(pending_definition)
        or approval.get("confirmedThreeSheetsReviewed") is not True
        or not approval.get("reviewer")
        or not approval.get("statement")
    ):
        raise ValueError("VISUAL_APPROVAL_CONTRACT_INVALID")
    definition_ids = {row["imageId"] for row in definition["rows"]}
    manifest_ids = {row["imageId"] for row in manifest}
    if (
        len(definition["rows"]) not in {120, 200}
        or len(manifest) != len(definition["rows"])
        or definition_ids != manifest_ids
        or not pending_definition
        or {row["imageId"] for row in pending_definition}
        != {row["imageId"] for row in pending_manifest}
        or any(
            row["humanReviewStatus"] not in {"approved", "pending"}
            for row in definition["rows"]
        )
        or any(
            row["humanReviewStatus"] not in {"approved", "pending"}
            for row in manifest
        )
    ):
        raise ValueError("VISUAL_APPROVAL_DATASET_MISMATCH")
    review_fields = {
        "humanReviewStatus": "approved",
        "humanReviewer": approval["reviewer"],
        "humanReviewedAt": approval["decidedAt"],
        "developmentTrainingAllowed": True,
        "productionTrainingAllowed": False,
    }
    approved_count = len(definition["rows"])
    approved_status = (
        "approved_for_definitive_training"
        if approved_count == 200
        else "approved_for_provisional_training"
    )
    approved_definition = {
        **definition,
        "status": approved_status,
        "definitiveContractSatisfied": approved_count == 200,
        "humanApproval": {
            "reviewer": approval["reviewer"],
            "decidedAt": approval["decidedAt"],
            "source": approval["source"],
            "approvedImageCount": approval["approvedImageCount"],
            "totalApprovedImageCount": approved_count,
        },
        "rows": [{**row, **review_fields} for row in definition["rows"]],
    }
    approved_manifest = [{**row, **review_fields} for row in manifest]
    output_definition_path.write_text(
        json.dumps(approved_definition, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    output_manifest_path.write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in approved_manifest),
        encoding="utf-8",
    )
    return approved_definition


def run() -> None:
    """CLI para aplicar una aprobación explícita a una versión congelada."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--definition", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--approval", type=Path, required=True)
    parser.add_argument("--output-definition", type=Path, required=True)
    parser.add_argument("--output-manifest", type=Path, required=True)
    args = parser.parse_args()
    result = apply_human_approval(
        args.definition,
        args.manifest,
        args.approval,
        args.output_definition,
        args.output_manifest,
    )
    print(json.dumps({"status": result["status"], "rows": len(result["rows"])}))


if __name__ == "__main__":
    run()
