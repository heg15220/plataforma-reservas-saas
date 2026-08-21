"""Informes Evidently complementarios, minimizados y subordinados al gate autoritativo.

La integración solo acepta proyecciones tabulares allowlisted sin identidades ni texto. Ejecuta
calidad y drift offline, exporta agregados content-addressed y conserva como única autoridad la
decisión `data-validation-v1`; Evidently nunca promueve, despliega ni modifica datos.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Literal

import evidently
import pandas as pd
from evidently import DataDefinition, Dataset, Report
from evidently.presets import DataDriftPreset, DataSummaryPreset
from pydantic import Field, model_validator

from .contracts import StrictContract, Version
from .data_validation import (
    DataValidationEvidence,
    DataValidationPolicy,
    evaluate_data_validation,
)


class EvidentlyColumnRule(StrictContract):
    """Tipo explícito y categorías cerradas de una columna apta para el informe."""

    kind: Literal["numerical", "categorical"]
    allowedValues: list[str] = Field(max_length=32)

    @model_validator(mode="after")
    def require_categories_only_for_categorical(self) -> "EvidentlyColumnRule":
        """Evita categorías libres y configuraciones ambiguas para columnas numéricas."""
        if self.kind == "categorical" and not self.allowedValues:
            raise ValueError("EVIDENTLY_CATEGORIES_REQUIRED")
        if self.kind == "numerical" and self.allowedValues:
            raise ValueError("EVIDENTLY_NUMERICAL_VALUES_FORBIDDEN")
        return self


class EvidentlyReportPolicy(StrictContract):
    """Contrato versionado de datos, límites, drift y autoridad de la integración."""

    schemaVersion: Literal[1]
    policyVersion: Version
    dataValidationPolicyVersion: Version
    driftMethod: Literal["psi"]
    maximumDriftedColumnShare: float = Field(gt=0.0, le=1.0)
    minimumRows: int = Field(ge=1)
    maximumRows: int = Field(ge=1)
    minimumColumns: int = Field(ge=1)
    maximumColumns: int = Field(ge=1)
    maximumNullRate: float = Field(ge=0.0, le=1.0)
    allowedColumns: dict[Version, EvidentlyColumnRule] = Field(min_length=1)
    prohibitedColumnTokens: list[Version] = Field(min_length=1)
    storeRawRows: Literal[False]
    automaticPromotionAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_bounds(self) -> "EvidentlyReportPolicy":
        """Exige intervalos coherentes y una allowlist compatible con el máximo declarado."""
        if (
            self.minimumRows > self.maximumRows
            or self.minimumColumns > self.maximumColumns
            or len(self.allowedColumns) > self.maximumColumns
        ):
            raise ValueError("EVIDENTLY_POLICY_BOUNDS_INVALID")
        return self

    @classmethod
    def load(cls, path: Path) -> "EvidentlyReportPolicy":
        """Carga la política sin defaults dependientes del entorno."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))


@dataclass(frozen=True, slots=True)
class EvidentlyAdvisory:
    """Resumen agregado para revisión; no es ni contiene un token de admisión."""

    driftedColumns: int
    driftedColumnShare: float
    testsTotal: int
    testsFailed: int
    reviewRequired: bool


@dataclass(frozen=True, slots=True)
class EvidentlyReportResult:
    """Resultado enlazado al gate de datos que declara explícitamente su falta de autoridad."""

    reportVersion: int
    policyVersion: str
    evidentlyVersion: str
    stage: str
    currentDatasetVersion: str
    referenceDatasetVersion: str
    dataValidationEvidenceSha256: str
    authoritativeDataGateAllowed: bool
    promotionAuthorized: bool
    evaluatedAt: str
    columns: tuple[str, ...]
    currentRows: int
    referenceRows: int
    advisory: EvidentlyAdvisory
    rawReportJson: str
    snapshot: object

    def manifest(self, raw_sha256: str, html_sha256: str) -> dict[str, object]:
        """Serializa exclusivamente metadatos/agregados y hashes de los artefactos completos."""
        return {
            "reportVersion": self.reportVersion,
            "policyVersion": self.policyVersion,
            "evidentlyVersion": self.evidentlyVersion,
            "stage": self.stage,
            "currentDatasetVersion": self.currentDatasetVersion,
            "referenceDatasetVersion": self.referenceDatasetVersion,
            "dataValidationEvidenceSha256": self.dataValidationEvidenceSha256,
            "authoritativeDataGateAllowed": self.authoritativeDataGateAllowed,
            "promotionAuthorized": self.promotionAuthorized,
            "evaluatedAt": self.evaluatedAt,
            "columns": list(self.columns),
            "currentRows": self.currentRows,
            "referenceRows": self.referenceRows,
            "advisory": asdict(self.advisory),
            "rawReportSha256": raw_sha256,
            "htmlReportSha256": html_sha256,
        }


def generate_evidently_report(
    policy: EvidentlyReportPolicy,
    validation_policy: DataValidationPolicy,
    evidence: DataValidationEvidence,
    current: pd.DataFrame,
    reference: pd.DataFrame,
    *,
    evaluated_at: datetime,
) -> EvidentlyReportResult:
    """Ejecuta Evidently y enlaza el informe a la decisión autoritativa recomputada."""
    if policy.dataValidationPolicyVersion != validation_policy.policyVersion:
        raise ValueError("EVIDENTLY_DATA_POLICY_VERSION_MISMATCH")
    if evaluated_at.tzinfo is None or evaluated_at.utcoffset() is None:
        raise ValueError("EVIDENTLY_EVALUATED_AT_TIMEZONE_REQUIRED")
    current_clean, definition = _validate_projection(policy, current, "current")
    reference_clean, reference_definition = _validate_projection(policy, reference, "reference")
    if tuple(current_clean.columns) != tuple(reference_clean.columns):
        raise ValueError("EVIDENTLY_SCHEMA_MISMATCH")
    if definition != reference_definition:
        raise ValueError("EVIDENTLY_DEFINITION_MISMATCH")
    if len(current_clean) != evidence.rowCount:
        raise ValueError("EVIDENTLY_CURRENT_ROW_COUNT_MISMATCH")

    data_decision = evaluate_data_validation(validation_policy, evidence)
    report = Report(
        [
            DataDriftPreset(
                method=policy.driftMethod,
                drift_share=policy.maximumDriftedColumnShare,
            ),
            DataSummaryPreset(),
        ],
        include_tests=True,
    )
    snapshot = report.run(
        Dataset.from_pandas(current_clean, data_definition=definition),
        Dataset.from_pandas(reference_clean, data_definition=reference_definition),
    )
    raw_json = snapshot.json()
    payload = json.loads(raw_json)
    drift = next(
        metric["value"]
        for metric in payload["metrics"]
        if metric["config"].get("type") == "evidently:metric_v2:DriftedColumnsCount"
    )
    failed = sum(test.get("status") != "SUCCESS" for test in payload.get("tests", []))
    advisory = EvidentlyAdvisory(
        driftedColumns=int(drift["count"]),
        driftedColumnShare=round(float(drift["share"]), 8),
        testsTotal=len(payload.get("tests", [])),
        testsFailed=failed,
        reviewRequired=(
            float(drift["share"]) >= policy.maximumDriftedColumnShare or failed > 0
        ),
    )
    return EvidentlyReportResult(
        reportVersion=1,
        policyVersion=policy.policyVersion,
        evidentlyVersion=evidently.__version__,
        stage=evidence.stage,
        currentDatasetVersion=evidence.datasetVersion,
        referenceDatasetVersion=evidence.baselineDatasetVersion,
        dataValidationEvidenceSha256=evidence.digest(),
        authoritativeDataGateAllowed=data_decision.allowed,
        promotionAuthorized=False,
        evaluatedAt=evaluated_at.astimezone(UTC).isoformat(),
        columns=tuple(current_clean.columns),
        currentRows=len(current_clean),
        referenceRows=len(reference_clean),
        advisory=advisory,
        rawReportJson=raw_json,
        snapshot=snapshot,
    )


def _validate_projection(
    policy: EvidentlyReportPolicy, frame: pd.DataFrame, name: str
) -> tuple[pd.DataFrame, DataDefinition]:
    """Copia y valida una proyección minimizada antes de entregarla a Evidently."""
    columns = list(frame.columns)
    if len(columns) != len(set(columns)):
        raise ValueError(f"EVIDENTLY_{name.upper()}_COLUMN_DUPLICATED")
    if not policy.minimumColumns <= len(columns) <= policy.maximumColumns:
        raise ValueError(f"EVIDENTLY_{name.upper()}_COLUMN_COUNT_INVALID")
    if not policy.minimumRows <= len(frame) <= policy.maximumRows:
        raise ValueError(f"EVIDENTLY_{name.upper()}_ROW_COUNT_INVALID")
    unknown = sorted(set(columns) - set(policy.allowedColumns))
    prohibited = sorted(
        column
        for column in columns
        if any(token.lower() in column.lower() for token in policy.prohibitedColumnTokens)
    )
    if unknown or prohibited:
        raise ValueError(f"EVIDENTLY_{name.upper()}_COLUMN_FORBIDDEN")

    clean = frame.copy(deep=True)
    numerical: list[str] = []
    categorical: list[str] = []
    for column in columns:
        rule = policy.allowedColumns[column]
        null_rate = float(clean[column].isna().mean())
        if null_rate > policy.maximumNullRate:
            raise ValueError(f"EVIDENTLY_{name.upper()}_NULL_RATE_INVALID")
        if rule.kind == "numerical":
            if not pd.api.types.is_numeric_dtype(clean[column]) or pd.api.types.is_bool_dtype(
                clean[column]
            ):
                raise ValueError(f"EVIDENTLY_{name.upper()}_NUMERICAL_TYPE_INVALID")
            non_null = clean[column].dropna()
            if any(not math.isfinite(float(value)) for value in non_null):
                raise ValueError(f"EVIDENTLY_{name.upper()}_NUMERICAL_VALUE_INVALID")
            numerical.append(column)
        else:
            normalized = clean[column].map(
                lambda value: None if pd.isna(value) else str(value).lower()
            )
            if not set(normalized.dropna()) <= {item.lower() for item in rule.allowedValues}:
                raise ValueError(f"EVIDENTLY_{name.upper()}_CATEGORY_FORBIDDEN")
            clean[column] = normalized
            categorical.append(column)
    return clean, DataDefinition(
        numerical_columns=numerical,
        categorical_columns=categorical,
    )


def write_evidently_artifacts(result: EvidentlyReportResult, output_directory: Path) -> Path:
    """Escribe JSON/HTML agregados y un manifiesto con hashes mediante reemplazo atómico."""
    output_directory.mkdir(parents=True, exist_ok=True)
    base = f"{result.currentDatasetVersion}.evidently"
    raw_path = output_directory / f"{base}.json"
    html_path = output_directory / f"{base}.html"
    manifest_path = output_directory / f"{base}.manifest.json"
    raw_path.write_text(result.rawReportJson, encoding="utf-8")
    result.snapshot.save_html(str(html_path))  # type: ignore[attr-defined]
    manifest = result.manifest(_sha256(raw_path), _sha256(html_path))
    temporary = manifest_path.with_suffix(".json.tmp")
    temporary.write_text(json.dumps(manifest, indent=2, sort_keys=True), encoding="utf-8")
    temporary.replace(manifest_path)
    return manifest_path


def _sha256(path: Path) -> str:
    """Calcula el identificador de contenido de un artefacto ya cerrado."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run() -> None:
    """CLI offline para Prefect; solo admite Parquet y evidencia que se vuelve a validar."""
    parser = argparse.ArgumentParser(description="Genera informe Evidently gobernado")
    parser.add_argument("--current", type=Path, required=True)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--validation-evidence", type=Path, required=True)
    parser.add_argument("--output-directory", type=Path, required=True)
    parser.add_argument("--evaluated-at", type=datetime.fromisoformat, required=True)
    args = parser.parse_args()
    if args.current.suffix.lower() != ".parquet" or args.reference.suffix.lower() != ".parquet":
        raise ValueError("EVIDENTLY_PARQUET_REQUIRED")
    root = Path(__file__).resolve().parents[2]
    report_policy = EvidentlyReportPolicy.load(root / "policies/evidently-report.v1.json")
    validation_policy = DataValidationPolicy.load(root / "policies/data-validation.v1.json")
    evidence = DataValidationEvidence.model_validate_json(
        args.validation_evidence.read_text(encoding="utf-8")
    )
    result = generate_evidently_report(
        report_policy,
        validation_policy,
        evidence,
        pd.read_parquet(args.current),
        pd.read_parquet(args.reference),
        evaluated_at=args.evaluated_at,
    )
    path = write_evidently_artifacts(result, args.output_directory)
    print(path)


if __name__ == "__main__":
    run()
