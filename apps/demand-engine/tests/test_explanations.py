"""Pruebas de trazabilidad, permisos, cardinalidad e i18n de explicaciones."""

from __future__ import annotations

import unittest
from pathlib import Path

from reserly_demand_engine.explanations import (
    ExplanationBuilder,
    ExplanationPermissions,
    ExplanationPolicy,
    ScoreContributionLike,
)
from reserly_demand_engine.fallback import FallbackEvidence


ROOT = Path(__file__).resolve().parents[1]


class ExplanationBuilderTests(unittest.TestCase):
    def setUp(self) -> None:
        self.builder = ExplanationBuilder(
            ExplanationPolicy.load(ROOT / "policies" / "explanation-mvp.v1.json")
        )

    def test_selects_top_two_real_score_contributions_in_requested_locale(self) -> None:
        contributions = [
            ScoreContributionLike(component="affinity", value=0.9, contribution=0.27),
            ScoreContributionLike(component="availability", value=0.8, contribution=0.12),
            ScoreContributionLike(component="proximity", value=0.7, contribution=0.105),
        ]
        result = self.builder.build_score(
            "es", contributions, ExplanationPermissions(personalization=True, location=True)
        )
        self.assertEqual(["CONTEXT_MATCH", "GOOD_AVAILABILITY"], [item.code for item in result])
        self.assertEqual([0.27, 0.12], [item.sourceContribution for item in result])
        self.assertIn("has permitido", result[0].text)

    def test_suppresses_personalization_location_and_internal_components_without_permission(self) -> None:
        contributions = [
            ScoreContributionLike(component="affinity", value=1, contribution=0.3),
            ScoreContributionLike(component="proximity", value=1, contribution=0.15),
            ScoreContributionLike(component="conversion", value=1, contribution=0.2),
            ScoreContributionLike(component="availability", value=0.1, contribution=0.015),
        ]
        result = self.builder.build_score("en", contributions, ExplanationPermissions())
        self.assertEqual([], result)

    def test_fallback_uses_only_applied_evidence_and_never_invents_contribution(self) -> None:
        evidence = [
            FallbackEvidence(
                component="contextualPopularity", value=0.9, applied=True,
                priority=1, sampleCount=20,
            ),
            FallbackEvidence(
                component="availability", value=0.8, applied=True, priority=2,
            ),
            FallbackEvidence(
                component="rating", value=1.0, applied=False, priority=3, sampleCount=1,
            ),
        ]
        result = self.builder.build_fallback("en", evidence, ExplanationPermissions())
        self.assertEqual(["POPULAR_IN_CONTEXT", "GOOD_AVAILABILITY"], [item.code for item in result])
        self.assertTrue(all(item.sourceContribution is None for item in result))
        self.assertTrue(all(item.locale == "en" for item in result))


if __name__ == "__main__":
    unittest.main()
