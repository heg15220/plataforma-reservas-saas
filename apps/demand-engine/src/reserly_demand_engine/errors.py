"""Errores opacos compartidos por todas las fronteras HTTP internas."""

from dataclasses import dataclass


@dataclass(slots=True)
class DemandEngineError(Exception):
    """Error esperado sin payload ni detalle de librería."""

    code: str
    status_code: int

