"""Generador reproducible de datos sintéticos para evaluación offline del recomendador.

El módulo crea locales ficticios, perfiles pseudónimos y sesiones de ranking con
cortes temporales y cohortes cold-start. Los datos nunca constituyen evidencia de
producción ni autorizan una promoción. Las especificaciones visuales se exportan
como prompts, pero se bloquean para entrenamiento hasta materialización, revisión
humana y registro de procedencia de cada imagen.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Iterable
from uuid import NAMESPACE_URL, uuid5


DATASET_VERSION = "synthetic-marketplace-v1"
DEFAULT_SEED = 1729
GENERATED_AT = "2026-07-01T00:00:00Z"

CATEGORY_DEFINITIONS = (
    {
        "code": "restaurante",
        "nameEs": "restaurante",
        "nameEn": "restaurant",
        "services": ("tableReservation", "tastingMenu", "groupDining", "brunchService"),
        "visualSubject": "sala de restaurante con mesas preparadas y cocina no visible",
    },
    {
        "code": "peluqueria",
        "nameEs": "peluquería",
        "nameEn": "hair salon",
        "services": ("hairCutService", "hairColorService", "hairStylingService", "hairTreatmentService"),
        "visualSubject": "salón de peluquería profesional",
    },
    {
        "code": "campo-de-futbol",
        "nameEs": "campo de fútbol",
        "nameEn": "football pitch",
        "services": ("footballMatch", "footballTraining", "youthTraining", "pitchRental"),
        "visualSubject": "campo de fútbol reservable con césped, porterías y gradas pequeñas",
    },
    {
        "code": "pista-de-padel",
        "nameEs": "pista de pádel",
        "nameEn": "padel court",
        "services": ("padelCourtRental", "padelLesson", "equipmentRental", "padelTournament"),
        "visualSubject": "pista de pádel profesional con cerramiento de cristal",
    },
    {
        "code": "instalacion-municipal",
        "nameEs": "instalación municipal",
        "nameEn": "municipal facility",
        "services": ("communityRoom", "auditoriumRental", "workshopRoom", "multipurposeCourt"),
        "visualSubject": "instalación municipal polivalente y accesible con espacios reservables",
    },
    {
        "code": "centro-deportivo",
        "nameEs": "centro deportivo",
        "nameEn": "sports center",
        "services": ("fitnessClass", "indoorPool", "sportsHall", "personalTraining"),
        "visualSubject": "centro deportivo contemporáneo con zona de entrenamiento reservable",
    },
    {
        "code": "centro-de-estetica",
        "nameEs": "centro de estética",
        "nameEn": "beauty center",
        "services": ("skinCareService", "bodyTreatmentService", "nailService", "makeupService"),
        "visualSubject": "centro de estética profesional con cabinas de tratamiento",
    },
    {
        "code": "otros",
        "nameEs": "otro espacio reservable",
        "nameEn": "other bookable venue",
        "services": ("coworkingDesk", "photographyStudio", "rehearsalRoom", "meetingRoom"),
        "visualSubject": "estudio creativo polivalente con zonas reservables de trabajo y reunión",
    },
)
SERVICES = tuple(service for category in CATEGORY_DEFINITIONS for service in category["services"])
ATTRIBUTES = (
    "naturalLight",
    "privateCabin",
    "multilingualService",
    "stepFreeAccess",
    "quietAtmosphere",
    "sustainableProducts",
    "familyFriendly",
    "lateAppointments",
)
CITIES = (
    ("santiago", "Santiago de Compostela", 42.8782, -8.5448),
    ("a-coruna", "A Coruña", 43.3623, -8.4115),
    ("vigo", "Vigo", 42.2406, -8.7207),
    ("pontevedra", "Pontevedra", 42.4336, -8.6481),
    ("lugo", "Lugo", 43.0097, -7.5568),
    ("ourense", "Ourense", 42.3358, -7.8639),
    ("ferrol", "Ferrol", 43.4896, -8.2193),
    ("ames", "Ames", 42.9048, -8.6553),
    ("teo", "Teo", 42.7969, -8.5480),
    ("oleiros", "Oleiros", 43.3336, -8.3138),
)
NAME_PREFIXES = ("Bruma", "Lume", "Néboda", "Savia", "Orballo", "Marea", "Xesta", "Lúa", "Silveira", "Faro")
NAME_SUFFIXES = ("Atelier", "Estudio", "Espazo", "Taller", "Casa", "Lab", "Ritual", "Salón", "Colectivo", "Boutique")
VISUAL_STYLES = ("minimalista", "biofílico", "art déco", "costero", "industrial cálido")
PALETTES = ("arena y terracota", "salvia y marfil", "azul atlántico y roble", "grafito y cobre", "rosa arcilla y lino")


@dataclass(frozen=True)
class GenerationResult:
    """Rutas y recuentos de una generación completada."""

    output_dir: Path
    venue_count: int
    profile_count: int
    session_count: int
    candidate_count: int


def _stable_id(kind: str, index: int) -> str:
    """Devuelve un UUID estable sin codificar identidad real ni PII."""

    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{kind}:{index:05d}"))


def _json_line(value: dict[str, Any]) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"


def _write_jsonl(path: Path, rows: Iterable[dict[str, Any]]) -> tuple[int, str]:
    """Escribe JSONL canónico y devuelve número de filas y SHA-256."""

    payload = "".join(_json_line(row) for row in rows).encode("utf-8")
    path.write_bytes(payload)
    return payload.count(b"\n"), hashlib.sha256(payload).hexdigest()


def _venue_cohort(index: int) -> str:
    if index < 70:
        return "warm"
    if index < 85:
        return "validationCold"
    return "testCold"


def _profile_cohort(index: int) -> str:
    if index < 28:
        return "warm"
    if index < 34:
        return "validationCold"
    return "testCold"


def _build_venues(rng: random.Random) -> list[dict[str, Any]]:
    venues: list[dict[str, Any]] = []
    for index in range(100):
        city_code, city_name, base_lat, base_lon = CITIES[index % len(CITIES)]
        # Los 17 primeros activos ya materializados son peluquerías. Desde el
        # índice 17 se rota el catálogo canónico completo, incluyendo nuevas
        # apariciones de peluquería en las cohortes cold-start.
        category = CATEGORY_DEFINITIONS[1] if index < 17 else CATEGORY_DEFINITIONS[(index - 17) % 8]
        pool = category["services"]
        services = sorted(rng.sample(pool, k=rng.randint(2, 4)))
        attributes = sorted(rng.sample(ATTRIBUTES, k=3))
        name = f"{NAME_PREFIXES[index % 10]} {NAME_SUFFIXES[index // 10]} {index + 1:03d}"
        venue_id = _stable_id("venue", index)
        style = VISUAL_STYLES[index % len(VISUAL_STYLES)]
        palette = PALETTES[(index // len(VISUAL_STYLES)) % len(PALETTES)]
        venues.append(
            {
                "venueId": venue_id,
                "synthetic": True,
                "entityCohort": _venue_cohort(index),
                "categoryCode": category["code"],
                "name": name,
                "descriptions": {
                    "es": f"{category['nameEs'].capitalize()} ficticio de {city_name} especializado en {', '.join(services)}; ambiente {style} y atención con {', '.join(attributes)}.",
                    "en": f"Fictional {category['nameEn']} in {city_name} focused on {', '.join(services)}, with a {style} atmosphere and {', '.join(attributes)}.",
                },
                "location": {
                    "countryCode": "ES",
                    "regionCode": "GA",
                    "cityCode": city_code,
                    "cityName": city_name,
                    "approximateZoneCode": f"{city_code}-z{(index // 10) + 1}",
                    "latitude": round(base_lat + rng.uniform(-0.018, 0.018), 6),
                    "longitude": round(base_lon + rng.uniform(-0.018, 0.018), 6),
                    "precision": "syntheticApproximate",
                },
                "serviceCodes": services,
                "attributeCodes": attributes,
                "qualityScore": round(rng.uniform(0.55, 0.94), 4),
                "priceTier": rng.choice([1, 2, 2, 3]),
                "imagePromptId": _stable_id("image-prompt", index),
                "imageStatus": "promptOnlyNotTrainingEligible",
                "visualStyle": style,
                "visualPalette": palette,
            }
        )
    return venues


def _build_profiles(rng: random.Random) -> list[dict[str, Any]]:
    profiles: list[dict[str, Any]] = []
    for index in range(40):
        consent = index % 7 != 0
        preferred_services = rng.sample(SERVICES, k=3) if consent else []
        preferred_attributes = rng.sample(ATTRIBUTES, k=2) if consent else []
        profiles.append(
            {
                "profileId": _stable_id("profile", index),
                "synthetic": True,
                "entityCohort": _profile_cohort(index),
                "locale": "es-ES" if index % 5 else "en-GB",
                "persistentPersonalizationConsent": consent,
                "permittedPreferences": {
                    "serviceWeights": {
                        code: round(0.62 + 0.17 * rank + rng.uniform(-0.03, 0.03), 4)
                        for rank, code in enumerate(preferred_services)
                    },
                    "attributeWeights": {
                        code: round(0.7 + 0.18 * rank + rng.uniform(-0.03, 0.03), 4)
                        for rank, code in enumerate(preferred_attributes)
                    },
                    "maximumDistanceKm": rng.choice([3, 5, 8, 12]) if consent else None,
                },
                "privacy": {
                    "containsDirectIdentifiers": False,
                    "containsSensitiveAttributes": False,
                    "revocationApplied": True,
                    "purpose": "offlineSyntheticRecommendationEvaluation",
                },
            }
        )
    return profiles


def _eligible_entities(split: str, entities: list[dict[str, Any]]) -> list[dict[str, Any]]:
    allowed = {
        "train": {"warm"},
        "validation": {"warm", "validationCold"},
        "test": {"warm", "validationCold", "testCold"},
    }[split]
    return [entity for entity in entities if entity["entityCohort"] in allowed]


def _sample_without_replacement(
    rng: random.Random, values: list[tuple[dict[str, Any], float]], count: int
) -> list[dict[str, Any]]:
    """Muestrea candidatos con pesos, sin repetir y sin usar su futura etiqueta."""

    pool = list(values)
    selected: list[dict[str, Any]] = []
    for _ in range(min(count, len(pool))):
        total = sum(weight for _, weight in pool)
        threshold = rng.random() * total
        cursor = 0.0
        chosen = len(pool) - 1
        for position, (_, weight) in enumerate(pool):
            cursor += weight
            if cursor >= threshold:
                chosen = position
                break
        selected.append(pool.pop(chosen)[0])
    return selected


def _sigmoid(value: float) -> float:
    return 1.0 / (1.0 + math.exp(-value))


def _build_sessions(
    rng: random.Random,
    venues: list[dict[str, Any]],
    profiles: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    sessions: list[dict[str, Any]] = []
    split_spec = (
        ("train", 1400, datetime(2026, 1, 1, tzinfo=UTC), 119),
        ("validation", 400, datetime(2026, 5, 1, tzinfo=UTC), 30),
        ("test", 600, datetime(2026, 6, 1, tzinfo=UTC), 29),
    )
    session_index = 0
    for split, count, start, day_span in split_spec:
        eligible_profiles = _eligible_entities(split, profiles)
        eligible_venues = _eligible_entities(split, venues)
        for local_index in range(count):
            profile = eligible_profiles[(local_index * 13 + rng.randrange(len(eligible_profiles))) % len(eligible_profiles)]
            preferences = profile["permittedPreferences"]["serviceWeights"]
            intent = max(preferences, key=preferences.get) if preferences else rng.choice(SERVICES)
            matching = [venue for venue in eligible_venues if intent in venue["serviceCodes"]]
            weighted = [(venue, 1.0 + venue["qualityScore"] * 0.35) for venue in matching]
            if len(weighted) < 8:
                weighted.extend((venue, 0.25) for venue in eligible_venues if venue not in matching)
            candidates = _sample_without_replacement(rng, weighted, 8)
            candidate_rows: list[dict[str, Any]] = []
            utilities: list[float] = []
            preferred_attrs = profile["permittedPreferences"]["attributeWeights"]
            for venue in candidates:
                service_match = 1.0 if intent in venue["serviceCodes"] else 0.1
                attribute_match = (
                    sum(preferred_attrs.get(code, 0.0) for code in venue["attributeCodes"])
                    / max(1, len(preferred_attrs))
                )
                distance = round(rng.uniform(300, 14_000), 2)
                availability = round(rng.uniform(0.25, 1.0), 4)
                context_match = round(min(1.0, service_match * 0.75 + attribute_match * 0.25), 4)
                utility = (
                    1.35 * context_match
                    + 0.65 * availability
                    + 0.45 * venue["qualityScore"]
                    - distance / 16_000
                    + rng.gauss(0, 0.42)
                )
                utilities.append(utility)
                candidate_rows.append(
                    {
                        "venueId": venue["venueId"],
                        "categoryCode": venue["categoryCode"],
                        "isNewVenue": venue["entityCohort"] != "warm",
                        "eligible": True,
                        "capacityAvailable": True,
                        "features": {
                            "contentAffinity": context_match,
                            "availabilityRatio": availability,
                            "qualityScore": venue["qualityScore"],
                            "distanceMeters": distance,
                            "priceTier": venue["priceTier"],
                        },
                        "labels": {"clicked": 0, "bookingCompleted": 0, "attendanceObserved": 0, "relevance": 0},
                    }
                )
            temperature = 0.7
            probabilities = [math.exp(value / temperature) for value in utilities]
            chosen = rng.choices(range(len(candidate_rows)), weights=probabilities, k=1)[0]
            click_probability = _sigmoid(utilities[chosen] - 0.55)
            clicked = rng.random() < click_probability
            booking_probability = _sigmoid(utilities[chosen] - 1.35) if clicked else 0.0
            booked = rng.random() < booking_probability
            attended = booked and rng.random() < 0.88
            if clicked:
                labels = candidate_rows[chosen]["labels"]
                labels["clicked"] = 1
                labels["relevance"] = 1
                if booked:
                    labels["bookingCompleted"] = 1
                    labels["relevance"] = 2
                if attended:
                    labels["attendanceObserved"] = 1
                    labels["relevance"] = 3
            occurred_at = start + timedelta(
                days=rng.randrange(day_span + 1), minutes=rng.randrange(24 * 60)
            )
            sessions.append(
                {
                    "sessionId": _stable_id("session", session_index),
                    "split": split,
                    "occurredAt": occurred_at.isoformat().replace("+00:00", "Z"),
                    "outcomeObservedAt": (occurred_at + timedelta(hours=24)).isoformat().replace("+00:00", "Z"),
                    "profileId": profile["profileId"],
                    "profileCohort": profile["entityCohort"],
                    "locale": profile["locale"],
                    "intentCode": intent,
                    "completeCandidateSet": True,
                    "candidates": candidate_rows,
                }
            )
            session_index += 1
    return sessions


def _build_image_prompts(venues: list[dict[str, Any]]) -> list[dict[str, Any]]:
    prompts: list[dict[str, Any]] = []
    for index, venue in enumerate(venues):
        city = venue["location"]["cityName"]
        category = next(item for item in CATEGORY_DEFINITIONS if item["code"] == venue["categoryCode"])
        prompts.append(
            {
                "imagePromptId": venue["imagePromptId"],
                "venueId": venue["venueId"],
                "promptVersion": "venue-interior-v1",
                "prompt": (
                    f"Fotografía editorial horizontal 4:3 de un {category['visualSubject']} totalmente ficticio en {city}, "
                    f"diseño {venue['visualStyle']}, paleta {venue['visualPalette']}, distribución espacial única número {index + 1:03d}, "
                    "luz natural realista, mobiliario profesional, sin personas, sin texto, sin logotipos, sin marcas de agua."
                ),
                "status": "promptOnly",
                "synthetic": True,
                "materialized": False,
                "humanReviewRequired": True,
                "trainingAllowed": False,
                "objectKey": None,
                "provenance": None,
            }
        )
    return prompts


def generate_dataset(output_dir: Path, seed: int = DEFAULT_SEED) -> GenerationResult:
    """Genera el dataset completo y su manifiesto reproducible.

    La carpeta de salida se crea si no existe. Solo sobrescribe los cinco
    artefactos conocidos del dataset y no elimina otros archivos del directorio.
    """

    output_dir.mkdir(parents=True, exist_ok=True)
    rng = random.Random(seed)
    venues = _build_venues(rng)
    profiles = _build_profiles(rng)
    sessions = _build_sessions(rng, venues, profiles)
    prompts = _build_image_prompts(venues)
    artifacts: dict[str, dict[str, Any]] = {}
    for name, rows in (
        ("venues.jsonl", venues),
        ("profiles.jsonl", profiles),
        ("ranking-sessions.jsonl", sessions),
        ("image-prompts.jsonl", prompts),
    ):
        row_count, sha256 = _write_jsonl(output_dir / name, rows)
        artifacts[name] = {"rows": row_count, "sha256": sha256}

    candidate_count = sum(len(session["candidates"]) for session in sessions)
    manifest = {
        "schemaVersion": 1,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "seed": seed,
        "synthetic": True,
        "productionEvidence": False,
        "promotionReviewAllowed": False,
        "containsPersonalData": False,
        "consentRevocationsApplied": True,
        "counts": {
            "venues": len(venues),
            "profiles": len(profiles),
            "rankingSessions": len(sessions),
            "candidates": candidate_count,
            "imagePrompts": len(prompts),
            "materializedImages": 0,
        },
        "categoryCoverage": {
            category["code"]: {
                "venues": sum(venue["categoryCode"] == category["code"] for venue in venues),
                "warm": sum(
                    venue["categoryCode"] == category["code"] and venue["entityCohort"] == "warm"
                    for venue in venues
                ),
                "validationCold": sum(
                    venue["categoryCode"] == category["code"]
                    and venue["entityCohort"] == "validationCold"
                    for venue in venues
                ),
                "testCold": sum(
                    venue["categoryCode"] == category["code"] and venue["entityCohort"] == "testCold"
                    for venue in venues
                ),
            }
            for category in CATEGORY_DEFINITIONS
        },
        "splits": {
            "train": {"sessions": 1400, "dateRange": ["2026-01-01", "2026-04-30"]},
            "validation": {"sessions": 400, "dateRange": ["2026-05-01", "2026-05-31"]},
            "test": {"sessions": 600, "dateRange": ["2026-06-01", "2026-06-30"]},
        },
        "coldStartPolicy": {
            "validationColdEntitiesAbsentFromTrain": True,
            "testColdEntitiesAbsentFromTrainAndValidation": True,
        },
        "visualAssets": {
            "status": "promptOnly",
            "trainingAllowed": False,
            "activationRequirements": [
                "materializeEachAssetOutsideGit",
                "recordObjectKeySha256AndGeneratorProvenance",
                "humanReviewForDuplicatedArtifactsAndBrandText",
                "passClipVisualEvaluationGate",
            ],
        },
        "limitations": [
            "Synthetic behavior cannot demonstrate production accuracy or causality.",
            "Metrics must be reported separately for warm and cold-start cohorts.",
            "Visual prompts are specifications, not image evidence or training assets.",
            "Generated outcomes include noise by design and must not be used as target thresholds.",
        ],
        "artifacts": artifacts,
    }
    (output_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
        encoding="utf-8",
    )
    return GenerationResult(output_dir, len(venues), len(profiles), len(sessions), candidate_count)


def run() -> None:
    """Punto de entrada CLI para regenerar el dataset versionado."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="Carpeta de salida del dataset")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED)
    args = parser.parse_args()
    result = generate_dataset(args.output, args.seed)
    print(
        json.dumps(
            {
                "output": str(result.output_dir),
                "venues": result.venue_count,
                "profiles": result.profile_count,
                "sessions": result.session_count,
                "candidates": result.candidate_count,
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    run()
