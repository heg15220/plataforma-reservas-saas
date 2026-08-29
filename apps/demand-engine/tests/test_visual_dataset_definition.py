"""Pruebas de congelación previa y paquete de revisión del dataset visual."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

import pytest
from PIL import Image

from reserly_demand_engine.visual_dataset_definition import (
    CATEGORIES,
    freeze_definition,
    freeze_definitive_v2,
)
from reserly_demand_engine.visual_dataset_authorization import apply_human_approval
from reserly_demand_engine.visual_embedding_dataset import build_embedding_dataset
from reserly_demand_engine.visual_review_package import apply_replacement_selection, inspect


def _source_manifest(path: Path) -> None:
    rows = []
    for category in CATEGORIES:
        for index in range(15):
            key = f"local-dev://synthetic-marketplace-v1/images/{category}-{index:02d}.png"
            rows.append(
                {
                    "categoryCode": category,
                    "objectKey": key,
                    "venueId": str(uuid5(NAMESPACE_URL, f"venue/{category}/{index}")),
                    "sha256": hashlib.sha256(key.encode()).hexdigest(),
                }
            )
    path.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")


def test_freeze_definition_is_balanced_disjoint_and_closed(tmp_path: Path) -> None:
    """El contrato 200 se fija sin permisos ni observación accidental del test."""

    dataset = tmp_path / "marketplace"
    output = dataset / "visual-training-dataset-v1"
    dataset.mkdir()
    _source_manifest(dataset / "image-assets.v2-development.jsonl")

    definition = freeze_definition(dataset, output)

    assert len(definition["rows"]) == 200
    assert definition["testOpenedAtFreeze"] is False
    assert definition["automaticAuthorizationAllowed"] is False
    assert len({row["imageId"] for row in definition["rows"]}) == 200
    assert len({row["venueId"] for row in definition["rows"]}) == 200
    assert all(row["humanReviewStatus"] == "pending" for row in definition["rows"])
    assert all(row["developmentTrainingAllowed"] is False for row in definition["rows"])
    for category in CATEGORIES:
        assert sum(
            row["categoryCode"] == category and row["split"] == "train"
            for row in definition["rows"]
        ) == 10
        assert sum(
            row["categoryCode"] == category and row["split"] == "validation"
            for row in definition["rows"]
        ) == 5
        assert sum(
            row["categoryCode"] == category and row["split"] == "test"
            for row in definition["rows"]
        ) == 10
    assert len((output / "generation-worklist.jsonl").read_text().splitlines()) == 80


def test_review_rejects_path_escape(tmp_path: Path) -> None:
    """Un manifiesto no puede leer imágenes fuera de la raíz autorizada."""

    definition_dir = tmp_path / "marketplace" / "definition"
    definition_dir.mkdir(parents=True)
    definition = {
        "datasetVersion": "visual-category-dataset-v1-provisional-120",
        "rows": [
            {
                "imageId": str(uuid5(NAMESPACE_URL, "image")),
                "venueId": str(uuid5(NAMESPACE_URL, "venue")),
                "categoryCode": "otros",
                "split": "test",
                "relativePath": "../../../escape.png",
                "imageSha256": "0" * 64,
            }
        ],
    }
    path = definition_dir / "definition.json"
    path.write_text(json.dumps(definition), encoding="utf-8")

    with pytest.raises(ValueError, match="VISUAL_REVIEW_PATH_INVALID"):
        inspect(path, tmp_path / "marketplace")


def test_review_detects_hash_mismatch_without_opening_predictions(tmp_path: Path) -> None:
    """QA comprueba el activo, pero no ejecuta ni registra inferencia de test."""

    root = tmp_path / "marketplace"
    definition_dir = root / "definition"
    definition_dir.mkdir(parents=True)
    image_path = definition_dir / "image.png"
    Image.new("RGB", (1024, 768), "navy").save(image_path)
    definition = {
        "datasetVersion": "visual-category-dataset-v1-provisional-120",
        "rows": [
            {
                "imageId": str(uuid5(NAMESPACE_URL, "image")),
                "venueId": str(uuid5(NAMESPACE_URL, "venue")),
                "categoryCode": "otros",
                "split": "test",
                "relativePath": "image.png",
                "imageSha256": "0" * 64,
                "humanReviewStatus": "pending",
                "developmentTrainingAllowed": False,
            }
        ],
    }
    path = definition_dir / "definition.json"
    path.write_text(json.dumps(definition), encoding="utf-8")

    _, report = inspect(path, root)

    assert report["testPredictionsObserved"] is False
    assert {item["code"] for item in report["violations"]} == {"IMAGE_HASH_MISMATCH"}
    assert report["humanReview"]["trainingAllowed"] is False


def test_human_approval_creates_authorized_copy_without_productive_permission(
    tmp_path: Path,
) -> None:
    """Una decisión explícita autoriza desarrollo sin mutar el freeze ni producción."""

    rows = [
        {
            "imageId": str(uuid5(NAMESPACE_URL, f"image/{index}")),
            "venueId": str(uuid5(NAMESPACE_URL, f"venue/{index}")),
            "categoryCode": CATEGORIES[index % len(CATEGORIES)],
            "split": "train",
            "relativePath": f"image-{index}.png",
            "imageSha256": f"{index + 1:064x}",
            "humanReviewStatus": "pending",
            "developmentTrainingAllowed": False,
            "productionTrainingAllowed": False,
        }
        for index in range(120)
    ]
    definition_path = tmp_path / "definition.json"
    manifest_path = tmp_path / "manifest.jsonl"
    approval_path = tmp_path / "approval.json"
    approved_path = tmp_path / "approved.json"
    approved_manifest_path = tmp_path / "approved.jsonl"
    definition_path.write_text(
        json.dumps(
            {
                "datasetVersion": "visual-category-dataset-v1-provisional-120",
                "status": "awaiting_human_review",
                "rows": rows,
            }
        ),
        encoding="utf-8",
    )
    manifest_path.write_text(
        "".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8"
    )
    approval_path.write_text(
        json.dumps(
            {
                "schemaVersion": 1,
                "decision": "approved",
                "scope": "all-provisional-images",
                "datasetVersion": "visual-category-dataset-v1-provisional-120",
                "approvedImageCount": 120,
                "reviewer": "owner",
                "decidedAt": "2026-08-29T10:00:00+02:00",
                "confirmedThreeSheetsReviewed": True,
                "source": "test",
                "statement": "approved",
            }
        ),
        encoding="utf-8",
    )

    approved = apply_human_approval(
        definition_path,
        manifest_path,
        approval_path,
        approved_path,
        approved_manifest_path,
    )

    assert approved["status"] == "approved_for_provisional_training"
    assert all(row["humanReviewStatus"] == "approved" for row in approved["rows"])
    assert all(row["developmentTrainingAllowed"] is True for row in approved["rows"])
    assert all(row["productionTrainingAllowed"] is False for row in approved["rows"])
    original = json.loads(definition_path.read_text(encoding="utf-8"))
    assert all(row["humanReviewStatus"] == "pending" for row in original["rows"])


def test_embedding_job_rejects_pending_dataset_before_loading_clip(tmp_path: Path) -> None:
    """Ni siquiera se carga CLIP cuando falta autorización humana."""

    definition = tmp_path / "pending.json"
    definition.write_text(
        json.dumps(
            {
                "status": "awaiting_human_review",
                "rows": [],
            }
        ),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="VISUAL_EMBEDDING_DATASET_NOT_AUTHORIZED"):
        build_embedding_dataset(
            definition,
            tmp_path,
            tmp_path / "missing-model.json",
            tmp_path / "output.json",
        )


def test_definitive_v2_converts_consumed_test_to_development_and_freezes_new_test(
    tmp_path: Path,
) -> None:
    """V2 usa 120 aprobadas como desarrollo y crea 80 identidades de test inéditas."""

    rows = []
    split_sizes = {"train": 5, "validation": 3, "test": 7}
    for category in CATEGORIES:
        ordinal = 0
        for split, size in split_sizes.items():
            for _ in range(size):
                ordinal += 1
                rows.append(
                    {
                        "imageId": str(uuid5(NAMESPACE_URL, f"v1-image/{category}/{ordinal}")),
                        "venueId": str(uuid5(NAMESPACE_URL, f"v1-venue/{category}/{ordinal}")),
                        "categoryCode": category,
                        "split": split,
                        "relativePath": f"images/{category}-{ordinal}.png",
                        "imageSha256": f"{len(rows) + 1:064x}",
                        "humanReviewStatus": "approved",
                        "developmentTrainingAllowed": True,
                        "productionTrainingAllowed": False,
                    }
                )
    source_dir = tmp_path / "v1"
    output_dir = tmp_path / "v2"
    source_dir.mkdir()
    source = source_dir / "approved.json"
    source.write_text(
        json.dumps(
            {
                "datasetVersion": "visual-category-dataset-v1-provisional-120",
                "status": "approved_for_provisional_training",
                "rows": rows,
            }
        ),
        encoding="utf-8",
    )

    definition = freeze_definitive_v2(source, output_dir)

    assert len(definition["rows"]) == 200
    assert definition["testOpenedAtFreeze"] is False
    assert definition["previousProvisionalTestConsumedAsDevelopment"] is True
    assert len({row["venueId"] for row in definition["rows"]}) == 200
    assert sum(row["split"] == "train" for row in definition["rows"]) == 80
    assert sum(row["split"] == "validation" for row in definition["rows"]) == 40
    new_test = [row for row in definition["rows"] if row["split"] == "test"]
    assert len(new_test) == 80
    assert all(row["humanReviewStatus"] == "pending" for row in new_test)
    assert len((output_dir / "generation-worklist.jsonl").read_text().splitlines()) == 80


def test_replacement_selection_is_versioned_and_preserves_identity(tmp_path: Path) -> None:
    """Un reemplazo cambia ruta/prompt, pero no identidad, etiqueta o split."""

    image_id = str(uuid5(NAMESPACE_URL, "replace-image"))
    definition_path = tmp_path / "definition.json"
    selection_path = tmp_path / "selection.json"
    output_path = tmp_path / "reviewed.json"
    definition_path.write_text(
        json.dumps(
            {
                "rows": [
                    {
                        "imageId": image_id,
                        "categoryCode": "instalacion-municipal",
                        "split": "test",
                        "relativePath": "images/original.png",
                        "generatorProvenance": {"promptVersion": "v1"},
                    }
                ]
            }
        ),
        encoding="utf-8",
    )
    selection_path.write_text(
        json.dumps(
            {
                "schemaVersion": 1,
                "selectionVersion": "r1",
                "selectedBeforeModelInference": True,
                "reason": "symbol",
                "replacements": [
                    {
                        "imageId": image_id,
                        "categoryCode": "instalacion-municipal",
                        "split": "test",
                        "originalPath": "images/original.png",
                        "replacementPath": "images/replacement.png",
                        "promptVersion": "v2-r1",
                        "prompt": "neutral civic room",
                    }
                ],
            }
        ),
        encoding="utf-8",
    )

    reviewed = apply_replacement_selection(
        definition_path, selection_path, output_path
    )

    assert reviewed["rows"][0]["imageId"] == image_id
    assert reviewed["rows"][0]["relativePath"] == "images/replacement.png"
    assert reviewed["rows"][0]["replacedPath"] == "images/original.png"
    assert reviewed["rows"][0]["generatorProvenance"]["promptVersion"] == "v2-r1"
