"""Punto de entrada ASGI; exige configuración válida al arrancar."""

from .application import create_app
from .config import DemandEngineSettings

app = create_app(DemandEngineSettings.from_env())
