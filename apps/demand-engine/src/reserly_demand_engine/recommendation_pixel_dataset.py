"""Dataset v3 para recomendación basada en patrones visuales CLIP reales.

El generador enlaza PNG aprobados con embeddings CLIP congelados, verifica el
SHA-256 de cada imagen y construye perfiles visuales únicamente con elecciones
anteriores del usuario. No genera imágenes ni vuelve a entrenar CLIP. Cada
afinidad visual se calcula antes del outcome de la sesión correspondiente.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from collections import Counter, defaultdict
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Iterable
from uuid import NAMESPACE_URL, uuid5

import numpy as np


DATASET_VERSION = "synthetic-marketplace-pixel-personalization-v4"
SEED = 3917
GENERATED_AT = "2026-08-30T18:00:00Z"


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
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


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _stable_id(kind: str, index: int) -> str:
    return str(uuid5(NAMESPACE_URL, f"reserly:{DATASET_VERSION}:{kind}:{index:06d}"))


def _normalise(values: list[float] | np.ndarray) -> np.ndarray:
    vector = np.asarray(values, dtype=np.float64)
    norm = float(np.linalg.norm(vector))
    if vector.shape != (512,) or not math.isfinite(norm) or norm < 0.99 or norm > 1.01:
        raise ValueError("RECOMMENDATION_PIXEL_EMBEDDING_INVALID")
    return vector / norm


def _cohorts(linked: list[dict[str, Any]]) -> dict[str, str]:
    """Reserva una imagen/local por categoría para validation-cold y test-cold."""

    by_category: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in linked:
        by_category[row["categoryCode"]].append(row)
    result: dict[str, str] = {}
    for category, rows in sorted(by_category.items()):
        ordered = sorted(rows, key=lambda row: row["venueId"])
        if len(ordered) < 8:
            raise ValueError(f"RECOMMENDATION_PIXEL_CATEGORY_COVERAGE_INVALID:{category}")
        for row in ordered[:-2]:
            result[row["venueId"]] = "warm"
        result[ordered[-2]["venueId"]] = "validationCold"
        result[ordered[-1]["venueId"]] = "testCold"
    return result


def _allowed(cohort: str, split: str) -> bool:
    return cohort in {
        "train": {"warm"},
        "validation": {"warm", "validationCold"},
        "test": {"warm", "validationCold", "testCold"},
    }[split]


def _profile_allowed(index: int, split: str) -> bool:
    limit = {"train": 28, "validation": 34, "test": 40}[split]
    return index < limit


def _load_and_verify_pixels(
    source_root: Path,
) -> tuple[list[dict[str, Any]], dict[str, np.ndarray], dict[str, dict[str, Any]]]:
    """Enlaza embeddings con PNG y valida que representan exactamente esos bytes."""

    venues = {row["venueId"]: row for row in _read_jsonl(source_root / "venues.jsonl")}
    visual_root = source_root / "visual-training-dataset-v2"
    definition = json.loads((visual_root / "approved-definition.json").read_text(encoding="utf-8"))
    embedding_artifact = json.loads(
        (visual_root / "approved-clip-embeddings.json").read_text(encoding="utf-8")
    )
    definitions = {row["imageId"]: row for row in definition["rows"]}
    linked: list[dict[str, Any]] = []
    embeddings: dict[str, np.ndarray] = {}
    for row in embedding_artifact["rows"]:
        if row["venueId"] not in venues:
            continue
        definition_row = definitions.get(row["imageId"])
        if definition_row is None or definition_row["humanReviewStatus"] != "approved":
            raise ValueError("RECOMMENDATION_PIXEL_IMAGE_NOT_APPROVED")
        image_path = (visual_root / definition_row["relativePath"]).resolve()
        if not image_path.is_file() or _sha(image_path) != row["imageSha256"]:
            raise ValueError("RECOMMENDATION_PIXEL_IMAGE_HASH_MISMATCH")
        embedding = _normalise(row["embedding"])
        embeddings[row["venueId"]] = embedding
        linked.append(
            {
                "venueId": row["venueId"],
                "imageId": row["imageId"],
                "imageSha256": row["imageSha256"],
                "categoryCode": row["categoryCode"],
                "embeddingDimension": len(row["embedding"]),
                "embeddingModelKey": embedding_artifact["baseModelKey"],
                "embeddingModelRevision": embedding_artifact["baseModelRevision"],
                "humanReviewStatus": "approved",
                "pixelHashVerified": True,
                "productionTrainingAllowed": False,
            }
        )
    if len(linked) != 70 or len(embeddings) != 70:
        raise ValueError("RECOMMENDATION_PIXEL_LINKAGE_COUNT_INVALID")
    return linked, embeddings, venues


def _onboarding_events(
    profiles: list[dict[str, Any]], linked: list[dict[str, Any]], cohorts: dict[str, str]
) -> list[dict[str, Any]]:
    warm_by_category: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in linked:
        if cohorts[row["venueId"]] == "warm":
            warm_by_category[row["categoryCode"]].append(row)
    categories = sorted(warm_by_category)
    events: list[dict[str, Any]] = []
    for profile_index, profile in enumerate(profiles):
        category = categories[profile_index % len(categories)]
        options = sorted(warm_by_category[category], key=lambda row: row["venueId"])
        selected = (options[profile_index % len(options)], options[(profile_index + 2) % len(options)])
        for rank, row in enumerate(selected):
            events.append(
                {
                    "eventId": _stable_id("visual-onboarding", profile_index * 2 + rank),
                    "occurredAt": f"2026-06-{20 + rank:02d}T10:00:00Z",
                    "profileId": profile["profileId"],
                    "venueId": row["venueId"],
                    "imageId": row["imageId"],
                    "eventType": "explicitVisualPreferenceSelection",
                    "synthetic": True,
                    "containsDirectIdentifiers": False,
                    "selectionUsedFutureOutcome": False,
                }
            )
    return events


def _visual_state(
    profiles: list[dict[str, Any]], events: list[dict[str, Any]], embeddings: dict[str, np.ndarray]
) -> tuple[dict[str, np.ndarray], Counter[str]]:
    sums = {profile["profileId"]: np.zeros(512, dtype=np.float64) for profile in profiles}
    counts: Counter[str] = Counter()
    for event in events:
        sums[event["profileId"]] += embeddings[event["venueId"]]
        counts[event["profileId"]] += 1
    return sums, counts


def _cosine(profile_sum: np.ndarray, venue_embedding: np.ndarray) -> float:
    norm = float(np.linalg.norm(profile_sum))
    return float(np.dot(profile_sum / norm, venue_embedding)) if norm else 0.0


def _build_sessions(
    profiles: list[dict[str, Any]],
    linked: list[dict[str, Any]],
    venues: dict[str, dict[str, Any]],
    labels: dict[str, dict[str, Any]],
    embeddings: dict[str, np.ndarray],
    cohorts: dict[str, str],
    onboarding: list[dict[str, Any]],
    rng: random.Random,
) -> list[dict[str, Any]]:
    sums, visual_counts = _visual_state(profiles, onboarding, embeddings)
    exposures: Counter[str] = Counter()
    linked_by_id = {row["venueId"]: row for row in linked}
    preferred_category: dict[str, str] = {}
    for event in onboarding:
        preferred_category.setdefault(event["profileId"], linked_by_id[event["venueId"]]["categoryCode"])
    split_spec = (
        ("train", 1_500, datetime(2026, 7, 1, tzinfo=UTC), 91),
        ("validation", 500, datetime(2026, 10, 1, tzinfo=UTC), 30),
        ("test", 700, datetime(2026, 11, 1, tzinfo=UTC), 29),
    )
    sessions: list[dict[str, Any]] = []
    pending_visual_outcomes: list[tuple[datetime, str, str]] = []
    session_index = 0
    for split, count, start, span in split_spec:
        eligible_profiles = [profile for index, profile in enumerate(profiles) if _profile_allowed(index, split)]
        eligible_rows = [row for row in linked if _allowed(cohorts[row["venueId"]], split)]
        for local_index in range(count):
            total_minutes = (span + 1) * 24 * 60
            interval_start = local_index * total_minutes // count
            interval_end = max(interval_start + 1, (local_index + 1) * total_minutes // count)
            occurred_at = start + timedelta(minutes=rng.randrange(interval_start, interval_end))
            matured = [item for item in pending_visual_outcomes if item[0] <= occurred_at]
            pending_visual_outcomes = [item for item in pending_visual_outcomes if item[0] > occurred_at]
            for _, matured_profile_id, matured_venue_id in matured:
                sums[matured_profile_id] += embeddings[matured_venue_id]
                visual_counts[matured_profile_id] += 1
            profile = eligible_profiles[(local_index * 13 + rng.randrange(len(eligible_profiles))) % len(eligible_profiles)]
            profile_id = profile["profileId"]
            intent_category = (
                preferred_category[profile_id]
                if session_index % 5 != 0
                else sorted({row["categoryCode"] for row in eligible_rows})[(session_index // 5) % 8]
            )
            same_category = [row for row in eligible_rows if row["categoryCode"] == intent_category]
            rng.shuffle(same_category)
            other = [row for row in eligible_rows if row["categoryCode"] != intent_category]
            rng.shuffle(other)
            candidate_links = (same_category[:6] + other)[:8]
            if len(candidate_links) != 8:
                raise ValueError("RECOMMENDATION_PIXEL_CANDIDATE_COUNT_INVALID")
            anchor = same_category[0] if same_category else candidate_links[0]
            anchor_venue = venues[anchor["venueId"]]
            desired_service = rng.choice(anchor_venue["serviceCodes"])
            desired_attribute = rng.choice(anchor_venue["attributeCodes"])
            desired_type = labels[anchor["venueId"]]["typeCode"]
            desired_family = labels[anchor["venueId"]]["familyCode"]
            max_exposure = max(exposures.values(), default=1)
            rows: list[dict[str, Any]] = []
            utilities: list[float] = []
            for candidate_link in candidate_links:
                venue_id = candidate_link["venueId"]
                venue = venues[venue_id]
                annotation = labels[venue_id]
                type_affinity = float(annotation["typeCode"] == desired_type)
                family_affinity = float(annotation["familyCode"] == desired_family)
                service_affinity = float(desired_service in venue["serviceCodes"])
                attribute_affinity = float(desired_attribute in venue["attributeCodes"])
                visual_affinity = _cosine(sums[profile_id], embeddings[venue_id])
                visual_affinity_scaled = max(0.0, min(1.0, (visual_affinity - 0.45) / 0.55))
                distance = rng.uniform(250, 14_000)
                proximity = math.exp(-distance / 6_000.0)
                availability = rng.uniform(0.08, 0.96)
                content = min(1.0, 0.32 * type_affinity + 0.20 * family_affinity + 0.32 * service_affinity + 0.16 * attribute_affinity)
                exposure = exposures[venue_id] / max_exposure
                scarcity = content * (1.0 - availability)
                low_exposure = content * (1.0 - exposure)
                capacity = scarcity * (1.0 - exposure)
                price_fit = max(0.0, 1.0 - abs(venue["priceTier"] - (1 + session_index % 3)) / 2.0)
                common_hour = 1.0 if occurred_at.hour in {9, 10, 17, 18, 19} else 0.5
                utility = (
                    1.25 * visual_affinity_scaled + 0.55 * content + 0.22 * service_affinity
                    + 0.18 * proximity + 0.14 * price_fit + 0.12 * common_hour
                    + 0.22 * capacity + 0.05 * venue["qualityScore"]
                )
                utilities.append(utility)
                rows.append(
                    {
                        "venueId": venue_id,
                        "imageId": candidate_link["imageId"],
                        "familyCode": annotation["familyCode"],
                        "typeCode": annotation["typeCode"],
                        "eligible": True,
                        "capacityAvailable": True,
                        "isNewVenue": cohorts[venue_id] != "warm",
                        "features": {
                            "taxonomyTypeAffinity": round(type_affinity, 8),
                            "taxonomyFamilyAffinity": round(family_affinity, 8),
                            "serviceAffinity": round(service_affinity, 8),
                            "attributeAffinity": round(attribute_affinity, 8),
                            "contentAffinity": round(content, 8),
                            "availabilityRatio": round(availability, 8),
                            "alignedScarcityOpportunity": round(scarcity, 8),
                            "qualityScore": venue["qualityScore"],
                            "proximity": round(proximity, 8),
                            "priceFit": round(price_fit, 8),
                            "lowExposureAffinity": round(low_exposure, 8),
                            "capacityOpportunity": round(capacity, 8),
                            "commonHourAffinity": round(common_hour, 8),
                            "isNewVenue": float(cohorts[venue_id] != "warm"),
                            "pixelVisualAffinity": round(visual_affinity_scaled, 8),
                            "pixelVisualHistoryConfidence": round(min(1.0, visual_counts[profile_id] / 8.0), 8),
                        },
                        "labels": {"relevance": 0, "clicked": 0, "bookingCompleted": 0},
                    }
                )
            best = max(range(8), key=lambda index: (utilities[index], rows[index]["venueId"]))
            ambiguous = (
                session_index % 50 in {3, 21, 37}
                if split == "test"
                else session_index % 25 in {3, 11, 19}
            )
            observed = best
            if ambiguous:
                observed = sorted(
                    (index for index in range(8) if index != best),
                    key=lambda index: (-utilities[index], rows[index]["venueId"]),
                )[0]
            rows[observed]["labels"] = {"relevance": 3, "clicked": 1, "bookingCompleted": 1}
            # La preferencia solo cambia cuando el outcome ha madurado 24 horas.
            pending_visual_outcomes.append(
                (occurred_at + timedelta(hours=24), profile_id, rows[observed]["venueId"])
            )
            for row in rows:
                exposures[row["venueId"]] += 1
            sessions.append(
                {
                    "sessionId": _stable_id("ranking-session", session_index),
                    "split": split,
                    "occurredAt": occurred_at.isoformat().replace("+00:00", "Z"),
                    "outcomeObservedAt": (occurred_at + timedelta(hours=24)).isoformat().replace("+00:00", "Z"),
                    "profileId": profile_id,
                    "completeCandidateSet": True,
                    "visualProfileEvidenceCount": visual_counts[profile_id],
                    "intent": {
                        "categoryCode": intent_category,
                        "familyCode": desired_family,
                        "typeCode": desired_type,
                        "serviceCode": desired_service,
                        "attributeCode": desired_attribute,
                    },
                    "visualProfileBuiltFromMaturePastOnly": True,
                    "ambiguousObservedChoice": ambiguous,
                    "candidates": rows,
                }
            )
            session_index += 1
    return sessions


def generate_pixel_dataset(
    source_root: Path, diverse_root: Path, output_root: Path, seed: int = SEED
) -> dict[str, Any]:
    """Genera sidecars y sesiones multimodales reproducibles sin nuevos píxeles."""

    output_root.mkdir(parents=True, exist_ok=True)
    linked, embeddings, venues = _load_and_verify_pixels(source_root)
    profiles = _read_jsonl(source_root / "profiles.jsonl")
    diverse_labels = {
        row["venueId"]: row for row in _read_jsonl(diverse_root / "venue-labels.jsonl")
    }
    if len(profiles) != 40 or not all(row["venueId"] in diverse_labels for row in linked):
        raise ValueError("RECOMMENDATION_PIXEL_SOURCE_CONTRACT_INVALID")
    cohorts = _cohorts(linked)
    for row in linked:
        row["recommendationCohort"] = cohorts[row["venueId"]]
    onboarding = _onboarding_events(profiles, linked, cohorts)
    sessions = _build_sessions(
        profiles, linked, venues, diverse_labels, embeddings, cohorts, onboarding, random.Random(seed)
    )
    development = [row for row in sessions if row["split"] != "test"]
    test = [row for row in sessions if row["split"] == "test"]
    artifacts = {
        "visual-linkage.jsonl": _write_jsonl(output_root / "visual-linkage.jsonl", linked),
        "visual-onboarding-events.jsonl": _write_jsonl(output_root / "visual-onboarding-events.jsonl", onboarding),
        "development-sessions.jsonl": _write_jsonl(output_root / "development-sessions.jsonl", development),
        "test-sessions.sealed.jsonl": _write_jsonl(output_root / "test-sessions.sealed.jsonl", test),
    }
    manifest = {
        "schemaVersion": 4,
        "datasetVersion": DATASET_VERSION,
        "generatedAt": GENERATED_AT,
        "seed": seed,
        "synthetic": True,
        "productionEvidence": False,
        "promotionAllowed": False,
        "counts": {
            "linkedApprovedImages": len(linked), "profiles": len(profiles),
            "onboardingEvents": len(onboarding), "sessions": len(sessions),
            "developmentSessions": len(development), "testSessions": len(test),
            "candidates": sum(len(row["candidates"]) for row in sessions),
            "families": len({diverse_labels[row["venueId"]]["familyCode"] for row in linked}),
            "types": len({diverse_labels[row["venueId"]]["typeCode"] for row in linked}),
        },
        "cohortCounts": dict(sorted(Counter(cohorts.values()).items())),
        "categoryCounts": dict(sorted(Counter(row["categoryCode"] for row in linked).items())),
        "splitCounts": dict(sorted(Counter(row["split"] for row in sessions).items())),
        "ambiguousObservedChoices": dict(sorted(Counter(row["split"] for row in sessions if row["ambiguousObservedChoice"]).items())),
        "pixelEvidence": {
            "rawPngHashesVerified": len(linked),
            "embeddingDimension": 512,
            "baseModelKey": linked[0]["embeddingModelKey"],
            "baseModelRevision": linked[0]["embeddingModelRevision"],
            "sourceEmbeddingArtifact": "../synthetic-marketplace-v1/visual-training-dataset-v2/approved-clip-embeddings.json",
            "sourceEmbeddingSha256": _sha(source_root / "visual-training-dataset-v2/approved-clip-embeddings.json"),
            "newImagesGenerated": 0,
            "pixelsUsedIndirectlyThroughFrozenEmbeddings": True,
        },
        "profilePolicy": {
            "initialSource": "two explicit pre-period visual selections per profile",
            "updates": "only observed clicks/bookings whose 24-hour outcome matured before scoring",
            "futureOutcomeUsed": False,
        },
        "testPolicy": "temporal-independent-open-once-after-baseline-and-multimodal-selection",
        "artifacts": artifacts,
        "limitations": [
            "Los intereses, eventos y outcomes son sintéticos y no demuestran comportamiento productivo.",
            "Solo 70 imágenes aprobadas enlazan de forma inequívoca con locales del marketplace.",
            "CLIP permanece congelado; el experimento mide similitud visual, no causalidad ni atributos sensibles.",
        ],
    }
    (output_root / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def run() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--diverse-root", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=SEED)
    args = parser.parse_args()
    report = generate_pixel_dataset(args.source_root, args.diverse_root, args.output_root, args.seed)
    print(json.dumps(report["counts"], ensure_ascii=False))


if __name__ == "__main__":
    run()
