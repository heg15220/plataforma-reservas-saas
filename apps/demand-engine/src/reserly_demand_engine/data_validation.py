"""Puerta MLOps previa a entrenamiento y promoción basada en perfiles agregados.

El módulo no recibe ni persiste filas. Consume evidencia producida dentro del perímetro de datos,
comprueba seis familias de riesgo y emite una decisión content-addressed apta para Prefect/MLflow.
Un fallo, una métrica ausente o una versión no alineada bloquean la operación solicitada.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Literal

from pydantic import Field, field_validator, model_validator

from .contracts import StrictContract, Version


class ColumnRule(StrictContract):
    """Esquema y límite de ausencia de una columna gobernada."""

    dataType: Literal["boolean", "integer", "number", "string", "datetime"]
    maximumNullRate: float = Field(ge=0.0, le=1.0)


class DataValidationPolicy(StrictContract):
    """Umbrales versionados; cualquier cambio material requiere otra versión."""

    schemaVersion: Literal[1]
    policyVersion: Version
    acceptedDatasetSchemaVersions: list[Version] = Field(min_length=1)
    requiredColumns: dict[Version, ColumnRule] = Field(min_length=1)
    minimumRows: int = Field(ge=1)
    maximumPopulationStabilityIndex: float = Field(gt=0.0)
    maximumDirectIdentifierMatches: Literal[0]
    maximumLeakageCorrelation: float = Field(gt=0.0, le=1.0)
    minimumBiasSliceRows: int = Field(ge=1)
    maximumPositiveRateGap: float = Field(ge=0.0, le=1.0)
    maximumFalseNegativeRateGap: float = Field(ge=0.0, le=1.0)
    prohibitedColumnTokens: list[Version] = Field(min_length=1)

    @classmethod
    def load(cls, path: Path) -> "DataValidationPolicy":
        """Carga una política estricta sin defaults dependientes del entorno."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


class ColumnProfile(StrictContract):
    """Perfil agregado de columna, sin muestras, cuantiles individuales ni categorías libres."""

    column: Version
    dataType: Literal["boolean", "integer", "number", "string", "datetime"]
    nullRate: float = Field(ge=0.0, le=1.0)
    uniqueRate: float = Field(ge=0.0, le=1.0)
    baselineDistribution: list[float] = Field(min_length=2, max_length=100)
    observedDistribution: list[float] = Field(min_length=2, max_length=100)
    directIdentifierMatches: int = Field(ge=0)
    availableAfterPredictionCount: int = Field(ge=0)
    absoluteTargetCorrelation: float | None = Field(default=None, ge=0.0, le=1.0)

    @model_validator(mode="after")
    def validate_distributions(self) -> "ColumnProfile":
        """Exige histogramas comparables, finitos, no negativos y con masa."""
        values = self.baselineDistribution + self.observedDistribution
        if (
            len(self.baselineDistribution) != len(self.observedDistribution)
            or any(not math.isfinite(value) or value < 0 for value in values)
            or sum(self.baselineDistribution) <= 0
            or sum(self.observedDistribution) <= 0
        ):
            raise ValueError("DATA_VALIDATION_DISTRIBUTION_INVALID")
        return self


class BiasSliceProfile(StrictContract):
    """Cohorte operacional permitida; nunca incluye una identidad o atributo sensible inferido."""

    sliceCode: Version
    rows: int = Field(ge=0)
    positiveRate: float = Field(ge=0.0, le=1.0)
    falseNegativeRate: float = Field(ge=0.0, le=1.0)


class DataValidationEvidence(StrictContract):
    """Evidencia agregada y reproducible generada antes de una operación MLOps."""

    evidenceVersion: Literal[1]
    stage: Literal["preTraining", "prePromotion"]
    policyVersion: Version
    datasetVersion: Version
    datasetSchemaVersion: Version
    baselineDatasetVersion: Version
    lineageManifestSha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    rowCount: int = Field(ge=0)
    duplicateRowCount: int = Field(ge=0)
    columns: list[ColumnProfile] = Field(min_length=1, max_length=256)
    biasSlices: list[BiasSliceProfile] = Field(min_length=2, max_length=64)

    @field_validator("columns")
    @classmethod
    def unique_columns(cls, values: list[ColumnProfile]) -> list[ColumnProfile]:
        if len({value.column for value in values}) != len(values):
            raise ValueError("DATA_VALIDATION_COLUMN_DUPLICATED")
        return values

    def digest(self) -> str:
        """Identifica exactamente la evidencia sin incorporar datos fuente."""
        payload = json.dumps(
            self.model_dump(mode="json"), sort_keys=True, separators=(",", ":")
        ).encode("utf-8")
        return hashlib.sha256(payload).hexdigest()


@dataclass(frozen=True, slots=True)
class ValidationCheck:
    """Resultado estable y sin contenido sensible de un control."""

    family: Literal["schema", "quality", "distribution", "pii", "leakage", "bias"]
    code: str
    passed: bool
    observed: float | int | str
    required: float | int | str


@dataclass(frozen=True, slots=True)
class DataValidationDecision:
    """Token de admisión: solo autoriza la etapa exacta y el dataset evaluado."""

    stage: str
    policyVersion: str
    datasetVersion: str
    evidenceSha256: str
    allowed: bool
    checks: tuple[ValidationCheck, ...]

    def require(self, *, stage: str, dataset_version: str) -> None:
        """Bloquea el job si la decisión falla o se intenta reutilizar en otra operación."""
        if not self.allowed or self.stage != stage or self.datasetVersion != dataset_version:
            raise ValueError("DATA_VALIDATION_ADMISSION_DENIED")

    def as_dict(self) -> dict[str, object]:
        return {
            "stage": self.stage,
            "policyVersion": self.policyVersion,
            "datasetVersion": self.datasetVersion,
            "evidenceSha256": self.evidenceSha256,
            "allowed": self.allowed,
            "checks": [asdict(check) for check in self.checks],
        }


def _psi(baseline: list[float], observed: list[float]) -> float:
    """Calcula PSI con suavizado determinista para celdas vacías."""
    epsilon = 1e-9
    baseline_total, observed_total = sum(baseline), sum(observed)
    return sum(
        (current - reference) * math.log(current / reference)
        for reference_value, current_value in zip(baseline, observed, strict=True)
        for reference, current in [
            (max(reference_value / baseline_total, epsilon), max(current_value / observed_total, epsilon))
        ]
    )


def evaluate_data_validation(
    policy: DataValidationPolicy, evidence: DataValidationEvidence
) -> DataValidationDecision:
    """Evalúa las seis familias; evidencia incompleta o desalineada falla cerrado."""
    if evidence.policyVersion != policy.policyVersion:
        raise ValueError("DATA_VALIDATION_POLICY_VERSION_MISMATCH")
    profiles = {profile.column: profile for profile in evidence.columns}
    checks: list[ValidationCheck] = []

    checks.append(ValidationCheck(
        "schema", "datasetSchemaVersion",
        evidence.datasetSchemaVersion in policy.acceptedDatasetSchemaVersions,
        evidence.datasetSchemaVersion, ",".join(policy.acceptedDatasetSchemaVersions),
    ))
    for name, rule in policy.requiredColumns.items():
        profile = profiles.get(name)
        checks.append(ValidationCheck(
            "schema", f"required:{name}", profile is not None and profile.dataType == rule.dataType,
            "missing" if profile is None else profile.dataType, rule.dataType,
        ))

    checks.append(ValidationCheck(
        "quality", "minimumRows", evidence.rowCount >= policy.minimumRows,
        evidence.rowCount, policy.minimumRows,
    ))
    checks.append(ValidationCheck(
        "quality", "duplicateRows", evidence.duplicateRowCount == 0,
        evidence.duplicateRowCount, 0,
    ))
    for name, rule in policy.requiredColumns.items():
        if name in profiles:
            checks.append(ValidationCheck(
                "quality", f"nullRate:{name}", profiles[name].nullRate <= rule.maximumNullRate,
                profiles[name].nullRate, rule.maximumNullRate,
            ))

    for profile in evidence.columns:
        psi = _psi(profile.baselineDistribution, profile.observedDistribution)
        checks.append(ValidationCheck(
            "distribution", f"psi:{profile.column}",
            psi <= policy.maximumPopulationStabilityIndex,
            round(psi, 8), policy.maximumPopulationStabilityIndex,
        ))

    identifier_matches = sum(profile.directIdentifierMatches for profile in evidence.columns)
    prohibited_names = sorted(
        profile.column for profile in evidence.columns
        if any(token.lower() in profile.column.lower() for token in policy.prohibitedColumnTokens)
    )
    checks.extend((
        ValidationCheck("pii", "directIdentifierMatches",
                        identifier_matches <= policy.maximumDirectIdentifierMatches,
                        identifier_matches, policy.maximumDirectIdentifierMatches),
        ValidationCheck("pii", "prohibitedColumnNames", not prohibited_names,
                        ",".join(prohibited_names) or "none", "none"),
    ))

    late = sum(profile.availableAfterPredictionCount for profile in evidence.columns)
    excessive_correlations = sorted(
        profile.column for profile in evidence.columns
        if profile.absoluteTargetCorrelation is not None
        and profile.absoluteTargetCorrelation >= policy.maximumLeakageCorrelation
    )
    checks.extend((
        ValidationCheck("leakage", "pointInTimeAvailability", late == 0, late, 0),
        ValidationCheck("leakage", "targetProxyCorrelation", not excessive_correlations,
                        ",".join(excessive_correlations) or "none", "none"),
    ))

    valid_slices = all(item.rows >= policy.minimumBiasSliceRows for item in evidence.biasSlices)
    positive_gap = max(item.positiveRate for item in evidence.biasSlices) - min(
        item.positiveRate for item in evidence.biasSlices
    )
    false_negative_gap = max(item.falseNegativeRate for item in evidence.biasSlices) - min(
        item.falseNegativeRate for item in evidence.biasSlices
    )
    checks.extend((
        ValidationCheck("bias", "minimumSliceRows", valid_slices,
                        min(item.rows for item in evidence.biasSlices), policy.minimumBiasSliceRows),
        ValidationCheck("bias", "positiveRateGap", positive_gap <= policy.maximumPositiveRateGap,
                        round(positive_gap, 8), policy.maximumPositiveRateGap),
        ValidationCheck("bias", "falseNegativeRateGap",
                        false_negative_gap <= policy.maximumFalseNegativeRateGap,
                        round(false_negative_gap, 8), policy.maximumFalseNegativeRateGap),
    ))
    return DataValidationDecision(
        stage=evidence.stage,
        policyVersion=policy.policyVersion,
        datasetVersion=evidence.datasetVersion,
        evidenceSha256=evidence.digest(),
        allowed=all(check.passed for check in checks),
        checks=tuple(checks),
    )


def run() -> None:
    """CLI para Prefect/CI; salida agregada y código 1 cuando la admisión se bloquea."""
    parser = argparse.ArgumentParser(description="Valida evidencia de datos previa a MLOps")
    parser.add_argument("evidence", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    policy = DataValidationPolicy.load(root / "policies/data-validation.v1.json")
    evidence = DataValidationEvidence.model_validate_json(args.evidence.read_text(encoding="utf-8"))
    decision = evaluate_data_validation(policy, evidence)
    print(json.dumps(decision.as_dict(), indent=2, sort_keys=True))
    if not decision.allowed:
        raise SystemExit(1)


if __name__ == "__main__":
    run()
