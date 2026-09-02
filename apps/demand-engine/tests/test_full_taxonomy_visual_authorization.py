"""Pruebas de autorización humana del corpus v2."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from reserly_demand_engine.full_taxonomy_visual_authorization import APPROVAL_PHRASE, authorize


REPO_ROOT = Path(__file__).resolve().parents[3]
ROOT = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"


def _copy_inputs(tmp_path: Path) -> tuple[Path, Path]:
    manifest = tmp_path / "manifest.json"
    qa = tmp_path / "qa.json"
    manifest.write_bytes((ROOT / "generation-manifest.v2.json").read_bytes())
    qa.write_bytes((ROOT / "qa-report.v2.json").read_bytes())
    return manifest, qa


def test_authorization_requires_exact_user_phrase(tmp_path: Path) -> None:
    manifest, qa = _copy_inputs(tmp_path)
    with pytest.raises(ValueError, match="APPROVAL_PHRASE_INVALID"):
        authorize(manifest, qa, tmp_path / "record.json", "apruebo")


def test_authorization_approves_offline_rows_but_never_production(tmp_path: Path) -> None:
    manifest, qa = _copy_inputs(tmp_path)
    record = authorize(manifest, qa, tmp_path / "record.json", APPROVAL_PHRASE)
    updated = json.loads(manifest.read_text(encoding="utf-8"))

    assert record["approvedImageCount"] == 508
    assert record["holdoutBudgetConsumed"] == 0
    assert all(row["humanReviewStatus"] == "approved" for row in updated["developmentRows"])
    assert all(row["developmentTrainingAllowed"] for row in updated["developmentRows"])
    assert all(row["humanReviewStatus"] == "approved" for row in updated["holdoutRows"])
    assert all(row["testEvaluationAllowed"] for row in updated["holdoutRows"])
    assert updated["productionTrainingAllowed"] is False
    assert updated["promotionAllowed"] is False
