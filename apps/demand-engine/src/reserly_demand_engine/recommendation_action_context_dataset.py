"""Dataset temporal v5 para recomendación por acciones, ubicación y huecos.

Genera sesiones sintéticas reproducibles a partir de los 100 locales y 40
perfiles ya versionados. Todas las features representan información disponible
antes de mostrar el ranking. La posición geográfica de sesión es efímera y no
se incorpora como identificador: solo se materializan distancia y ajuste al
radio consentido.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
import uuid
from collections import Counter
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Iterable

from .recommendation_diverse_dataset import _annotations, _read_jsonl


DATASET_VERSION = "synthetic-marketplace-action-context-v5"
SEED = 5279
GENERATED_AT = "2026-09-02T12:00:00Z"
ACTION_TYPES = (
    "search", "category_filter", "venue_view", "service_view", "map_open",
    "availability_check", "save", "compare", "booking_start", "booking_complete",
)
FEATURE_NAMES = [
    "recentActionTypeAffinity", "recentActionFamilyAffinity", "recentActionServiceAffinity",
    "searchQueryAffinity", "actionSequenceMomentum", "persistentPreferenceAffinity",
    "taxonomyTypeAffinity", "taxonomyFamilyAffinity", "serviceAffinity", "attributeAffinity",
    "contentAffinity", "currentLocationProximity", "withinPreferredRadius",
    "distanceDecayKm", "requestedDayAffinity", "requestedHourAffinity",
    "availabilityRatio", "remainingSlotUrgency", "alignedScarcityOpportunity",
    "lowExposureAffinity", "qualityScore", "priceFit", "isNewVenue",
]


def _stable_id(kind: str, index: int, dataset_version: str = DATASET_VERSION) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"reserly:{dataset_version}:{kind}:{index}"))


def _write_jsonl(path: Path, rows: Iterable[dict[str, Any]]) -> dict[str, Any]:
    payload = b"".join(
        (json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
        for row in rows
    )
    path.write_bytes(payload)
    return {"path": path.name, "sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload)}


def _haversine_km(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Distancia de gran círculo; evita usar coordenadas crudas como feature."""

    lat1, lon1, lat2, lon2 = map(math.radians, (a[0], a[1], b[0], b[1]))
    dlat, dlon = lat2 - lat1, lon2 - lon1
    value = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 6371.0088 * 2 * math.asin(math.sqrt(value))


def _offset_location(latitude: float, longitude: float, distance_km: float, bearing: float) -> tuple[float, float]:
    angle = bearing * math.pi / 180.0
    return (
        latitude + distance_km * math.cos(angle) / 111.32,
        longitude + distance_km * math.sin(angle) / (111.32 * max(0.25, math.cos(math.radians(latitude)))),
    )


def _action_history(
    rng: random.Random, annotation: dict[str, Any], service: str, occurred_at: datetime, session_index: int,
) -> list[dict[str, Any]]:
    """Crea un recorrido point-in-time diverso con un posible cambio de intención."""

    length = 3 + session_index % 6
    actions: list[dict[str, Any]] = []
    for index in range(length):
        action_type = ACTION_TYPES[(session_index * 3 + index * 7) % len(ACTION_TYPES)]
        aligned = index >= max(0, length - 4) or rng.random() > 0.22
        actions.append({
            "actionType": action_type,
            "occurredAt": (occurred_at - timedelta(minutes=(length - index) * (2 + session_index % 5))).isoformat().replace("+00:00", "Z"),
            "familyCode": annotation["familyCode"] if aligned else "previous-intent",
            "typeCode": annotation["typeCode"] if aligned else "previous-intent",
            "serviceCode": service if aligned else "previous-service",
            "queryToken": annotation["typeCode"] if action_type == "search" and aligned else None,
        })
    return actions


def _recent_affinity(actions: list[dict[str, Any]], field: str, value: str) -> float:
    weights = [0.42, 0.26, 0.17, 0.10, 0.05]
    recent = list(reversed(actions[-len(weights):]))
    return min(1.0, sum(weight for action, weight in zip(recent, weights, strict=False) if action.get(field) == value))


def _build_sessions(
    venues: list[dict[str, Any]], profiles: list[dict[str, Any]], annotations: list[dict[str, Any]], rng: random.Random,
    dataset_version: str = DATASET_VERSION, adjudicate_utility: bool = False,
    candidate_temporal_affinity: bool = False,
) -> list[dict[str, Any]]:
    annotation_by_venue = {row["venueId"]: row for row in annotations}
    venue_index = {row["venueId"]: index for index, row in enumerate(venues)}
    exposure: Counter[str] = Counter()
    sessions: list[dict[str, Any]] = []
    split_spec = (
        ("train", 1_800, datetime(2026, 1, 1, tzinfo=UTC), 119),
        ("validation", 600, datetime(2026, 5, 1, tzinfo=UTC), 60),
        ("test", 800, datetime(2026, 7, 1, tzinfo=UTC), 61),
    )
    session_index = 0
    for split, count, start, day_span in split_spec:
        venue_limit = {"train": 70, "validation": 85, "test": 100}[split]
        profile_limit = {"train": 28, "validation": 34, "test": 40}[split]
        allowed = venues[:venue_limit]
        for local_index in range(count):
            profile = profiles[(local_index * 11 + rng.randrange(profile_limit)) % profile_limit]
            target = allowed[(local_index * 31 + rng.randrange(len(allowed))) % len(allowed)]
            target_annotation = annotation_by_venue[target["venueId"]]
            requested_hour = (8, 9, 10, 12, 14, 16, 17, 18, 19, 20)[session_index % 10]
            occurred_at = start + timedelta(days=rng.randrange(day_span + 1), hours=requested_hour, minutes=rng.randrange(60))
            desired_service = target["serviceCodes"][session_index % len(target["serviceCodes"])]
            desired_attribute = target["attributeCodes"][session_index % len(target["attributeCodes"])]
            actions = _action_history(rng, target_annotation, desired_service, occurred_at, session_index)

            # La ubicación cambia por sesión (viaje, trabajo, domicilio) y queda cerca
            # del objetivo entre 0,15 y 6 km. Los candidatos se eligen por geografía,
            # familia y hard negatives para que proximidad no sea una respuesta trivial.
            target_location = (target["location"]["latitude"], target["location"]["longitude"])
            user_location = _offset_location(target_location[0], target_location[1], rng.uniform(.15, 6.0), rng.uniform(0, 360))
            distances = {
                venue["venueId"]: _haversine_km(user_location, (venue["location"]["latitude"], venue["location"]["longitude"]))
                for venue in allowed
            }
            same_family = [
                venue for venue in allowed if venue["venueId"] != target["venueId"]
                and annotation_by_venue[venue["venueId"]]["familyCode"] == target_annotation["familyCode"]
            ]
            same_family.sort(key=lambda venue: distances[venue["venueId"]])
            nearest = sorted(
                (venue for venue in allowed if venue["venueId"] != target["venueId"]),
                key=lambda venue: distances[venue["venueId"]],
            )
            candidates = [target] + same_family[:3]
            candidates.extend(venue for venue in nearest if venue not in candidates)
            candidates = candidates[:8]
            rng.shuffle(candidates)
            preferred_radius = float(profile["permittedPreferences"].get("maximumDistanceKm") or 12.0)
            preferred_radius = max(preferred_radius, distances[target["venueId"]] + .5)
            maximum_exposure = max(exposure.values(), default=1)
            rows: list[dict[str, Any]] = []
            utilities: list[float] = []
            scarce_case = session_index % 5 in {0, 1}
            requested_day = occurred_at.weekday()
            for candidate in candidates:
                annotation = annotation_by_venue[candidate["venueId"]]
                type_affinity = float(annotation["typeCode"] == target_annotation["typeCode"])
                family_affinity = float(annotation["familyCode"] == target_annotation["familyCode"])
                service_affinity = float(desired_service in candidate["serviceCodes"])
                attribute_affinity = float(desired_attribute in candidate["attributeCodes"])
                action_type = _recent_affinity(actions, "typeCode", annotation["typeCode"])
                action_family = _recent_affinity(actions, "familyCode", annotation["familyCode"])
                action_service = _recent_affinity(actions, "serviceCode", desired_service) * service_affinity
                search_affinity = float(any(action.get("queryToken") == annotation["typeCode"] for action in actions))
                momentum = min(1.0, (action_type + action_family + action_service) / 2.1)
                preferences = profile["permittedPreferences"] if profile["persistentPersonalizationConsent"] else {"serviceWeights": {}, "attributeWeights": {}}
                persistent = max(
                    float(preferences.get("serviceWeights", {}).get(desired_service, 0.0)) * service_affinity,
                    float(preferences.get("attributeWeights", {}).get(desired_attribute, 0.0)) * attribute_affinity,
                )
                content = min(1.0, .30 * action_type + .18 * action_family + .18 * service_affinity + .12 * type_affinity + .08 * attribute_affinity + .08 * search_affinity + .06 * persistent)
                distance_km = distances[candidate["venueId"]]
                proximity = math.exp(-distance_km / 5.0)
                within_radius = float(distance_km <= preferred_radius)
                candidate_index = venue_index[candidate["venueId"]]
                if candidate_temporal_affinity:
                    preferred_days = {candidate_index % 7, (candidate_index + 2) % 7, (candidate_index + 4) % 7}
                    preferred_hours = (
                        {8, 9, 10, 12, 14} if candidate_index % 3 == 0
                        else {16, 17, 18, 19, 20} if candidate_index % 3 == 1
                        else {10, 12, 14, 17, 19}
                    )
                    day_affinity = 1.0 if requested_day in preferred_days else .35
                    hour_affinity = 1.0 if requested_hour in preferred_hours else .42
                else:
                    day_affinity = 1.0
                    hour_affinity = 1.0 if requested_hour in {9, 10, 17, 18, 19} else .62
                total_slots = 6 + venue_index[candidate["venueId"]] % 15
                remaining_slots = (1 + session_index % 2) if candidate["venueId"] == target["venueId"] and scarce_case else 3 + (session_index + venue_index[candidate["venueId"]]) % max(3, total_slots - 2)
                remaining_slots = min(total_slots, remaining_slots)
                availability = remaining_slots / total_slots
                urgency = 1.0 - availability
                aligned_scarcity = content * proximity * within_radius * urgency
                normalized_exposure = exposure[candidate["venueId"]] / maximum_exposure
                low_exposure = content * (1.0 - normalized_exposure)
                price_fit = max(0.0, 1.0 - abs(candidate["priceTier"] - (1 + session_index % 3)) / 2.0)
                utility = (
                    1.45 * content + .62 * action_type + .35 * momentum + .58 * proximity
                    + .20 * within_radius + .26 * service_affinity + .16 * persistent
                    + .18 * day_affinity + .22 * hour_affinity + .32 * aligned_scarcity
                    + .12 * low_exposure + .07 * candidate["qualityScore"] + .10 * price_fit
                )
                utilities.append(utility)
                rows.append({
                    "venueId": candidate["venueId"], "familyCode": annotation["familyCode"], "typeCode": annotation["typeCode"],
                    "eligible": remaining_slots > 0, "capacityAvailable": remaining_slots > 0,
                    "availability": {"requestedDay": requested_day, "requestedHour": requested_hour, "totalSlots": total_slots, "remainingSlots": remaining_slots},
                    "distanceKm": round(distance_km, 6),
                    "features": {
                        "recentActionTypeAffinity": round(action_type, 6), "recentActionFamilyAffinity": round(action_family, 6),
                        "recentActionServiceAffinity": round(action_service, 6), "searchQueryAffinity": search_affinity,
                        "actionSequenceMomentum": round(momentum, 6), "persistentPreferenceAffinity": round(persistent, 6),
                        "taxonomyTypeAffinity": type_affinity, "taxonomyFamilyAffinity": family_affinity,
                        "serviceAffinity": service_affinity, "attributeAffinity": attribute_affinity,
                        "contentAffinity": round(content, 6), "currentLocationProximity": round(proximity, 6),
                        "withinPreferredRadius": within_radius, "distanceDecayKm": round(proximity, 6),
                        "requestedDayAffinity": day_affinity, "requestedHourAffinity": hour_affinity,
                        "availabilityRatio": round(availability, 6), "remainingSlotUrgency": round(urgency, 6),
                        "alignedScarcityOpportunity": round(aligned_scarcity, 6), "lowExposureAffinity": round(low_exposure, 6),
                        "qualityScore": candidate["qualityScore"], "priceFit": round(price_fit, 6),
                        "isNewVenue": float(venue_index[candidate["venueId"]] >= 70),
                    },
                    "labels": {"relevance": 0, "clicked": 0, "bookingCompleted": 0},
                })
            # v6 adjudica la relevancia contra la utilidad point-in-time completa.
            # El objetivo inicial únicamente genera el recorrido; no se convierte
            # automáticamente en label si otro candidato satisface mejor contexto,
            # ubicación, horario y capacidad.
            intended_position = (
                max(range(8), key=lambda index: (utilities[index], rows[index]["venueId"]))
                if adjudicate_utility
                else next(index for index, candidate in enumerate(candidates) if candidate["venueId"] == target["venueId"])
            )
            observed_position = intended_position
            ambiguous = session_index % (20 if split != "test" else 34) in {3, 11}
            if ambiguous:
                observed_position = max((index for index in range(8) if index != intended_position), key=lambda index: utilities[index])
            rows[observed_position]["labels"]["relevance"] = 3
            rows[observed_position]["labels"]["clicked"] = 1
            rows[observed_position]["labels"]["bookingCompleted"] = int(session_index % 3 != 0)
            for row in rows:
                exposure[row["venueId"]] += 1
            sessions.append({
                "sessionId": _stable_id("session", session_index, dataset_version), "split": split,
                "occurredAt": occurred_at.isoformat().replace("+00:00", "Z"),
                "outcomeObservedAt": (occurred_at + timedelta(hours=24)).isoformat().replace("+00:00", "Z"),
                "profileId": profile["profileId"], "locationContext": {"source": "ephemeral-session-location", "precision": "synthetic", "preferredRadiusKm": round(preferred_radius, 4)},
                "actionHistory": actions, "requestedSlot": {"weekday": requested_day, "hour": requested_hour},
                "ambiguousObservedChoice": ambiguous, "completeCandidateSet": True, "candidates": rows,
            })
            session_index += 1
    return sessions


def generate_action_context_dataset(
    source_root: Path, taxonomy_path: Path, output_root: Path, seed: int = SEED,
    dataset_version: str = DATASET_VERSION, adjudicate_utility: bool = False,
    candidate_temporal_affinity: bool = False,
) -> dict[str, Any]:
    """Materializa desarrollo y un holdout temporal nuevo, sin abrirlo."""

    output_root.mkdir(parents=True, exist_ok=True)
    venues = _read_jsonl(source_root / "venues.jsonl")
    profiles = _read_jsonl(source_root / "profiles.jsonl")
    taxonomy = json.loads(taxonomy_path.read_text(encoding="utf-8"))
    if len(venues) != 100 or len(profiles) != 40:
        raise ValueError("RECOMMENDATION_ACTION_CONTEXT_SOURCE_INVALID")
    annotations = _annotations(venues, taxonomy)
    sessions = _build_sessions(
        venues, profiles, annotations, random.Random(seed), dataset_version,
        adjudicate_utility, candidate_temporal_affinity,
    )
    development = [row for row in sessions if row["split"] != "test"]
    test = [row for row in sessions if row["split"] == "test"]
    artifacts = {
        "venue-labels.jsonl": _write_jsonl(output_root / "venue-labels.jsonl", annotations),
        "development-sessions.jsonl": _write_jsonl(output_root / "development-sessions.jsonl", development),
        "test-sessions.sealed.jsonl": _write_jsonl(output_root / "test-sessions.sealed.jsonl", test),
    }
    manifest = {
        "schemaVersion": int(dataset_version.rsplit("v", 1)[-1]) if dataset_version.rsplit("v", 1)[-1].isdigit() else (6 if adjudicate_utility else 5),
        "datasetVersion": dataset_version, "generatedAt": GENERATED_AT, "seed": seed,
        "synthetic": True, "productionEvidence": False, "promotionAllowed": False,
        "counts": {"venues": 100, "profiles": 40, "sessions": len(sessions), "candidates": len(sessions) * 8,
                   "actionEvents": sum(len(row["actionHistory"]) for row in sessions),
                   "actionTypes": len({action["actionType"] for row in sessions for action in row["actionHistory"]}),
                   "families": len({row["familyCode"] for row in annotations}), "types": len({row["typeCode"] for row in annotations})},
        "splitCounts": dict(Counter(row["split"] for row in sessions)), "featureNames": FEATURE_NAMES,
        "locationPolicy": {"source": "ephemeralPointInTimeSessionLocation", "rawCoordinatesAsFeatures": False, "distanceMethod": "haversine", "distanceIsRequired": True},
        "scarcityPolicy": {"hardCapacityFilter": True, "formula": "contentAffinity*currentLocationProximity*withinPreferredRadius*remainingSlotUrgency", "globalScarcityBoostForbidden": True},
        "leakagePolicy": {"featuresAvailableBeforeRanking": True, "idsPositionsAndOutcomesExcluded": True},
        "relevancePolicy": "adjudicatedPointInTimeUtilityThenObservedChoiceNoise" if adjudicate_utility else "initialIntentThenObservedChoiceNoise",
        "temporalAffinityPolicy": "candidateSpecificDayAndHour" if candidate_temporal_affinity else "legacySessionLevel",
        "foldStrategy": "five-fold-rolling-origin", "testPolicy": "fresh-temporal-independent-open-once-after-selection",
        "artifacts": artifacts,
        "limitations": ["La evidencia es sintética y no demuestra causalidad o conversión productiva.", "La ubicación es sintética y efímera; producción requiere consentimiento y minimización."],
    }
    (output_root / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--taxonomy", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=SEED)
    parser.add_argument("--dataset-version", default=DATASET_VERSION)
    parser.add_argument("--adjudicate-utility", action="store_true")
    parser.add_argument("--candidate-temporal-affinity", action="store_true")
    args = parser.parse_args()
    print(json.dumps(generate_action_context_dataset(
        args.source_root, args.taxonomy, args.output_root, args.seed, args.dataset_version,
        args.adjudicate_utility, args.candidate_temporal_affinity,
    ), ensure_ascii=False))


if __name__ == "__main__":
    run()
