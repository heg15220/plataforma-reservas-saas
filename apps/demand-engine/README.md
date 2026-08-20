# Reserly Demand Engine

Servicio FastAPI interno y consultivo. Spring conserva autoridad sobre publicación, filtros,
disponibilidad, capacidad, holds y reservas. El navegador nunca debe llamar este servicio.

## Desarrollo local

```powershell
$env:PYTHONPATH='src;../../packages/demand-contracts/src'
$env:RESERLY_DEMAND_ENGINE_ENVIRONMENT='local'
$env:RESERLY_DEMAND_ENGINE_SERVICE_ID='spring-api'
$env:RESERLY_DEMAND_ENGINE_SERVICE_TOKEN='replace-with-at-least-32-random-characters'
python -m reserly_demand_engine
```

Health checks internos:

- `GET /internal/demand/v1/health/live`
- `GET /internal/demand/v1/health/ready`

Los endpoints funcionales exigen `X-Reserly-Service-Id` y `X-Reserly-Service-Token`. Los logs nunca
incluyen bodies, candidatos, texto libre, tokens ni datos personales.

El perfil inicial se calcula mediante `POST
/internal/demand/v1/venues/{venue_id}/attributes/evaluate` y se consulta con `GET
/internal/demand/v1/venues/{venue_id}/attributes`. Solo admite el piloto de cuidado personal con cita
individual y devuelve fuentes/reglas versionadas; Spring debe persistir la proyección porque la caché
local LRU no es autoritativa ni durable.

El explorador básico se invoca con `POST /internal/demand/v1/exploration/select` después de que
Spring haya formado el conjunto elegible. Aplica de nuevo restricciones duras, calidad mínima y una
cuota máxima del 10 % mediante Thompson Sampling Beta-Bernoulli reproducible por `requestId`. Los
outcomes se aplican con `POST /internal/demand/v1/exploration/update`; Spring debe persistir el
posterior y el ledger de `outcomeEventId` con unicidad y actualización atómica. El servicio no
mantiene estado durable ni puede publicar, reservar o consumir capacidad.

## Tests

```powershell
$env:PYTHONPATH='src;../../packages/demand-contracts/src'
python -m unittest discover -s tests -v
```

La promoción de ranking se evalúa con artefactos versionados y un snapshot agregado:

```powershell
$env:PYTHONPATH='src;../../packages/demand-contracts/src'
python -m reserly_demand_engine.promotion path\to\promotion-snapshot.json
```

La CLI exige coincidencia exacta de política, dataset y baseline. Distingue `shadowToPilot` de
`pilotToRollout`, falla ante métricas ausentes o desconocidas y nunca acepta filas individuales.
