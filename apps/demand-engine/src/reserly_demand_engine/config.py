"""Configuración validada y explícita por entorno del Demand Engine."""

from __future__ import annotations

import os
from collections.abc import Mapping
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, SecretStr


class DemandEngineSettings(BaseModel):
    """Settings inmutables; el token nunca se representa como texto en logs o errores."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    environment: Literal["local", "test", "staging", "production"] = "local"
    host: str = "127.0.0.1"
    port: int = Field(default=8090, ge=1, le=65535)
    service_id: str = Field(pattern=r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    service_token: SecretStr = Field(min_length=32)
    request_timeout_seconds: float = Field(default=0.2, gt=0, le=5)
    maximum_request_bytes: int = Field(default=65_536, ge=1_024, le=1_048_576)
    docs_enabled: bool = False
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"

    @classmethod
    def from_env(cls, environ: Mapping[str, str] | None = None) -> "DemandEngineSettings":
        """Carga únicamente variables conocidas y deja que Pydantic rechace valores inseguros."""
        values = os.environ if environ is None else environ
        environment = values.get("RESERLY_DEMAND_ENGINE_ENVIRONMENT", "local")
        docs_default = "true" if environment == "local" else "false"
        return cls(
            environment=environment,
            host=values.get("RESERLY_DEMAND_ENGINE_HOST", "127.0.0.1"),
            port=values.get("RESERLY_DEMAND_ENGINE_PORT", "8090"),
            service_id=values.get("RESERLY_DEMAND_ENGINE_SERVICE_ID", ""),
            service_token=values.get("RESERLY_DEMAND_ENGINE_SERVICE_TOKEN", ""),
            request_timeout_seconds=values.get(
                "RESERLY_DEMAND_ENGINE_REQUEST_TIMEOUT_SECONDS", "0.2"
            ),
            maximum_request_bytes=values.get(
                "RESERLY_DEMAND_ENGINE_MAXIMUM_REQUEST_BYTES", "65536"
            ),
            docs_enabled=values.get("RESERLY_DEMAND_ENGINE_DOCS_ENABLED", docs_default),
            log_level=values.get("RESERLY_DEMAND_ENGINE_LOG_LEVEL", "INFO"),
        )
