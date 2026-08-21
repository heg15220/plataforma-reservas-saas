"""Pruebas de routing, comparación, rollback, promoción y fallback del rollout."""

from __future__ import annotations

import unittest
from pathlib import Path
from uuid import UUID, uuid4

from reserly_demand_engine.model_rollout import (
    DeploymentState,
    InMemoryAliasClient,
    InMemoryLockProvider,
    PromotionApproval,
    RolloutMetrics,
    RolloutObservation,
    RolloutPolicy,
    ShadowApproval,
    activate_rules_fallback,
    begin_shadow,
    evaluate_rollout,
    execute_automatic_rollback,
    promote_champion,
    route_request,
)


ROOT = Path(__file__).parents[1]


class ModelRolloutTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = RolloutPolicy.load(ROOT / "policies/model-rollout.v1.json")

    def state(self, phase="shadow", traffic=0, revision=1, decision="candidate-registered"):
        return DeploymentState(
            stateVersion=1, policyVersion=self.policy.policyVersion, modelName="ranking-model",
            revision=revision, phase=phase, candidateVersion="model-v2",
            championVersion="model-v1", previousChampionVersion=None,
            canaryTrafficBasisPoints=traffic, rulesFallbackActive=False,
            killSwitchActive=False, lastDecisionCode=decision,
        )

    def metrics(self, **updates):
        values = dict(
            qualityScore=0.82, errorRate=0.004, latencyP95Ms=120.0, fallbackRate=0.005,
            calibrationError=0.04, biasGap=0.05, driftPsi=0.08,
            privacyViolationCount=0, hardConstraintViolationCount=0,
        )
        values.update(updates)
        return RolloutMetrics(**values)

    def observation(self, state, candidate=None, champion=None, requests=None):
        return RolloutObservation(
            observationVersion=1, policyVersion=self.policy.policyVersion, phase=state.phase,
            candidateVersion=state.candidateVersion, championVersion=state.championVersion,
            requests=requests if requests is not None else (
                self.policy.minimumShadowRequests if state.phase == "shadow"
                else self.policy.minimumCanaryRequestsPerStep
            ),
            trafficBasisPoints=state.canaryTrafficBasisPoints,
            candidate=candidate or self.metrics(),
            champion=champion or self.metrics(qualityScore=0.81, errorRate=0.003, latencyP95Ms=110),
        )

    def test_shadow_never_serves_candidate_but_emits_mirror_version(self):
        registry = InMemoryAliasClient("model-v1")
        state = begin_shadow(
            self.policy,
            ShadowApproval(
                approvalVersion=1, policyVersion=self.policy.policyVersion,
                modelName="ranking-model", candidateVersion="model-v2",
                expectedChampionVersion="model-v1", approvedBy="ml-governance-reviewer",
                dataValidationEvidenceSha256="a" * 64, automaticApproval=False,
            ),
            registry, InMemoryLockProvider(),
        )
        self.assertEqual("model-v2", registry.aliases["shadow"])
        route = route_request(self.policy, state, uuid4())
        self.assertEqual("shadow", route.mode)
        self.assertEqual("model-v1", route.modelVersion)
        self.assertEqual("model-v2", route.shadowVersion)

    def test_canary_routing_is_deterministic_and_respects_exact_budget(self):
        state = self.state("canary", 1000)
        request_ids = [UUID(int=index) for index in range(10_000)]
        first = [route_request(self.policy, state, item).mode for item in request_ids]
        second = [route_request(self.policy, state, item).mode for item in request_ids]
        self.assertEqual(first, second)
        selected = first.count("canary")
        self.assertGreater(selected, 850)
        self.assertLess(selected, 1150)

    def test_passing_shadow_and_canary_advance_one_step_only(self):
        shadow = self.state()
        canary = evaluate_rollout(self.policy, shadow, self.observation(shadow))
        self.assertEqual("canary", canary.state.phase)
        self.assertEqual(100, canary.state.canaryTrafficBasisPoints)
        next_step = evaluate_rollout(
            self.policy, canary.state, self.observation(canary.state)
        )
        self.assertEqual(500, next_step.state.canaryTrafficBasisPoints)
        self.assertFalse(next_step.reviewRequired)

    def test_each_safety_family_triggers_automatic_rollback(self):
        cases = {
            "privacy": {"privacyViolationCount": 1},
            "hardConstraints": {"hardConstraintViolationCount": 1},
            "quality": {"qualityScore": 0.5},
            "errorRate": {"errorRate": 0.02},
            "latency": {"latencyP95Ms": 300},
            "fallbackRate": {"fallbackRate": 0.1},
            "calibration": {"calibrationError": 0.2},
            "bias": {"biasGap": 0.3},
            "drift": {"driftPsi": 0.4},
        }
        for expected, updates in cases.items():
            with self.subTest(expected=expected):
                state = self.state("canary", 1000)
                decision = evaluate_rollout(
                    self.policy, state, self.observation(state, candidate=self.metrics(**updates))
                )
                self.assertTrue(decision.automaticRollback)
                self.assertIn(expected, decision.failedChecks)
                self.assertEqual("champion", decision.state.phase)
                self.assertIsNone(decision.state.candidateVersion)

                registry = InMemoryAliasClient("model-v2")
                restored = execute_automatic_rollback(
                    decision.state, registry, InMemoryLockProvider()
                )
                self.assertEqual("model-v1", registry.aliases["champion"])
                self.assertFalse(restored.rulesFallbackActive)

    def test_final_canary_requires_human_and_atomic_alias_swap_keeps_previous(self):
        state = self.state("canary", 10_000)
        decision = evaluate_rollout(self.policy, state, self.observation(state))
        self.assertTrue(decision.reviewRequired)
        self.assertEqual("canary", decision.state.phase)
        registry = InMemoryAliasClient("model-v1")
        promoted = promote_champion(
            self.policy, decision.state,
            PromotionApproval(
                approvalVersion=1, policyVersion=self.policy.policyVersion,
                modelName="ranking-model", candidateVersion="model-v2",
                expectedChampionVersion="model-v1", expectedStateRevision=decision.state.revision,
                approvedBy="ml-governance-reviewer", promotionDecisionSha256="a" * 64,
                dataValidationEvidenceSha256="b" * 64, automaticApproval=False,
            ),
            registry, InMemoryLockProvider(),
        )
        self.assertEqual("model-v2", registry.aliases["champion"])
        self.assertEqual("model-v1", registry.aliases["previous-champion"])
        self.assertEqual("model-v1", promoted.previousChampionVersion)

    def test_compare_and_swap_rejects_stale_champion(self):
        state = self.state("canary", 10_000, decision="promotion-review-required")
        approval = PromotionApproval(
            approvalVersion=1, policyVersion=self.policy.policyVersion, modelName="ranking-model",
            candidateVersion="model-v2", expectedChampionVersion="model-v1",
            expectedStateRevision=state.revision, approvedBy="ml-governance-reviewer",
            promotionDecisionSha256="a" * 64, dataValidationEvidenceSha256="b" * 64,
            automaticApproval=False,
        )
        with self.assertRaisesRegex(ValueError, "ROLLOUT_CHAMPION_COMPARE_AND_SWAP_FAILED"):
            promote_champion(
                self.policy, state, approval, InMemoryAliasClient("model-v3"), InMemoryLockProvider()
            )

    def test_partial_registry_failure_restores_champion(self):
        state = self.state("canary", 10_000, decision="promotion-review-required")
        registry = InMemoryAliasClient("model-v1")
        registry.fail_alias_once = "champion"
        approval = PromotionApproval(
            approvalVersion=1, policyVersion=self.policy.policyVersion, modelName="ranking-model",
            candidateVersion="model-v2", expectedChampionVersion="model-v1",
            expectedStateRevision=state.revision, approvedBy="ml-governance-reviewer",
            promotionDecisionSha256="a" * 64, dataValidationEvidenceSha256="b" * 64,
            automaticApproval=False,
        )
        with self.assertRaisesRegex(RuntimeError, "REGISTRY_WRITE_FAILED"):
            promote_champion(self.policy, state, approval, registry, InMemoryLockProvider())
        self.assertEqual("model-v1", registry.aliases["champion"])

    def test_kill_switch_registry_failure_and_missing_champion_fall_back_to_rules(self):
        state = self.state().model_copy(update={"killSwitchActive": True})
        route = route_request(self.policy, state, uuid4())
        self.assertEqual("rulesFallback", route.mode)
        self.assertEqual("fallback-mvp-v1", route.fallbackPolicyVersion)

        degraded = activate_rules_fallback(self.state(), "registry-unavailable")
        self.assertEqual("fallback", degraded.phase)
        self.assertTrue(degraded.rulesFallbackActive)
        self.assertEqual("rulesFallback", route_request(self.policy, degraded, uuid4()).mode)

        missing = self.state().model_copy(update={"championVersion": None})
        self.assertEqual("rulesFallback", route_request(self.policy, missing, uuid4()).mode)

        rollback = self.state("canary", 1000).model_copy(update={
            "phase": "champion", "candidateVersion": None, "canaryTrafficBasisPoints": 0,
            "lastDecisionCode": "automatic-rollback",
        })
        unavailable = InMemoryAliasClient("model-v2")
        unavailable.fail_alias_once = "champion"
        failed = execute_automatic_rollback(rollback, unavailable, InMemoryLockProvider())
        self.assertEqual("fallback", failed.phase)
        self.assertTrue(failed.rulesFallbackActive)


if __name__ == "__main__":
    unittest.main()
