"""Arranque Uvicorn sin shell ni interpolación de secretos."""

import uvicorn

from .config import DemandEngineSettings


def run() -> None:
    """Valida settings y arranca un único proceso; el orquestador controla workers."""
    settings = DemandEngineSettings.from_env()
    uvicorn.run(
        "reserly_demand_engine.main:app",
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level.lower(),
        access_log=False,
    )
