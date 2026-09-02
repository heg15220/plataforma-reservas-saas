"""Contrato del ranking productivo anterior al umbral gobernado de v10."""

from __future__ import annotations

import json
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import UUID, uuid4

import pytest
from pydantic import ValidationError

from reserly_demand_engine.production_bootstrap_ranking import (
    ProductionBootstrapPolicy,
    ProductionBootstrapPolicyVersionMismatch,
    ProductionBootstrapRanker,
    ProductionBootstrapRequest,
    ProductionSearchCounterInvalid,
)


ROOT = Path(__file__).resolve().parents[1]
NOW = datetime(2026, 9, 3, 12, tzinfo=UTC)


def _constraints(**overrides: object) -> dict[str, object]:
    values: dict[str, object] = {
        "venuePublished": True,
        "serviceBookable": True,
        "eligibilityAllowed": True,
        "permissionAllowed": True,
        "filtersMatched": True,
        "frequencyAllowed": True,
        "availableCapacity": 2,
        "requestedCapacity": 1,
        "validUntil": (NOW + timedelta(minutes=5)).isoformat(),
    }
    values.update(overrides)
    return values


def _candidate(venue_id: UUID, **overrides: object) -> dict[str, object]:
    values: dict[str, object] = {
        "venueId": str(venue_id),
        "constraints": _constraints(),
        "locationPermissionGranted": True,
        "distanceMeters": 1_000,
        "approvedVisualEvidence": True,
        "visualAffinity": 0.5,
        "intentAlignment": 0.5,
        "totalSlotCapacity": 10,
        "verifiedReviewAverage": 4.0,
        "verifiedReviewCount": 20,
    }
    values.update(overrides)
    return values


def _request(candidates: list[dict[str, object]], count: int = 9_999) -> ProductionBootstrapRequest:
    return ProductionBootstrapRequest.model_validate(
        {
            "requestId": str(uuid4()),
            "schemaVersion": 1,
            "occurredAt": NOW.isoformat(),
            "locale": "es",
            "policyVersion": "production-bootstrap-ranking-v1",
            "searchHistory": {
                "environment": "production",
                "source": "spring-behavior-events-production-aggregate",
                "metric": "accepted-active-search-history",
                "count": count,
                "asOf": NOW.isoformat(),
            },
            "candidates": candidates,
        }
    )


@pytest.fixture
def ranker() -> ProductionBootstrapRanker:
    policy = ProductionBootstrapPolicy.load(
        ROOT / "policies/production-bootstrap-ranking.v1.json"
    )
    return ProductionBootstrapRanker(
        policy,
        ROOT / "policies/recommendation-joint-scale.v10.json",
        ROOT / "models/joint-context-visual-ranker.v10.linear.json",
    )


def test_policy_fixes_threshold_order_and_keeps_v10_automatic_promotion_off() -> None:
    policy = ProductionBootstrapPolicy.load(
        ROOT / "policies/production-bootstrap-ranking.v1.json"
    )
    assert policy.productionSearchThreshold == 10_000
    assert policy.priorityOrder == (
        "location", "approvedVisualAffinity", "alignedScarcity", "verifiedReviewQuality"
    )
    assert policy.v10ModelVersion == "joint-context-visual-ranker-v10"
    assert len(policy.v10PolicySha256) == len(policy.v10ModelSha256) == 64
    assert policy.automaticV10PromotionAllowed is False

    raw = json.loads((ROOT / "policies/production-bootstrap-ranking.v1.json").read_text("utf-8"))
    raw["productionSearchThreshold"] = 9_999
    with pytest.raises(ValidationError):
        ProductionBootstrapPolicy.model_validate(raw)

    with pytest.raises(ValueError, match="V10_MODEL_HASH_MISMATCH"):
        ProductionBootstrapRanker(
            policy,
            ROOT / "policies/recommendation-joint-scale.v10.json",
            ROOT / "policies/recommendation-joint-scale.v10.json",
        )


def test_priority_is_lexicographic_and_never_compensated_by_lower_signals(
    ranker: ProductionBootstrapRanker,
) -> None:
    ids = [UUID(int=value) for value in range(1, 5)]
    closer = _candidate(
        ids[0], distanceMeters=999, visualAffinity=0.0, intentAlignment=0.0,
        verifiedReviewAverage=0.0, verifiedReviewCount=100,
    )
    visually_better = _candidate(
        ids[1], distanceMeters=1_000, visualAffinity=1.0, intentAlignment=1.0,
        verifiedReviewAverage=5.0, verifiedReviewCount=100,
    )
    scarce = _candidate(ids[2], visualAffinity=0.5, intentAlignment=1.0)
    scarce["constraints"] = _constraints(availableCapacity=1)
    reviewed = _candidate(
        ids[3], visualAffinity=0.5, intentAlignment=0.5,
        verifiedReviewAverage=5.0, verifiedReviewCount=100,
    )

    result = ranker.rank(_request([reviewed, scarce, visually_better, closer]))
    assert result.mode == "bootstrap_priority"
    assert [item.venueId for item in result.items] == [ids[0], ids[1], ids[2], ids[3]]
    assert result.searchesRemaining == 1
    assert result.items[2].priorityValues.alignedScarcity == 0.9
    assert result.items[3].priorityValues.verifiedReviewQuality > 0.9


def test_missing_or_unapproved_evidence_is_neutral_and_only_verified_reviews_count(
    ranker: ProductionBootstrapRanker,
) -> None:
    candidate = _candidate(
        uuid4(), locationPermissionGranted=False, distanceMeters=None,
        approvedVisualEvidence=False, visualAffinity=None,
        verifiedReviewAverage=None, verifiedReviewCount=0,
    )
    values = ranker.rank(_request([candidate], count=0)).items[0].priorityValues
    assert values.location == 0
    assert values.approvedVisualAffinity == 0
    assert values.verifiedReviewQuality == 0.7

    with pytest.raises(ValidationError):
        ProductionBootstrapRequest.model_validate(
            _request([candidate]).model_dump()
            | {"candidates": [_candidate(uuid4(), approvedVisualEvidence=False)]}
        )


def test_exact_threshold_stops_bootstrap_and_requests_v10_without_ranking(
    ranker: ProductionBootstrapRanker,
) -> None:
    result = ranker.rank(_request([_candidate(uuid4())], count=10_000))
    assert result.mode == "joint_v10"
    assert result.status == "v10_handoff_required"
    assert result.modelVersion == "joint-context-visual-ranker-v10"
    assert result.v10HandoffRequired is True
    assert result.automaticV10PromotionAllowed is False
    assert result.searchesRemaining == 0
    assert result.items == []


def test_constraints_counter_freshness_and_policy_are_fail_closed(
    ranker: ProductionBootstrapRanker,
) -> None:
    rejected = _candidate(uuid4())
    rejected["constraints"] = _constraints(availableCapacity=0)
    result = ranker.rank(_request([rejected]))
    assert result.status == "no_eligible_candidates"
    assert result.excluded[0].reasonCodes == ["INSUFFICIENT_CAPACITY"]

    stale = _request([_candidate(uuid4())]).model_copy(
        update={
            "searchHistory": _request([_candidate(uuid4())]).searchHistory.model_copy(
                update={"asOf": NOW - timedelta(seconds=301)}
            )
        }
    )
    with pytest.raises(ProductionSearchCounterInvalid):
        ranker.rank(stale)
    with pytest.raises(ProductionBootstrapPolicyVersionMismatch):
        ranker.rank(_request([_candidate(uuid4())]).model_copy(update={"policyVersion": "wrong"}))
