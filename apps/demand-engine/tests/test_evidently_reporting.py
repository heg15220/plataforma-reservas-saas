"""Pruebas de integración real Evidently y subordinación al gate de datos."""

from __future__ import annotations

import json
import tempfile
import unittest
from datetime import UTC, datetime
from pathlib import Path

import pandas as pd

from reserly_demand_engine.data_validation import (
    BiasSliceProfile,
    ColumnProfile,
    DataValidationEvidence,
    DataValidationPolicy,
)
from reserly_demand_engine.evidently_reporting import (
    EvidentlyReportPolicy,
    generate_evidently_report,
    write_evidently_artifacts,
)


ROOT = Path(__file__).parents[1]


class EvidentlyReportingTests(unittest.TestCase):
    """Protege privacidad, artefactos y falta de autoridad de la herramienta auxiliar."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.report_policy = EvidentlyReportPolicy.load(
            ROOT / "policies/evidently-report.v1.json"
        )
        cls.validation_policy = DataValidationPolicy.load(
            ROOT / "policies/data-validation.v1.json"
        )

    def evidence(self, *, drift: bool = False) -> DataValidationEvidence:
        """Construye evidencia autoritativa independiente del dataframe entregado a Evidently."""
        types = {
            "observationId": "string",
            "occurredAt": "datetime",
            "outcomeObservedAt": "datetime",
            "locale": "string",
            "label": "boolean",
        }
        columns = [
            ColumnProfile(
                column=name,
                dataType=data_type,
                nullRate=0,
                uniqueRate=0.5,
                baselineDistribution=[50, 50],
                observedDistribution=[1, 99] if drift and name == "locale" else [50, 50],
                directIdentifierMatches=0,
                availableAfterPredictionCount=0,
                absoluteTargetCorrelation=None if name == "label" else 0.2,
            )
            for name, data_type in types.items()
        ]
        return DataValidationEvidence(
            evidenceVersion=1,
            stage="prePromotion",
            policyVersion=self.validation_policy.policyVersion,
            datasetVersion="dataset-current-v1",
            datasetSchemaVersion="demand-training-schema-v1",
            baselineDatasetVersion="dataset-reference-v1",
            lineageManifestSha256="a" * 64,
            rowCount=100,
            duplicateRowCount=0,
            columns=columns,
            biasSlices=[
                BiasSliceProfile(
                    sliceCode="locale-es",
                    rows=50,
                    positiveRate=0.4,
                    falseNegativeRate=0.1,
                ),
                BiasSliceProfile(
                    sliceCode="locale-en",
                    rows=50,
                    positiveRate=0.42,
                    falseNegativeRate=0.11,
                ),
            ],
        )

    @staticmethod
    def frame(*, shifted: bool = False) -> pd.DataFrame:
        """Crea una proyección sintética sin IDs, texto ni atributos sensibles."""
        if shifted:
            affinity = [0.95 + (index % 5) / 1000 for index in range(100)]
            locale = ["es"] * 95 + ["en"] * 5
        else:
            affinity = [0.2 + (index % 20) / 100 for index in range(100)]
            locale = ["es"] * 50 + ["en"] * 50
        return pd.DataFrame(
            {
                "affinity": affinity,
                "conversion": [0.3 + (index % 10) / 100 for index in range(100)],
                "locale": locale,
            }
        )

    def report(
        self, current: pd.DataFrame, reference: pd.DataFrame, *, evidence_drift: bool = False
    ):
        """Ejecuta el adapter real con tiempo fijo para aserciones reproducibles."""
        return generate_evidently_report(
            self.report_policy,
            self.validation_policy,
            self.evidence(drift=evidence_drift),
            current,
            reference,
            evaluated_at=datetime(2026, 8, 21, 12, tzinfo=UTC),
        )

    def test_real_report_is_advisory_and_never_authorizes_promotion(self) -> None:
        result = self.report(self.frame(), self.frame())

        self.assertEqual("0.7.21", result.evidentlyVersion)
        self.assertTrue(result.authoritativeDataGateAllowed)
        self.assertFalse(result.promotionAuthorized)
        self.assertFalse(result.advisory.reviewRequired)
        self.assertEqual(0, result.advisory.driftedColumns)
        self.assertIn("DriftedColumnsCount", result.rawReportJson)

    def test_evidently_drift_opens_review_but_does_not_override_gate(self) -> None:
        result = self.report(self.frame(shifted=True), self.frame())

        self.assertTrue(result.advisory.reviewRequired)
        self.assertGreater(result.advisory.driftedColumnShare, 0.3)
        self.assertTrue(result.authoritativeDataGateAllowed)
        self.assertFalse(result.promotionAuthorized)

    def test_authoritative_gate_denial_wins_even_when_evidently_is_stable(self) -> None:
        result = self.report(self.frame(), self.frame(), evidence_drift=True)

        self.assertFalse(result.authoritativeDataGateAllowed)
        self.assertFalse(result.promotionAuthorized)
        self.assertEqual(0, result.advisory.driftedColumns)

    def test_unknown_identifier_free_text_and_bad_categories_fail_closed(self) -> None:
        for column, values in (
            ("customerId", ["customer-secret"] * 100),
            ("reviewText", ["private review"] * 100),
            ("locale", ["fr"] * 100),
        ):
            current = self.frame()
            current[column] = values
            if column == "locale":
                current = current[["affinity", "conversion", "locale"]]
            with self.assertRaisesRegex(ValueError, "EVIDENTLY_CURRENT_"):
                self.report(current, self.frame())

    def test_artifacts_are_hashed_and_manifest_contains_no_rows(self) -> None:
        result = self.report(self.frame(), self.frame())
        with tempfile.TemporaryDirectory() as directory:
            manifest_path = write_evidently_artifacts(result, Path(directory))
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

            self.assertRegex(manifest["rawReportSha256"], r"^[a-f0-9]{64}$")
            self.assertRegex(manifest["htmlReportSha256"], r"^[a-f0-9]{64}$")
            serialized = json.dumps(manifest).lower()
            self.assertNotIn("customer-secret", serialized)
            self.assertNotIn("private review", serialized)
            self.assertNotIn("records", manifest)
            self.assertFalse(manifest["promotionAuthorized"])
            self.assertEqual(
                {"json", "html"},
                {path.suffix.removeprefix(".") for path in Path(directory).iterdir()
                 if "manifest" not in path.name},
            )


if __name__ == "__main__":
    unittest.main()
