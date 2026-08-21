"""Arranca Prefect server/worker construyendo secretos solo en memoria del proceso."""

from __future__ import annotations

import os
from urllib.parse import quote


MINIMUM_SECRET_LENGTH = 32


def required(environment: dict[str, str], name: str, *, secret: bool = False) -> str:
    """Rechaza ausencias, inyección multilínea y credenciales débiles."""
    value = environment.get(name, "")
    if not value or "\n" in value or "\r" in value:
        raise RuntimeError(f"PREFECT_CONFIGURATION_INVALID:{name}")
    if secret and len(value) < MINIMUM_SECRET_LENGTH:
        raise RuntimeError(f"PREFECT_SECRET_TOO_SHORT:{name}")
    return value


def configure(environment: dict[str, str]) -> tuple[dict[str, str], list[str]]:
    """Crea auth y URI escapada; selecciona exclusivamente server o process worker."""
    role = required(environment, "RESERLY_PREFECT_ROLE")
    username = required(environment, "RESERLY_PREFECT_AUTH_USERNAME")
    password = required(environment, "RESERLY_PREFECT_AUTH_PASSWORD", secret=True)
    if ":" in username:
        raise RuntimeError("PREFECT_CONFIGURATION_INVALID:RESERLY_PREFECT_AUTH_USERNAME")
    configured = dict(environment)
    configured["PREFECT_API_AUTH_STRING"] = f"{username}:{password}"
    if role == "server":
        database_username = quote(
            required(environment, "RESERLY_PREFECT_DATABASE_USERNAME"), safe=""
        )
        database_password = quote(
            required(environment, "RESERLY_PREFECT_DATABASE_PASSWORD", secret=True), safe=""
        )
        database_name = quote(required(environment, "RESERLY_PREFECT_DATABASE_NAME"), safe="")
        configured["PREFECT_SERVER_DATABASE_CONNECTION_URL"] = (
            f"postgresql+asyncpg://{database_username}:{database_password}"
            f"@prefect-postgres:5432/{database_name}"
        )
        configured["PREFECT_SERVER_API_AUTH_STRING"] = configured["PREFECT_API_AUTH_STRING"]
        return configured, ["prefect", "server", "start", "--host", "0.0.0.0"]
    if role == "worker":
        return configured, [
            "prefect",
            "worker",
            "start",
            "--pool",
            "reserly-demand-batch",
            "--type",
            "process",
        ]
    raise RuntimeError("PREFECT_CONFIGURATION_INVALID:RESERLY_PREFECT_ROLE")


def main() -> None:
    """Valida, actualiza el entorno y reemplaza el proceso sin imprimir secretos."""
    configured, arguments = configure(dict(os.environ))
    os.environ.clear()
    os.environ.update(configured)
    os.execvp("prefect", arguments)


if __name__ == "__main__":
    main()
