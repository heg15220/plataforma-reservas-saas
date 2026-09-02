"""Regresión de la evaluación pixel del corpus taxonómico parcial."""

from __future__ import annotations

import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
DATASET_ROOT = (
    REPO_ROOT
    / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v1"
)
REPORT_PATH = (
    REPO_ROOT
    / "apps/demand-engine/evaluation/results/full-taxonomy-visual-evaluation.v1.json"
)


def test_sealed_manifest_reports_only_materialized_evidence() -> None:
    manifest = json.loads((DATASET_ROOT / "generation-manifest.json").read_text(encoding="utf-8"))
    materialized = [
        row for row in manifest["rows"] if row["generation"]["status"] == "materializedPendingHumanReview"
    ]

    assert manifest["materialization"]["materializedCount"] == 220
    assert manifest["materialization"]["missingCount"] == 34
    assert len(materialized) == 220
    assert len({row["generation"]["imageSha256"] for row in materialized}) == 220
    assert all(row["humanReviewStatus"] == "pendingHumanReview" for row in materialized)
    assert not any(row["productionTrainingAllowed"] for row in manifest["rows"])


def test_pixel_evaluation_is_honest_about_coverage_and_failed_top1_gate() -> None:
    report = json.loads(REPORT_PATH.read_text(encoding="utf-8"))

    assert report["coverage"]["materializedImages"] == 220
    assert report["coverage"]["materializedTypes"] == 220
    assert report["coverage"]["presentFamilies"] == 21
    assert report["coverage"]["missingTypes"] == 34
    assert report["coverage"]["missingFamilies"] == [
        "finanzas-seguros-e-inmobiliario",
        "otros-servicios-al-publico",
    ]
    assert report["qualityGates"]["accuracyPassed"] is False
    assert report["qualityGates"]["errorPassed"] is False
    assert report["promotionAllowed"] is False
    assert report["trainingAllowed"] is False


def test_pixels_have_measurable_signal_without_duplicate_or_prompt_leakage() -> None:
    report = json.loads(REPORT_PATH.read_text(encoding="utf-8"))
    qa = report["technicalQa"]
    classifier = report["familyClassification"]

    assert qa["decodablePngCount"] == 220
    assert qa["exactDuplicatePairs"] == 0
    assert qa["nearDuplicatePairsDhashDistanceLe4"] == 0
    assert qa["imagesWithExif"] == 0
    assert 0.999 <= qa["embeddingNormMin"] <= 1.001
    assert 0.999 <= qa["embeddingNormMax"] <= 1.001
    assert classifier["classifier"] == "nearest-family-centroid-cosine"
    assert classifier["trainableParameterCount"] == 0
    assert classifier["familyRecallAt3"] >= 0.90
    assert report["pixelSignalAccuracyUplift"] >= 0.25
    assert classifier["test"]["accuracy"] > report["permutedLabelControl"]["test"]["accuracy"]
