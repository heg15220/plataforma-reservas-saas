"""QA pre-inferencia y paquete de revisión del corpus visual multivista v3.

El job inspecciona exclusivamente las 254 vistas development C nuevas y las
254 vistas holdout v3. Comprueba bytes, formato, resolución, EXIF, diversidad
perceptual y texto OCR; además genera hojas de contacto y un checklist humano.
Nunca carga CLIP, calcula predicciones, entrena ni consume el holdout.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import shutil
import subprocess
from collections import Counter
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageOps, ImageStat


QA_VERSION = "full-taxonomy-visual-multiview-v3-qa-v1"
OCR_MIN_CONFIDENCE = 70.0


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    """Resuelve una ruta del manifiesto sin permitir escapes de ``evaluation``."""

    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_V3_QA_PATH_ESCAPE")
    return path


def _dhash(image: Image.Image) -> int:
    """Calcula dHash de 64 bits para detectar copias y variantes triviales."""

    values = np.asarray(image.convert("L").resize((9, 8), Image.Resampling.LANCZOS))
    result = 0
    for bit in (values[:, 1:] > values[:, :-1]).ravel():
        result = (result << 1) | int(bit)
    return result


def _inspect(path: Path) -> dict[str, Any]:
    """Verifica un PNG y devuelve métricas neutrales respecto al modelo."""

    payload = path.read_bytes()
    with Image.open(path) as source:
        source.verify()
    with Image.open(path) as source:
        rgb = source.convert("RGB")
        stat = ImageStat.Stat(rgb.resize((128, 128)))
        return {
            "sha256": hashlib.sha256(payload).hexdigest(),
            "format": source.format,
            "width": rgb.width,
            "height": rgb.height,
            "exifEntryCount": len(source.getexif()),
            "channelStdMean": round(float(np.mean(stat.stddev)), 6),
            "dhash": f"{_dhash(rgb):016x}",
        }


def _parse_tesseract_tsv(payload: str) -> list[dict[str, Any]]:
    """Extrae tokens fiables de la salida TSV de Tesseract.

    Los tokens son evidencia para revisión humana. No se incorporan al dataset,
    al entrenamiento ni a ninguna feature del recomendador.
    """

    findings: list[dict[str, Any]] = []
    # Tesseract emite TSV literal: una comilla reconocida es contenido, no un
    # delimitador CSV. Desactivar quoting evita concatenar varias filas cuando
    # el motor confunde un detalle visual con `"`.
    reader = csv.DictReader(io.StringIO(payload), delimiter="\t", quoting=csv.QUOTE_NONE)
    for row in reader:
        text = (row.get("text") or "").strip()
        try:
            confidence = float(row.get("conf") or -1)
        except ValueError:
            continue
        normalized = "".join(character for character in text if character.isalnum())
        if confidence < OCR_MIN_CONFIDENCE or len(normalized) < 2:
            continue
        findings.append(
            {
                "text": text[:80],
                "confidence": round(confidence, 3),
                "left": int(row.get("left") or 0),
                "top": int(row.get("top") or 0),
                "width": int(row.get("width") or 0),
                "height": int(row.get("height") or 0),
            }
        )
    return findings


def _ocr(path: Path, executable: Path) -> list[dict[str, Any]]:
    """Ejecuta OCR local con argumentos cerrados, timeout y sin shell."""

    completed = subprocess.run(
        [str(executable), str(path), "stdout", "--psm", "11", "-l", "eng", "tsv"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=45,
    )
    if completed.returncode != 0:
        raise ValueError(f"FULL_TAXONOMY_V3_QA_OCR_FAILED:{path.name}")
    return _parse_tesseract_tsv(completed.stdout)


def _contact_sheets(
    rows: list[dict[str, Any]],
    paths: list[Path],
    output_dir: Path,
    split: str,
    per_sheet: int = 64,
) -> list[str]:
    """Crea hojas de contacto etiquetadas solo para revisión humana."""

    output_dir.mkdir(parents=True, exist_ok=True)
    generated: list[str] = []
    tile_width, tile_height, label_height = 260, 195, 58
    columns = 4
    for sheet_index, start in enumerate(range(0, len(rows), per_sheet), start=1):
        batch_rows = rows[start : start + per_sheet]
        batch_paths = paths[start : start + per_sheet]
        line_count = (len(batch_rows) + columns - 1) // columns
        canvas = Image.new(
            "RGB", (columns * tile_width, line_count * (tile_height + label_height)), "white"
        )
        draw = ImageDraw.Draw(canvas)
        for offset, (row, path) in enumerate(zip(batch_rows, batch_paths, strict=True)):
            column, line = offset % columns, offset // columns
            x, y = column * tile_width, line * (tile_height + label_height)
            with Image.open(path) as source:
                thumbnail = ImageOps.fit(
                    source.convert("RGB"),
                    (tile_width, tile_height),
                    method=Image.Resampling.LANCZOS,
                )
            canvas.paste(thumbnail, (x, y))
            draw.rectangle(
                (x, y + tile_height, x + tile_width, y + tile_height + label_height),
                fill="white",
            )
            draw.text(
                (x + 5, y + tile_height + 3),
                f"{row['sourceId']:03d} {row['typeCode']}"[:42],
                fill="black",
            )
            draw.text(
                (x + 5, y + tile_height + 20), row["familyCode"][:38], fill="#444444"
            )
            draw.text(
                (x + 5, y + tile_height + 37),
                f"people={row['peoplePolicy']['mode']}"[:42],
                fill="#666666",
            )
        filename = f"{split}-{sheet_index:02d}.jpg"
        canvas.save(output_dir / filename, "JPEG", quality=88, optimize=True)
        generated.append(filename)
    return generated


def _new_rows(manifest: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    """Selecciona exactamente las 508 imágenes nuevas gobernadas por 23.22.b."""

    development = [
        row for row in manifest["developmentRows"] if row.get("developmentView") == "C"
    ]
    holdout = list(manifest["holdoutRows"])
    if len(development) != 254 or len(holdout) != 254:
        raise ValueError("FULL_TAXONOMY_V3_QA_ROW_COUNT_INVALID")
    return {"development-c": development, "sealed-holdout-v3": holdout}


def evaluate(
    manifest_path: Path,
    output_path: Path,
    contact_sheet_dir: Path,
    checklist_path: Path,
    ocr_executable: Path | None = None,
) -> dict[str, Any]:
    """Ejecuta QA y prepara revisión sin inferencia ni apertura del holdout."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("materialization", {}).get("complete") is not True:
        raise ValueError("FULL_TAXONOMY_V3_QA_MATERIALIZATION_INCOMPLETE")
    if checklist_path.is_file():
        existing_checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
        if existing_checklist.get("summary", {}).get("approvedCount", 0) > 0:
            raise ValueError("FULL_TAXONOMY_V3_QA_CHECKLIST_ALREADY_AUTHORIZED")
    dataset_root = manifest_path.parent
    evaluation_root = dataset_root.parent
    split_rows = _new_rows(manifest)
    paths_by_split: dict[str, list[Path]] = {}
    inspected: list[dict[str, Any]] = []
    checklist_rows: list[dict[str, Any]] = []

    resolved_ocr = ocr_executable
    if resolved_ocr is None:
        discovered = shutil.which("tesseract")
        resolved_ocr = Path(discovered) if discovered else None
    if resolved_ocr is not None and not resolved_ocr.is_file():
        raise ValueError("FULL_TAXONOMY_V3_QA_OCR_EXECUTABLE_INVALID")

    for split, rows in split_rows.items():
        paths = [_resolve(evaluation_root, dataset_root, row["relativePath"]) for row in rows]
        paths_by_split[split] = paths
        for row, path in zip(rows, paths, strict=True):
            quality = _inspect(path)
            if quality["sha256"] != row["generation"]["imageSha256"]:
                raise ValueError("FULL_TAXONOMY_V3_QA_HASH_MISMATCH")
            findings = _ocr(path, resolved_ocr) if resolved_ocr is not None else []
            inspected.append(
                {
                    "split": split,
                    "imageId": row["imageId"],
                    "sourceId": row["sourceId"],
                    "typeCode": row["typeCode"],
                    "familyCode": row["familyCode"],
                    "peoplePolicy": row["peoplePolicy"],
                    "ocrFindingCount": len(findings),
                    "ocrFindings": findings,
                    **quality,
                }
            )
            checklist_rows.append(
                {
                    "imageId": row["imageId"],
                    "venueId": row["venueId"],
                    "sourceId": row["sourceId"],
                    "typeCode": row["typeCode"],
                    "typeLabelEs": row["typeLabelEs"],
                    "familyCode": row["familyCode"],
                    "split": split,
                    "relativePath": row["relativePath"],
                    "imageSha256": quality["sha256"],
                    "peoplePolicy": row["peoplePolicy"],
                    "automatedOcrFindings": findings,
                    "reviewChecks": {
                        "categorySignalsCorrect": None,
                        "noLegibleTextOrBrand": None,
                        "peoplePolicyCompliant": None,
                        "noMinorPatientOrSensitiveSituation": None,
                        "independentVenueAndComposition": None,
                    },
                    "humanReviewStatus": "pendingHumanReview",
                    "humanReviewer": None,
                    "humanReviewNotes": None,
                }
            )

    hashes = [row["sha256"] for row in inspected]
    dhashes = [int(row["dhash"], 16) for row in inspected]
    near_pairs: list[dict[str, Any]] = []
    minimum_distance = 64
    for left in range(len(inspected)):
        for right in range(left + 1, len(inspected)):
            distance = (dhashes[left] ^ dhashes[right]).bit_count()
            minimum_distance = min(minimum_distance, distance)
            if distance <= 4:
                near_pairs.append(
                    {
                        "leftImageId": inspected[left]["imageId"],
                        "rightImageId": inspected[right]["imageId"],
                        "leftSplit": inspected[left]["split"],
                        "rightSplit": inspected[right]["split"],
                        "distance": distance,
                    }
                )

    sheets = {
        split: _contact_sheets(rows, paths_by_split[split], contact_sheet_dir, split)
        for split, rows in split_rows.items()
    }
    structural_passed = (
        len(inspected) == 508
        and all(row["format"] == "PNG" for row in inspected)
        and len(hashes) == len(set(hashes))
        and all(row["exifEntryCount"] == 0 for row in inspected)
        and min(row["width"] for row in inspected) >= 1024
        and min(row["height"] for row in inspected) >= 768
        and min(row["channelStdMean"] for row in inspected) >= 5
    )
    perceptual_passed = not near_pairs
    ocr_completed = resolved_ocr is not None
    report = {
        "schemaVersion": 1,
        "qaVersion": QA_VERSION,
        "datasetVersion": manifest["datasetVersion"],
        "evaluatedImageCount": len(inspected),
        "developmentCImageCount": len(split_rows["development-c"]),
        "holdoutV3ImageCount": len(split_rows["sealed-holdout-v3"]),
        "familyCountPerSplit": {
            split: len({row["familyCode"] for row in rows})
            for split, rows in split_rows.items()
        },
        "archetypeCountPerSplit": {
            split: len({row["visualArchetype"]["code"] for row in rows})
            for split, rows in split_rows.items()
        },
        "decodablePngCount": sum(row["format"] == "PNG" for row in inspected),
        "uniqueSha256Count": len(set(hashes)),
        "minimumWidth": min(row["width"] for row in inspected),
        "minimumHeight": min(row["height"] for row in inspected),
        "minimumChannelStdMean": min(row["channelStdMean"] for row in inspected),
        "imagesWithExif": sum(row["exifEntryCount"] > 0 for row in inspected),
        "exactDuplicatePairs": len(hashes) - len(set(hashes)),
        "minimumPerceptualHammingDistance": minimum_distance,
        "nearDuplicatePairsDhashDistanceLe4": len(near_pairs),
        "nearDuplicatePairs": near_pairs,
        "sameSourceCrossSplitHashOverlap": sum(
            left["sha256"] == right["sha256"]
            for left, right in zip(inspected[:254], inspected[254:], strict=True)
        ),
        "ocr": {
            "status": "completed" if ocr_completed else "unavailable",
            "engine": str(resolved_ocr) if resolved_ocr is not None else None,
            "minimumConfidence": OCR_MIN_CONFIDENCE,
            "scannedImageCount": len(inspected) if ocr_completed else 0,
            "flaggedImageCount": sum(row["ocrFindingCount"] > 0 for row in inspected),
            "findingCount": sum(row["ocrFindingCount"] for row in inspected),
            "findings": [
                {
                    "imageId": row["imageId"],
                    "split": row["split"],
                    "sourceId": row["sourceId"],
                    "typeCode": row["typeCode"],
                    "tokens": row["ocrFindings"],
                }
                for row in inspected
                if row["ocrFindings"]
            ],
        },
        "contactSheets": sheets,
        "reviewChecklist": checklist_path.name,
        "structuralQaPassed": structural_passed,
        "perceptualQaPassed": perceptual_passed,
        "ocrScanComplete": ocr_completed,
        "qaPassed": structural_passed and perceptual_passed and ocr_completed,
        "clipLoaded": False,
        "embeddingsExtracted": False,
        "holdoutPredictionsComputed": False,
        "holdoutBudgetConsumed": 0,
        "humanReviewComplete": False,
        "developmentTrainingAllowed": False,
        "holdoutEvaluationAllowed": False,
        "promotionAllowed": False,
    }
    checklist = {
        "schemaVersion": 1,
        "datasetVersion": manifest["datasetVersion"],
        "qaVersion": QA_VERSION,
        "instructions": {
            "requiredChecks": list(checklist_rows[0]["reviewChecks"]),
            "approvalRequiresAllChecksTrue": True,
            "reviewDoesNotConsumeHoldout": True,
            "modelInferenceForbidden": True,
        },
        "summary": {
            "rowCount": len(checklist_rows),
            "pendingCount": len(checklist_rows),
            "approvedCount": 0,
            "rejectedCount": 0,
        },
        "rows": checklist_rows,
    }
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    checklist_path.write_text(
        json.dumps(checklist, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return report


def run() -> None:
    """CLI reproducible de QA y preparación de revisión v3."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=root / "generation-manifest.v3.json")
    parser.add_argument("--output", type=Path, default=root / "qa-report.v3.json")
    parser.add_argument("--contact-sheets", type=Path, default=root / "review-contact-sheets")
    parser.add_argument("--checklist", type=Path, default=root / "human-review-checklist.v3.json")
    parser.add_argument("--ocr-executable", type=Path)
    args = parser.parse_args()
    result = evaluate(
        args.manifest,
        args.output,
        args.contact_sheets,
        args.checklist,
        args.ocr_executable,
    )
    print(
        json.dumps(
            {
                key: result[key]
                for key in (
                    "evaluatedImageCount",
                    "exactDuplicatePairs",
                    "nearDuplicatePairsDhashDistanceLe4",
                    "structuralQaPassed",
                    "perceptualQaPassed",
                    "ocrScanComplete",
                    "qaPassed",
                )
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    run()
