"""Pruebas unitarias de separación, rotación y mínimo privilegio MLflow."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path
from types import SimpleNamespace


ROOT = Path(__file__).parents[3]
MODULE = ROOT / "infrastructure/mlflow/bootstrap_access.py"
SPEC = importlib.util.spec_from_file_location("reserly_mlflow_access", MODULE)
assert SPEC and SPEC.loader
access = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = access
SPEC.loader.exec_module(access)


class MissingUser(Exception):
    pass


class FakeClient:
    """Doble mínimo que conserva únicamente metadatos, nunca hashes de secretos."""

    def __init__(self) -> None:
        self.users = set()
        self.password_updates = []
        self.roles = []
        self.permissions = {}
        self.assignments = {}

    def get_user(self, username):
        if username not in self.users:
            raise MissingUser("RESOURCE_DOES_NOT_EXIST: user does not exist")
        return SimpleNamespace(username=username)

    def create_user(self, username, password):
        self.users.add(username)

    def update_user_password(self, username, password):
        self.password_updates.append(username)

    def list_all_roles(self): return self.roles

    def create_role(self, workspace, name, description=None):
        role = SimpleNamespace(id=len(self.roles) + 1, workspace=workspace, name=name)
        self.roles.append(role)
        self.permissions[role.id] = []
        return role

    def list_role_permissions(self, role_id): return self.permissions[role_id]

    def add_role_permission(self, role_id, resource_type, resource_pattern, permission):
        self.permissions[role_id].append(SimpleNamespace(
            resource_type=resource_type, resource_pattern=resource_pattern, permission=permission
        ))

    def list_user_roles(self, username): return self.assignments.get(username, [])

    def assign_role(self, username, role_id):
        role = next(role for role in self.roles if role.id == role_id)
        self.assignments.setdefault(username, []).append(role)


class MlflowAccessTests(unittest.TestCase):
    def environment(self):
        values = {
            "RESERLY_ENVIRONMENT": "local",
            "RESERLY_MLFLOW_ADMIN_USERNAME": "reserly-mlops-admin",
            "RESERLY_MLFLOW_ADMIN_PASSWORD": "admin-password-with-at-least-32-characters",
        }
        for index, purpose in enumerate(access.PURPOSES, 1):
            prefix = f"RESERLY_MLFLOW_{purpose.upper()}"
            values[f"{prefix}_USERNAME"] = f"reserly-local-{purpose}-v1"
            values[f"{prefix}_PASSWORD"] = f"{purpose}-{index}-password-with-at-least-32-characters"
            values[f"{prefix}_SECRET_VERSION"] = f"{purpose}-2026-08-v1"
        return values

    def test_policy_separates_three_roles_and_transactional_access(self):
        policy = access.load_policy()
        self.assertTrue(policy["independentDeploymentPerEnvironment"])
        self.assertFalse(policy["crossEnvironmentAccessAllowed"])
        self.assertEqual({("experiment", "*", "EDIT")}, access.desired_permissions(policy, "training"))
        self.assertEqual({("registered_model", "*", "READ")}, access.desired_permissions(policy, "inference"))
        self.assertNotEqual(
            access.desired_permissions(policy, "training"),
            access.desired_permissions(policy, "registration"),
        )
        compose = (ROOT / "infrastructure/compose.yaml").read_text(encoding="utf-8")
        bootstrap = compose.split("  mlflow-access-bootstrap:\n", 1)[1].split(
            "\n  prefect-postgres:", 1
        )[0]
        self.assertIn("condition: service_healthy", bootstrap)
        self.assertIn("read_only: true", bootstrap)
        self.assertIn("RESERLY_MLFLOW_INFERENCE_SECRET_VERSION", bootstrap)
        self.assertNotIn("RESERLY_DATABASE_PASSWORD", bootstrap)

    def test_principals_are_versioned_separate_and_secrets_unique(self):
        configuration = access.AccessConfiguration.from_environment(self.environment())
        self.assertEqual(3, len({item.username for item in configuration.identities}))
        for item in configuration.identities:
            self.assertIn(f"-{item.purpose}-v1", item.username)

        duplicate = self.environment()
        duplicate["RESERLY_MLFLOW_INFERENCE_PASSWORD"] = duplicate["RESERLY_MLFLOW_TRAINING_PASSWORD"]
        with self.assertRaisesRegex(RuntimeError, "MLFLOW_IDENTITY_REUSE_FORBIDDEN"):
            access.AccessConfiguration.from_environment(duplicate)

    def test_bootstrap_is_idempotent_and_rotates_existing_passwords(self):
        client = FakeClient()
        configuration = access.AccessConfiguration.from_environment(self.environment())
        policy = access.load_policy()
        access.apply_access(client, configuration, policy)
        access.apply_access(client, configuration, policy)
        self.assertEqual(3, len(client.users))
        self.assertEqual(3, len(client.roles))
        self.assertEqual(3, len(client.password_updates))
        self.assertTrue(all(len(roles) == 1 for roles in client.assignments.values()))

    def test_permission_drift_fails_instead_of_silently_escalating(self):
        client = FakeClient()
        configuration = access.AccessConfiguration.from_environment(self.environment())
        policy = access.load_policy()
        access.apply_access(client, configuration, policy)
        training = next(role for role in client.roles if role.name.endswith("training"))
        client.permissions[training.id].append(SimpleNamespace(
            resource_type="registered_model", resource_pattern="*", permission="MANAGE"
        ))
        with self.assertRaisesRegex(RuntimeError, "MLFLOW_ROLE_PERMISSION_DRIFT:training"):
            access.apply_access(client, configuration, policy)

    def test_short_secret_and_cross_environment_principal_fail(self):
        short = self.environment()
        short["RESERLY_MLFLOW_TRAINING_PASSWORD"] = "short"
        with self.assertRaisesRegex(RuntimeError, "MLFLOW_ACCESS_SECRET_TOO_SHORT"):
            access.AccessConfiguration.from_environment(short)
        cross = self.environment()
        cross["RESERLY_MLFLOW_INFERENCE_USERNAME"] = "reserly-production-inference-v1"
        with self.assertRaisesRegex(RuntimeError, "MLFLOW_PRINCIPAL_VERSION_INVALID:inference"):
            access.AccessConfiguration.from_environment(cross)


if __name__ == "__main__":
    unittest.main()
