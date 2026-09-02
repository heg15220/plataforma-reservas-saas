"""Construye el contrato del corpus visual que cubre la taxonomía completa.

Este módulo no genera píxeles. Produce un manifiesto determinista con un local y
un prompt independiente por cada tipo gobernado de ``venue-taxonomy.v1``. Las
imágenes se materializan mediante el generador autorizado y después se sellan
con ``seal``; esa separación evita confundir prompts previstos con evidencia
visual realmente disponible o revisada.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any
from uuid import NAMESPACE_URL, uuid5


DATASET_VERSION = "synthetic-marketplace-full-taxonomy-visual-v1"
GENERATED_AT = "2026-08-30T20:00:00Z"

_CITIES = (
    ("Madrid", "Centro", 40.4168, -3.7038),
    ("Barcelona", "Eixample", 41.3874, 2.1686),
    ("Valencia", "Ciutat Vella", 39.4699, -0.3763),
    ("Sevilla", "Triana", 37.3826, -5.9963),
    ("Bilbao", "Indautxu", 43.2630, -2.9350),
    ("Málaga", "Soho", 36.7213, -4.4214),
    ("Zaragoza", "Centro", 41.6488, -0.8891),
    ("A Coruña", "Ensanche", 43.3623, -8.4115),
    ("Palma", "Santa Catalina", 39.5696, 2.6502),
    ("Valladolid", "Campo Grande", 41.6523, -4.7245),
    ("Granada", "Realejo", 37.1773, -3.5986),
    ("Santander", "Puertochico", 43.4623, -3.8099),
)

_LIGHTING = (
    "luz natural suave de mañana",
    "luz diurna neutra",
    "luz cálida de última hora de la tarde",
    "día nublado con iluminación uniforme",
)

_COMPOSITIONS = (
    "vista interior amplia desde la entrada",
    "perspectiva interior a altura de los ojos",
    "vista diagonal que muestre distribución y equipamiento",
    "encuadre documental del espacio principal",
)

_FAMILY_CUES = {
    "restauracion-y-bebidas": "mesas, zona de servicio y elementos propios de hostelería",
    "comercio-alimentario": "expositores alimentarios, mostrador y circulación de tienda",
    "moda-y-complementos": "expositores de producto, probadores o mobiliario comercial",
    "farmacia-cosmetica-y-salud-retail": "estanterías ordenadas y mostrador sanitario o cosmético",
    "hogar-y-bricolaje": "muestras de hogar, herramientas o materiales organizados",
    "tecnologia-y-oficina": "dispositivos, accesorios o material de oficina expuesto",
    "comercio-cultural-y-especializado": "producto especializado y mobiliario temático reconocible",
    "flores-jardineria-y-mascotas": "plantas, flores o suministros para animales claramente visibles",
    "automocion-y-movilidad": "vehículos, recambios o equipamiento de movilidad",
    "grandes-superficies-y-comercio-general": "pasillos amplios y surtido variado por secciones",
    "alojamiento": "recepción, habitación o zona común propia de alojamiento",
    "salud-y-clinicas": "consulta o sala clínica limpia con equipamiento no invasivo",
    "veterinaria-y-cuidado-animal": "espacio de atención animal limpio, sin procedimientos médicos",
    "servicios-sociales": "espacio accesible, acogedor y preparado para asistencia cotidiana",
    "belleza-y-cuidado-personal": "puestos o cabinas de tratamiento y equipamiento de cuidado personal",
    "deporte-y-actividad-fisica": "superficie deportiva y equipamiento específico de la actividad",
    "educacion-y-formacion": "aula o taller didáctico con material propio de la especialidad",
    "ocio-cultura-y-entretenimiento": "espacio público con infraestructura cultural o recreativa",
    "fotografia-y-reparaciones": "mesa de trabajo, herramientas o equipo fotográfico especializado",
    "viajes-alquiler-y-movilidad": "mostrador de atención y elementos físicos del servicio de movilidad",
    "servicios-profesionales-y-empresas": "despacho o área de atención profesional claramente funcional",
    "finanzas-seguros-e-inmobiliario": "oficina de atención con mesas de consulta y privacidad",
    "otros-servicios-al-publico": "mostrador y equipamiento específico del servicio presencial",
}


def _sha256(path: Path) -> str:
    """Devuelve el SHA-256 del contenido exacto de un archivo."""

    return hashlib.sha256(path.read_bytes()).hexdigest()


def _stable_id(kind: str, code: str) -> str:
    """Genera identificadores reproducibles sin incorporar datos personales."""

    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{kind}:{code}"))


def _prompt(type_row: dict[str, Any], family_name: str, index: int) -> str:
    """Compone un prompt visual sin texto que fuerza señales del establecimiento."""

    type_name = type_row["name"]["es"]
    cues = _FAMILY_CUES[type_row["familyCode"]]
    return (
        f"Fotografía documental fotorrealista de un local físico de tipo «{type_name}» "
        f"perteneciente a «{family_name}», en España. {_COMPOSITIONS[index % len(_COMPOSITIONS)]}, "
        f"{_LIGHTING[index % len(_LIGHTING)]}. Deben distinguirse por la arquitectura, el mobiliario "
        f"y el equipamiento estas señales: {cues}; añade además detalles inequívocos y realistas "
        f"específicos de {type_name}. Espacio operativo, ordenado, accesible y creíble, composición "
        "horizontal 4:3, lente natural de 35 mm, profundidad de campo moderada. Sin personas "
        "identificables, sin logotipos, sin marcas, sin carteles, sin letras, sin números, sin marcas "
        "de agua, sin collage y sin texto superpuesto. Una única escena y un único establecimiento."
    )


def build_manifest(taxonomy_path: Path, output_root: Path) -> dict[str, Any]:
    """Crea el manifiesto verificable de cobertura completa.

    Raises:
        ValueError: si la taxonomía no contiene exactamente 23 familias y 254
            códigos de tipo únicos o si algún tipo referencia una familia ajena.
    """

    taxonomy = json.loads(taxonomy_path.read_text(encoding="utf-8"))
    families = {row["code"]: row for row in taxonomy["families"]}
    types = taxonomy["types"]
    if len(families) != 23 or len(types) != 254:
        raise ValueError("FULL_TAXONOMY_VISUAL_EXPECTED_23_FAMILIES_254_TYPES")
    codes = [row["code"] for row in types]
    if len(set(codes)) != len(codes):
        raise ValueError("FULL_TAXONOMY_VISUAL_DUPLICATE_TYPE_CODE")
    if set(row["familyCode"] for row in types) - set(families):
        raise ValueError("FULL_TAXONOMY_VISUAL_UNKNOWN_FAMILY")

    rows: list[dict[str, Any]] = []
    for index, type_row in enumerate(sorted(types, key=lambda row: row["sourceId"])):
        family = families[type_row["familyCode"]]
        city, district, latitude, longitude = _CITIES[index % len(_CITIES)]
        filename = f"{type_row['sourceId']:03d}-{type_row['code']}.png"
        rows.append(
            {
                "imageId": _stable_id("image", type_row["code"]),
                "venueId": _stable_id("venue", type_row["code"]),
                "sourceId": type_row["sourceId"],
                "typeCode": type_row["code"],
                "typeLabelEs": type_row["name"]["es"],
                "familyCode": type_row["familyCode"],
                "familyLabelEs": family["name"]["es"],
                "location": {
                    "countryCode": "ES",
                    "city": city,
                    "district": district,
                    "latitude": round(latitude + ((index % 7) - 3) * 0.0017, 6),
                    "longitude": round(longitude + ((index % 5) - 2) * 0.0019, 6),
                },
                "relativePath": f"images/{filename}",
                "prompt": _prompt(type_row, family["name"]["es"], index),
                "generation": {
                    "provider": "openai-imagegen",
                    "mode": "built-in-distinct-asset",
                    "status": "pending",
                    "imageSha256": None,
                },
                "humanReviewStatus": "pendingHumanReview",
                "productionTrainingAllowed": False,
                "synthetic": True,
            }
        )

    family_counts = Counter(row["familyCode"] for row in rows)
    manifest = {
        "schemaVersion": 1,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "taxonomyVersion": taxonomy["taxonomyVersion"],
        "taxonomySha256": _sha256(taxonomy_path),
        "purpose": "Cobertura visual sintética de catálogo; no constituye evidencia productiva.",
        "coverage": {
            "familyCount": len(family_counts),
            "typeCount": len(rows),
            "imageCountExpected": len(rows),
            "imagesPerType": 1,
            "familyCounts": dict(sorted(family_counts.items())),
        },
        "leakagePolicy": {
            "ocrLabelForbidden": True,
            "reuseAcrossSplitsForbidden": True,
            "nearDuplicateHashForbidden": True,
            "promptTextAsModelFeatureForbidden": True,
        },
        "limitations": [
            "Una imagen por tipo demuestra cobertura, no generalización visual intratipo.",
            "Las imágenes sintéticas no sustituyen un test sellado con fotografías reales.",
            "Cada activo requiere revisión humana antes de habilitar cualquier entrenamiento.",
        ],
        "rows": rows,
    }
    output_root.mkdir(parents=True, exist_ok=True)
    output_path = output_root / "generation-manifest.json"
    output_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def seal_manifest(manifest_path: Path) -> dict[str, Any]:
    """Sella imágenes materializadas sin aprobarlas automáticamente.

    Solo cambia el estado a ``materializedPendingHumanReview`` y registra el
    hash. La aprobación visual sigue siendo una decisión humana explícita.
    """

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    root = manifest_path.parent
    missing: list[str] = []
    for row in manifest["rows"]:
        image_path = root / row["relativePath"]
        if not image_path.is_file():
            missing.append(row["relativePath"])
            continue
        row["generation"]["status"] = "materializedPendingHumanReview"
        row["generation"]["imageSha256"] = _sha256(image_path)
    manifest["materialization"] = {
        "materializedCount": len(manifest["rows"]) - len(missing),
        "missingCount": len(missing),
        "complete": not missing,
        "missing": missing,
    }
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def run() -> None:
    """CLI para construir o sellar el manifiesto del corpus visual."""

    repo_root = Path(__file__).resolve().parents[4]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "seal"), nargs="?", default="build")
    parser.add_argument(
        "--taxonomy",
        type=Path,
        default=repo_root / "packages/demand-contracts/catalog/venue-taxonomy.v1.json",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=repo_root
        / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v1",
    )
    args = parser.parse_args()
    manifest_path = args.output / "generation-manifest.json"
    result = (
        build_manifest(args.taxonomy, args.output)
        if args.command == "build"
        else seal_manifest(manifest_path)
    )
    print(json.dumps(result.get("coverage", result.get("materialization")), ensure_ascii=False))


if __name__ == "__main__":
    run()
