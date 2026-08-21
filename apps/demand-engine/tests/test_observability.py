"""Aceptación de métricas, alertas y dashboard sin dimensiones de alta cardinalidad."""

from __future__ import annotations

import json
import unittest
from pathlib import Path
from uuid import uuid4

from fastapi.testclient import TestClient
from prometheus_client.exposition import generate_latest

from reserly_demand_engine.application import create_app
from reserly_demand_engine.config import DemandEngineSettings
from reserly_demand_engine.metrics import DemandMetrics


ROOT = Path(__file__).resolve().parents[3]
TOKEN = "observability-test-token-at-least-32-characters"


class DemandMetricsTests(unittest.TestCase):
    """Protege contrato, privacidad, cardinalidad y artefactos provisionados."""

    def test_registry_exposes_required_families_and_rejects_invalid_values(self) -> None:
        metrics = DemandMetrics()
        metrics.set_model_health(
            "champion", drift_psi=0.12, calibration_error=0.04, segment="overall"
        )
        metrics.set_population_health(
            surface="overall", coverage=0.9, diversity=0.7, new_venue_exposure=0.2
        )
        metrics.set_business_value("incrementalNetRevenue", 125.50)
        exposition = generate_latest(metrics.registry).decode("utf-8")

        for family in (
            "reserly_demand_http_request_duration_seconds",
            "reserly_demand_model_errors_total",
            "reserly_demand_drift_psi",
            "reserly_demand_calibration_error_ratio",
            "reserly_demand_coverage_ratio",
            "reserly_demand_diversity_ratio",
            "reserly_demand_exposure_ratio",
            "reserly_demand_business_value_eur",
        ):
            self.assertIn(family, exposition)
        with self.assertRaises(ValueError):
            metrics.set_population_health(
                surface=str(uuid4()), coverage=1, diversity=1, new_venue_exposure=1
            )
        with self.assertRaises(ValueError):
            metrics.set_business_value("customerRevenue", 1)
        with self.assertRaises(ValueError):
            metrics.set_rollout_traffic("canary", 1.01)

    def test_scrape_is_internal_and_paths_are_normalized(self) -> None:
        settings = DemandEngineSettings(
            environment="test", service_id="spring-api", service_token=TOKEN
        )
        client = TestClient(create_app(settings), raise_server_exceptions=False)
        venue_id = uuid4()
        client.get(
            f"/internal/demand/v1/demand/{venue_id}",
            headers={
                "X-Reserly-Service-Id": "spring-api",
                "X-Reserly-Service-Token": TOKEN,
            },
        )
        response = client.get("/internal/demand/v1/metrics")

        self.assertEqual(200, response.status_code)
        self.assertIn("text/plain", response.headers["content-type"])
        self.assertIn('route="/internal/demand/v1/demand/{venue_id}"', response.text)
        self.assertNotIn(str(venue_id), response.text)
        self.assertNotIn(TOKEN, response.text)

    def test_dashboard_and_alerts_cover_all_governed_dimensions(self) -> None:
        dashboard_path = ROOT / "infrastructure/grafana/dashboards/demand-engine.json"
        dashboard = json.loads(dashboard_path.read_text(encoding="utf-8"))
        queries = "\n".join(
            target["expr"]
            for panel in dashboard["panels"]
            for target in panel.get("targets", [])
        )
        alerts = (ROOT / "infrastructure/prometheus/alerts.yml").read_text(
            encoding="utf-8"
        )
        for metric in (
            "http_request_duration_seconds",
            "http_requests_total",
            "drift_psi",
            "calibration_error_ratio",
            "coverage_ratio",
            "diversity_ratio",
            "exposure_ratio",
            "business_value_eur",
        ):
            self.assertIn(f"reserly_demand_{metric}", queries)
        for alert in (
            "DemandEngineHighErrorRate",
            "DemandEngineLatencyBudgetExceeded",
            "DemandModelDriftHigh",
            "DemandModelCalibrationDegraded",
            "DemandCoverageLow",
            "DemandDiversityLow",
            "DemandNewVenueExposureLow",
        ):
            self.assertIn(alert, alerts)

    def test_compose_hardens_and_pins_observability_services(self) -> None:
        compose = (ROOT / "infrastructure/compose.yaml").read_text(encoding="utf-8")
        for image in ("prom/prometheus:v3.5.3@sha256:", "grafana/grafana:13.1.0@sha256:"):
            self.assertIn(image, compose)
        self.assertIn("profiles: [observability]", compose)
        self.assertIn("127.0.0.1:${RESERLY_PROMETHEUS_PORT:-9090}:9090", compose)
        self.assertIn("127.0.0.1:${RESERLY_GRAFANA_PORT:-3000}:3000", compose)
        self.assertGreaterEqual(compose.count("read_only: true"), 4)


if __name__ == "__main__":
    unittest.main()
