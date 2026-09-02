"""Pruebas de autorización humana explícita del corpus visual v3."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiview_v3_authorization import authorize
from reserly_demand_engine.full_taxonomy_visual_multiview_v3_review import (
    REQUIRED_APPROVAL_PHRASE,
)


REPO_ROOT = Path(__file__).resolve().parents[3]
ROOT = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"


def _copy_inputs(tmp_path: Path) -> tuple[Path, Path, Path, Path]:
    names = (
        "generation-manifest.v3.json",
        "qa-report.v3.json",
        "human-review-checklist.v3.json",
        "human-review-summary.v3.json",
    )
    paths: list[Path] = []
    for name in names:
        target = tmp_path / name
        target.write_bytes((ROOT / name).read_bytes())
        paths.append(target)
    manifest = json.loads(paths[0].read_text(encoding="utf-8"))
    manifest.update(
        {
            "humanReviewComplete": False,
            "developmentTrainingAllowed": False,
            "holdoutEvaluationAllowed": False,
            "trainingAllowed": False,
            "productionTrainingAllowed": False,
            "promotionAllowed": False,
        }
    )
    manifest.pop("humanReviewedAt", None)
    manifest.pop("authorizationRecord", None)
    for row in manifest["developmentRows"]:
        if row.get("developmentView") != "C":
            continue
        row["generation"]["status"] = "materializedPendingHumanReview"
        row["humanReviewStatus"] = "pendingHumanReview"
        row["developmentTrainingAllowed"] = False
        row.pop("humanReviewer", None)
        row.pop("humanReviewedAt", None)
    for row in manifest["holdoutRows"]:
        row["generation"]["status"] = "materializedPendingHumanReview"
        row["humanReviewStatus"] = "pendingHumanReview"
        row["testEvaluationAllowed"] = False
        row.pop("humanReviewer", None)
        row.pop("humanReviewedAt", None)
    paths[0].write_text(json.dumps(manifest), encoding="utf-8")

    checklist = json.loads(paths[2].read_text(encoding="utf-8"))
    for row in checklist["rows"]:
        row["reviewChecks"] = {key: None for key in row["reviewChecks"]}
        row["humanReviewStatus"] = "pendingHumanReview"
        row["humanReviewer"] = None
        row.pop("humanReviewedAt", None)
        row["humanReviewNotes"] = None
    checklist["summary"] = {
        "rowCount": 508,
        "pendingCount": 508,
        "approvedCount": 0,
        "rejectedCount": 0,
    }
    paths[2].write_text(json.dumps(checklist), encoding="utf-8")

    review = json.loads(paths[3].read_text(encoding="utf-8"))
    review.update(
        {
            "reviewStatus": "awaitingExplicitHumanApproval",
            "humanReviewComplete": False,
            "developmentTrainingAllowed": False,
            "holdoutEvaluationAllowed": False,
            "productionTrainingAllowed": False,
            "promotionAllowed": False,
        }
    )
    review.pop("humanReviewedAt", None)
    review.pop("authorizationRecord", None)
    paths[3].write_text(json.dumps(review), encoding="utf-8")
    return paths[0], paths[1], paths[2], paths[3]


def test_canonical_authorization_is_complete_and_production_stays_blocked() -> None:
    record = json.loads(
        (ROOT / "human-review-authorization.v3.json").read_text(encoding="utf-8")
    )

    assert record["approvedImageCount"] == 508
    assert record["humanReviewComplete"] is True
    assert record["developmentTrainingAllowed"] is True
    assert record["holdoutEvaluationAllowed"] is True
    assert record["productionTrainingAllowed"] is False
    assert record["promotionAllowed"] is False
    assert record["holdoutBudgetConsumed"] == 0


def test_authorization_requires_exact_v3_phrase(tmp_path: Path) -> None:
    manifest, qa, checklist, review = _copy_inputs(tmp_path)
    with pytest.raises(ValueError, match="APPROVAL_PHRASE_INVALID"):
        authorize(manifest, qa, checklist, review, tmp_path / "authorization.json", "apruebo")


def test_authorization_binds_lineage_and_enables_only_offline_use(tmp_path: Path) -> None:
    manifest_path, qa, checklist_path, review_path = _copy_inputs(tmp_path)
    record = authorize(
        manifest_path,
        qa,
        checklist_path,
        review_path,
        tmp_path / "authorization.json",
        REQUIRED_APPROVAL_PHRASE,
        "2026-09-02T12:00:00+00:00",
    )
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
    review = json.loads(review_path.read_text(encoding="utf-8"))
    development_c = [
        row for row in manifest["developmentRows"] if row.get("developmentView") == "C"
    ]

    assert record["approvedImageCount"] == 508
    assert record["ocrFlaggedImageCountReviewed"] == 184
    assert record["ocrFindingCountReviewed"] == 253
    assert record["holdoutBudgetConsumed"] == 0
    assert all(row["humanReviewStatus"] == "approved" for row in development_c)
    assert all(row["developmentTrainingAllowed"] for row in development_c)
    assert all(row["humanReviewStatus"] == "approved" for row in manifest["holdoutRows"])
    assert all(row["testEvaluationAllowed"] for row in manifest["holdoutRows"])
    assert checklist["summary"]["approvedCount"] == 508
    assert all(all(row["reviewChecks"].values()) for row in checklist["rows"])
    assert review["humanReviewComplete"] is True
    assert manifest["developmentTrainingAllowed"] is True
    assert manifest["holdoutEvaluationAllowed"] is True
    assert manifest["productionTrainingAllowed"] is False
    assert manifest["promotionAllowed"] is False


def test_authorization_rejects_checklist_hash_mismatch(tmp_path: Path) -> None:
    manifest, qa, checklist_path, review = _copy_inputs(tmp_path)
    checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
    checklist["rows"][0]["imageSha256"] = "0" * 64
    checklist_path.write_text(json.dumps(checklist), encoding="utf-8")

    with pytest.raises(ValueError, match="APPROVAL_LINEAGE_MISMATCH"):
        authorize(
            manifest,
            qa,
            checklist_path,
            review,
            tmp_path / "authorization.json",
            REQUIRED_APPROVAL_PHRASE,
        )
