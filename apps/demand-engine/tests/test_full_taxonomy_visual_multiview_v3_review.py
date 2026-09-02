"""Pruebas del paquete de revisión humana v3 previo a autorización."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_multiview_v3_review import (
    REQUIRED_APPROVAL_PHRASE,
    build_review_package,
)


REPO_ROOT = Path(__file__).resolve().parents[3]
ROOT = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"


def _pending_checklist(tmp_path: Path) -> Path:
    checklist = json.loads(
        (ROOT / "human-review-checklist.v3.json").read_text(encoding="utf-8")
    )
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
    path = tmp_path / "human-review-checklist.v3.json"
    path.write_text(json.dumps(checklist), encoding="utf-8")
    return path


def test_review_package_is_complete_but_cannot_self_authorize(tmp_path: Path) -> None:
    checklist = _pending_checklist(tmp_path)
    summary = build_review_package(
        checklist,
        ROOT / "qa-report.v3.json",
        tmp_path / "sheets",
        tmp_path / "summary.json",
        ROOT,
    )

    assert summary["reviewImageCount"] == 508
    assert summary["developmentCImageCount"] == summary["holdoutV3ImageCount"] == 254
    assert summary["emptyVenuePreferredCount"] == 377
    assert summary["backgroundAdultsNonIdentifiableCount"] == 131
    assert summary["ocrFlaggedImageCount"] == 184
    assert summary["ocrFindingCount"] == 253
    assert len(summary["ocrAlertSheets"]) == 8
    assert summary["approvalPhraseRequired"] == REQUIRED_APPROVAL_PHRASE
    assert summary["reviewStatus"] == "awaitingExplicitHumanApproval"
    assert summary["humanReviewComplete"] is False
    assert summary["developmentTrainingAllowed"] is False
    assert summary["holdoutEvaluationAllowed"] is False
    assert summary["holdoutPredictionsComputed"] is False
    assert summary["holdoutBudgetConsumed"] == 0
    assert len(list((tmp_path / "sheets").glob("*.jpg"))) == 8


def test_review_package_rejects_incomplete_qa(tmp_path: Path) -> None:
    checklist = _pending_checklist(tmp_path)
    qa = json.loads((ROOT / "qa-report.v3.json").read_text(encoding="utf-8"))
    qa["ocrScanComplete"] = False
    qa_path = tmp_path / "qa.json"
    qa_path.write_text(json.dumps(qa), encoding="utf-8")

    with pytest.raises(ValueError, match="REVIEW_QA_INCOMPLETE"):
        build_review_package(
            checklist,
            qa_path,
            tmp_path / "sheets",
            tmp_path / "summary.json",
            ROOT,
        )
