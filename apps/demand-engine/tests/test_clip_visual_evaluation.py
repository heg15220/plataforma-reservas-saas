"""Pruebas de CLIP auxiliar, allowlist visual, privacidad y revisión humana."""

from __future__ import annotations

import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

from reserly_demand_engine.clip_visual_evaluation import (
    ClipVisualEvaluationRequest,
    ClipVisualEvaluator,
    ClipVisualManifest,
    ClipVisualPolicy,
)


ROOT = Path(__file__).resolve().parents[1]
MODEL_REVISION = "fbf5e647b25f3514e526849b05cc0196b206d822"


class ClipVisualEvaluationTests(unittest.TestCase):
    """Asegura que el evaluador no convierte similitud visual en verdad ni atributo sensible."""

    def setUp(self) -> None:
        self.now = datetime(2026, 8, 21, 10, 0, tzinfo=UTC)
        self.manifest = ClipVisualManifest.load(
            ROOT / "models" / "clip-vit-b32-visual-evidence.v1.json"
        )
        self.evaluator = ClipVisualEvaluator(
            ClipVisualPolicy.load(ROOT / "policies" / "clip-visual-evaluation.v1.json"),
            self.manifest,
        )

    def _vector(self, index: int, sign: float = 1.0, revision: str = MODEL_REVISION) -> dict:
        values = [0.0] * 512
        values[index] = sign
        return {
            "modelKey": "clip-vit-b32-visual-evidence-v1",
            "modelRevision": revision,
            "values": values,
        }

    def _request(
        self,
        *,
        production: bool,
        people_at: set[int] | None = None,
        requested: list[str] | None = None,
        revision: str = MODEL_REVISION,
    ) -> ClipVisualEvaluationRequest:
        codes = requested or ["modernStyle", "naturalLight"]
        prompts = [
            {
                "attributeCode": code,
                "positive": self._vector(index, revision=revision),
                "negative": self._vector(index, -1.0, revision),
            }
            for index, code in enumerate(codes)
        ]
        images = []
        for index in range(20):
            code_index = index % len(codes)
            images.append(
                {
                    "imageId": str(uuid4()),
                    "imageSha256": f"{index + 1:064x}",
                    "venueId": str(uuid4()),
                    "capturedAt": (self.now - timedelta(days=1)).isoformat(),
                    "venueAuthorized": True,
                    "metadataStripped": True,
                    "peopleDetected": index in (people_at or set()),
                    "embedding": self._vector(code_index, revision=revision),
                    "humanVerifiedAttributes": [codes[code_index]],
                }
            )
        return ClipVisualEvaluationRequest.model_validate(
            {
                "requestId": str(uuid4()),
                "schemaVersion": 1,
                "occurredAt": self.now.isoformat(),
                "locale": "es",
                "policyVersion": "clip-visual-evaluation-v1",
                "datasetVersion": "clip-fixture-v1",
                "productionEvidence": production,
                "containsPersonalData": False,
                "requestedAttributeCodes": codes,
                "promptPairs": prompts,
                "images": images,
            }
        )

    def test_quality_passes_but_synthetic_evidence_cannot_enter_review(self) -> None:
        result = self.evaluator.evaluate(self._request(production=False))
        self.assertTrue(result.qualityGatesPassed)
        self.assertEqual(1.0, result.macroPrecision)
        self.assertEqual(1.0, result.macroRecall)
        self.assertFalse(result.auxiliaryEvidenceReviewAllowed)
        self.assertEqual([], result.evidences)
        self.assertFalse(result.automaticProfileMutationAllowed)

    def test_productive_quality_emits_only_auxiliary_review_candidates(self) -> None:
        result = self.evaluator.evaluate(self._request(production=True))
        self.assertTrue(result.auxiliaryEvidenceReviewAllowed)
        self.assertEqual(20, len(result.evidences))
        self.assertTrue(all(evidence.humanReviewRequired for evidence in result.evidences))
        self.assertTrue(all(evidence.source == "imageAuxiliary" for evidence in result.evidences))
        self.assertEqual(0, result.prohibitedClaimsEmitted)

    def test_people_images_are_suppressed_and_cannot_satisfy_minimum_sample(self) -> None:
        result = self.evaluator.evaluate(
            self._request(production=True, people_at={0, 1, 2, 3, 4})
        )
        self.assertEqual(15, result.evaluatedImageCount)
        self.assertEqual(5, result.suppressedPeopleImageCount)
        self.assertFalse(result.qualityGatesPassed)
        self.assertEqual([], result.evidences)

    def test_non_visual_or_sensitive_claim_is_rejected_by_allowlist(self) -> None:
        with self.assertRaisesRegex(ValueError, "CLIP_ATTRIBUTE_NOT_ALLOWED"):
            self.evaluator.evaluate(
                self._request(production=True, requested=["cleanliness"])
            )

    def test_vector_revision_drift_fails_closed(self) -> None:
        with self.assertRaisesRegex(ValueError, "CLIP_VECTOR_VERSION_MISMATCH"):
            self.evaluator.evaluate(
                self._request(production=True, revision="0" * 40)
            )

    def test_manifest_forbids_sensitive_and_unverifiable_claims(self) -> None:
        self.assertTrue(
            {"health", "identity", "safety", "cleanliness", "tranquility"}
            <= set(self.manifest.prohibitedClaims)
        )
        self.assertEqual(
            {"modernStyle", "classicStyle", "naturalLight", "dedicatedWaitingArea"},
            set(self.manifest.allowedAttributeCodes),
        )


if __name__ == "__main__":
    unittest.main()
