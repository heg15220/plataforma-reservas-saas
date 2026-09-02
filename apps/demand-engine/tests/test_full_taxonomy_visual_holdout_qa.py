"""Regresión del sellado y QA pre-inferencia del corpus v2."""

from __future__ import annotations

import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
ROOT = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"


def test_manifest_is_complete_disjoint_and_offline_approved() -> None:
    manifest = json.loads((ROOT / "generation-manifest.v2.json").read_text(encoding="utf-8"))
    development = manifest["developmentRows"]
    holdout = manifest["holdoutRows"]

    assert manifest["materialization"]["complete"] is True
    assert len(development) == len(holdout) == 254
    assert len({row["generation"]["imageSha256"] for row in development}) == 254
    assert len({row["generation"]["imageSha256"] for row in holdout}) == 254
    assert {row["generation"]["imageSha256"] for row in development}.isdisjoint(
        row["generation"]["imageSha256"] for row in holdout
    )
    assert {row["venueId"] for row in development}.isdisjoint(row["venueId"] for row in holdout)
    assert len({row["familyCode"] for row in development}) == 23
    assert len({row["familyCode"] for row in holdout}) == 23
    assert manifest["humanReviewComplete"] is True
    assert manifest["trainingAllowed"] is True
    assert manifest["productionTrainingAllowed"] is False
    assert manifest["promotionAllowed"] is False


def test_qa_passes_without_loading_clip_or_consuming_holdout() -> None:
    report = json.loads((ROOT / "qa-report.v2.json").read_text(encoding="utf-8"))

    assert report["evaluatedImageCount"] == 508
    assert report["developmentImageCount"] == report["holdoutImageCount"] == 254
    assert report["familyCountPerSplit"] == {"development": 23, "holdout": 23}
    assert report["decodablePngCount"] == 508
    assert report["exactDuplicatePairs"] == 0
    assert report["nearDuplicatePairsDhashDistanceLe4"] == 0
    assert report["sameTypeCrossSplitHashOverlap"] == 0
    assert report["imagesWithExif"] == 0
    assert len(report["contactSheets"]["development"]) == 4
    assert len(report["contactSheets"]["holdout"]) == 4
    assert report["clipLoaded"] is False
    assert report["holdoutPredictionsComputed"] is False
    assert report["holdoutBudgetConsumed"] == 0
    assert report["humanReviewComplete"] is False
    assert report["trainingAllowed"] is False
    assert report["qaPassed"] is True
