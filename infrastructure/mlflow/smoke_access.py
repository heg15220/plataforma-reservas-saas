"""Smoke real de RBAC: prueba capacidades positivas y negativas sin mostrar secretos."""

from __future__ import annotations

import os

from mlflow import MlflowClient


TRACKING_URI = "http://mlflow:5000"
EXPERIMENT = os.environ.get("RESERLY_MLFLOW_SMOKE_EXPERIMENT", "reserly-smoke-task-23-1")
MODEL = os.environ.get("RESERLY_MLFLOW_SMOKE_MODEL", "reserly-smoke-model-task-23-1")


def client(purpose: str) -> MlflowClient:
    """Construye un cliente aislado sustituyendo la identidad anterior del proceso."""
    prefix = f"RESERLY_MLFLOW_{purpose.upper()}"
    os.environ["MLFLOW_TRACKING_USERNAME"] = os.environ[f"{prefix}_USERNAME"]
    os.environ["MLFLOW_TRACKING_PASSWORD"] = os.environ[f"{prefix}_PASSWORD"]
    return MlflowClient(TRACKING_URI)


def must_deny(operation, code: str) -> None:
    """Acepta únicamente denegación 403/permission; cualquier éxito rompe mínimo privilegio."""
    try:
        operation()
    except Exception as error:
        message = str(error).lower()
        if "403" not in message and "permission" not in message:
            raise RuntimeError(f"MLFLOW_RBAC_UNEXPECTED_ERROR:{code}") from error
        return
    raise RuntimeError(f"MLFLOW_RBAC_EXCESSIVE_ACCESS:{code}")


def main() -> None:
    """Verifica la matriz efectiva sobre recursos reales creados por el smoke de 23.1."""
    training = client("training")
    experiment = training.get_experiment_by_name(EXPERIMENT)
    if experiment is None:
        raise RuntimeError("MLFLOW_RBAC_SMOKE_EXPERIMENT_MISSING")
    training.set_experiment_tag(experiment.experiment_id, "reserly.rbac.smoke", "training")
    must_deny(lambda: training.get_registered_model(MODEL), "training-model-read")

    registration = client("registration")
    experiment = registration.get_experiment_by_name(EXPERIMENT)
    if experiment is None:
        raise RuntimeError("MLFLOW_RBAC_REGISTRATION_EXPERIMENT_MISSING")
    registration.set_registered_model_tag(MODEL, "reserly.rbac.smoke", "registration")
    must_deny(
        lambda: registration.set_experiment_tag(
            experiment.experiment_id, "reserly.rbac.forbidden", "registration"
        ),
        "registration-experiment-edit",
    )

    inference = client("inference")
    inference.get_registered_model(MODEL)
    must_deny(
        lambda: inference.set_registered_model_tag(MODEL, "reserly.rbac.forbidden", "inference"),
        "inference-model-edit",
    )
    if os.environ.get("RESERLY_MLFLOW_SMOKE_REQUIRE_ALIASES") == "true":
        champion = inference.get_model_version_by_alias(MODEL, "champion")
        shadow = inference.get_model_version_by_alias(MODEL, "shadow")
        must_deny(
            lambda: inference.set_registered_model_alias(MODEL, "champion", shadow.version),
            "inference-alias-edit",
        )
        if champion.version == shadow.version:
            raise RuntimeError("MLFLOW_RBAC_SMOKE_ALIAS_VERSIONS_EQUAL")
    print(
        '{"experiment":"read/edit separation verified",'
        '"model":"manage/read separation verified","secretsLogged":false}'
    )


if __name__ == "__main__":
    main()
