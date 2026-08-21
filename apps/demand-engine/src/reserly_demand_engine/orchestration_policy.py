"""Política ejecutable para revisar Prefect frente a Airflow u otro orquestador.

El módulo no migra ni modifica infraestructura. Convierte métricas agregadas de treinta días y
necesidades operativas aprobadas en una recomendación auditable de evaluación. La decisión final
requiere benchmark, coste total y revisión humana.
"""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


class OrchestrationThresholds(StrictContract):
    """Límites sostenidos a partir de los cuales la operación inicial debe reevaluarse."""

    activeDeployments: int = Field(ge=50, le=10000)
    taskRunsPerDayP95: int = Field(ge=1000, le=10_000_000)
    concurrentTaskRunsP95: int = Field(ge=50, le=100_000)
    schedulerDelaySecondsP95: float = Field(ge=10, le=3600)
    backfillDurationHoursP95: float = Field(ge=1, le=168)


class OrchestrationObservation(StrictContract):
    """Ventana agregada sin payloads, nombres de clientes ni parámetros de flows."""

    windowDays: int = Field(ge=1, le=366)
    activeDeployments: int = Field(ge=0)
    taskRunsPerDayP95: int = Field(ge=0)
    concurrentTaskRunsP95: int = Field(ge=0)
    schedulerDelaySecondsP95: float = Field(ge=0)
    backfillDurationHoursP95: float = Field(ge=0)
    approvedHardRequirements: list[str] = Field(default_factory=list, max_length=3)


class OrchestrationAssessment(StrictContract):
    """Resultado explicable; nunca sustituye la comparación ni aprueba una migración."""

    selectedTool: Literal["prefect"]
    openAlternativeEvaluation: bool
    quantitativeBreaches: list[str]
    hardRequirements: list[str]
    automaticMigrationAllowed: Literal[False] = False
    reason: Literal[
        "insufficientMeasurementWindow",
        "prefectWithinInitialEnvelope",
        "quantitativeEnvelopeExceeded",
        "hardRequirementNotCovered",
    ]


class OrchestrationSelectionPolicy(StrictContract):
    """Fija herramienta, versión y regla medible para impedir migraciones por preferencia."""

    schemaVersion: Literal[1]
    policyVersion: Version
    selectedTool: Literal["prefect"]
    selectedVersion: str = Field(pattern=r"^3\.\d+\.\d+$")
    selectionReasons: list[str] = Field(min_length=3, max_length=5)
    reviewCadenceDays: int = Field(ge=30, le=180)
    measurementWindowDays: int = Field(ge=14, le=90)
    minimumQuantitativeBreaches: int = Field(ge=2, le=5)
    quantitativeThresholds: OrchestrationThresholds
    hardRequirements: list[str] = Field(min_length=3, max_length=3)
    migrationDecision: str = Field(min_length=100, max_length=500)
    automaticMigrationAllowed: Literal[False]

    @model_validator(mode="after")
    def validate_hard_requirements(self) -> "OrchestrationSelectionPolicy":
        if len(self.hardRequirements) != len(set(self.hardRequirements)):
            raise ValueError("ORCHESTRATION_HARD_REQUIREMENT_DUPLICATED")
        return self

    @classmethod
    def load(cls, path: Path) -> "OrchestrationSelectionPolicy":
        """Carga la decisión versionada desde UTF-8 y rechaza campos desconocidos."""
        return cls.model_validate_json(path.read_text(encoding="utf-8"))

    def assess(self, observation: OrchestrationObservation) -> OrchestrationAssessment:
        """Evalúa una ventana; las necesidades duras solo cuentan si están predefinidas."""
        unknown = set(observation.approvedHardRequirements) - set(self.hardRequirements)
        if unknown:
            raise ValueError("ORCHESTRATION_HARD_REQUIREMENT_UNKNOWN")
        if observation.windowDays < self.measurementWindowDays:
            return OrchestrationAssessment(
                selectedTool="prefect",
                openAlternativeEvaluation=False,
                quantitativeBreaches=[],
                hardRequirements=observation.approvedHardRequirements,
                reason="insufficientMeasurementWindow",
            )

        thresholds = self.quantitativeThresholds.model_dump()
        measurements = observation.model_dump()
        breaches = [name for name, limit in thresholds.items() if measurements[name] > limit]
        hard_requirements = observation.approvedHardRequirements
        if hard_requirements:
            reason = "hardRequirementNotCovered"
            open_evaluation = True
        elif len(breaches) >= self.minimumQuantitativeBreaches:
            reason = "quantitativeEnvelopeExceeded"
            open_evaluation = True
        else:
            reason = "prefectWithinInitialEnvelope"
            open_evaluation = False
        return OrchestrationAssessment(
            selectedTool="prefect",
            openAlternativeEvaluation=open_evaluation,
            quantitativeBreaches=breaches,
            hardRequirements=hard_requirements,
            reason=reason,
        )
