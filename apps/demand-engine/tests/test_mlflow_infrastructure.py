"""Pruebas del despliegue MLflow protegido y reproducible."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).parents[3]
ENTRYPOINT = ROOT / "infrastructure" / "mlflow" / "entrypoint.py"
SPEC = importlib.util.spec_from_file_location("reserly_mlflow_entrypoint", ENTRYPOINT)
assert SPEC and SPEC.loader
mlflow_entrypoint = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mlflow_entrypoint)


class MlflowInfrastructureTests(unittest.TestCase):
    """Impide degradar autenticación, aislamiento o persistencia MLOps por accidente."""

    def setUp(self) -> None:
        self.environment = {
            "RESERLY_MLFLOW_DATABASE_USERNAME": "mlflow",
            "RESERLY_MLFLOW_DATABASE_PASSWORD": "database-secret-with-at-least-32-characters",
            "RESERLY_MLFLOW_DATABASE_NAME": "mlflow",
            "RESERLY_MLFLOW_ADMIN_USERNAME": "reserly-mlops-admin",
            "RESERLY_MLFLOW_ADMIN_PASSWORD": "admin-secret-with-at-least-32-characters",
            "RESERLY_MLFLOW_S3_BUCKET": "reserly-mlflow-local",
            "RESERLY_MLFLOW_ALLOWED_HOSTS": "localhost:*,127.0.0.1:*,mlflow:5000",
        }

    def test_auth_configuration_denies_implicit_access(self) -> None:
        database_uri = mlflow_entrypoint.build_database_uri(self.environment)
        config = mlflow_entrypoint.render_auth_config(self.environment, database_uri)
        self.assertIn("default_permission = NO_PERMISSIONS", config)
        self.assertIn("grant_default_workspace_access = false", config)
        self.assertNotIn("password1234", config)

    def test_database_credentials_are_url_encoded(self) -> None:
        self.environment["RESERLY_MLFLOW_DATABASE_PASSWORD"] = "a/b?c#d%with-more-than-32-characters"
        uri = mlflow_entrypoint.build_database_uri(self.environment)
        self.assertIn("a%2Fb%3Fc%23d%25", uri)
        self.assertNotIn("a/b?c#d%", uri)

    def test_short_or_multiline_secrets_fail_closed(self) -> None:
        for value in ("short", "safe-looking-secret-with-32-chars\ninjected=true"):
            with self.subTest(value=value):
                self.environment["RESERLY_MLFLOW_ADMIN_PASSWORD"] = value
                with self.assertRaises(RuntimeError):
                    mlflow_entrypoint.render_auth_config(
                        self.environment,
                        mlflow_entrypoint.build_database_uri(self.environment),
                    )

    def test_server_uses_basic_auth_postgres_and_proxied_s3(self) -> None:
        uri = mlflow_entrypoint.build_database_uri(self.environment)
        arguments = mlflow_entrypoint.server_arguments(self.environment, uri)
        self.assertIn("basic-auth", arguments)
        self.assertIn("--backend-store-uri", arguments)
        self.assertIn("--artifacts-destination", arguments)
        self.assertIn("s3://reserly-mlflow-local/artifacts", arguments)
        self.assertNotIn("--no-serve-artifacts", arguments)

    def test_compose_keeps_metadata_private_and_ui_on_loopback(self) -> None:
        compose = (ROOT / "infrastructure" / "compose.yaml").read_text(encoding="utf-8")
        mlflow_block = compose.split("  mlflow:\n", 1)[1].split("\n  clamav:", 1)[0]
        postgres_block = compose.split("  mlflow-postgres:\n", 1)[1].split("\n  mlflow-minio-init:", 1)[0]
        self.assertIn("127.0.0.1:${RESERLY_MLFLOW_PORT:-5000}:5000", mlflow_block)
        self.assertIn("read_only: true", mlflow_block)
        self.assertIn("profiles: [mlops]", mlflow_block)
        self.assertNotIn("ports:", postgres_block)
        self.assertIn("mlflow-postgres-data", postgres_block)


if __name__ == "__main__":
    unittest.main()
