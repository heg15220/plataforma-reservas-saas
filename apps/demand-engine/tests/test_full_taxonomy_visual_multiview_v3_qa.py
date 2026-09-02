"""Regresión del QA y paquete de revisión pre-inferencia de v3."""

from __future__ import annotations

import json
from pathlib import Path

from reserly_demand_engine.full_taxonomy_visual_multiview_v3_qa import (
    _parse_tesseract_tsv,
)


REPO_ROOT = Path(__file__).resolve().parents[3]
ROOT = REPO_ROOT / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"


def test_tesseract_parser_keeps_only_confident_nontrivial_tokens() -> None:
    payload = (
        "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
        "5\t1\t1\t1\t1\t1\t10\t20\t30\t12\t91.5\tSALE\n"
        "5\t1\t1\t1\t1\t2\t45\t20\t5\t12\t99\tA\n"
        "5\t1\t1\t1\t1\t3\t55\t20\t30\t12\t42\t1234\n"
    )

    assert _parse_tesseract_tsv(payload) == [
        {
            "text": "SALE",
            "confidence": 91.5,
            "left": 10,
            "top": 20,
            "width": 30,
            "height": 12,
        }
    ]


def test_tesseract_parser_treats_recognized_quote_as_literal_tsv_text() -> None:
    payload = (
        "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n"
        '5\t1\t1\t1\t1\t1\t10\t20\t5\t12\t99\t"\n'
        "5\t1\t1\t1\t1\t2\t20\t20\t30\t12\t91\tSALE\n"
    )

    assert _parse_tesseract_tsv(payload) == [
        {
            "text": "SALE",
            "confidence": 91.0,
            "left": 20,
            "top": 20,
            "width": 30,
            "height": 12,
        }
    ]


def test_v3_qa_artifacts_cover_only_the_508_new_images() -> None:
    report = json.loads((ROOT / "qa-report.v3.json").read_text(encoding="utf-8"))
    checklist = json.loads(
        (ROOT / "human-review-checklist.v3.json").read_text(encoding="utf-8")
    )

    assert report["evaluatedImageCount"] == 508
    assert report["developmentCImageCount"] == report["holdoutV3ImageCount"] == 254
    assert report["familyCountPerSplit"] == {
        "development-c": 23,
        "sealed-holdout-v3": 23,
    }
    assert report["archetypeCountPerSplit"] == {
        "development-c": 38,
        "sealed-holdout-v3": 38,
    }
    assert report["decodablePngCount"] == 508
    assert report["uniqueSha256Count"] == 508
    assert report["exactDuplicatePairs"] == 0
    assert report["sameSourceCrossSplitHashOverlap"] == 0
    assert report["imagesWithExif"] == 0
    assert report["ocr"]["status"] == "completed"
    assert report["ocr"]["scannedImageCount"] == 508
    assert report["ocrScanComplete"] is True
    assert all(
        control not in token["text"]
        for finding in report["ocr"]["findings"]
        for token in finding["tokens"]
        for control in ("\r", "\n", "\t")
    )
    assert len(report["contactSheets"]["development-c"]) == 4
    assert len(report["contactSheets"]["sealed-holdout-v3"]) == 4
    assert report["clipLoaded"] is False
    assert report["embeddingsExtracted"] is False
    assert report["holdoutPredictionsComputed"] is False
    assert report["holdoutBudgetConsumed"] == 0
    assert report["humanReviewComplete"] is False
    assert report["developmentTrainingAllowed"] is False
    assert report["holdoutEvaluationAllowed"] is False
    assert report["promotionAllowed"] is False
    assert report["qaPassed"] is (
        report["structuralQaPassed"]
        and report["perceptualQaPassed"]
        and report["ocrScanComplete"]
    )

    assert checklist["summary"] == {
        "rowCount": 508,
        "pendingCount": 0,
        "approvedCount": 508,
        "rejectedCount": 0,
    }
    assert len(checklist["rows"]) == 508
    assert all(row["humanReviewStatus"] == "approved" for row in checklist["rows"])
    assert all(all(row["reviewChecks"].values()) for row in checklist["rows"])
    assert {row["split"] for row in checklist["rows"]} == {
        "development-c",
        "sealed-holdout-v3",
    }
