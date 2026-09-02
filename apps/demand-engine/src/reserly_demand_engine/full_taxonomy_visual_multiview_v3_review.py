"""Prepara la revisión humana explícita del corpus visual multivista v3.

Genera hojas anotadas para las alertas OCR sin aprobar filas, cargar CLIP,
extraer embeddings ni consumir el holdout. La autorización es un paso posterior
que requiere una declaración inequívoca de la persona revisora.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageOps


REQUIRED_APPROVAL_PHRASE = "Apruebo las 508 imágenes nuevas del dataset visual taxonómico v3"


def _resolve(dataset_root: Path, relative_path: str) -> Path:
    """Resuelve un activo sin permitir que la ruta salga del dataset v3."""

    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(dataset_root.resolve()):
        raise ValueError("FULL_TAXONOMY_V3_REVIEW_PATH_ESCAPE")
    return path


def _annotated_ocr_sheets(
    rows: list[dict[str, Any]], dataset_root: Path, output_dir: Path
) -> list[str]:
    """Dibuja cajas OCR y tokens para revisión, sin alterar los PNG sellados."""

    flagged = [row for row in rows if row["automatedOcrFindings"]]
    output_dir.mkdir(parents=True, exist_ok=True)
    per_sheet, columns = 24, 4
    tile_width, image_height, footer_height = 360, 270, 82
    filenames: list[str] = []
    for sheet_number, start in enumerate(range(0, len(flagged), per_sheet), start=1):
        batch = flagged[start : start + per_sheet]
        lines = (len(batch) + columns - 1) // columns
        canvas = Image.new(
            "RGB", (columns * tile_width, lines * (image_height + footer_height)), "white"
        )
        draw = ImageDraw.Draw(canvas)
        for offset, row in enumerate(batch):
            column, line = offset % columns, offset // columns
            x, y = column * tile_width, line * (image_height + footer_height)
            path = _resolve(dataset_root, row["relativePath"])
            with Image.open(path) as source:
                original = source.convert("RGB")
                annotated = original.copy()
                source_draw = ImageDraw.Draw(annotated)
                for finding in row["automatedOcrFindings"]:
                    left, top = finding["left"], finding["top"]
                    right = left + finding["width"]
                    bottom = top + finding["height"]
                    source_draw.rectangle((left, top, right, bottom), outline="red", width=5)
                thumbnail = ImageOps.fit(
                    annotated,
                    (tile_width, image_height),
                    method=Image.Resampling.LANCZOS,
                )
            canvas.paste(thumbnail, (x, y))
            draw.rectangle(
                (x, y + image_height, x + tile_width, y + image_height + footer_height),
                fill="white",
            )
            draw.text(
                (x + 5, y + image_height + 4),
                f"{row['split']} #{row['sourceId']:03d}",
                fill="black",
            )
            draw.text(
                (x + 5, y + image_height + 23), row["typeCode"][:50], fill="#333333"
            )
            token_text = " | ".join(
                f"{finding['text']} ({finding['confidence']:.1f})"
                for finding in row["automatedOcrFindings"]
            )
            draw.text((x + 5, y + image_height + 43), token_text[:58], fill="#9b0000")
            if len(token_text) > 58:
                draw.text((x + 5, y + image_height + 61), token_text[58:116], fill="#9b0000")
        filename = f"ocr-alerts-{sheet_number:02d}.jpg"
        canvas.save(output_dir / filename, "JPEG", quality=90, optimize=True)
        filenames.append(filename)
    return filenames


def build_review_package(
    checklist_path: Path,
    qa_path: Path,
    output_dir: Path,
    summary_path: Path,
    dataset_root: Path | None = None,
) -> dict[str, Any]:
    """Construye evidencia para que una persona decida, sin autoaprobar."""

    checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
    qa = json.loads(qa_path.read_text(encoding="utf-8"))
    if qa.get("qaPassed") is not True or qa.get("ocrScanComplete") is not True:
        raise ValueError("FULL_TAXONOMY_V3_REVIEW_QA_INCOMPLETE")
    rows = checklist["rows"]
    if len(rows) != 508 or any(row["humanReviewStatus"] != "pendingHumanReview" for row in rows):
        raise ValueError("FULL_TAXONOMY_V3_REVIEW_CHECKLIST_INVALID")
    if qa.get("holdoutBudgetConsumed") != 0 or qa.get("holdoutPredictionsComputed") is not False:
        raise ValueError("FULL_TAXONOMY_V3_REVIEW_HOLDOUT_ALREADY_OBSERVED")

    sheets = _annotated_ocr_sheets(rows, dataset_root or checklist_path.parent, output_dir)
    flagged = [row for row in rows if row["automatedOcrFindings"]]
    summary = {
        "schemaVersion": 1,
        "datasetVersion": checklist["datasetVersion"],
        "reviewImageCount": len(rows),
        "developmentCImageCount": sum(row["split"] == "development-c" for row in rows),
        "holdoutV3ImageCount": sum(row["split"] == "sealed-holdout-v3" for row in rows),
        "emptyVenuePreferredCount": sum(
            row["peoplePolicy"]["mode"] == "emptyVenuePreferred" for row in rows
        ),
        "backgroundAdultsNonIdentifiableCount": sum(
            row["peoplePolicy"]["mode"] == "backgroundAdultsNonIdentifiable"
            for row in rows
        ),
        "ocrFlaggedImageCount": len(flagged),
        "ocrFindingCount": sum(len(row["automatedOcrFindings"]) for row in rows),
        "ocrAlertSheets": sheets,
        "generalReviewSheets": qa["contactSheets"],
        "requiredChecksPerImage": checklist["instructions"]["requiredChecks"],
        "approvalPhraseRequired": REQUIRED_APPROVAL_PHRASE,
        "reviewStatus": "awaitingExplicitHumanApproval",
        "humanReviewComplete": False,
        "developmentTrainingAllowed": False,
        "holdoutEvaluationAllowed": False,
        "clipLoaded": False,
        "embeddingsExtracted": False,
        "holdoutPredictionsComputed": False,
        "holdoutBudgetConsumed": 0,
    }
    summary_path.write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return summary


def run() -> None:
    """CLI para materializar el paquete de revisión humana v3."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checklist", type=Path, default=root / "human-review-checklist.v3.json")
    parser.add_argument("--qa", type=Path, default=root / "qa-report.v3.json")
    parser.add_argument("--output-dir", type=Path, default=root / "ocr-review-contact-sheets")
    parser.add_argument("--summary", type=Path, default=root / "human-review-summary.v3.json")
    args = parser.parse_args()
    result = build_review_package(args.checklist, args.qa, args.output_dir, args.summary)
    print(
        json.dumps(
            {
                "reviewImageCount": result["reviewImageCount"],
                "ocrFlaggedImageCount": result["ocrFlaggedImageCount"],
                "ocrAlertSheetCount": len(result["ocrAlertSheets"]),
                "reviewStatus": result["reviewStatus"],
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    run()
