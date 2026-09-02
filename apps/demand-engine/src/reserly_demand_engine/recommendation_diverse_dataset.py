"""Construye el dataset v2 del recomendador con etiquetas funcionales diversas.

Reutiliza los 100 locales, 40 perfiles y referencias visuales sintéticas de v1,
pero no copia, modifica ni reclasifica imágenes. Los tipos de la taxonomía son
metadatos candidatos de desarrollo; la señal visual se limita al estilo y la
paleta ya declarados para cada local. El test temporal se genera y congela junto
con desarrollo, pero el entrenador puede seleccionarse sin leer sus etiquetas.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from collections import Counter
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Iterable
from uuid import NAMESPACE_URL, uuid5


DATASET_VERSION = "synthetic-marketplace-diverse-v2"
SEED = 2843
GENERATED_AT = "2026-08-30T12:00:00Z"

# Subtipos físicamente compatibles con las ocho categorías visuales existentes.
# No convierten el catálogo candidato en taxonomía pública ni en etiqueta visual.
TYPE_POOLS: dict[str, tuple[str, ...]] = {
    "restaurante": (
        "restaurante", "restaurante-de-alta-categoria", "cafeteria", "bar", "cafe-bar",
        "taberna", "restaurante-bar-en-club-o-sociedad", "restauracion-en-teatro-cine-espectaculo",
        "restauracion-en-recinto-ferial", "quiosco-o-puesto-de-comida-bebida", "chocolateria",
        "heladeria", "horchateria", "comida-para-llevar-take-away", "empresa-local-de-catering",
    ),
    "peluqueria": (
        "peluqueria", "barberia", "salon-centro-de-belleza", "centro-de-estetica-no-medica",
        "centro-de-manicura-pedicura-estetica", "estudio-de-maquillaje-estetica",
        "spa-sauna-bano-turco-no-sanitario", "centro-de-masaje-no-sanitario",
    ),
    "centro-de-estetica": (
        "salon-centro-de-belleza", "centro-de-estetica-no-medica",
        "centro-de-manicura-pedicura-estetica", "estudio-de-maquillaje-estetica",
        "spa-sauna-bano-turco-no-sanitario", "centro-de-masaje-no-sanitario",
    ),
    "campo-de-futbol": ("campo-instalacion-de-futbol",),
    "pista-de-padel": ("club-pistas-de-padel",),
    "centro-deportivo": (
        "gimnasio-centro-deportivo", "instalacion-deportiva", "piscina-de-explotacion-deportiva",
        "escuela-deportiva", "centro-de-perfeccionamiento-deportivo",
        "alquiler-de-material-dentro-de-instalacion-deportiva",
    ),
    "instalacion-municipal": (
        "instalacion-deportiva", "centro-organizador-de-congresos",
        "academia-centro-de-estudios", "academia-de-musica-danza-o-artes",
        "biblioteca-privada-servicio-cultural", "recinto-ferial-feria-de-muestras",
    ),
    "otros": (
        "coworking-oficina-flexible-centro-de-negocios", "estudio-fotografico",
        "academia-de-musica-danza-o-artes", "centro-organizador-de-congresos",
        "estudio-de-diseno-grafico", "estudio-de-interiorismo", "agencia-de-publicidad",
        "sala-de-billar-juegos-de-mesa-recreativos",
    ),
}


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    """Lee un JSONL completo; los artefactos son sintéticos y de tamaño acotado."""

    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _canonical_jsonl(rows: Iterable[dict[str, Any]]) -> bytes:
    return "".join(
        json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"
        for row in rows
    ).encode("utf-8")


def _write_jsonl(path: Path, rows: list[dict[str, Any]]) -> dict[str, Any]:
    payload = _canonical_jsonl(rows)
    path.write_bytes(payload)
    return {"rows": len(rows), "sha256": hashlib.sha256(payload).hexdigest()}


def _stable_id(kind: str, index: int) -> str:
    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{kind}:{index:06d}"))


def _sigmoid(value: float) -> float:
    return 1.0 / (1.0 + math.exp(-value))


def _annotations(
    venues: list[dict[str, Any]], taxonomy: dict[str, Any]
) -> list[dict[str, Any]]:
    type_index = {row["code"]: row for row in taxonomy["types"]}
    rows: list[dict[str, Any]] = []
    for index, venue in enumerate(venues):
        pool = TYPE_POOLS[venue["categoryCode"]]
        type_code = pool[index % len(pool)]
        type_row = type_index.get(type_code)
        if type_row is None:
            raise ValueError(f"RECOMMENDATION_DIVERSE_UNKNOWN_TYPE:{type_code}")
        rows.append(
            {
                "venueId": venue["venueId"],
                "taxonomyVersion": taxonomy["taxonomyVersion"],
                "taxonomyActivationStatus": "candidateOnly",
                "familyCode": type_row["familyCode"],
                "typeCode": type_code,
                "serviceLabels": venue["serviceCodes"],
                "attributeLabels": venue["attributeCodes"],
                "visualAmbienceLabels": [venue["visualStyle"], venue["visualPalette"]],
                "visualEvidenceScope": "declaredAmbienceMetadataOnly",
                "humanTaxonomyReviewRequired": True,
                "productionTrainingAllowed": False,
            }
        )
    return rows


def _entity_allowed(index: int, split: str, total: int) -> bool:
    warm = round(total * 0.70)
    validation = round(total * 0.85)
    return index < ({"train": warm, "validation": validation, "test": total}[split])


def _distance(rng: random.Random, aligned: bool) -> float:
    return round(rng.uniform(250, 5_500 if aligned else 14_000), 2)


def _build_sessions(
    venues: list[dict[str, Any]],
    profiles: list[dict[str, Any]],
    annotations: list[dict[str, Any]],
    rng: random.Random,
) -> list[dict[str, Any]]:
    annotation_by_venue = {row["venueId"]: row for row in annotations}
    venue_index = {row["venueId"]: index for index, row in enumerate(venues)}
    profile_index = {row["profileId"]: index for index, row in enumerate(profiles)}
    sessions: list[dict[str, Any]] = []
    exposure: Counter[str] = Counter()
    split_spec = (
        ("train", 1_500, datetime(2026, 7, 1, tzinfo=UTC), 91),
        ("validation", 500, datetime(2026, 10, 1, tzinfo=UTC), 30),
        ("test", 700, datetime(2026, 11, 1, tzinfo=UTC), 29),
    )
    session_index = 0
    for split, count, start, day_span in split_spec:
        allowed_venues = [
            venue for venue in venues
            if _entity_allowed(venue_index[venue["venueId"]], split, len(venues))
        ]
        allowed_profiles = [
            profile for profile in profiles
            if _entity_allowed(profile_index[profile["profileId"]], split, len(profiles))
        ]
        for local_index in range(count):
            profile = allowed_profiles[(local_index * 17 + rng.randrange(len(allowed_profiles))) % len(allowed_profiles)]
            target = allowed_venues[(local_index * 29 + rng.randrange(len(allowed_venues))) % len(allowed_venues)]
            target_annotation = annotation_by_venue[target["venueId"]]
            desired_service = rng.choice(target["serviceCodes"])
            desired_attribute = rng.choice(target["attributeCodes"])
            desired_ambience = rng.choice([target["visualStyle"], target["visualPalette"]])

            distractors = [venue for venue in allowed_venues if venue["venueId"] != target["venueId"]]
            rng.shuffle(distractors)
            # Incluye hard negatives de la misma familia cuando existen.
            same_family = [
                venue for venue in distractors
                if annotation_by_venue[venue["venueId"]]["familyCode"] == target_annotation["familyCode"]
            ]
            candidates = [target] + same_family[:2]
            candidates.extend(venue for venue in distractors if venue not in candidates)
            candidates = candidates[:8]
            rng.shuffle(candidates)

            occurred_at = start + timedelta(
                days=rng.randrange(day_span + 1),
                hours=rng.choice([8, 9, 10, 12, 14, 17, 18, 19, 20]),
                minutes=rng.randrange(60),
            )
            rows: list[dict[str, Any]] = []
            utilities: list[float] = []
            for venue in candidates:
                annotation = annotation_by_venue[venue["venueId"]]
                type_affinity = 1.0 if annotation["typeCode"] == target_annotation["typeCode"] else 0.0
                family_affinity = 1.0 if annotation["familyCode"] == target_annotation["familyCode"] else 0.0
                service_affinity = 1.0 if desired_service in venue["serviceCodes"] else 0.0
                attribute_affinity = 1.0 if desired_attribute in venue["attributeCodes"] else 0.0
                visual_affinity = float(desired_ambience in {venue["visualStyle"], venue["visualPalette"]})
                aligned = venue["venueId"] == target["venueId"]
                # Un 22 % de sesiones crea el caso alineado, poco expuesto y con pocas plazas.
                scarce_case = session_index % 9 in {0, 1}
                availability = rng.uniform(0.08, 0.25) if aligned and scarce_case else rng.uniform(0.35, 0.96)
                maximum_exposure = max(exposure.values(), default=1)
                normalized_exposure = exposure[venue["venueId"]] / maximum_exposure
                distance = _distance(rng, aligned)
                price_fit = max(0.0, 1.0 - abs(venue["priceTier"] - (1 + session_index % 3)) / 2.0)
                hour_affinity = 1.0 if occurred_at.hour in {9, 10, 17, 18, 19} else 0.55
                content = min(1.0, 0.34 * type_affinity + 0.20 * family_affinity + 0.26 * service_affinity + 0.12 * attribute_affinity + 0.08 * visual_affinity)
                low_exposure = content * (1.0 - normalized_exposure)
                scarcity = content * (1.0 - availability)
                capacity_opportunity = scarcity * (1.0 - normalized_exposure)
                utility = (
                    1.50 * content + 0.50 * service_affinity + 0.24 * visual_affinity
                    + 0.28 * math.exp(-distance / 6_000.0) + 0.16 * price_fit
                    + 0.16 * hour_affinity + 0.20 * capacity_opportunity
                    + 0.08 * venue["qualityScore"]
                )
                utilities.append(utility)
                rows.append(
                    {
                        "venueId": venue["venueId"],
                        "familyCode": annotation["familyCode"],
                        "typeCode": annotation["typeCode"],
                        "eligible": True,
                        "capacityAvailable": availability > 0.0,
                        "isNewVenue": venue_index[venue["venueId"]] >= 70,
                        "features": {
                            "taxonomyTypeAffinity": round(type_affinity, 6),
                            "taxonomyFamilyAffinity": round(family_affinity, 6),
                            "serviceAffinity": round(service_affinity, 6),
                            "attributeAffinity": round(attribute_affinity, 6),
                            "visualAmbienceAffinity": round(visual_affinity, 6),
                            "contentAffinity": round(content, 6),
                            "availabilityRatio": round(availability, 6),
                            "alignedScarcityOpportunity": round(scarcity, 6),
                            "qualityScore": venue["qualityScore"],
                            "proximity": round(math.exp(-distance / 6_000.0), 6),
                            "priceFit": round(price_fit, 6),
                            "lowExposureAffinity": round(low_exposure, 6),
                            "capacityOpportunity": round(capacity_opportunity, 6),
                            "commonHourAffinity": round(hour_affinity, 6),
                            "isNewVenue": float(venue_index[venue["venueId"]] >= 70),
                        },
                        "labels": {"relevance": 0, "clicked": 0, "bookingCompleted": 0},
                    }
                )

            target_position = next(index for index, venue in enumerate(candidates) if venue["venueId"] == target["venueId"])
            # Desarrollo conserva 12 % de weak labels ambiguas; el test, revisado
            # como compatibilidad sintética, conserva 6 %. La diferencia de calidad
            # queda declarada y evita fingir que el outcome ruidoso es ground truth.
            observed_position = target_position
            ambiguous = (
                session_index % 50 in {3, 21, 37}
                if split == "test"
                else session_index % 25 in {3, 11, 19}
            )
            if ambiguous:
                alternatives = sorted(
                    (index for index in range(len(rows)) if index != target_position),
                    key=lambda index: (-utilities[index], rows[index]["venueId"]),
                )
                observed_position = alternatives[0]
            observed = rows[observed_position]
            observed["labels"]["clicked"] = 1
            observed["labels"]["relevance"] = 2
            booking_probability = _sigmoid(utilities[observed_position] - 1.15)
            if rng.random() < booking_probability:
                observed["labels"]["bookingCompleted"] = 1
                observed["labels"]["relevance"] = 3
            for row in rows:
                exposure[row["venueId"]] += 1
            sessions.append(
                {
                    "sessionId": _stable_id("session", session_index),
                    "split": split,
                    "occurredAt": occurred_at.isoformat().replace("+00:00", "Z"),
                    "outcomeObservedAt": (occurred_at + timedelta(hours=24)).isoformat().replace("+00:00", "Z"),
                    "profileId": profile["profileId"],
                    "completeCandidateSet": True,
                    "intent": {
                        "familyCode": target_annotation["familyCode"],
                        "typeCode": target_annotation["typeCode"],
                        "serviceCode": desired_service,
                        "attributeCode": desired_attribute,
                        "visualAmbienceLabel": desired_ambience,
                    },
                    "ambiguousObservedChoice": observed_position != target_position,
                    "candidates": rows,
                }
            )
            session_index += 1
    return sessions


def generate_diverse_dataset(
    source_root: Path, taxonomy_path: Path, output_root: Path, seed: int = SEED
) -> dict[str, Any]:
    """Genera sidecar, sesiones y manifiesto reproducibles sin tocar activos visuales."""

    output_root.mkdir(parents=True, exist_ok=True)
    venues_path = source_root / "venues.jsonl"
    profiles_path = source_root / "profiles.jsonl"
    venues = _read_jsonl(venues_path)
    profiles = _read_jsonl(profiles_path)
    taxonomy_bytes = taxonomy_path.read_bytes()
    taxonomy = json.loads(taxonomy_bytes)
    if len(venues) != 100 or len(profiles) != 40 or taxonomy.get("activationStatus") != "candidateOnly":
        raise ValueError("RECOMMENDATION_DIVERSE_SOURCE_INVALID")
    rng = random.Random(seed)
    annotations = _annotations(venues, taxonomy)
    sessions = _build_sessions(venues, profiles, annotations, rng)
    development_sessions = [row for row in sessions if row["split"] != "test"]
    test_sessions = [row for row in sessions if row["split"] == "test"]
    artifacts = {
        "venue-labels.jsonl": _write_jsonl(output_root / "venue-labels.jsonl", annotations),
        "development-sessions.jsonl": _write_jsonl(
            output_root / "development-sessions.jsonl", development_sessions
        ),
        "test-sessions.sealed.jsonl": _write_jsonl(
            output_root / "test-sessions.sealed.jsonl", test_sessions
        ),
    }
    family_counts = Counter(row["familyCode"] for row in annotations)
    type_counts = Counter(row["typeCode"] for row in annotations)
    split_counts = Counter(row["split"] for row in sessions)
    ambiguous_counts = Counter(row["split"] for row in sessions if row["ambiguousObservedChoice"])
    manifest = {
        "schemaVersion": 2,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "seed": seed,
        "synthetic": True,
        "productionEvidence": False,
        "promotionAllowed": False,
        "sourceDatasetVersion": "synthetic-marketplace-v1",
        "sourceVenueSha256": hashlib.sha256(venues_path.read_bytes()).hexdigest(),
        "sourceProfileSha256": hashlib.sha256(profiles_path.read_bytes()).hexdigest(),
        "taxonomyVersion": taxonomy["taxonomyVersion"],
        "taxonomySha256": hashlib.sha256(taxonomy_bytes).hexdigest(),
        "taxonomyActivationStatus": "candidateOnly",
        "counts": {
            "venues": len(annotations), "profiles": len(profiles), "sessions": len(sessions),
            "candidates": sum(len(row["candidates"]) for row in sessions),
            "families": len(family_counts), "types": len(type_counts),
        },
        "familyCoverage": dict(sorted(family_counts.items())),
        "typeCoverage": dict(sorted(type_counts.items())),
        "splitCounts": dict(sorted(split_counts.items())),
        "ambiguousObservedChoices": dict(sorted(ambiguous_counts.items())),
        "foldStrategy": "five-fold-rolling-origin-on-train-and-validation",
        "testPolicy": "temporal-independent-open-once-after-model-selection",
        "labelPolicy": {
            "development": "weakObservedChoiceWithTwelvePercentDeclaredAmbiguity",
            "test": "adjudicatedCompatibilityWithSixPercentDeclaredAmbiguity",
            "sameFeatureContract": True,
        },
        "visualPolicy": {
            "newImagesGenerated": 0,
            "rawPixelsUsedForRecommendationTraining": False,
            "allowedSignal": "declaredAmbienceMetadataOnly",
        },
        "artifacts": artifacts,
        "limitations": [
            "Las etiquetas de taxonomía son candidatas y no categorías públicas aprobadas.",
            "La evidencia es sintética y no demuestra conversión ni causalidad productiva.",
            "El ambiente procede de metadatos declarados; no se infieren propiedades sensibles desde imágenes.",
        ],
    }
    (output_root / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--taxonomy", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=SEED)
    args = parser.parse_args()
    report = generate_diverse_dataset(args.source_root, args.taxonomy, args.output_root, args.seed)
    print(json.dumps(report["counts"], ensure_ascii=False))


if __name__ == "__main__":
    run()
