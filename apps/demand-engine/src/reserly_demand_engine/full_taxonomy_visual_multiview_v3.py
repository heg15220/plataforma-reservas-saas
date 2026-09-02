"""Construye el contrato multivista v3 sin abrir ni inferir sobre su holdout.

V3 convierte las dos vistas v2 ya consumidas en material exclusivamente de
desarrollo, añade una tercera vista nueva por tipo y reserva un establecimiento
completamente nuevo por tipo como holdout. Los arquetipos son objetivos auxiliares
gobernados; nunca pueden utilizarse como features verdaderas durante inferencia.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any
from uuid import NAMESPACE_URL, uuid5


DATASET_VERSION = "synthetic-marketplace-full-taxonomy-visual-v3"
GENERATED_AT = "2026-08-31T12:00:00Z"

# Rangos inclusivos de sourceId. Los códigos describen forma espacial observable,
# no identidad fiscal ni categoría administrativa.
_ARCHETYPE_RANGES: tuple[tuple[int, int, str, str], ...] = (
    (1, 3, "dining-room", "sala de restauración"),
    (4, 9, "hospitality-counter", "barra y sala hostelera"),
    (10, 15, "food-service-counter", "mostrador de servicio alimentario"),
    (16, 23, "fresh-food-counter", "mostrador de alimento fresco"),
    (24, 30, "specialty-food-shop", "comercio alimentario especializado"),
    (31, 35, "grocery-aisles", "pasillos de alimentación"),
    (36, 46, "fashion-boutique", "boutique de moda"),
    (47, 55, "health-retail", "comercio de salud y cuidado"),
    (56, 61, "home-showroom", "showroom de hogar"),
    (62, 69, "hardware-showroom", "comercio de bricolaje y materiales"),
    (70, 73, "technology-office-retail", "comercio de tecnología y oficina"),
    (74, 90, "specialist-cultural-retail", "comercio cultural especializado"),
    (91, 95, "garden-pet-retail", "comercio de jardinería o mascotas"),
    (96, 102, "vehicle-showroom", "exposición de vehículos y maquinaria"),
    (103, 104, "fuel-service", "estación o comercio de combustible"),
    (105, 110, "general-retail-hall", "gran superficie o comercio general"),
    (111, 117, "lodging-interior", "alojamiento interior"),
    (118, 118, "camping-outdoor", "camping exterior"),
    (119, 122, "hospital-facility", "instalación sanitaria"),
    (123, 136, "clinical-consultation", "consulta o tratamiento clínico"),
    (137, 140, "animal-care", "atención y cuidado animal"),
    (141, 147, "social-care", "centro residencial o social"),
    (148, 156, "personal-care-stations", "puestos de belleza y cuidado"),
    (157, 159, "garment-service-workshop", "servicio y arreglo textil"),
    (160, 161, "fitness-facility", "instalación de actividad física"),
    (162, 169, "sports-court-venue", "pista o recinto deportivo"),
    (170, 177, "classroom", "aula educativa"),
    (178, 181, "specialist-training", "formación especializada o residencia"),
    (182, 184, "cultural-venue", "equipamiento cultural"),
    (185, 186, "nature-attraction", "atracción natural"),
    (187, 196, "entertainment-event-venue", "ocio y eventos"),
    (197, 199, "photo-reprography-studio", "estudio fotográfico o reprografía"),
    (200, 208, "repair-workshop", "taller de reparación"),
    (209, 218, "travel-rental-mobility", "viajes, alquiler o movilidad"),
    (219, 238, "professional-office", "oficina profesional"),
    (239, 247, "financial-property-office", "oficina financiera o inmobiliaria"),
    (248, 251, "public-service-counter", "mostrador de servicio al público"),
    (252, 254, "association-personal-service", "sede o servicio personal"),
)

_SENSITIVE_OR_MINOR_RANGES = ((40, 40), (119, 159), (170, 172))
_PEOPLE_USE_RANGES = (
    (1, 39), (41, 118), (160, 169), (173, 196), (209, 254)
)

_DEVELOPMENT_SCENES = (
    "vista operacional amplia desde un lateral, mostrando circulación y equipamiento",
    "perspectiva a altura de los ojos desde el acceso secundario",
    "encuadre diagonal que conecta mostrador, mobiliario y zona principal",
    "vista desde el fondo con profundidad espacial y elementos funcionales completos",
)
_HOLDOUT_SCENES = (
    "vista documental independiente desde una esquina frontal",
    "perspectiva amplia desde la zona de circulación opuesta",
    "encuadre natural ligeramente elevado que muestra la distribución completa",
    "vista ambiental desde un punto de atención distinto y arquitectura nueva",
)
_DEVELOPMENT_LIGHT = (
    "luz natural templada de mañana",
    "luz neutra de mediodía con iluminación interior equilibrada",
    "luz lateral suave de última hora de la tarde",
    "día cubierto con materiales y colores realistas",
)
_HOLDOUT_LIGHT = (
    "luz fría natural de primera hora con sombras suaves",
    "luz solar difusa de mediodía desde otra orientación",
    "luz cálida de tarde combinada con iluminación funcional",
    "luz uniforme de día lluvioso sin dramatización",
)


def _sha256(path: Path) -> str:
    """Devuelve el hash de los bytes exactos versionados por el contrato."""

    return hashlib.sha256(path.read_bytes()).hexdigest()


def _stable_id(role: str, code: str) -> str:
    """Genera identidades deterministas, nuevas y disjuntas por rol."""

    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{role}:{code}"))


def _in_ranges(source_id: int, ranges: tuple[tuple[int, int], ...]) -> bool:
    return any(start <= source_id <= end for start, end in ranges)


def archetype_for(source_id: int) -> dict[str, str]:
    """Mapea cada tipo a un arquetipo espacial auxiliar exhaustivo."""

    for start, end, code, label in _ARCHETYPE_RANGES:
        if start <= source_id <= end:
            return {"code": code, "labelEs": label}
    raise ValueError(f"FULL_TAXONOMY_V3_ARCHETYPE_MISSING:{source_id}")


def _people_policy(source_id: int, index: int) -> dict[str, Any]:
    """Permite personas solo como contexto secundario no identificable y no sensible."""

    sensitive = _in_ranges(source_id, _SENSITIVE_OR_MINOR_RANGES)
    allowed = not sensitive and _in_ranges(source_id, _PEOPLE_USE_RANGES) and index % 3 == 0
    return {
        "mode": "backgroundAdultsNonIdentifiable" if allowed else "emptyVenuePreferred",
        "allowed": allowed,
        "maxAdults": 3 if allowed else 0,
        "identifiableFacesForbidden": True,
        "minorsForbidden": True,
        "patientsOrSensitiveSituationsForbidden": True,
        "biometricOrSensitiveInferenceForbidden": True,
    }


def _prompt(type_row: dict[str, Any], family_label: str, index: int, split: str) -> str:
    """Crea una vista nueva sin OCR y con variación independiente por split."""

    source_id = int(type_row["sourceId"])
    archetype = archetype_for(source_id)
    people = _people_policy(source_id, index + (1 if split == "sealedHoldoutV3" else 0))
    if split == "developmentViewC":
        scene = _DEVELOPMENT_SCENES[index % len(_DEVELOPMENT_SCENES)]
        light = _DEVELOPMENT_LIGHT[(index + 1) % len(_DEVELOPMENT_LIGHT)]
        independence = "No imitar ninguna de las dos vistas v2 consumidas."
    else:
        scene = _HOLDOUT_SCENES[(index + 2) % len(_HOLDOUT_SCENES)]
        light = _HOLDOUT_LIGHT[(index + 3) % len(_HOLDOUT_LIGHT)]
        independence = (
            "Establecimiento completamente nuevo y no relacionado: cambiar arquitectura, mobiliario, "
            "paleta, distribución, objetos, orientación, punto de vista y condiciones de luz respecto "
            "a todas las vistas de desarrollo."
        )
    people_clause = (
        "Puede haber entre una y tres personas adultas pequeñas al fondo, de espaldas o desenfocadas, "
        "sin rostro reconocible; nunca son el sujeto principal."
        if people["allowed"]
        else "Escena vacía y preparada; no mostrar personas, pacientes ni menores."
    )
    return (
        "Use case: photorealistic-natural. Asset type: evidencia visual taxonómica offline. "
        f"Fotografía documental fotorrealista horizontal 4:3 en España de un local físico de tipo "
        f"«{type_row['name']['es']}», familia «{family_label}», con arquetipo espacial "
        f"«{archetype['labelEs']}». {scene}; {light}. Mostrar señales inequívocas mediante arquitectura, "
        "mobiliario, equipamiento, materiales y distribución; el tipo debe entenderse sin texto. "
        f"{independence} {people_clause} Lente natural 35 mm, perspectiva creíble, detalle alto y "
        "texturas cotidianas. Sin logotipos, marcas, carteles, letras, números, texto legible o "
        "superpuesto, marcas de agua, collage ni etiqueta de categoría. Una única escena y un único local."
    )


def _reuse_row(row: dict[str, Any], view: str, source_manifest: str) -> dict[str, Any]:
    """Copia trazabilidad v2 y anula para siempre su elegibilidad como test."""

    return {
        **{key: row[key] for key in (
            "imageId", "venueId", "sourceId", "typeCode", "typeLabelEs",
            "familyCode", "familyLabelEs", "relativePath", "prompt", "synthetic"
        )},
        "split": "development",
        "developmentView": view,
        "relativePath": f"../synthetic-marketplace-full-taxonomy-visual-v2/{row['relativePath']}",
        "visualArchetype": archetype_for(int(row["sourceId"])),
        "peoplePolicy": _people_policy(int(row["sourceId"]), int(row["sourceId"]) - 1),
        "generation": {
            "provider": row["generation"]["provider"],
            "mode": f"reused-consumed-v2-{source_manifest}-as-v3-development",
            "status": "reusedConsumedAsDevelopment",
            "imageSha256": row["generation"]["imageSha256"],
        },
        "humanReviewStatus": "approvedFromV2",
        "developmentTrainingAllowed": True,
        "testEligible": False,
        "productionTrainingAllowed": False,
    }


def build_manifest(
    taxonomy_path: Path,
    v2_manifest_path: Path,
    v2_opening_record_path: Path,
    output_root: Path,
) -> dict[str, Any]:
    """Materializa el plan 3 vistas development + un holdout nuevo por tipo."""

    taxonomy = json.loads(taxonomy_path.read_text(encoding="utf-8"))
    v2 = json.loads(v2_manifest_path.read_text(encoding="utf-8"))
    opening = json.loads(v2_opening_record_path.read_text(encoding="utf-8"))
    if opening.get("consumed") != 1 or opening.get("reopenAllowed") is not False:
        raise ValueError("FULL_TAXONOMY_V3_V2_HOLDOUT_NOT_CONSUMED")
    types = sorted(taxonomy["types"], key=lambda row: row["sourceId"])
    families = {row["code"]: row for row in taxonomy["families"]}
    if len(types) != 254 or len(families) != 23:
        raise ValueError("FULL_TAXONOMY_V3_TAXONOMY_INVALID")
    v2_development = {row["typeCode"]: row for row in v2["developmentRows"]}
    v2_holdout = {row["typeCode"]: row for row in v2["holdoutRows"]}
    if set(v2_development) != set(v2_holdout) or len(v2_development) != 254:
        raise ValueError("FULL_TAXONOMY_V3_V2_TYPE_SET_INVALID")

    development_rows: list[dict[str, Any]] = []
    holdout_rows: list[dict[str, Any]] = []
    for index, type_row in enumerate(types):
        code = type_row["code"]
        family = families[type_row["familyCode"]]
        development_rows.extend((
            _reuse_row(v2_development[code], "A", "development"),
            _reuse_row(v2_holdout[code], "B", "holdout"),
        ))
        archetype = archetype_for(int(type_row["sourceId"]))
        people = _people_policy(int(type_row["sourceId"]), index)
        development_rows.append({
            "imageId": _stable_id("development-view-c-image", code),
            "venueId": _stable_id("development-view-c-venue", code),
            "sourceId": type_row["sourceId"],
            "typeCode": code,
            "typeLabelEs": type_row["name"]["es"],
            "familyCode": type_row["familyCode"],
            "familyLabelEs": family["name"]["es"],
            "visualArchetype": archetype,
            "peoplePolicy": people,
            "split": "development",
            "developmentView": "C",
            "relativePath": f"development-view-c-images/{type_row['sourceId']:03d}-{code}-development-c-v3.png",
            "prompt": _prompt(type_row, family["name"]["es"], index, "developmentViewC"),
            "generation": {"provider": "openai-imagegen", "mode": "built-in-distinct-asset", "status": "pending", "imageSha256": None},
            "humanReviewStatus": "pendingHumanReview",
            "developmentTrainingAllowed": False,
            "testEligible": False,
            "productionTrainingAllowed": False,
            "synthetic": True,
        })
        holdout_people = _people_policy(int(type_row["sourceId"]), index + 1)
        holdout_rows.append({
            "imageId": _stable_id("holdout-v3-image", code),
            "venueId": _stable_id("holdout-v3-venue", code),
            "sourceId": type_row["sourceId"],
            "typeCode": code,
            "typeLabelEs": type_row["name"]["es"],
            "familyCode": type_row["familyCode"],
            "familyLabelEs": family["name"]["es"],
            "visualArchetype": archetype,
            "peoplePolicy": holdout_people,
            "split": "sealedHoldoutV3",
            "relativePath": f"sealed-holdout-v3-images/{type_row['sourceId']:03d}-{code}-holdout-v3.png",
            "prompt": _prompt(type_row, family["name"]["es"], index, "sealedHoldoutV3"),
            "generation": {"provider": "openai-imagegen", "mode": "built-in-distinct-asset", "status": "pending", "imageSha256": None},
            "humanReviewStatus": "pendingHumanReview",
            "testEvaluationAllowed": False,
            "testEligible": True,
            "productionTrainingAllowed": False,
            "synthetic": True,
        })

    archetype_counts = Counter(row["visualArchetype"]["code"] for row in holdout_rows)
    manifest = {
        "schemaVersion": 1,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "taxonomyVersion": taxonomy["taxonomyVersion"],
        "taxonomySha256": _sha256(taxonomy_path),
        "sourceV2ManifestSha256": _sha256(v2_manifest_path),
        "sourceV2OpeningRecordSha256": _sha256(v2_opening_record_path),
        "protocol": {
            "developmentViewsPerType": 3,
            "trainingViewsPerType": 2,
            "validationViewsPerType": 1,
            "validationRotatesByView": True,
            "sameImageInTrainAndValidationForbidden": True,
            "holdoutViewsPerType": 1,
            "v2ConsumedHoldoutReclassifiedAsDevelopmentOnly": True,
            "v3HoldoutReuseForbidden": True,
            "sameVenueAcrossDevelopmentAndV3HoldoutForbidden": True,
            "sameImageAcrossDevelopmentAndV3HoldoutForbidden": True,
            "promptTypeFamilyOrTrueArchetypeAsFeatureForbidden": True,
            "archetypeAuxiliaryTargetMustBePredictedFromPixelsAtInference": True,
            "holdoutPredictionBudget": 1,
            "selectionUsesDevelopmentOnly": True,
            "humanReviewRequiredBeforeEmbeddingExtraction": True,
        },
        "peoplePolicy": {
            "purpose": "contexto ambiental secundario",
            "identifiableFacesForbidden": True,
            "minorsForbidden": True,
            "patientsOrSensitiveSituationsForbidden": True,
            "biometricOrSensitiveInferenceForbidden": True,
        },
        "expectedCoverage": {
            "typeCount": 254,
            "familyCount": 23,
            "archetypeCount": len(archetype_counts),
            "developmentImageCount": 762,
            "newDevelopmentImageCount": 254,
            "holdoutImageCount": 254,
            "newImageCount": 508,
            "totalReferencedImageCount": 1016,
            "familyCountsPerView": dict(sorted(Counter(row["familyCode"] for row in holdout_rows).items())),
            "archetypeCountsPerView": dict(sorted(archetype_counts.items())),
        },
        "developmentRows": development_rows,
        "holdoutRows": holdout_rows,
        "materialization": {
            "reusedDevelopmentCount": 508,
            "newDevelopmentCount": 0,
            "holdoutCount": 0,
            "newImageCount": 0,
            "complete": False,
        },
        "humanReviewComplete": False,
        "developmentTrainingAllowed": False,
        "holdoutEvaluationAllowed": False,
        "promotionAllowed": False,
    }
    output_root.mkdir(parents=True, exist_ok=True)
    (output_root / "generation-manifest.v3.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def _resolve(evaluation_root: Path, dataset_root: Path, relative_path: str) -> Path:
    path = (dataset_root / relative_path).resolve()
    if not path.is_relative_to(evaluation_root.resolve()):
        raise ValueError("FULL_TAXONOMY_V3_PATH_ESCAPE")
    return path


def seal_manifest(manifest_path: Path) -> dict[str, Any]:
    """Sella imágenes materializadas sin aprobarlas ni consumir el holdout."""

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    dataset_root, evaluation_root = manifest_path.parent, manifest_path.parent.parent
    for row in manifest["developmentRows"] + manifest["holdoutRows"]:
        path = _resolve(evaluation_root, dataset_root, row["relativePath"])
        if not path.is_file():
            continue
        digest = _sha256(path)
        previous = row["generation"].get("imageSha256")
        if previous is not None and previous != digest:
            raise ValueError("FULL_TAXONOMY_V3_SOURCE_HASH_CHANGED")
        row["generation"]["imageSha256"] = digest
        if row["generation"]["status"] == "pending":
            row["generation"]["status"] = "materializedPendingHumanReview"
    reused = sum(row["generation"]["status"] == "reusedConsumedAsDevelopment" for row in manifest["developmentRows"])
    new_development = sum(row["generation"]["status"] == "materializedPendingHumanReview" for row in manifest["developmentRows"])
    holdout = sum(row["generation"]["status"] == "materializedPendingHumanReview" for row in manifest["holdoutRows"])
    manifest["materialization"] = {
        "reusedDevelopmentCount": reused,
        "newDevelopmentCount": new_development,
        "holdoutCount": holdout,
        "newImageCount": new_development + holdout,
        "developmentComplete": reused == 508 and new_development == 254,
        "holdoutComplete": holdout == 254,
        "complete": reused == 508 and new_development == 254 and holdout == 254,
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def run() -> None:
    """CLI reproducible para construir o sellar el manifiesto v3."""

    repo_root = Path(__file__).resolve().parents[4]
    evaluation_root = repo_root / "apps/demand-engine/evaluation"
    output_root = evaluation_root / DATASET_VERSION
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "seal"), nargs="?", default="build")
    parser.add_argument("--taxonomy", type=Path, default=repo_root / "packages/demand-contracts/catalog/venue-taxonomy.v1.json")
    parser.add_argument("--v2-manifest", type=Path, default=evaluation_root / "synthetic-marketplace-full-taxonomy-visual-v2/generation-manifest.v2.json")
    parser.add_argument("--v2-opening", type=Path, default=evaluation_root / "synthetic-marketplace-full-taxonomy-visual-v2/test-opening-record.v2.json")
    parser.add_argument("--output", type=Path, default=output_root)
    args = parser.parse_args()
    result = (
        build_manifest(args.taxonomy, args.v2_manifest, args.v2_opening, args.output)
        if args.command == "build"
        else seal_manifest(args.output / "generation-manifest.v3.json")
    )
    print(json.dumps(result["materialization"], ensure_ascii=False))


if __name__ == "__main__":
    run()
