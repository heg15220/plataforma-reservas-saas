"""Dataset escalado para un ranker conjunto contextual y visual.

Materializa 2.500 perfiles, 3.000 locales y 24.000 sesiones temporales con
doce alternativas. Las imágenes existentes se asignan una sola vez a 1.016
locales; el resto ejercita explícitamente el fallback sin visión.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

import numpy as np

from .full_taxonomy_visual_hybrid_v5 import predict_scores


VERSION = "synthetic-marketplace-joint-scale-v9"
RANDOM_SEED = 90317
USER_COUNT = 2500
VENUE_COUNT = 3000
DEVELOPMENT_SESSIONS = 18000
TEST_SESSIONS = 6000
CANDIDATES_PER_SESSION = 12
ACTION_TYPES = (
    "search", "category_filter", "venue_view", "service_view", "map_open",
    "availability_check", "save", "compare", "booking_start", "booking_complete",
)
FEATURE_NAMES = (
    "recentActionTypeAffinity", "recentActionFamilyAffinity",
    "recentActionServiceAffinity", "searchQueryAffinity", "actionSequenceMomentum",
    "persistentPreferenceAffinity", "taxonomyTypeAffinity", "taxonomyFamilyAffinity",
    "serviceAffinity", "attributeAffinity", "contentAffinity",
    "currentLocationProximity", "withinPreferredRadius", "distanceDecayKm",
    "requestedDayAffinity", "requestedHourAffinity", "availabilityRatio",
    "remainingSlotUrgency", "alignedScarcityOpportunity", "lowExposureAffinity",
    "qualityScore", "priceFit", "isNewVenue", "pixelVisualAffinity",
    "visualFamilyAffinity", "visualClassifierConfidence", "visualClassifierMargin",
    "visualEvidenceAvailable", "visualHistoryConfidence", "alignedVisualOpportunity",
)
CONTEXT_FEATURE_COUNT = 23
FLAG_NAMES = (
    "locationSensitive", "scarceAligned", "visualChallenge", "visualMissing",
    "evening", "intentPivot", "coldVenue",
)


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n" for row in rows),
        encoding="utf-8",
    )


def _softmax(scores: np.ndarray) -> np.ndarray:
    shifted = scores - scores.max(axis=1, keepdims=True)
    values = np.exp(shifted)
    return values / values.sum(axis=1, keepdims=True)


def _haversine(lat: float, lon: float, target_lat: np.ndarray, target_lon: np.ndarray) -> np.ndarray:
    radius = 6371.0088
    lat1, lon1 = np.radians(lat), np.radians(lon)
    lat2, lon2 = np.radians(target_lat), np.radians(target_lon)
    delta_lat, delta_lon = lat2 - lat1, lon2 - lon1
    value = np.sin(delta_lat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(delta_lon / 2) ** 2
    return 2 * radius * np.arcsin(np.sqrt(value).clip(0, 1))


def _visual_evidence(repo: Path) -> dict[str, Any]:
    dataset = repo / "apps/demand-engine/evaluation/synthetic-marketplace-full-taxonomy-visual-v3"
    rows = json.loads((dataset / "development-multiregion-embeddings.v4.json").read_text(encoding="utf-8"))["rows"]
    model_path = repo / "apps/demand-engine/models/full-taxonomy-visual-hybrid-classifier.v5.json"
    model = json.loads(model_path.read_text(encoding="utf-8"))
    with np.load(dataset / "development-classic-pixel-features.v5.npz", allow_pickle=False) as artifact:
        image_ids = artifact["image_ids"].tolist()
        classic = np.asarray(artifact["features"], dtype=np.float64)
    if image_ids != [row["imageId"] for row in rows]:
        raise ValueError("RECOMMENDATION_JOINT_VISUAL_ALIGNMENT_INVALID")
    global_x = np.asarray([row["globalEmbedding"] for row in rows], dtype=np.float64)
    center_x = np.asarray([row["centerEmbedding"] for row in rows], dtype=np.float64)
    combined = np.column_stack((global_x, center_x))
    combined /= np.linalg.norm(combined, axis=1, keepdims=True).clip(min=1e-12)
    probabilities = _softmax(predict_scores(model, global_x, center_x, classic))
    order = np.sort(probabilities, axis=1)
    return {
        "rows": rows,
        "combined": combined,
        "probabilities": probabilities,
        "confidence": order[:, -1],
        "margin": order[:, -1] - order[:, -2],
        "classes": model["classes"],
        "modelSha256": _sha256(model_path),
        "embeddingSha256": _sha256(dataset / "development-multiregion-embeddings.v4.json"),
        "classicSha256": _sha256(dataset / "development-classic-pixel-features.v5.npz"),
    }


def _catalog(repo: Path) -> tuple[list[dict[str, Any]], dict[str, int]]:
    raw = json.loads((repo / "packages/demand-contracts/catalog/venue-taxonomy.v1.json").read_text(encoding="utf-8"))
    family_codes = [row["code"] for row in raw["families"]]
    return raw["types"], {code: index for index, code in enumerate(family_codes)}


def _build_entities(repo: Path, visual: dict[str, Any]) -> dict[str, Any]:
    types, _ = _catalog(repo)
    family_index = {code: index for index, code in enumerate(visual["classes"])}
    type_index = {row["code"]: index for index, row in enumerate(types)}
    cities = np.asarray([
        (40.4168, -3.7038), (41.3874, 2.1686), (37.3891, -5.9845),
        (39.4699, -0.3763), (43.2630, -2.9350), (36.7213, -4.4214),
        (37.1773, -3.5986), (42.2406, -8.7207), (41.6488, -0.8891),
        (38.3452, -0.4810), (37.9922, -1.1307), (39.8628, -4.0273),
        (43.3623, -8.4115), (28.1235, -15.4363), (39.5696, 2.6502),
        (43.5322, -5.6611), (42.8782, -8.5448), (36.5297, -6.2927),
        (40.9701, -5.6635), (41.6523, -4.7245),
    ], dtype=np.float64)
    venue_type = np.empty(VENUE_COUNT, dtype=np.int16)
    venue_family = np.empty(VENUE_COUNT, dtype=np.int16)
    visual_rows = visual["rows"]
    for index in range(VENUE_COUNT):
        if index < len(visual_rows):
            code = visual_rows[index]["typeCode"]
        else:
            code = types[(index * 37) % len(types)]["code"]
        venue_type[index] = type_index[code]
        venue_family[index] = family_index[types[type_index[code]]["familyCode"]]
    venue_city = np.arange(VENUE_COUNT, dtype=np.int16) % len(cities)
    phase = np.arange(VENUE_COUNT, dtype=np.float64)
    lat = cities[venue_city, 0] + 0.035 * np.sin(phase * 0.71)
    lon = cities[venue_city, 1] + 0.045 * np.cos(phase * 0.53)
    services = (venue_type * 7 + np.arange(VENUE_COUNT)) % 24
    attributes = (venue_type * 11 + np.arange(VENUE_COUNT) * 3) % 18
    price = 1 + (np.arange(VENUE_COUNT) * 5 + venue_type) % 4
    quality = 0.45 + 0.5 * ((np.arange(VENUE_COUNT) * 17) % 101) / 100
    exposure = ((np.arange(VENUE_COUNT) * 29) % 101) / 100
    is_new = (np.arange(VENUE_COUNT) % 9 == 0).astype(np.float64)
    preferred_hour_group = np.arange(VENUE_COUNT) % 3
    preferred_day_a = np.arange(VENUE_COUNT) % 7
    preferred_day_b = (preferred_day_a + 2) % 7
    visual_available = np.arange(VENUE_COUNT) < len(visual_rows)
    visual_embedding = np.zeros((VENUE_COUNT, visual["combined"].shape[1]), dtype=np.float32)
    visual_embedding[:len(visual_rows)] = visual["combined"].astype(np.float32)
    visual_probabilities = np.zeros((VENUE_COUNT, len(visual["classes"])), dtype=np.float32)
    visual_probabilities[:len(visual_rows)] = visual["probabilities"].astype(np.float32)
    confidence = np.zeros(VENUE_COUNT, dtype=np.float32)
    margin = np.zeros(VENUE_COUNT, dtype=np.float32)
    confidence[:len(visual_rows)] = visual["confidence"]
    margin[:len(visual_rows)] = visual["margin"]
    family_members = {family: np.flatnonzero(venue_family == family) for family in np.unique(venue_family)}
    image_family_members = {
        family: np.flatnonzero((venue_family == family) & visual_available)
        for family in np.unique(venue_family)
    }
    profile_type = (np.arange(USER_COUNT) * 17) % len(types)
    profile_family = np.asarray([family_index[types[index]["familyCode"]] for index in profile_type], dtype=np.int16)
    profile_service = (profile_type * 7 + np.arange(USER_COUNT)) % 24
    profile_attribute = (profile_type * 11 + np.arange(USER_COUNT) * 3) % 18
    profile_city = np.arange(USER_COUNT, dtype=np.int16) % len(cities)
    max_radius = 5 + (np.arange(USER_COUNT) * 7) % 36
    preferred_price = 1 + (np.arange(USER_COUNT) * 3) % 4
    consent = np.arange(USER_COUNT) % 5 != 0
    history_index = np.zeros(USER_COUNT, dtype=np.int32)
    for user in range(USER_COUNT):
        options = image_family_members[int(profile_family[user])]
        history_index[user] = options[(user * 13) % len(options)]
    history_embedding = visual_embedding[history_index].astype(np.float64)
    history_confidence = (0.45 + 0.55 * consent.astype(np.float64)).astype(np.float32)
    return locals()


def _profile_rows(entities: dict[str, Any], types: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "profileId": f"scale-user-{index + 1:05d}",
            "locale": "es" if index % 4 else "en",
            "preferredTypeCode": types[int(entities["profile_type"][index])]["code"],
            "preferredFamilyIndex": int(entities["profile_family"][index]),
            "preferredServiceIndex": int(entities["profile_service"][index]),
            "preferredAttributeIndex": int(entities["profile_attribute"][index]),
            "maximumDistanceKm": int(entities["max_radius"][index]),
            "persistentPreferenceConsent": bool(entities["consent"][index]),
            "visualHistorySource": "approved-image-interactions-before-session",
        }
        for index in range(USER_COUNT)
    ]


def _venue_rows(entities: dict[str, Any], types: list[dict[str, Any]], visual_rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        {
            "venueId": f"scale-venue-{index + 1:05d}",
            "typeCode": types[int(entities["venue_type"][index])]["code"],
            "familyCode": types[int(entities["venue_type"][index])]["familyCode"],
            "cityIndex": int(entities["venue_city"][index]),
            "latitude": round(float(entities["lat"][index]), 6),
            "longitude": round(float(entities["lon"][index]), 6),
            "serviceIndex": int(entities["services"][index]),
            "attributeIndex": int(entities["attributes"][index]),
            "priceBand": int(entities["price"][index]),
            "visualEvidence": (
                {"imageId": visual_rows[index]["imageId"], "source": "approved-v3-development"}
                if index < len(visual_rows) else None
            ),
        }
        for index in range(VENUE_COUNT)
    ]


def _candidate_group(
    target: int, family: int, entities: dict[str, Any], rng: np.random.Generator,
    visual_challenge: bool,
) -> np.ndarray:
    selected = [target]
    family_pool = entities["image_family_members"][family] if visual_challenge else entities["family_members"][family]
    if len(family_pool):
        for value in rng.permutation(family_pool):
            if int(value) not in selected:
                selected.append(int(value))
            if len(selected) >= 6:
                break
    city_pool = np.flatnonzero(entities["venue_city"] == entities["venue_city"][target])
    for value in rng.permutation(city_pool):
        if int(value) not in selected:
            selected.append(int(value))
        if len(selected) >= 9:
            break
    while len(selected) < CANDIDATES_PER_SESSION:
        value = int(rng.integers(0, VENUE_COUNT))
        if value not in selected:
            selected.append(value)
    return np.asarray(selected, dtype=np.int32)


def _actions(global_index: int, visual_challenge: bool) -> np.ndarray:
    length = 3 + global_index % 6
    sequence = np.full(8, 255, dtype=np.uint8)
    base = [(global_index + step * 3) % len(ACTION_TYPES) for step in range(length)]
    if visual_challenge:
        base[-min(3, length):] = [2, 6, 7][-min(3, length):]
    elif global_index % 4 == 0:
        base[-min(3, length):] = [3, 5, 8][-min(3, length):]
    sequence[:length] = base
    return sequence


def _build_split(
    entities: dict[str, Any], count: int, start_index: int, seed: int, test: bool,
) -> dict[str, np.ndarray]:
    rng = np.random.default_rng(seed)
    features = np.zeros((count, CANDIDATES_PER_SESSION, len(FEATURE_NAMES)), dtype=np.float32)
    candidates = np.zeros((count, CANDIDATES_PER_SESSION), dtype=np.int32)
    labels = np.zeros(count, dtype=np.int16)
    users = np.zeros(count, dtype=np.int32)
    actions = np.full((count, 8), 255, dtype=np.uint8)
    flags = np.zeros((count, len(FLAG_NAMES)), dtype=np.uint8)
    requested_day = np.zeros(count, dtype=np.uint8)
    requested_hour = np.zeros(count, dtype=np.uint8)
    for row_index in range(count):
        global_index = start_index + row_index
        user = global_index % USER_COUNT
        visual_challenge = global_index % 10 in (0, 1, 2)
        intent_pivot = global_index % 7 == 3
        if visual_challenge:
            target = int(entities["history_index"][user])
            intent_family = int(entities["profile_family"][user])
            intent_type = -1
        else:
            target = (global_index * 37) % VENUE_COUNT
            intent_family = int(entities["venue_family"][target])
            intent_type = int(entities["venue_type"][target])
        group = _candidate_group(target, intent_family, entities, rng, visual_challenge)
        candidates[row_index] = group
        users[row_index] = user
        actions[row_index] = _actions(global_index, visual_challenge)
        day = int((global_index * 3 + user) % 7)
        hour = int(8 + (global_index * 5 + user) % 14)
        requested_day[row_index], requested_hour[row_index] = day, hour
        angle = float(rng.uniform(0, 2 * np.pi))
        distance_offset = float(rng.uniform(0.15, 6.0))
        user_lat = float(entities["lat"][target] + distance_offset / 111 * np.cos(angle))
        user_lon = float(entities["lon"][target] + distance_offset / 85 * np.sin(angle))
        distance = _haversine(user_lat, user_lon, entities["lat"][group], entities["lon"][group])
        type_match = (entities["venue_type"][group] == intent_type).astype(float) if intent_type >= 0 else np.zeros(len(group))
        family_match = (entities["venue_family"][group] == intent_family).astype(float)
        service_match = (entities["services"][group] == entities["profile_service"][user]).astype(float)
        attribute_match = (entities["attributes"][group] == entities["profile_attribute"][user]).astype(float)
        search = np.maximum(type_match, 0.72 * family_match)
        action_type = (0.72 + 0.25 * ((global_index % 5) / 4)) * np.maximum(type_match, 0.55 * family_match)
        action_family = (0.8 + 0.18 * ((global_index % 3) / 2)) * family_match
        action_service = np.maximum(service_match, 0.35 * family_match)
        momentum = np.full(len(group), 0.35 + 0.55 * (actions[row_index, 2] != 255)) * family_match
        persistent = (
            np.maximum(
                (entities["venue_type"][group] == entities["profile_type"][user]).astype(float),
                0.6 * (entities["venue_family"][group] == entities["profile_family"][user]).astype(float),
            ) if entities["consent"][user] else np.zeros(len(group))
        )
        taxonomy_type = type_match
        taxonomy_family = family_match
        content = (
            0.30 * action_type + 0.18 * action_family + 0.18 * action_service
            + 0.12 * taxonomy_type + 0.08 * attribute_match + 0.08 * search
            + 0.06 * persistent
        )
        proximity = np.exp(-distance / 9.0)
        within_radius = (distance <= entities["max_radius"][user]).astype(float)
        distance_decay = 1 / (1 + distance)
        day_affinity = (
            (entities["preferred_day_a"][group] == day)
            | (entities["preferred_day_b"][group] == day)
        ).astype(float)
        hour_group = entities["preferred_hour_group"][group]
        hour_affinity = np.where(
            hour_group == 0, float(hour <= 13),
            np.where(hour_group == 1, float(hour >= 16), np.ones(len(group))),
        )
        total_slots = 6 + (group * 7 + global_index) % 15
        remaining = 3 + (group * 11 + global_index * 3) % np.maximum(3, total_slots - 2)
        if global_index % 5 in (0, 1):
            remaining[0] = 1 + global_index % 2
        remaining = np.minimum(remaining, total_slots)
        availability = remaining / total_slots
        urgency = 1 - availability
        scarcity = content * proximity * within_radius * urgency
        low_exposure = content * (1 - entities["exposure"][group])
        price_fit = 1 - np.abs(entities["price"][group] - entities["preferred_price"][user]) / 3
        visual_available = entities["visual_available"][group].astype(float)
        visual_affinity = np.zeros(len(group))
        available_mask = visual_available > 0
        if available_mask.any():
            cosine = entities["visual_embedding"][group[available_mask]] @ entities["history_embedding"][user]
            visual_affinity[available_mask] = np.clip((cosine + 1) / 2, 0, 1)
        class_affinity = np.zeros(len(group))
        class_affinity[available_mask] = entities["visual_probabilities"][group[available_mask], intent_family]
        history_confidence = np.full(len(group), entities["history_confidence"][user])
        aligned_visual = visual_affinity * class_affinity * history_confidence * visual_available
        matrix = np.column_stack((
            action_type, action_family, action_service, search, momentum, persistent,
            taxonomy_type, taxonomy_family, service_match, attribute_match, content,
            proximity, within_radius, distance_decay, day_affinity, hour_affinity,
            availability, urgency, scarcity, low_exposure, entities["quality"][group],
            price_fit, entities["is_new"][group], visual_affinity, class_affinity,
            entities["confidence"][group], entities["margin"][group], visual_available,
            history_confidence, aligned_visual,
        ))
        features[row_index] = matrix.astype(np.float32)
        utility = (
            1.45 * content + 0.58 * proximity + 0.18 * within_radius + 0.18 * day_affinity
            + 0.22 * hour_affinity + 0.32 * scarcity + 0.12 * low_exposure
            + 0.07 * entities["quality"][group] + 0.10 * price_fit
            + 0.18 * visual_affinity + 0.20 * class_affinity + 0.75 * aligned_visual
        )
        ranking = np.argsort(utility)
        ambiguous = (global_index % 20 == 7) if test else (global_index % 10 == 3)
        labels[row_index] = int(ranking[-2] if ambiguous else ranking[-1])
        flags[row_index] = (
            distance.max() - distance.min() >= 5,
            remaining[labels[row_index]] <= 2 and content[labels[row_index]] >= 0.55,
            visual_challenge,
            visual_available[labels[row_index]] == 0,
            hour >= 16,
            intent_pivot,
            entities["exposure"][group[labels[row_index]]] <= 0.25,
        )
    return {
        "features": features, "candidateVenueIndices": candidates, "positiveIndices": labels,
        "userIndices": users, "actionCodes": actions, "scenarioFlags": flags,
        "requestedDay": requested_day, "requestedHour": requested_hour,
    }


def _save_npz(path: Path, values: dict[str, np.ndarray]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(path, **values)


def build(repo: Path, output: Path) -> dict[str, Any]:
    """Construye una versión nueva; nunca sobrescribe un dataset sellado."""

    if output.exists():
        raise ValueError("RECOMMENDATION_JOINT_SCALE_DATASET_ALREADY_EXISTS")
    output.mkdir(parents=True)
    visual = _visual_evidence(repo)
    types, _ = _catalog(repo)
    entities = _build_entities(repo, visual)
    profiles = _profile_rows(entities, types)
    venues = _venue_rows(entities, types, visual["rows"])
    profiles_path, venues_path = output / "profiles.jsonl", output / "venues.jsonl"
    development_path, test_path = output / "development.npz", output / "test.sealed.npz"
    _write_jsonl(profiles_path, profiles)
    _write_jsonl(venues_path, venues)
    development = _build_split(entities, DEVELOPMENT_SESSIONS, 0, RANDOM_SEED, False)
    test = _build_split(entities, TEST_SESSIONS, DEVELOPMENT_SESSIONS, RANDOM_SEED + 991, True)
    _save_npz(development_path, development)
    _save_npz(test_path, test)
    manifest = {
        "schemaVersion": 1,
        "datasetVersion": VERSION,
        "randomSeed": RANDOM_SEED,
        "counts": {
            "users": USER_COUNT,
            "venues": VENUE_COUNT,
            "venuesWithUniqueApprovedImageEvidence": len(visual["rows"]),
            "venuesWithoutVisualEvidence": VENUE_COUNT - len(visual["rows"]),
            "developmentSessions": DEVELOPMENT_SESSIONS,
            "testSessions": TEST_SESSIONS,
            "candidatesPerSession": CANDIDATES_PER_SESSION,
            "candidateRows": (DEVELOPMENT_SESSIONS + TEST_SESSIONS) * CANDIDATES_PER_SESSION,
            "actionTypes": len(ACTION_TYPES),
            "taxonomyTypes": len(types),
            "taxonomyFamilies": len(visual["classes"]),
        },
        "featureNames": list(FEATURE_NAMES),
        "contextFeatureCount": CONTEXT_FEATURE_COUNT,
        "actionTypes": list(ACTION_TYPES),
        "scenarioFlagNames": list(FLAG_NAMES),
        "splitPolicy": "development-then-strictly-later-sealed-temporal-test",
        "locationPolicy": "point-in-time-haversine-no-raw-coordinate-features",
        "visualPolicy": {
            "source": "approved-consumed-v3-images-through-frozen-v5-candidate",
            "uniqueImageVenueMappings": len(visual["rows"]),
            "missingVisualEvidenceUsesZeroSignalFallback": True,
            "sensitiveInferenceAllowed": False,
            "visualModelSha256": visual["modelSha256"],
            "embeddingSha256": visual["embeddingSha256"],
            "classicSha256": visual["classicSha256"],
        },
        "leakagePolicy": {
            "featuresComputedBeforeOutcome": True,
            "rawIdentifiersAreFeatures": False,
            "candidatePositionIsFeature": False,
            "futureActionsAreFeatures": False,
        },
        "files": {
            "profiles": {"path": profiles_path.name, "sha256": _sha256(profiles_path)},
            "venues": {"path": venues_path.name, "sha256": _sha256(venues_path)},
            "development": {"path": development_path.name, "sha256": _sha256(development_path)},
            "sealedTest": {"path": test_path.name, "sha256": _sha256(test_path)},
        },
        "testOpenBudget": 1,
        "testOpenCount": 0,
        "productionEvidence": False,
        "promotionAllowed": False,
    }
    _write_json(output / "manifest.json", manifest)
    return manifest


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[4]
    output = args.output or repo / "apps/demand-engine/evaluation/synthetic-marketplace-joint-scale-v9"
    print(json.dumps(build(repo, output)["counts"], ensure_ascii=False))


if __name__ == "__main__":
    run()
