"""Pruebas de las seis puertas previas a entrenamiento y promoción."""

from __future__ import annotations

import unittest
from pathlib import Path

from reserly_demand_engine.data_validation import (
    BiasSliceProfile,
    ColumnProfile,
    DataValidationEvidence,
    DataValidationPolicy,
    evaluate_data_validation,
)


ROOT = Path(__file__).parents[1]


class DataValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = DataValidationPolicy.load(ROOT / "policies/data-validation.v1.json")

    def evidence(self, stage: str = "preTraining") -> DataValidationEvidence:
        types = {
            "observationId": "string", "occurredAt": "datetime",
            "outcomeObservedAt": "datetime", "locale": "string", "label": "boolean",
        }
        columns = [
            ColumnProfile(
                column=name, dataType=data_type, nullRate=0.0, uniqueRate=0.5,
                baselineDistribution=[50, 50], observedDistribution=[49, 51],
                directIdentifierMatches=0, availableAfterPredictionCount=0,
                absoluteTargetCorrelation=None if name == "label" else 0.2,
            )
            for name, data_type in types.items()
        ]
        return DataValidationEvidence(
            evidenceVersion=1, stage=stage, policyVersion=self.policy.policyVersion,
            datasetVersion="dataset-v1", datasetSchemaVersion="demand-training-schema-v1",
            baselineDatasetVersion="dataset-baseline-v1", lineageManifestSha256="a" * 64,
            rowCount=1000, duplicateRowCount=0, columns=columns,
            biasSlices=[
                BiasSliceProfile(sliceCode="locale-es", rows=500, positiveRate=0.40, falseNegativeRate=0.10),
                BiasSliceProfile(sliceCode="locale-en", rows=500, positiveRate=0.43, falseNegativeRate=0.12),
            ],
        )

    def test_complete_evidence_admits_only_its_exact_stage_and_dataset(self) -> None:
        decision = evaluate_data_validation(self.policy, self.evidence())
        self.assertTrue(decision.allowed)
        self.assertEqual(6, len({check.family for check in decision.checks}))
        decision.require(stage="preTraining", dataset_version="dataset-v1")
        with self.assertRaisesRegex(ValueError, "DATA_VALIDATION_ADMISSION_DENIED"):
            decision.require(stage="prePromotion", dataset_version="dataset-v1")

    def test_schema_quality_and_distribution_fail_closed(self) -> None:
        evidence = self.evidence()
        bad = evidence.columns[0].model_copy(update={
            "dataType": "integer", "nullRate": 0.2,
            "observedDistribution": [1, 99],
        })
        decision = evaluate_data_validation(
            self.policy, evidence.model_copy(update={"rowCount": 5, "duplicateRowCount": 1,
                                                     "columns": [bad, *evidence.columns[1:]]})
        )
        self.assertFalse(decision.allowed)
        self.assertTrue(any(not c.passed and c.family == "schema" for c in decision.checks))
        self.assertTrue(any(not c.passed and c.family == "quality" for c in decision.checks))
        self.assertTrue(any(not c.passed and c.family == "distribution" for c in decision.checks))

    def test_pii_and_leakage_are_blocking_without_echoing_values(self) -> None:
        evidence = self.evidence()
        leaked = evidence.columns[0].model_copy(update={
            "column": "emailHash", "directIdentifierMatches": 2,
            "availableAfterPredictionCount": 1, "absoluteTargetCorrelation": 0.999,
        })
        decision = evaluate_data_validation(
            self.policy, evidence.model_copy(update={"columns": [leaked, *evidence.columns[1:]]})
        )
        self.assertFalse(decision.allowed)
        self.assertTrue(any(not c.passed and c.family == "pii" for c in decision.checks))
        self.assertTrue(any(not c.passed and c.family == "leakage" for c in decision.checks))
        self.assertNotIn("@", str(decision.as_dict()))

    def test_bias_requires_representative_slices_and_bounded_gaps(self) -> None:
        evidence = self.evidence("prePromotion")
        decision = evaluate_data_validation(self.policy, evidence.model_copy(update={"biasSlices": [
            BiasSliceProfile(sliceCode="locale-es", rows=900, positiveRate=0.8, falseNegativeRate=0.02),
            BiasSliceProfile(sliceCode="locale-en", rows=10, positiveRate=0.2, falseNegativeRate=0.4),
        ]}))
        self.assertFalse(decision.allowed)
        self.assertEqual(3, sum(not c.passed for c in decision.checks if c.family == "bias"))

    def test_policy_or_distribution_contract_mismatch_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "DATA_VALIDATION_POLICY_VERSION_MISMATCH"):
            evaluate_data_validation(
                self.policy, self.evidence().model_copy(update={"policyVersion": "other-v1"})
            )
        with self.assertRaisesRegex(ValueError, "DATA_VALIDATION_DISTRIBUTION_INVALID"):
            values = self.evidence().columns[0].model_dump()
            values["observedDistribution"] = [1, 2, 3]
            ColumnProfile.model_validate(values)


if __name__ == "__main__":
    unittest.main()
