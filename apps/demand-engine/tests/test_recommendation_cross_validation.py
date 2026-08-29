from __future__ import annotations

from datetime import UTC, datetime, timedelta

from reserly_demand_engine.recommendation_cross_validation import _rolling_folds, business_scenarios


FEATURES = [
    "contentAffinity", "serviceAffinity", "attributeAffinity", "visualAmbienceAffinity",
    "availabilityRatio", "alignedScarcityOpportunity", "qualityScore", "proximity",
    "priceFit", "lowExposureAffinity", "capacityOpportunity", "historicalCategoryAffinity",
    "commonUserHourAffinity", "commonVenueHourAffinity", "isNewVenue",
]


def test_five_rolling_folds_never_train_on_the_future() -> None:
    start = datetime(2026, 1, 1, tzinfo=UTC)
    sessions = [
        {"occurredAt": (start + timedelta(hours=index)).isoformat(), "sessionId": f"s-{index:03d}"}
        for index in range(120)
    ]

    folds = _rolling_folds(sessions, 5)

    assert len(folds) == 5
    for train, validation in folds:
        assert max(datetime.fromisoformat(row["occurredAt"]) for row in train) <= min(
            datetime.fromisoformat(row["occurredAt"]) for row in validation
        )


def test_business_suite_covers_requested_diverse_flows() -> None:
    scenarios = business_scenarios(FEATURES)
    codes = {row["scenarioCode"] for row in scenarios}

    assert len(scenarios) == 10
    assert {
        "aligned-scarce-underexposed",
        "visual-ambience-match",
        "common-user-time",
        "nearby-compatible",
        "specialty-match",
        "repeat-booking-affinity",
        "cold-start-exploration",
        "quality-does-not-override-intent",
        "available-capacity",
        "price-and-distance-balance",
    } == codes
