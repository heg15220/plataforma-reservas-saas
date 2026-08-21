"""Pruebas de integridad y cobertura del linaje MLOps end-to-end."""

from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path

from reserly_demand_engine.lineage import LineageManifest


ROOT = Path(__file__).parents[1]
REPOSITORY_ROOT = ROOT.parents[1]
MANIFEST_PATH = ROOT / "lineage" / "end-to-end-lineage.v1.json"


class LineageManifestTests(unittest.TestCase):
    """Impide romper la cadena entre datos, ejecución, modelo y promoción."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.raw = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
        cls.manifest = LineageManifest.model_validate(cls.raw)

    def artifact(self, values: dict[str, object], artifact_type: str) -> dict[str, object]:
        return next(
            artifact
            for artifact in values["artifacts"]  # type: ignore[union-attr]
            if artifact["artifactType"] == artifact_type
        )

    def test_manifest_covers_all_eight_versioned_asset_types(self) -> None:
        self.assertEqual(
            {
                "dataset",
                "featureSet",
                "ontology",
                "embedding",
                "configuration",
                "model",
                "experiment",
                "promotionDecision",
            },
            {artifact.artifactType for artifact in self.manifest.artifacts},
        )

    def test_every_repository_artifact_matches_its_sha256(self) -> None:
        self.manifest.verify_repository_artifacts(REPOSITORY_ROOT)

    def test_mlflow_tags_include_manifest_and_every_asset_digest(self) -> None:
        tags = self.manifest.mlflow_tags()
        self.assertEqual(19, len(tags))
        self.assertEqual(self.manifest.digest(), tags["reserly.lineage.manifestSha256"])
        for artifact in self.manifest.artifacts:
            self.assertEqual(
                artifact.sha256, tags[f"reserly.lineage.{artifact.artifactType}.sha256"]
            )

    def test_parent_digest_mismatch_fails_closed(self) -> None:
        values = copy.deepcopy(self.raw)
        model = self.artifact(values, "model")
        model["parents"][0]["sha256"] = "0" * 64  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, "LINEAGE_PARENT_INTEGRITY_MISMATCH"):
            LineageManifest.model_validate(values)

    def test_missing_required_parent_type_fails_closed(self) -> None:
        values = copy.deepcopy(self.raw)
        promotion = self.artifact(values, "promotionDecision")
        promotion["parents"] = [
            parent
            for parent in promotion["parents"]  # type: ignore[union-attr]
            if parent["artifactId"] != "experiment-personal-care-ranking-v1"
        ]
        with self.assertRaisesRegex(ValueError, "LINEAGE_REQUIRED_PARENT_MISSING:promotionDecision"):
            LineageManifest.model_validate(values)

    def test_cycle_is_rejected_even_when_references_have_valid_digests(self) -> None:
        values = copy.deepcopy(self.raw)
        model = self.artifact(values, "model")
        promotion = self.artifact(values, "promotionDecision")
        model["parents"].append(  # type: ignore[union-attr]
            {
                "artifactId": promotion["artifactId"],
                "version": promotion["version"],
                "sha256": promotion["sha256"],
            }
        )
        with self.assertRaisesRegex(ValueError, "LINEAGE_CYCLE_DETECTED"):
            LineageManifest.model_validate(values)

    def test_approved_promotion_requires_actor_and_status_transition(self) -> None:
        values = copy.deepcopy(self.raw)
        promotion = self.artifact(values, "promotionDecision")
        promotion["decision"] = "approved"
        with self.assertRaisesRegex(ValueError, "LINEAGE_APPROVED_PROMOTION_INVALID"):
            LineageManifest.model_validate(values)

    def test_repository_uri_cannot_escape_the_selected_root(self) -> None:
        values = copy.deepcopy(self.raw)
        dataset = self.artifact(values, "dataset")
        dataset["uri"] = "repo://../outside.json"
        manifest = LineageManifest.model_validate(values)
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "LINEAGE_REPOSITORY_URI_INVALID"):
                manifest.verify_repository_artifacts(Path(directory))


if __name__ == "__main__":
    unittest.main()
