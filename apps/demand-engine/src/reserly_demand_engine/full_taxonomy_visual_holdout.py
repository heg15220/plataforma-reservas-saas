"""Prepara el corpus v2 con una vista development y otra holdout por tipo.

Las 220 imágenes v1 ya consumidas se referencian exclusivamente como desarrollo.
Los 34 tipos ausentes reciben una nueva vista de desarrollo y los 254 tipos reciben
una vista holdout distinta. Este módulo construye y sella bytes; nunca abre el
holdout para inferencia ni aprueba revisión humana de forma automática.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any
from uuid import NAMESPACE_URL, uuid5


DATASET_VERSION = "synthetic-marketplace-full-taxonomy-visual-v2"
GENERATED_AT = "2026-08-30T23:00:00Z"

_DEVELOPMENT_SCENES = (
    "vista interior lateral del área principal",
    "vista interior desde el fondo hacia la entrada",
    "encuadre interior centrado en el equipamiento característico",
    "vista oblicua interior con distribución completa",
)

_HOLDOUT_SCENES = (
    "vista interior desde una esquina distinta, con composición amplia",
    "perspectiva documental a altura de los ojos desde el área de atención",
    "vista interior diagonal que prioriza actividad, mobiliario y circulación",
    "encuadre ambiental del espacio funcional desde un punto de vista independiente",
)

_HOLDOUT_LIGHT = (
    "luz natural difusa de primera hora",
    "luz neutra de mediodía con sombras suaves",
    "luz natural de tarde entrando desde un lateral",
    "día cubierto con iluminación interior equilibrada",
)


def _stable_id(role: str, code: str) -> str:
    """Genera un UUID reproducible y disjunto por rol y tipo."""

    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{role}:{code}"))


def _sha256(path: Path) -> str:
    """Calcula el hash de los bytes exactos que consumirá CLIP."""

    return hashlib.sha256(path.read_bytes()).hexdigest()


def _prompt(type_row: dict[str, Any], family_label: str, index: int, role: str) -> str:
    """Crea prompts distintos por split sin codificar rótulos en los píxeles."""

    type_label = type_row["name"]["es"]
    if role == "development":
        scene = _DEVELOPMENT_SCENES[index % len(_DEVELOPMENT_SCENES)]
        light = "iluminación natural y artificial equilibrada"
        independence = "No imitar ninguna fotografía anterior."
    else:
        scene = _HOLDOUT_SCENES[(index + 1) % len(_HOLDOUT_SCENES)]
        light = _HOLDOUT_LIGHT[(index + 2) % len(_HOLDOUT_LIGHT)]
        independence = (
            "Debe ser un establecimiento diferente, con arquitectura, mobiliario, distribución, "
            "paleta y punto de vista independientes de cualquier vista de desarrollo."
        )
    return (
        f"Use case: photorealistic-natural. Asset type: dataset visual de local físico. "
        f"Fotografía documental fotorrealista en España de un establecimiento de tipo «{type_label}», "
        f"familia «{family_label}». {scene}; {light}. Mostrar mediante arquitectura, mobiliario, "
        f"equipamiento y distribución señales inequívocas propias de {type_label}, sin depender de "
        f"texto. {independence} Una única escena horizontal 4:3, lente natural 35 mm, materiales "
        "realistas, detalle alto. Sin personas identificables, sin logotipos, sin marcas, sin "
        "carteles, sin letras, sin números, sin texto legible o superpuesto, sin marcas de agua y "
        "sin collage. No mostrar etiquetas de categoría."
    )


def build_manifest(taxonomy_path: Path, v1_manifest_path: Path, output_root: Path) -> dict[str, Any]:
    """Construye el plan 254 development + 254 holdout sin abrir test."""

    taxonomy = json.loads(taxonomy_path.read_text(encoding="utf-8"))
    v1 = json.loads(v1_manifest_path.read_text(encoding="utf-8"))
    families = {row["code"]: row for row in taxonomy["families"]}
    types = sorted(taxonomy["types"], key=lambda row: row["sourceId"])
    if len(families) != 23 or len(types) != 254:
        raise ValueError("FULL_TAXONOMY_HOLDOUT_TAXONOMY_INVALID")
    v1_by_type = {row["typeCode"]: row for row in v1["rows"]}
    if set(v1_by_type) != {row["code"] for row in types}:
        raise ValueError("FULL_TAXONOMY_HOLDOUT_V1_TYPE_SET_INVALID")

    development: list[dict[str, Any]] = []
    holdout: list[dict[str, Any]] = []
    for index, type_row in enumerate(types):
        code = type_row["code"]
        family = families[type_row["familyCode"]]
        old = v1_by_type[code]
        old_materialized = old["generation"]["status"] == "materializedPendingHumanReview"
        development.append(
            {
                "imageId": old["imageId"] if old_materialized else _stable_id("development-image", code),
                "venueId": old["venueId"] if old_materialized else _stable_id("development-venue", code),
                "sourceId": type_row["sourceId"],
                "typeCode": code,
                "typeLabelEs": type_row["name"]["es"],
                "familyCode": type_row["familyCode"],
                "familyLabelEs": family["name"]["es"],
                "split": "development",
                "relativePath": (
                    f"../synthetic-marketplace-full-taxonomy-visual-v1/{old['relativePath']}"
                    if old_materialized
                    else f"development-images/{type_row['sourceId']:03d}-{code}-development-v2.png"
                ),
                "prompt": None if old_materialized else _prompt(type_row, family["name"]["es"], index, "development"),
                "generation": {
                    "provider": old["generation"]["provider"] if old_materialized else "openai-imagegen",
                    "mode": "reused-consumed-development-v1" if old_materialized else "built-in-distinct-asset",
                    "status": "reusedConsumedDevelopment" if old_materialized else "pending",
                    "imageSha256": old["generation"]["imageSha256"] if old_materialized else None,
                },
                "humanReviewStatus": old["humanReviewStatus"] if old_materialized else "pendingHumanReview",
                "testEligible": False,
                "productionTrainingAllowed": False,
                "synthetic": True,
            }
        )
        holdout.append(
            {
                "imageId": _stable_id("holdout-image", code),
                "venueId": _stable_id("holdout-venue", code),
                "sourceId": type_row["sourceId"],
                "typeCode": code,
                "typeLabelEs": type_row["name"]["es"],
                "familyCode": type_row["familyCode"],
                "familyLabelEs": family["name"]["es"],
                "split": "sealedHoldout",
                "relativePath": f"sealed-holdout-images/{type_row['sourceId']:03d}-{code}-holdout-v2.png",
                "prompt": _prompt(type_row, family["name"]["es"], index, "holdout"),
                "generation": {
                    "provider": "openai-imagegen",
                    "mode": "built-in-distinct-asset",
                    "status": "pending",
                    "imageSha256": None,
                },
                "humanReviewStatus": "pendingHumanReview",
                "testEligible": True,
                "productionTrainingAllowed": False,
                "synthetic": True,
            }
        )

    manifest = {
        "schemaVersion": 1,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "taxonomyVersion": taxonomy["taxonomyVersion"],
        "taxonomySha256": _sha256(taxonomy_path),
        "sourceDevelopmentDatasetVersion": v1["datasetVersion"],
        "sourceDevelopmentManifestSha256": _sha256(v1_manifest_path),
        "protocol": {
            "developmentViewsPerType": 1,
            "holdoutViewsPerType": 1,
            "developmentMayReuseConsumedV1": True,
            "holdoutReuseForbidden": True,
            "sameVenueAcrossSplitsForbidden": True,
            "sameImageAcrossSplitsForbidden": True,
            "promptAsFeatureForbidden": True,
            "holdoutPredictionBudget": 1,
            "selectionUsesDevelopmentOnly": True,
        },
        "expectedCoverage": {
            "typeCount": 254,
            "familyCount": 23,
            "developmentImageCount": 254,
            "holdoutImageCount": 254,
            "totalImageCount": 508,
            "familyCountsPerSplit": dict(sorted(Counter(row["familyCode"] for row in development).items())),
        },
        "developmentRows": development,
        "holdoutRows": holdout,
        "materialization": {
            "reusedDevelopmentCount": sum(row["generation"]["status"] == "reusedConsumedDevelopment" for row in development),
            "newDevelopmentCount": 0,
            "holdoutCount": 0,
            "complete": False,
        },
        "humanReviewComplete": False,
        "trainingAllowed": False,
        "promotionAllowed": False,
    }
    output_root.mkdir(parents=True, exist_ok=True)
    (output_root / "generation-manifest.v2.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    """Resuelve rutas v1/v2 sin permitir escapar del árbol evaluation."""

    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_HOLDOUT_PATH_ESCAPE")
    return path


def seal_manifest(manifest_path: Path) -> dict[str, Any]:
    """Sella todas las vistas existentes manteniendo revisión y test sin abrir."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    dataset_root = manifest_path.parent
    evaluation_root = dataset_root.parent
    for row in manifest["developmentRows"] + manifest["holdoutRows"]:
        path = _resolve(evaluation_root, dataset_root, row["relativePath"])
        if not path.is_file():
            continue
        digest = _sha256(path)
        previous = row["generation"].get("imageSha256")
        if previous is not None and previous != digest:
            raise ValueError("FULL_TAXONOMY_HOLDOUT_SOURCE_HASH_CHANGED")
        row["generation"]["imageSha256"] = digest
        if row["generation"]["status"] == "pending":
            row["generation"]["status"] = "materializedPendingHumanReview"

    development_count = sum(row["generation"]["imageSha256"] is not None for row in manifest["developmentRows"])
    holdout_count = sum(row["generation"]["imageSha256"] is not None for row in manifest["holdoutRows"])
    manifest["materialization"] = {
        "reusedDevelopmentCount": sum(row["generation"]["status"] == "reusedConsumedDevelopment" for row in manifest["developmentRows"]),
        "newDevelopmentCount": sum(row["generation"]["status"] == "materializedPendingHumanReview" for row in manifest["developmentRows"]),
        "holdoutCount": holdout_count,
        "developmentComplete": development_count == 254,
        "holdoutComplete": holdout_count == 254,
        "complete": development_count == 254 and holdout_count == 254,
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def run() -> None:
    """CLI build/seal del protocolo v2."""

    repo_root = Path(__file__).resolve().parents[4]
    evaluation_root = repo_root / "apps/demand-engine/evaluation"
    output_root = evaluation_root / "synthetic-marketplace-full-taxonomy-visual-v2"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "seal"), nargs="?", default="build")
    parser.add_argument("--taxonomy", type=Path, default=repo_root / "packages/demand-contracts/catalog/venue-taxonomy.v1.json")
    parser.add_argument("--v1-manifest", type=Path, default=evaluation_root / "synthetic-marketplace-full-taxonomy-visual-v1/generation-manifest.json")
    parser.add_argument("--output", type=Path, default=output_root)
    args = parser.parse_args()
    result = (
        build_manifest(args.taxonomy, args.v1_manifest, args.output)
        if args.command == "build"
        else seal_manifest(args.output / "generation-manifest.v2.json")
    )
    print(json.dumps(result["materialization"], ensure_ascii=False))


if __name__ == "__main__":
    run()
