"""QA pre-inferencia y hojas de revisión del corpus visual v2.

El job solo inspecciona estructura, metadatos y similitud perceptual. No carga
CLIP, no entrena y no calcula predicciones del holdout; por tanto no consume su
presupuesto de apertura. Las hojas de contacto facilitan la revisión humana.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageOps, ImageStat


QA_VERSION = "full-taxonomy-visual-holdout-qa-v2"


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    """Resuelve una ruta manteniéndola dentro de evaluation."""

    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_HOLDOUT_QA_PATH_ESCAPE")
    return path


def _dhash(image: Image.Image) -> int:
    """Calcula dHash 64-bit para detectar copias o variantes triviales."""

    values = np.asarray(image.convert("L").resize((9, 8), Image.Resampling.LANCZOS))
    result = 0
    for bit in (values[:, 1:] > values[:, :-1]).ravel():
        result = (result << 1) | int(bit)
    return result


def _inspect(path: Path) -> dict[str, Any]:
    """Verifica decodificación y devuelve métricas que no revelan predicción."""

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


def _contact_sheets(
    rows: list[dict[str, Any]],
    paths: list[Path],
    output_dir: Path,
    split: str,
    per_sheet: int = 64,
) -> list[str]:
    """Renderiza hojas etiquetadas para revisión humana, no para el modelo."""

    output_dir.mkdir(parents=True, exist_ok=True)
    generated: list[str] = []
    tile_width, tile_height, label_height = 260, 195, 44
    columns = 4
    for sheet_index, start in enumerate(range(0, len(rows), per_sheet), start=1):
        batch_rows = rows[start : start + per_sheet]
        batch_paths = paths[start : start + per_sheet]
        line_count = (len(batch_rows) + columns - 1) // columns
        canvas = Image.new("RGB", (columns * tile_width, line_count * (tile_height + label_height)), "white")
        draw = ImageDraw.Draw(canvas)
        for offset, (row, path) in enumerate(zip(batch_rows, batch_paths, strict=True)):
            column, line = offset % columns, offset // columns
            x, y = column * tile_width, line * (tile_height + label_height)
            with Image.open(path) as source:
                thumb = ImageOps.fit(source.convert("RGB"), (tile_width, tile_height), method=Image.Resampling.LANCZOS)
            canvas.paste(thumb, (x, y))
            label = f"{row['sourceId']:03d} {row['typeCode']}"
            draw.rectangle((x, y + tile_height, x + tile_width, y + tile_height + label_height), fill="white")
            draw.text((x + 5, y + tile_height + 4), label[:42], fill="black")
            draw.text((x + 5, y + tile_height + 21), row["familyCode"][:38], fill="#444444")
        filename = f"{split}-{sheet_index:02d}.jpg"
        canvas.save(output_dir / filename, "JPEG", quality=88, optimize=True)
        generated.append(filename)
    return generated


def evaluate(manifest_path: Path, output_path: Path, contact_sheet_dir: Path) -> dict[str, Any]:
    """Ejecuta QA sobre 254+254 imágenes sin observar etiquetas predichas."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("materialization", {}).get("complete") is not True:
        raise ValueError("FULL_TAXONOMY_HOLDOUT_QA_MATERIALIZATION_INCOMPLETE")
    dataset_root = manifest_path.parent
    evaluation_root = dataset_root.parent
    split_rows = {
        "development": manifest["developmentRows"],
        "holdout": manifest["holdoutRows"],
    }
    inspected: list[dict[str, Any]] = []
    paths_by_split: dict[str, list[Path]] = {}
    for split, rows in split_rows.items():
        paths = [_resolve(evaluation_root, dataset_root, row["relativePath"]) for row in rows]
        paths_by_split[split] = paths
        for row, path in zip(rows, paths, strict=True):
            quality = _inspect(path)
            if quality["sha256"] != row["generation"]["imageSha256"]:
                raise ValueError("FULL_TAXONOMY_HOLDOUT_QA_HASH_MISMATCH")
            inspected.append(
                {
                    "split": split,
                    "sourceId": row["sourceId"],
                    "typeCode": row["typeCode"],
                    "familyCode": row["familyCode"],
                    **quality,
                }
            )

    hashes = [row["sha256"] for row in inspected]
    dhashes = [int(row["dhash"], 16) for row in inspected]
    near_pairs: list[dict[str, Any]] = []
    for left in range(len(inspected)):
        for right in range(left + 1, len(inspected)):
            distance = (dhashes[left] ^ dhashes[right]).bit_count()
            if distance <= 4:
                near_pairs.append(
                    {
                        "leftSplit": inspected[left]["split"],
                        "leftTypeCode": inspected[left]["typeCode"],
                        "rightSplit": inspected[right]["split"],
                        "rightTypeCode": inspected[right]["typeCode"],
                        "distance": distance,
                    }
                )

    sheets = {
        split: _contact_sheets(rows, paths_by_split[split], contact_sheet_dir, split)
        for split, rows in split_rows.items()
    }
    report = {
        "schemaVersion": 1,
        "qaVersion": QA_VERSION,
        "datasetVersion": manifest["datasetVersion"],
        "evaluatedImageCount": len(inspected),
        "developmentImageCount": len(split_rows["development"]),
        "holdoutImageCount": len(split_rows["holdout"]),
        "familyCountPerSplit": {
            split: len({row["familyCode"] for row in rows}) for split, rows in split_rows.items()
        },
        "decodablePngCount": sum(row["format"] == "PNG" for row in inspected),
        "minimumWidth": min(row["width"] for row in inspected),
        "minimumHeight": min(row["height"] for row in inspected),
        "minimumChannelStdMean": min(row["channelStdMean"] for row in inspected),
        "imagesWithExif": sum(row["exifEntryCount"] > 0 for row in inspected),
        "exactDuplicatePairs": len(hashes) - len(set(hashes)),
        "nearDuplicatePairsDhashDistanceLe4": len(near_pairs),
        "nearDuplicatePairs": near_pairs,
        "sameTypeCrossSplitHashOverlap": sum(
            left["sha256"] == right["sha256"]
            for left, right in zip(inspected[:254], inspected[254:], strict=True)
        ),
        "contactSheets": sheets,
        "clipLoaded": False,
        "holdoutPredictionsComputed": False,
        "holdoutBudgetConsumed": 0,
        "humanReviewComplete": False,
        "trainingAllowed": False,
        "qaPassed": (
            len(inspected) == 508
            and all(row["format"] == "PNG" for row in inspected)
            and len(hashes) == len(set(hashes))
            and not near_pairs
            and all(row["exifEntryCount"] == 0 for row in inspected)
            and min(row["width"] for row in inspected) >= 1024
            and min(row["height"] for row in inspected) >= 768
        ),
    }
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def run() -> None:
    """CLI de QA pre-inferencia."""

    repo_root = Path(__file__).resolve().parents[4]
    root = repo_root / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v2"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=root / "generation-manifest.v2.json")
    parser.add_argument("--output", type=Path, default=root / "qa-report.v2.json")
    parser.add_argument("--contact-sheets", type=Path, default=root / "review-contact-sheets")
    args = parser.parse_args()
    result = evaluate(args.manifest, args.output, args.contact_sheets)
    print(json.dumps({key: result[key] for key in ("evaluatedImageCount", "exactDuplicatePairs", "nearDuplicatePairsDhashDistanceLe4", "qaPassed")}, ensure_ascii=False))


if __name__ == "__main__":
    run()
