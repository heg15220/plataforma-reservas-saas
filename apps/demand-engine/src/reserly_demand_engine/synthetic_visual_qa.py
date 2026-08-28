"""QA reproducible de imágenes sintéticas materializadas del marketplace.

Valida integridad, metadatos, relación 4:3, unicidad exacta/perceptual y, de
forma opcional, reconocimiento de las ocho categorías con el CLIP fijado por
el proyecto. El informe es diagnóstico sintético: nunca habilita evidencia
productiva ni sustituye la revisión humana requerida por la política visual.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter, defaultdict
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from .clip_visual_evaluation import ClipVisualManifest, HuggingFaceClipEmbedder


CATEGORY_PROMPTS = {
    "restaurante": "a professional restaurant dining room prepared for table reservations",
    "peluqueria": "a professional hair salon with styling chairs and mirrors",
    "campo-de-futbol": "a bookable football soccer pitch with goals and small stands",
    "pista-de-padel": "a professional padel court enclosed by glass walls",
    "instalacion-municipal": "a bookable municipal community facility or public multipurpose hall",
    "centro-deportivo": "a modern sports center gym with training equipment",
    "centro-de-estetica": "a professional beauty treatment center with private treatment rooms",
    "otros": "a bookable creative coworking photography or rehearsal studio",
}
PEOPLE_PROMPTS = (
    "an empty venue with no people visible",
    "a venue with one or more people visibly present",
)


def _rows(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _resolve_asset_path(dataset_dir: Path, relative_path: str | Path) -> Path:
    """Resuelve un activo local y rechaza rutas absolutas o escapes del dataset."""

    relative = Path(relative_path)
    if relative.is_absolute():
        raise ValueError("VISUAL_ASSET_PATH_INVALID")
    dataset_root = dataset_dir.resolve()
    candidate = (dataset_root / relative).resolve()
    if not candidate.is_relative_to(dataset_root):
        raise ValueError("VISUAL_ASSET_PATH_INVALID")
    return candidate


def _dhash(path: Path) -> int:
    """Calcula dHash de 64 bits para detectar clones o transformaciones mínimas."""

    from PIL import Image

    with Image.open(path) as source:
        pixels = list(source.convert("L").resize((9, 8)).getdata())
    value = 0
    for y in range(8):
        for x in range(8):
            value = (value << 1) | int(pixels[y * 9 + x] > pixels[y * 9 + x + 1])
    return value


def _softmax(values: list[float], scale: float = 100.0) -> list[float]:
    shifted = [value * scale for value in values]
    maximum = max(shifted)
    exponentials = [math.exp(value - maximum) for value in shifted]
    total = sum(exponentials)
    return [value / total for value in exponentials]


def _classification_summary(predictions: list[dict[str, Any]], codes: list[str]) -> dict[str, Any]:
    """Calcula métricas macro y matriz de confusión sin ponderar categorías grandes."""

    confusion: dict[str, Counter[str]] = {code: Counter() for code in codes}
    for item in predictions:
        confusion[item["actual"]][item["predicted"]] += 1
    metrics: list[dict[str, Any]] = []
    for code in codes:
        tp = confusion[code][code]
        fp = sum(confusion[actual][code] for actual in codes if actual != code)
        fn = sum(confusion[code][predicted] for predicted in codes if predicted != code)
        precision = tp / (tp + fp) if tp + fp else 0.0
        recall = tp / (tp + fn) if tp + fn else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        metrics.append(
            {"categoryCode": code, "precision": round(precision, 8), "recall": round(recall, 8), "f1": round(f1, 8)}
        )
    macro_precision = sum(item["precision"] for item in metrics) / len(metrics)
    macro_recall = sum(item["recall"] for item in metrics) / len(metrics)
    macro_f1 = sum(item["f1"] for item in metrics) / len(metrics)
    accuracy = sum(item["actual"] == item["predicted"] for item in predictions) / len(predictions)
    return {
        "imageCount": len(predictions),
        "accuracy": round(accuracy, 8),
        "macroPrecision": round(macro_precision, 8),
        "macroRecall": round(macro_recall, 8),
        "macroF1": round(macro_f1, 8),
        "metrics": metrics,
        "confusionMatrix": {actual: dict(confusion[actual]) for actual in codes},
    }


def _cohort_classification_summaries(
    predictions: list[dict[str, Any]], codes: list[str]
) -> dict[str, dict[str, Any]]:
    """Resume únicamente las cohortes presentes para admitir evaluaciones aisladas."""

    return {
        cohort: _classification_summary(
            [item for item in predictions if item["entityCohort"] == cohort], codes
        )
        for cohort in sorted({item["entityCohort"] for item in predictions})
    }


def inspect_assets(
    dataset_dir: Path,
    replacements: dict[str, str] | None = None,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    """Inspecciona los PNG y devuelve manifiesto por activo y resumen estructural."""

    from PIL import Image

    venues = _rows(dataset_dir / "venues.jsonl")
    prompts = _rows(dataset_dir / "image-prompts.jsonl")
    if len(venues) != len(prompts):
        raise ValueError("VISUAL_QA_CONTRACT_COUNT_MISMATCH")
    assets: list[dict[str, Any]] = []
    hashes: dict[str, list[str]] = defaultdict(list)
    perceptual: list[tuple[str, int]] = []
    violations: list[dict[str, str]] = []
    for index, (venue, prompt) in enumerate(zip(venues, prompts, strict=True), start=1):
        filename = f"venue-{index:03d}.png"
        relative_path = (replacements or {}).get(filename, f"images/{filename}")
        path = _resolve_asset_path(dataset_dir, relative_path)
        if not path.is_file():
            violations.append({"image": filename, "code": "IMAGE_MISSING"})
            continue
        raw = path.read_bytes()
        sha256 = hashlib.sha256(raw).hexdigest()
        try:
            with Image.open(path) as source:
                source.verify()
            with Image.open(path) as source:
                width, height = source.size
                image_format = source.format
                mode = source.mode
                metadata_stripped = not bool(source.info)
        except Exception:
            violations.append({"image": filename, "code": "IMAGE_CORRUPTED"})
            continue
        if image_format != "PNG":
            violations.append({"image": filename, "code": "IMAGE_FORMAT_INVALID"})
        if width * 3 != height * 4:
            violations.append({"image": filename, "code": "IMAGE_ASPECT_RATIO_INVALID"})
        if width < 1024 or height < 768:
            violations.append({"image": filename, "code": "IMAGE_RESOLUTION_INSUFFICIENT"})
        if mode not in {"RGB", "RGBA"}:
            violations.append({"image": filename, "code": "IMAGE_MODE_INVALID"})
        if not metadata_stripped:
            violations.append({"image": filename, "code": "IMAGE_METADATA_PRESENT"})
        hashes[sha256].append(filename)
        perceptual.append((filename, _dhash(path)))
        assets.append(
            {
                "imagePromptId": prompt["imagePromptId"],
                "venueId": venue["venueId"],
                "categoryCode": venue["categoryCode"],
                "entityCohort": venue["entityCohort"],
                "objectKey": (
                    "local-dev://synthetic-marketplace-v1/"
                    f"{Path(relative_path).as_posix()}"
                ),
                "sha256": sha256,
                "format": image_format,
                "width": width,
                "height": height,
                "metadataStripped": metadata_stripped,
                "generatorProvenance": {
                    "provider": "openai-built-in-imagegen",
                    "modelKey": "managed-built-in",
                    "modelRevision": "notExposedByProvider",
                    "promptVersion": (
                        "venue-category-disambiguation-v2"
                        if filename in (replacements or {})
                        else prompt["promptVersion"]
                    ),
                    "generatedAt": "2026-08-27",
                },
                "syntheticEvaluationAllowed": True,
                "developmentTrainingAllowed": False,
                "productionTrainingAllowed": False,
                "humanReviewStatus": "pending",
            }
        )
    exact_duplicates = [names for names in hashes.values() if len(names) > 1]
    near_duplicates: list[dict[str, Any]] = []
    minimum_distance = 64
    for left_index, (left_name, left_hash) in enumerate(perceptual):
        for right_name, right_hash in perceptual[left_index + 1 :]:
            distance = (left_hash ^ right_hash).bit_count()
            minimum_distance = min(minimum_distance, distance)
            if distance <= 4:
                near_duplicates.append(
                    {"left": left_name, "right": right_name, "hammingDistance": distance}
                )
    if exact_duplicates:
        violations.append({"image": "dataset", "code": "IMAGE_EXACT_DUPLICATE"})
    if near_duplicates:
        violations.append({"image": "dataset", "code": "IMAGE_PERCEPTUAL_DUPLICATE"})
    category_counts = Counter(asset["categoryCode"] for asset in assets)
    cohort_category_counts: dict[str, Counter[str]] = defaultdict(Counter)
    for asset in assets:
        cohort_category_counts[asset["entityCohort"]][asset["categoryCode"]] += 1
    if set(category_counts) != set(CATEGORY_PROMPTS):
        violations.append({"image": "dataset", "code": "IMAGE_CATEGORY_COVERAGE_INCOMPLETE"})
    if any(set(counts) != set(CATEGORY_PROMPTS) for counts in cohort_category_counts.values()):
        violations.append({"image": "dataset", "code": "IMAGE_COHORT_CATEGORY_COVERAGE_INCOMPLETE"})
    return assets, {
        "expectedImageCount": len(venues),
        "materializedImageCount": len(assets),
        "categoryCounts": dict(sorted(category_counts.items())),
        "cohortCategoryCounts": {
            cohort: dict(sorted(counts.items()))
            for cohort, counts in sorted(cohort_category_counts.items())
        },
        "exactDuplicateGroups": exact_duplicates,
        "nearDuplicatePairs": near_duplicates,
        "minimumPerceptualHammingDistance": minimum_distance,
        "violations": violations,
        "passed": not violations and len(assets) == len(venues),
    }


def inspect_holdout_assets(
    dataset_dir: Path, definition: dict[str, Any]
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    """Valida un holdout congelado y construye activos sin abrir aún el clasificador."""

    from PIL import Image

    images_directory = Path(definition["imagesDirectory"])
    allowed_aspect_ratios = [
        tuple(int(part) for part in ratio.split(":"))
        for ratio in definition.get("allowedAspectRatios", ["4:3"])
    ]
    assets: list[dict[str, Any]] = []
    violations: list[dict[str, str]] = []
    hashes: dict[str, list[str]] = defaultdict(list)
    perceptual: list[tuple[str, int]] = []
    categories = definition["categories"]
    if set(categories) != set(CATEGORY_PROMPTS):
        violations.append({"image": "holdout", "code": "HOLDOUT_CATEGORY_COVERAGE_INVALID"})
    if any(len(items) != 3 for items in categories.values()):
        violations.append({"image": "holdout", "code": "HOLDOUT_CATEGORY_BALANCE_INVALID"})
    for category_code, items in categories.items():
        for ordinal, item in enumerate(items, start=1):
            filename = item["file"]
            relative_path = images_directory / filename
            path = _resolve_asset_path(dataset_dir, relative_path)
            if not path.is_file():
                violations.append({"image": filename, "code": "IMAGE_MISSING"})
                continue
            raw = path.read_bytes()
            sha256 = hashlib.sha256(raw).hexdigest()
            try:
                with Image.open(path) as source:
                    source.verify()
                with Image.open(path) as source:
                    width, height = source.size
                    image_format = source.format
                    mode = source.mode
            except Exception:
                violations.append({"image": filename, "code": "IMAGE_CORRUPTED"})
                continue
            if image_format != "PNG":
                violations.append({"image": filename, "code": "IMAGE_FORMAT_INVALID"})
            if not any(
                width * ratio_height == height * ratio_width
                for ratio_width, ratio_height in allowed_aspect_ratios
            ):
                violations.append({"image": filename, "code": "IMAGE_ASPECT_RATIO_INVALID"})
            if width < 1024 or height < 768:
                violations.append({"image": filename, "code": "IMAGE_RESOLUTION_INSUFFICIENT"})
            if mode not in {"RGB", "RGBA"}:
                violations.append({"image": filename, "code": "IMAGE_MODE_INVALID"})
            hashes[sha256].append(filename)
            perceptual.append((filename, _dhash(path)))
            assets.append(
                {
                    "imagePromptId": f"{definition['holdoutVersion']}:{category_code}:{ordinal}",
                    "venueId": f"holdout:{category_code}:{ordinal}",
                    "categoryCode": category_code,
                    "entityCohort": "finalHoldout",
                    "variant": item["variant"],
                    "objectKey": (
                        "local-dev://synthetic-marketplace-v1/"
                        f"{relative_path.as_posix()}"
                    ),
                    "sha256": sha256,
                    "format": image_format,
                    "width": width,
                    "height": height,
                    "generatorProvenance": {
                        **definition["generation"],
                        "generatedAt": "2026-08-28",
                    },
                    "syntheticEvaluationAllowed": True,
                    "developmentTrainingAllowed": False,
                    "productionTrainingAllowed": False,
                    "humanReviewStatus": "pending",
                }
            )
    exact_duplicates = [names for names in hashes.values() if len(names) > 1]
    near_duplicates: list[dict[str, Any]] = []
    minimum_distance = 64
    for left_index, (left_name, left_hash) in enumerate(perceptual):
        for right_name, right_hash in perceptual[left_index + 1 :]:
            distance = (left_hash ^ right_hash).bit_count()
            minimum_distance = min(minimum_distance, distance)
            if distance <= 4:
                near_duplicates.append(
                    {"left": left_name, "right": right_name, "hammingDistance": distance}
                )
    if exact_duplicates:
        violations.append({"image": "holdout", "code": "IMAGE_EXACT_DUPLICATE"})
    if near_duplicates:
        violations.append({"image": "holdout", "code": "IMAGE_PERCEPTUAL_DUPLICATE"})
    category_counts = Counter(asset["categoryCode"] for asset in assets)
    expected_count = sum(len(items) for items in categories.values())
    return assets, {
        "expectedImageCount": expected_count,
        "materializedImageCount": len(assets),
        "categoryCounts": dict(sorted(category_counts.items())),
        "exactDuplicateGroups": exact_duplicates,
        "nearDuplicatePairs": near_duplicates,
        "minimumPerceptualHammingDistance": minimum_distance,
        "violations": violations,
        "passed": not violations and len(assets) == expected_count,
    }


def evaluate_categories(
    dataset_dir: Path,
    assets: list[dict[str, Any]],
    manifest_path: Path,
    batch_size: int = 8,
) -> dict[str, Any]:
    """Ejecuta CLIP real para top-1 de categoría y screening auxiliar de personas."""

    manifest = ClipVisualManifest.load(manifest_path)
    embedder = HuggingFaceClipEmbedder(manifest, local_files_only=True)
    codes = list(CATEGORY_PROMPTS)
    text_vectors = embedder.encode_prompts([CATEGORY_PROMPTS[code] for code in codes])
    people_vectors = embedder.encode_prompts(list(PEOPLE_PROMPTS))
    predictions: list[dict[str, Any]] = []
    for start in range(0, len(assets), batch_size):
        batch = assets[start : start + batch_size]
        object_key_prefix = "local-dev://synthetic-marketplace-v1/"
        if any(not asset["objectKey"].startswith(object_key_prefix) for asset in batch):
            raise ValueError("VISUAL_ASSET_OBJECT_KEY_INVALID")
        paths = [
            _resolve_asset_path(dataset_dir, asset["objectKey"].removeprefix(object_key_prefix))
            for asset in batch
        ]
        image_vectors = embedder.encode_images(paths)
        for asset, vector in zip(batch, image_vectors, strict=True):
            category_scores = [
                sum(a * b for a, b in zip(vector.values, prompt.values, strict=True))
                for prompt in text_vectors
            ]
            category_probabilities = _softmax(category_scores)
            predicted_index = max(range(len(codes)), key=category_probabilities.__getitem__)
            people_scores = [
                sum(a * b for a, b in zip(vector.values, prompt.values, strict=True))
                for prompt in people_vectors
            ]
            people_probability = _softmax(people_scores)[1]
            predictions.append(
                {
                    "sha256": asset["sha256"],
                    "actual": asset["categoryCode"],
                    "entityCohort": asset["entityCohort"],
                    "predicted": codes[predicted_index],
                    "confidence": round(category_probabilities[predicted_index], 8),
                    "peopleRiskProbability": round(people_probability, 8),
                }
            )
    summary = _classification_summary(predictions, codes)
    cohort_metrics = _cohort_classification_summaries(predictions, codes)
    people_flagged = sum(item["peopleRiskProbability"] >= 0.5 for item in predictions)
    return {
        "modelKey": manifest.modelKey,
        "modelRevision": manifest.revision,
        "libraryVersion": manifest.libraryVersion,
        "evaluationKind": "syntheticCategoryDiagnostic",
        "accuracy": summary["accuracy"],
        "macroPrecision": summary["macroPrecision"],
        "macroRecall": summary["macroRecall"],
        "macroF1": summary["macroF1"],
        "minimumMacroPrecision": 0.8,
        "minimumMacroRecall": 0.8,
        "categoryQualityPassed": summary["macroPrecision"] >= 0.8 and summary["macroRecall"] >= 0.8,
        "metrics": summary["metrics"],
        "confusionMatrix": summary["confusionMatrix"],
        "cohortMetrics": cohort_metrics,
        "peopleRiskThreshold": 0.5,
        "peopleRiskFlaggedCount": people_flagged,
        "peopleScreeningIsDiagnosticOnly": True,
        "peopleScreeningUsable": people_flagged not in {0, len(predictions)},
        "peopleScreeningResult": (
            "inconclusive"
            if people_flagged in {0, len(predictions)}
            else "diagnosticOnly"
        ),
        "predictions": predictions,
    }


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--clip-manifest", type=Path)
    parser.add_argument("--selection", type=Path)
    parser.add_argument("--holdout-definition", type=Path)
    parser.add_argument("--report-suffix", default="")
    parser.add_argument(
        "--evaluation-cohort",
        choices=("warm", "validationCold", "testCold"),
    )
    parser.add_argument("--promote-active", action="store_true")
    args = parser.parse_args()
    if args.holdout_definition:
        if args.selection or args.evaluation_cohort or args.promote_active or args.report_suffix:
            raise ValueError("VISUAL_HOLDOUT_ARGUMENTS_INCOMPATIBLE")
        definition = json.loads(args.holdout_definition.read_text(encoding="utf-8"))
        assets, structural = inspect_holdout_assets(args.dataset, definition)
        clip = (
            evaluate_categories(args.dataset, assets, args.clip_manifest)
            if args.clip_manifest and structural["passed"]
            else None
        )
        automated_passed = bool(
            structural["passed"] and clip and clip["categoryQualityPassed"]
        )
        report = {
            "schemaVersion": 1,
            "reportVersion": "synthetic-marketplace-visual-holdout-v2",
            "datasetVersion": "synthetic-marketplace-v1",
            "holdoutVersion": definition["holdoutVersion"],
            "definitionSha256": hashlib.sha256(
                args.holdout_definition.read_bytes()
            ).hexdigest(),
            "evaluatedAt": datetime.now(UTC).isoformat(),
            "synthetic": True,
            "productionEvidence": False,
            "structural": structural,
            "clipCategoryDiagnostic": clip,
            "automatedQualityPassed": automated_passed,
            "humanReviewCompleted": False,
            "trainingAllowed": False,
            "overallPassed": False,
            "limitations": [
                (
                    "The balanced synthetic holdout is small and does not represent "
                    "production traffic."
                ),
                "CLIP labels are an automated diagnostic and do not replace human visual review.",
                "Passing this gate never enables development or production training automatically.",
            ],
        }
        output_directory = args.holdout_definition.parent
        assets_path = output_directory / "image-assets.jsonl"
        report_path = output_directory / "visual-qa-report.json"
        assets_path.write_text(
            "".join(
                json.dumps(asset, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
                + "\n"
                for asset in assets
            ),
            encoding="utf-8",
        )
        report_path.write_text(
            json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
            encoding="utf-8",
        )
        print(
            json.dumps(
                {
                    "structural": structural["passed"],
                    "images": len(assets),
                    "automatedQualityPassed": automated_passed,
                    "accuracy": clip["accuracy"] if clip else None,
                    "macroPrecision": clip["macroPrecision"] if clip else None,
                    "macroRecall": clip["macroRecall"] if clip else None,
                    "macroF1": clip["macroF1"] if clip else None,
                },
                ensure_ascii=False,
            )
        )
        return
    selection = (
        json.loads(args.selection.read_text(encoding="utf-8")) if args.selection else None
    )
    replacements = selection["replacements"] if selection else None
    assets, structural = inspect_assets(args.dataset, replacements)
    evaluation_assets = (
        [asset for asset in assets if asset["entityCohort"] == args.evaluation_cohort]
        if args.evaluation_cohort
        else assets
    )
    clip = (
        evaluate_categories(args.dataset, evaluation_assets, args.clip_manifest)
        if args.clip_manifest
        else None
    )
    report = {
        "schemaVersion": 1,
        "reportVersion": "synthetic-marketplace-visual-qa-v1",
        "datasetVersion": "synthetic-marketplace-v1",
        "selectionVersion": selection["selectionVersion"] if selection else "visual-selection-v1",
        "evaluationCohort": args.evaluation_cohort or "all",
        "evaluatedAt": datetime.now(UTC).isoformat(),
        "synthetic": True,
        "productionEvidence": False,
        "structural": structural,
        "clipCategoryDiagnostic": clip,
        "humanReviewCompleted": False,
        "productionTrainingAllowed": False,
        "automatedQualityPassed": bool(
            structural["passed"] and clip and clip["categoryQualityPassed"]
        ),
        "overallPassed": False,
        "limitations": [
            "CLIP category labels are synthetic diagnostics, not human ground truth.",
            "People screening by prompt similarity is not a certified detector.",
            "Human review remains mandatory before any training or production evidence use.",
        ],
    }
    suffix = args.report_suffix
    if suffix and not suffix.startswith("."):
        raise ValueError("VISUAL_QA_REPORT_SUFFIX_INVALID")
    assets_path = args.dataset / f"image-assets{suffix}.jsonl"
    report_path = args.dataset / f"visual-qa-report{suffix}.json"
    assets_path.write_text(
        "".join(
            json.dumps(asset, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            + "\n"
            for asset in assets
        ),
        encoding="utf-8",
    )
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    if args.promote_active:
        manifest_path = args.dataset / "manifest.json"
        dataset_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        dataset_manifest["counts"]["materializedImages"] = len(assets)
        dataset_manifest["visualAssets"].update(
            {
                "status": "materializedPendingHumanReview",
                "trainingAllowed": False,
                "assetManifest": assets_path.name,
                "qaReport": report_path.name,
                "automatedQualityPassed": report["automatedQualityPassed"],
                "humanReviewCompleted": False,
            }
        )
        for path, rows in ((assets_path, len(assets)), (report_path, 1)):
            dataset_manifest["artifacts"][path.name] = {
                "rows": rows,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            }
        manifest_path.write_text(
            json.dumps(dataset_manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
            encoding="utf-8",
        )
    print(
        json.dumps(
            {"structural": structural["passed"], "images": len(assets), "clip": clip},
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    run()
