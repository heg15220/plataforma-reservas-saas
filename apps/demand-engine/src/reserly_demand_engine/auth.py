"""Autenticación servicio-a-servicio local mediante comparación constante."""

from __future__ import annotations

import hmac
from collections.abc import Callable

from fastapi import Header

from .config import DemandEngineSettings
from .errors import DemandEngineError


def service_auth_dependency(settings: DemandEngineSettings) -> Callable[..., None]:
    """Construye una dependencia deny-by-default sin reutilizar cookies o identidad humana."""

    def authenticate(
        service_id: str | None = Header(default=None, alias="X-Reserly-Service-Id"),
        service_token: str | None = Header(default=None, alias="X-Reserly-Service-Token"),
    ) -> None:
        expected_token = settings.service_token.get_secret_value()
        if (
            service_id is None
            or service_token is None
            or not hmac.compare_digest(service_id, settings.service_id)
            or not hmac.compare_digest(service_token, expected_token)
        ):
            raise DemandEngineError("SERVICE_AUTH_INVALID", 401)

    return authenticate
