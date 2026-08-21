import json
import tempfile
import unittest
from pathlib import Path

from prometheus_client.exposition import generate_latest

from reserly_demand_engine.metrics import DemandMetrics
from reserly_demand_engine.operational_readiness import validate_operational_readiness


ROOT = Path(__file__).parents[3]


class OperationalReadinessTests(unittest.TestCase):
    """Valida objetivos, cobertura física y métricas sin declarar salud productiva."""

    def test_repository_has_complete_slo_alert_and_runbook_coverage(self) -> None:
        evidence = validate_operational_readiness(ROOT)

        self.assertEqual(4, evidence.serviceCount)
        self.assertEqual(8, evidence.alertCount)
        self.assertEqual(5, evidence.runbookCount)
        self.assertTrue(evidence.configurationComplete)
        self.assertFalse(evidence.productionSloMet)
        self.assertTrue(all(len(value) == 64 for value in evidence.artifactsSha256.values()))

    def test_metrics_publish_freshness_cost_and_capacity_without_high_cardinality(self) -> None:
        metrics = DemandMetrics()
        metrics.record_pipeline_success("quality", 1_787_310_000)
        metrics.record_cost("training", 12.5)
        metrics.set_monthly_cost_budget(600, 750)
        metrics.set_capacity_saturation("batch-workers", 3, 4)

        scrape = generate_latest(metrics.registry).decode()
        self.assertIn('reserly_demand_pipeline_last_success_timestamp_seconds{pipeline="quality"}', scrape)
        self.assertIn('reserly_demand_cost_eur_total{cost_type="training"} 12.5', scrape)
        self.assertIn("reserly_demand_monthly_cost_budget_ratio 0.8", scrape)
        self.assertIn('reserly_demand_capacity_saturation_ratio{resource="batch-workers"} 0.75', scrape)
        self.assertNotIn("venueId", scrape)

    def test_unknown_dimensions_and_invalid_values_fail_closed(self) -> None:
        metrics = DemandMetrics()
        invalid_calls = [
            lambda: metrics.record_pipeline_success("customer-id", 1),
            lambda: metrics.record_cost("venue", 1),
            lambda: metrics.set_monthly_cost_budget(1, 0),
            lambda: metrics.set_capacity_saturation("gpu-user", -1, 1),
        ]
        for call in invalid_calls:
            with self.assertRaises(ValueError):
                call()

    def test_missing_required_alert_fails_closed(self) -> None:
        import shutil

        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "repo"
            shutil.copytree(ROOT / "apps" / "demand-engine" / "operations", target / "apps" / "demand-engine" / "operations")
            alerts_target = target / "infrastructure" / "prometheus"
            alerts_target.mkdir(parents=True)
            source = (ROOT / "infrastructure" / "prometheus" / "alerts.yml").read_text()
            alerts_target.joinpath("alerts.yml").write_text(source.replace("alert: DemandErrorBudgetFastBurn", "alert: Removed"))
            dashboard_target = target / "infrastructure" / "grafana" / "dashboards"
            dashboard_target.mkdir(parents=True)
            shutil.copy(
                ROOT / "infrastructure" / "grafana" / "dashboards" / "demand-engine.json",
                dashboard_target / "demand-engine.json",
            )

            with self.assertRaisesRegex(ValueError, "ALERT_COVERAGE_INCOMPLETE"):
                validate_operational_readiness(target)


if __name__ == "__main__":
    unittest.main()
