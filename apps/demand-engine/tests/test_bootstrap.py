"""Pruebas de configuración, health, autenticación, límites y timeout."""

import asyncio
import unittest

from fastapi import Depends
from fastapi.testclient import TestClient
from pydantic import ValidationError

from reserly_demand_engine.application import create_app
from reserly_demand_engine.auth import service_auth_dependency
from reserly_demand_engine.config import DemandEngineSettings


TOKEN = "test-demand-engine-token-at-least-32-characters"


def settings(**overrides: object) -> DemandEngineSettings:
    """Construye settings aislados sin leer secretos del proceso."""
    values: dict[str, object] = {
        "environment": "test",
        "service_id": "spring-api",
        "service_token": TOKEN,
        "request_timeout_seconds": 0.2,
    }
    values.update(overrides)
    return DemandEngineSettings(**values)


class DemandEngineBootstrapTests(unittest.TestCase):
    """Protege el arranque seguro antes de añadir endpoints funcionales."""

    def test_health_is_independent_and_docs_are_disabled(self) -> None:
        app = create_app(settings())
        client = TestClient(app)

        self.assertEqual(client.get("/internal/demand/v1/health/live").status_code, 200)
        self.assertEqual(
            client.get("/internal/demand/v1/health/ready").json()["status"], "ready"
        )
        self.assertEqual(client.get("/internal/demand/docs").status_code, 404)

    def test_service_auth_is_constant_contract_and_returns_opaque_error(self) -> None:
        configured = settings()
        app = create_app(configured)

        @app.get(
            "/internal/demand/v1/protected",
            dependencies=[Depends(service_auth_dependency(configured))],
        )
        async def protected() -> dict[str, bool]:
            return {"ok": True}

        client = TestClient(app)
        denied = client.get("/internal/demand/v1/protected")
        allowed = client.get(
            "/internal/demand/v1/protected",
            headers={
                "X-Reserly-Service-Id": "spring-api",
                "X-Reserly-Service-Token": TOKEN,
            },
        )

        self.assertEqual(denied.status_code, 401)
        self.assertEqual(set(denied.json()), {"code", "requestId"})
        self.assertEqual(denied.json()["code"], "SERVICE_AUTH_INVALID")
        self.assertEqual(allowed.status_code, 200)

    def test_timeout_and_body_limit_fail_with_stable_codes(self) -> None:
        app = create_app(settings(request_timeout_seconds=0.01, maximum_request_bytes=1024))

        @app.get("/internal/demand/v1/slow")
        async def slow() -> dict[str, bool]:
            await asyncio.sleep(0.05)
            return {"ok": True}

        client = TestClient(app)
        timeout = client.get("/internal/demand/v1/slow")
        oversized = client.post(
            "/internal/demand/v1/unknown", content=b"x" * 1025
        )

        self.assertEqual(timeout.status_code, 504)
        self.assertEqual(timeout.json()["code"], "REQUEST_TIMEOUT")
        self.assertEqual(oversized.status_code, 413)
        self.assertEqual(oversized.json()["code"], "REQUEST_TOO_LARGE")

    def test_environment_rejects_short_token_and_invalid_timeout(self) -> None:
        with self.assertRaises(ValidationError):
            settings(service_token="short")
        with self.assertRaises(ValidationError):
            settings(request_timeout_seconds=0)


if __name__ == "__main__":
    unittest.main()
