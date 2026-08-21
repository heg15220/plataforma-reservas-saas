"""Puerta offline que enlaza SLO, coste, capacidad, alertas y runbooks versionados."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Literal

from pydantic import Field, model_validator

from .contracts import StrictContract, Version


class ServiceObjective(StrictContract):
    """Objetivo medible por servicio y ventana, con latencia/freshness explícitas."""
    service: Version
    availabilityTarget: float = Field(ge=0.9, lt=1)
    p95LatencyMs: int = Field(gt=0)
    p99LatencyMs: int = Field(gt=0)
    maximumErrorRatio: float = Field(gt=0, le=0.1)
    freshnessSeconds: int = Field(gt=0)

    @model_validator(mode="after")
    def latency_order(self) -> "ServiceObjective":
        if self.p99LatencyMs < self.p95LatencyMs:
            raise ValueError("OPERATIONAL_SLO_LATENCY_ORDER_INVALID")
        return self


class ErrorBudgetPolicy(StrictContract):
    """Acciones fail-safe según consumo del error budget."""
    warningBurnRatio: float = Field(gt=0, lt=1)
    exhaustedBurnRatio: Literal[1.0]
    warningAction: Literal["freeze-rollout"]
    exhaustedAction: Literal["fallback-and-change-freeze"]
    automaticPromotionAllowed: Literal[False]


class SloPolicy(StrictContract):
    """Fuente versionada de objetivos, alertas y procedimientos requeridos."""
    schemaVersion: Literal[1]
    policyVersion: Version
    windowDays: int = Field(ge=7, le=90)
    services: list[ServiceObjective] = Field(min_length=4)
    errorBudgetPolicy: ErrorBudgetPolicy
    requiredAlerts: list[str] = Field(min_length=8)
    requiredRunbooks: list[str] = Field(min_length=5)

    @model_validator(mode="after")
    def unique_coverage(self) -> "SloPolicy":
        if len({item.service for item in self.services}) != len(self.services):
            raise ValueError("OPERATIONAL_SLO_SERVICE_DUPLICATED")
        if any(not name.startswith("Demand") or not name.isalnum() for name in self.requiredAlerts):
            raise ValueError("OPERATIONAL_SLO_ALERT_NAME_INVALID")
        if len(set(self.requiredAlerts)) != len(self.requiredAlerts) or len(set(self.requiredRunbooks)) != len(self.requiredRunbooks):
            raise ValueError("OPERATIONAL_SLO_REFERENCE_DUPLICATED")
        return self


class UnitBudget(StrictContract):
    costType: Version
    unit: Version
    maximumEur: float = Field(gt=0)


class CostBudget(StrictContract):
    """Techo de coste que nunca permite sacrificar seguridad o calidad."""
    schemaVersion: Literal[1]
    budgetVersion: Version
    currency: Literal["EUR"]
    period: Literal["monthly"]
    maximumTotalEur: float = Field(gt=0)
    warningRatio: float = Field(gt=0.5, lt=1)
    unitBudgets: list[UnitBudget] = Field(min_length=4)
    overBudgetAction: Literal["freeze-nonessential-training-and-rollout"]
    qualityOrSafetyMayBeDisabledToSaveCost: Literal[False]
    automaticBudgetIncreaseAllowed: Literal[False]


class CapacityResource(StrictContract):
    resource: Version
    planned: int = Field(gt=0)
    maximum: int = Field(gt=0)
    warningRatio: float = Field(gt=0.5, lt=1)

    @model_validator(mode="after")
    def capacity_order(self) -> "CapacityResource":
        if self.maximum < self.planned:
            raise ValueError("OPERATIONAL_CAPACITY_MAXIMUM_INVALID")
        return self


class CapacityPlan(StrictContract):
    """Supuestos y gates de load test previos a escala o promoción."""
    schemaVersion: Literal[1]
    planVersion: Version
    assumptions: dict[str, float]
    resources: list[CapacityResource] = Field(min_length=3)
    loadTestGate: dict[str, float | bool]
    scaleOutRequiresHumanApproval: Literal[True]
    fallback: Literal["fallback-mvp-v1"]


class OperationalReadinessEvidence(StrictContract):
    """Evidencia por hash; valida cobertura pero no afirma que producción cumpla el SLO."""
    policyVersion: Version
    serviceCount: int
    alertCount: int
    runbookCount: int
    artifactsSha256: dict[Version, str]
    configurationComplete: Literal[True]
    productionSloMet: Literal[False]


def validate_operational_readiness(root: Path) -> OperationalReadinessEvidence:
    """Valida políticas y referencias físicas a Prometheus/runbooks sin ejecutar infraestructura."""
    operations = root / "apps" / "demand-engine" / "operations"
    paths = {
        "slo": operations / "slo-policy.v1.json",
        "cost": operations / "cost-budget.v1.json",
        "capacity": operations / "capacity-plan.v1.json",
    }
    slo = SloPolicy.model_validate_json(paths["slo"].read_text(encoding="utf-8"))
    CostBudget.model_validate_json(paths["cost"].read_text(encoding="utf-8"))
    CapacityPlan.model_validate_json(paths["capacity"].read_text(encoding="utf-8"))
    alerts = (root / "infrastructure" / "prometheus" / "alerts.yml").read_text(encoding="utf-8")
    dashboard_path = root / "infrastructure" / "grafana" / "dashboards" / "demand-engine.json"
    dashboard = dashboard_path.read_text(encoding="utf-8")
    missing_alerts = [name for name in slo.requiredAlerts if f"alert: {name}" not in alerts]
    if missing_alerts:
        raise ValueError("OPERATIONAL_ALERT_COVERAGE_INCOMPLETE")
    required_panels = {
        "reserly_demand_pipeline_last_success_timestamp_seconds",
        "reserly_demand_monthly_cost_budget_ratio",
        "reserly_demand_capacity_saturation_ratio",
    }
    if any(metric not in dashboard for metric in required_panels):
        raise ValueError("OPERATIONAL_DASHBOARD_COVERAGE_INCOMPLETE")
    for runbook in slo.requiredRunbooks:
        path = operations / "runbooks" / runbook
        if not path.is_file() or any(section not in path.read_text(encoding="utf-8") for section in ("Owner:", "fallback" if runbook == "demand-engine-unavailable.md" else "1.")):
            raise ValueError("OPERATIONAL_RUNBOOK_INCOMPLETE")
    digests = {name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in paths.items()}
    digests["prometheus-alerts"] = hashlib.sha256(alerts.encode()).hexdigest()
    digests["grafana-dashboard"] = hashlib.sha256(dashboard.encode()).hexdigest()
    return OperationalReadinessEvidence(policyVersion=slo.policyVersion, serviceCount=len(slo.services), alertCount=len(slo.requiredAlerts), runbookCount=len(slo.requiredRunbooks), artifactsSha256=digests, configurationComplete=True, productionSloMet=False)


def run() -> None:
    """CLI de CI; no consulta métricas ni declara cumplimiento productivo."""
    parser = argparse.ArgumentParser(description="Valida preparación operativa del Demand Engine")
    parser.add_argument("--repository-root", type=Path, required=True)
    arguments = parser.parse_args()
    print(validate_operational_readiness(arguments.repository_root).model_dump_json())


if __name__ == "__main__":
    run()
