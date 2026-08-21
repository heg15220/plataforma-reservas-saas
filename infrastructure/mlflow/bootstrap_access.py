"""Bootstrap idempotente de identidades MLOps con mínimo privilegio.

La ejecución usa temporalmente el administrador ya inyectado para crear/rotar tres principales de
servicio y asignar roles exactos. Nunca imprime contraseñas, nunca concede admin/workspace MANAGE y
falla si un rol existente tiene permisos distintos de la política versionada.
"""

from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol


POLICY_PATH = Path(__file__).with_name("access-policy.v1.json")
MINIMUM_SECRET_LENGTH = 32
PURPOSES = ("training", "registration", "inference")


class AuthClient(Protocol):
    """Superficie mínima del cliente MLflow para facilitar pruebas sin servidor."""

    def get_user(self, username: str): ...
    def create_user(self, username: str, password: str): ...
    def update_user_password(self, username: str, password: str): ...
    def list_all_roles(self): ...
    def create_role(self, workspace: str, name: str, description: str | None = None): ...
    def list_role_permissions(self, role_id: int): ...
    def add_role_permission(
        self, role_id: int, resource_type: str, resource_pattern: str, permission: str
    ): ...
    def list_user_roles(self, username: str): ...
    def assign_role(self, username: str, role_id: int): ...


@dataclass(frozen=True, slots=True)
class ServiceIdentity:
    """Principal activo y referencia de secreto; el secreto nunca forma parte de repr/logs."""

    purpose: str
    username: str
    password: str
    secret_version: str


@dataclass(frozen=True, slots=True)
class AccessConfiguration:
    """Configuración validada para un único despliegue ambiental."""

    environment: str
    admin_username: str
    admin_password: str
    identities: tuple[ServiceIdentity, ...]

    @classmethod
    def from_environment(cls, values: dict[str, str]) -> "AccessConfiguration":
        """Rechaza reutilización, principales no versionados y secretos débiles/multilínea."""
        environment = _required(values, "RESERLY_ENVIRONMENT")
        admin_username = _required(values, "RESERLY_MLFLOW_ADMIN_USERNAME")
        admin_password = _required(values, "RESERLY_MLFLOW_ADMIN_PASSWORD", secret=True)
        identities = []
        for purpose in PURPOSES:
            prefix = f"RESERLY_MLFLOW_{purpose.upper()}"
            identity = ServiceIdentity(
                purpose=purpose,
                username=_required(values, f"{prefix}_USERNAME"),
                password=_required(values, f"{prefix}_PASSWORD", secret=True),
                secret_version=_required(values, f"{prefix}_SECRET_VERSION"),
            )
            expected = rf"^reserly-{re.escape(environment)}-{purpose}-v[1-9][0-9]*$"
            if not re.fullmatch(expected, identity.username):
                raise RuntimeError(f"MLFLOW_PRINCIPAL_VERSION_INVALID:{purpose}")
            identities.append(identity)
        usernames = [admin_username, *(item.username for item in identities)]
        passwords = [admin_password, *(item.password for item in identities)]
        if len(usernames) != len(set(usernames)) or len(passwords) != len(set(passwords)):
            raise RuntimeError("MLFLOW_IDENTITY_REUSE_FORBIDDEN")
        return cls(environment, admin_username, admin_password, tuple(identities))


def _required(values: dict[str, str], name: str, *, secret: bool = False) -> str:
    value = values.get(name, "")
    if not value or "\n" in value or "\r" in value:
        raise RuntimeError(f"MLFLOW_ACCESS_CONFIGURATION_INVALID:{name}")
    if secret and len(value) < MINIMUM_SECRET_LENGTH:
        raise RuntimeError(f"MLFLOW_ACCESS_SECRET_TOO_SHORT:{name}")
    return value


def load_policy(path: Path = POLICY_PATH) -> dict[str, object]:
    """Valida estructura crítica y evita ampliar recursos mediante JSON inesperado."""
    policy = json.loads(path.read_text(encoding="utf-8"))
    if (
        policy.get("schemaVersion") != 1
        or policy.get("policyVersion") != "mlflow-access-v1"
        or set(policy.get("roles", {})) != set(PURPOSES)
        or policy.get("workspace") != "default"
        or policy.get("independentDeploymentPerEnvironment") is not True
        or policy.get("crossEnvironmentAccessAllowed") is not False
    ):
        raise RuntimeError("MLFLOW_ACCESS_POLICY_INVALID")
    return policy


def desired_permissions(policy: dict[str, object], purpose: str) -> set[tuple[str, str, str]]:
    """Extrae la allowlist y rechaza admin, workspace MANAGE o tipos desconocidos."""
    roles = policy["roles"]
    assert isinstance(roles, dict)
    role = roles[purpose]
    assert isinstance(role, dict)
    if role.get("transactionalDatabaseAccess") is not False:
        raise RuntimeError("MLFLOW_TRANSACTIONAL_ACCESS_FORBIDDEN")
    permissions = {
        (item["resourceType"], item["resourcePattern"], item["permission"])
        for item in role["permissions"]
    }
    allowed_types = {"experiment", "registered_model"}
    allowed_levels = {"READ", "EDIT", "MANAGE"}
    if (
        any(resource not in allowed_types or level not in allowed_levels for resource, _, level in permissions)
        or any(resource == "experiment" and level == "MANAGE" for resource, _, level in permissions)
        or purpose != "registration" and any(level == "MANAGE" for _, _, level in permissions)
    ):
        raise RuntimeError("MLFLOW_EXCESSIVE_PERMISSION_FORBIDDEN")
    return permissions


def apply_access(client: AuthClient, configuration: AccessConfiguration, policy: dict[str, object]) -> None:
    """Crea o rota usuarios y roles; cualquier drift de permisos detiene el bootstrap."""
    workspace = str(policy["workspace"])
    existing_roles = {(role.workspace, role.name): role for role in client.list_all_roles()}
    for identity in configuration.identities:
        try:
            client.get_user(identity.username)
        except Exception as error:  # MLflow usa RestException; el protocolo mantiene import opcional.
            if "RESOURCE_DOES_NOT_EXIST" not in str(error) and "does not exist" not in str(error):
                raise
            client.create_user(identity.username, identity.password)
        else:
            # Actualización in-place permite rotación coordinada; un nuevo username vN permite solape.
            client.update_user_password(identity.username, identity.password)

        role_name = f"reserly-{configuration.environment}-{identity.purpose}"
        role = existing_roles.get((workspace, role_name))
        if role is None:
            role = client.create_role(
                workspace=workspace,
                name=role_name,
                description=f"Reserly {identity.purpose} least-privilege role",
            )
            existing_roles[(workspace, role_name)] = role
        desired = desired_permissions(policy, identity.purpose)
        current_objects = client.list_role_permissions(role.id)
        current = {
            (item.resource_type, item.resource_pattern, item.permission) for item in current_objects
        }
        if current and current != desired:
            raise RuntimeError(f"MLFLOW_ROLE_PERMISSION_DRIFT:{identity.purpose}")
        for resource_type, resource_pattern, permission in sorted(desired - current):
            client.add_role_permission(role.id, resource_type, resource_pattern, permission)
        assigned_ids = {assigned.id for assigned in client.list_user_roles(identity.username)}
        if role.id not in assigned_ids:
            client.assign_role(identity.username, role.id)


def main() -> None:
    """Autentica como admin solo durante bootstrap y no conserva esa identidad en clientes."""
    configuration = AccessConfiguration.from_environment(dict(os.environ))
    policy = load_policy()
    if configuration.environment not in policy["supportedEnvironments"]:
        raise RuntimeError("MLFLOW_ENVIRONMENT_UNSUPPORTED")
    os.environ["MLFLOW_TRACKING_USERNAME"] = configuration.admin_username
    os.environ["MLFLOW_TRACKING_PASSWORD"] = configuration.admin_password
    from mlflow.server import get_app_client

    client = get_app_client("basic-auth", tracking_uri="http://mlflow:5000")
    apply_access(client, configuration, policy)
    print(json.dumps({
        "policyVersion": policy["policyVersion"],
        "environment": configuration.environment,
        "principals": [identity.username for identity in configuration.identities],
        "secretsLogged": False,
    }, sort_keys=True))


if __name__ == "__main__":
    main()
