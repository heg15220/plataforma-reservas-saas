"""Construye el paquete de revisión humana del dataset visual provisional.

Comprueba integridad de PNG, dimensiones, relación de aspecto, hashes y clones
perceptuales. También crea hojas de contacto; no aprueba etiquetas ni cambia
permisos de entrenamiento, porque esas decisiones pertenecen a una persona.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any


def _dhash(path: Path) -> int:
    """Calcula dHash de 64 bits para detectar copias visuales cercanas."""

    from PIL import Image

    with Image.open(path) as source:
        pixels = list(source.convert("L").resize((9, 8)).getdata())
    value = 0
    for y in range(8):
        for x in range(8):
            value = (value << 1) | int(
                pixels[y * 9 + x] > pixels[y * 9 + x + 1]
            )
    return value


def _resolve(dataset_root: Path, definition_dir: Path, relative: str) -> Path:
    """Resuelve rutas del manifiesto sin permitir escapes del dataset padre."""

    root = dataset_root.resolve()
    path = (definition_dir / relative).resolve()
    if not path.is_relative_to(root):
        raise ValueError("VISUAL_REVIEW_PATH_INVALID")
    return path


def inspect(definition_path: Path, dataset_root: Path) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    """Inspecciona los activos sin ejecutar inferencia ni observar métricas de test."""

    from PIL import Image

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    reviewed: list[dict[str, Any]] = []
    violations: list[dict[str, str]] = []
    perceptual: list[tuple[str, int]] = []
    for row in definition["rows"]:
        path = _resolve(dataset_root, definition_path.parent, row["relativePath"])
        item = {
            "imageId": row["imageId"],
            "venueId": row["venueId"],
            "categoryCode": row["categoryCode"],
            "split": row["split"],
            "relativePath": row["relativePath"],
            "imageSha256": row["imageSha256"],
            "humanReviewStatus": row["humanReviewStatus"],
            "humanReviewer": row.get("humanReviewer"),
            "humanReviewNotes": row.get("humanReviewNotes"),
            "developmentTrainingAllowed": row["developmentTrainingAllowed"],
        }
        if not path.is_file():
            violations.append({"imageId": row["imageId"], "code": "IMAGE_MISSING"})
            reviewed.append(item)
            continue
        raw_hash = hashlib.sha256(path.read_bytes()).hexdigest()
        if raw_hash != row["imageSha256"]:
            violations.append({"imageId": row["imageId"], "code": "IMAGE_HASH_MISMATCH"})
        try:
            with Image.open(path) as source:
                source.verify()
            with Image.open(path) as source:
                width, height = source.size
                image_format = source.format
                mode = source.mode
        except Exception:
            violations.append({"imageId": row["imageId"], "code": "IMAGE_CORRUPTED"})
            reviewed.append(item)
            continue
        ratio = width / height
        if image_format != "PNG":
            violations.append({"imageId": row["imageId"], "code": "IMAGE_FORMAT_INVALID"})
        if min(width, height) < 768:
            violations.append({"imageId": row["imageId"], "code": "IMAGE_RESOLUTION_INSUFFICIENT"})
        if min(abs(ratio - 4 / 3), abs(ratio - 3 / 2)) > 0.02:
            violations.append({"imageId": row["imageId"], "code": "IMAGE_ASPECT_RATIO_INVALID"})
        if mode not in {"RGB", "RGBA"}:
            violations.append({"imageId": row["imageId"], "code": "IMAGE_MODE_INVALID"})
        item.update({"width": width, "height": height, "format": image_format})
        perceptual.append((row["imageId"], _dhash(path)))
        reviewed.append(item)
    near_duplicates: list[dict[str, Any]] = []
    minimum_distance = 64
    for index, (left_id, left_hash) in enumerate(perceptual):
        for right_id, right_hash in perceptual[index + 1 :]:
            distance = (left_hash ^ right_hash).bit_count()
            minimum_distance = min(minimum_distance, distance)
            if distance <= 4:
                near_duplicates.append(
                    {"leftImageId": left_id, "rightImageId": right_id, "distance": distance}
                )
    if near_duplicates:
        violations.append({"imageId": "dataset", "code": "IMAGE_PERCEPTUAL_DUPLICATE"})
    split_counts = Counter(row["split"] for row in reviewed)
    category_counts = Counter(row["categoryCode"] for row in reviewed)
    review_counts = Counter(row["humanReviewStatus"] for row in reviewed)
    report = {
        "schemaVersion": 1,
        "datasetVersion": definition["datasetVersion"],
        "testPredictionsObserved": False,
        "structuralQaPassed": not violations and len(reviewed) == len(definition["rows"]),
        "materializedImageCount": len(reviewed),
        "splitCounts": dict(sorted(split_counts.items())),
        "categoryCounts": dict(sorted(category_counts.items())),
        "uniqueSha256Count": len({row["imageSha256"] for row in reviewed}),
        "minimumPerceptualHammingDistance": minimum_distance,
        "nearDuplicatePairs": near_duplicates,
        "violations": violations,
        "humanReview": {
            "required": True,
            "approved": review_counts["approved"],
            "rejected": review_counts["rejected"],
            "pending": review_counts["pending"],
            "trainingAllowed": bool(reviewed)
            and all(
                row["humanReviewStatus"] == "approved"
                and row["developmentTrainingAllowed"] is True
                for row in reviewed
            ),
        },
        "definitiveContractSatisfied": definition.get("definitiveContractSatisfied", False),
    }
    return reviewed, report


def materialize_hashes(
    definition_path: Path, dataset_root: Path, output_path: Path
) -> dict[str, Any]:
    """Fija SHA-256 de activos nuevos y verifica los hashes heredados antes del QA."""

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    rows: list[dict[str, Any]] = []
    for row in definition["rows"]:
        path = _resolve(dataset_root, definition_path.parent, row["relativePath"])
        if not path.is_file():
            raise ValueError("VISUAL_MATERIALIZATION_IMAGE_MISSING")
        sha256 = hashlib.sha256(path.read_bytes()).hexdigest()
        if row.get("imageSha256") not in {None, sha256}:
            raise ValueError("VISUAL_MATERIALIZATION_HASH_MISMATCH")
        rows.append({**row, "imageSha256": sha256})
    materialized = {
        **definition,
        "status": "materialized_awaiting_test_human_review",
        "materializedImageCount": len(rows),
        "rows": rows,
    }
    output_path.write_text(
        json.dumps(materialized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return materialized


def apply_replacement_selection(
    definition_path: Path, selection_path: Path, output_path: Path
) -> dict[str, Any]:
    """Aplica reemplazos de QA versionados sin sobrescribir el activo rechazado."""

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    selection = json.loads(selection_path.read_text(encoding="utf-8"))
    if (
        selection.get("schemaVersion") != 1
        or selection.get("selectedBeforeModelInference") is not True
    ):
        raise ValueError("VISUAL_REPLACEMENT_SELECTION_INVALID")
    replacements = {item["imageId"]: item for item in selection["replacements"]}
    if len(replacements) != len(selection["replacements"]):
        raise ValueError("VISUAL_REPLACEMENT_SELECTION_DUPLICATE")
    applied: set[str] = set()
    rows: list[dict[str, Any]] = []
    for row in definition["rows"]:
        replacement = replacements.get(row["imageId"])
        if replacement is None:
            rows.append(row)
            continue
        if (
            row["categoryCode"] != replacement["categoryCode"]
            or row["split"] != replacement["split"]
            or row["relativePath"] != replacement["originalPath"]
        ):
            raise ValueError("VISUAL_REPLACEMENT_ROW_MISMATCH")
        rows.append(
            {
                **row,
                "relativePath": replacement["replacementPath"],
                "prompt": replacement["prompt"],
                "replacedPath": replacement["originalPath"],
                "replacementReason": selection["reason"],
                "generatorProvenance": {
                    **row["generatorProvenance"],
                    "promptVersion": replacement["promptVersion"],
                },
            }
        )
        applied.add(row["imageId"])
    if applied != set(replacements):
        raise ValueError("VISUAL_REPLACEMENT_IMAGE_NOT_FOUND")
    updated = {
        **definition,
        "selectionVersion": selection["selectionVersion"],
        "replacementCount": len(applied),
        "rows": rows,
    }
    output_path.write_text(
        json.dumps(updated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return updated


def render_contact_sheets(
    definition_path: Path, dataset_root: Path, output_dir: Path
) -> list[Path]:
    """Renderiza una hoja por split con etiqueta, ordinal y categoría esperada."""

    from PIL import Image, ImageDraw, ImageFont

    definition = json.loads(definition_path.read_text(encoding="utf-8"))
    output_dir.mkdir(parents=True, exist_ok=True)
    font = ImageFont.load_default(size=18)
    created: list[Path] = []
    for split in ("train", "validation", "test"):
        rows = [row for row in definition["rows"] if row["split"] == split]
        columns = 5 if split != "test" else 7
        thumb_width, thumb_height, label_height = 240, 180, 46
        sheet_rows = (len(rows) + columns - 1) // columns
        sheet = Image.new(
            "RGB", (columns * thumb_width, sheet_rows * (thumb_height + label_height)), "white"
        )
        draw = ImageDraw.Draw(sheet)
        for index, row in enumerate(rows):
            path = _resolve(dataset_root, definition_path.parent, row["relativePath"])
            with Image.open(path) as source:
                image = source.convert("RGB")
                image.thumbnail((thumb_width, thumb_height))
            x = (index % columns) * thumb_width
            y = (index // columns) * (thumb_height + label_height)
            sheet.paste(
                image,
                (x + (thumb_width - image.width) // 2, y + (thumb_height - image.height) // 2),
            )
            draw.rectangle((x, y + thumb_height, x + thumb_width, y + thumb_height + label_height), fill="white")
            draw.text(
                (x + 5, y + thumb_height + 3),
                f"{index + 1:02d}  {row['categoryCode']}",
                fill="black",
                font=font,
            )
        target = output_dir / f"review-{split}.jpg"
        sheet.save(target, format="JPEG", quality=90, optimize=True)
        created.append(target)
    return created


def run() -> None:
    """CLI de QA y renderizado del paquete de revisión."""

    parser = argparse.ArgumentParser()
    parser.add_argument("--definition", type=Path, required=True)
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--materialized-definition-output", type=Path)
    parser.add_argument("--replacement-selection", type=Path)
    parser.add_argument("--replacement-definition-output", type=Path)
    args = parser.parse_args()
    definition_path = args.definition
    if bool(args.replacement_selection) != bool(args.replacement_definition_output):
        raise ValueError("VISUAL_REPLACEMENT_ARGUMENTS_INCOMPLETE")
    if args.replacement_selection:
        apply_replacement_selection(
            definition_path,
            args.replacement_selection,
            args.replacement_definition_output,
        )
        definition_path = args.replacement_definition_output
    if args.materialized_definition_output:
        materialize_hashes(
            definition_path, args.dataset_root, args.materialized_definition_output
        )
        definition_path = args.materialized_definition_output
    rows, report = inspect(definition_path, args.dataset_root)
    manifest = definition_path.parent / "human-review-manifest.jsonl"
    manifest.write_text(
        "".join(json.dumps(row, ensure_ascii=False) + "\n" for row in rows), encoding="utf-8"
    )
    report_path = definition_path.parent / "qa-report.json"
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    sheets = render_contact_sheets(definition_path, args.dataset_root, args.output_dir)
    print(json.dumps({"report": str(report_path), "sheets": [str(path) for path in sheets]}))


if __name__ == "__main__":
    run()
