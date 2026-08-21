"""Arranque endurecido del servidor MLflow con autenticación y almacenes remotos.

El proceso construye en ``/tmp`` la configuración de autenticación a partir de secretos
inyectados. Nunca persiste credenciales en la imagen ni admite el usuario/contraseña por
defecto de MLflow. El tracking y la autorización comparten una base PostgreSQL dedicada;
los artefactos se sirven por el proxy autenticado de MLflow y no por MinIO al cliente.
"""

from __future__ import annotations

import os
from pathlib import Path
from urllib.parse import quote


AUTH_CONFIG_PATH = Path("/tmp/mlflow-auth.ini")
MINIMUM_SECRET_LENGTH = 32


def _required(environment: dict[str, str], name: str, *, secret: bool = False) -> str:
    """Obtiene una variable obligatoria y rechaza valores inseguros o multilínea."""
    value = environment.get(name, "")
    if not value or "\n" in value or "\r" in value:
        raise RuntimeError(f"MLFLOW_CONFIGURATION_INVALID:{name}")
    if secret and len(value) < MINIMUM_SECRET_LENGTH:
        raise RuntimeError(f"MLFLOW_SECRET_TOO_SHORT:{name}")
    return value


def build_database_uri(environment: dict[str, str]) -> str:
    """Crea una URI PostgreSQL escapando credenciales sin imprimirlas en logs."""
    username = quote(_required(environment, "RESERLY_MLFLOW_DATABASE_USERNAME"), safe="")
    password = quote(
        _required(environment, "RESERLY_MLFLOW_DATABASE_PASSWORD", secret=True), safe=""
    )
    database = quote(_required(environment, "RESERLY_MLFLOW_DATABASE_NAME"), safe="")
    return f"postgresql://{username}:{password}@mlflow-postgres:5432/{database}"


def render_auth_config(environment: dict[str, str], database_uri: str) -> str:
    """Renderiza RBAC fail-closed; un usuario nuevo no obtiene lectura implícita."""
    admin_username = _required(environment, "RESERLY_MLFLOW_ADMIN_USERNAME")
    admin_password = _required(environment, "RESERLY_MLFLOW_ADMIN_PASSWORD", secret=True)
    return (
        "[mlflow]\n"
        "default_permission = NO_PERMISSIONS\n"
        "grant_default_workspace_access = false\n"
        f"database_uri = {database_uri}\n"
        f"admin_username = {admin_username}\n"
        f"admin_password = {admin_password}\n"
    )


def server_arguments(environment: dict[str, str], database_uri: str) -> list[str]:
    """Devuelve el contrato de servidor, incluyendo proxy S3 y middleware de hosts."""
    bucket = _required(environment, "RESERLY_MLFLOW_S3_BUCKET")
    allowed_hosts = _required(environment, "RESERLY_MLFLOW_ALLOWED_HOSTS")
    return [
        "mlflow",
        "server",
        "--app-name",
        "basic-auth",
        "--host",
        "0.0.0.0",
        "--port",
        "5000",
        "--backend-store-uri",
        database_uri,
        "--artifacts-destination",
        f"s3://{bucket}/artifacts",
        "--allowed-hosts",
        allowed_hosts,
    ]


def main() -> None:
    """Valida secretos, crea el INI efímero con modo 0600 y reemplaza el proceso."""
    environment = dict(os.environ)
    database_uri = build_database_uri(environment)
    _required(environment, "MLFLOW_FLASK_SERVER_SECRET_KEY", secret=True)
    AUTH_CONFIG_PATH.write_text(render_auth_config(environment, database_uri), encoding="utf-8")
    AUTH_CONFIG_PATH.chmod(0o600)
    os.environ["MLFLOW_AUTH_CONFIG_PATH"] = str(AUTH_CONFIG_PATH)
    os.execvp("mlflow", server_arguments(environment, database_uri))


if __name__ == "__main__":
    main()
