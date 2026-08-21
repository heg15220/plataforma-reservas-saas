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

El baseline logístico de conversión se entrena offline con un dataset JSON gobernado:

```powershell
reserly-demand-train-conversion --dataset path\dataset.json `
  --policy policies\conversion-logistic-training.v1.json `
  --model-card models\conversion-logistic-baseline.v1.model-card.json `
  --output path\artifact.json
```

La CLI valida revocaciones, finalidad, allowlist, madurez de etiquetas y los tres splits temporales.
El artefacto es JSON no ejecutable y candidato; escribirlo no lo registra ni promueve.

El modelo de elección condicional usa conjuntos completos y la opción de no elegir:

```powershell
reserly-demand-train-choice --dataset path\choice-dataset.json `
  --policy policies\discrete-choice-training.v1.json `
  --model-card models\discrete-choice-baseline.v1.model-card.json `
  --output path\choice-artifact.json
```

La CLI rechaza conjuntos truncados y features posteriores a la elección. Sus coeficientes y odds ratios
son asociaciones condicionales al conjunto; no autorizan cambiar precio, elegibilidad o capacidad.

La analítica de conversión por local se calcula offline desde exposiciones minimizadas:

```powershell
reserly-demand-conversion-analytics --dataset path\conversion-analytics.json `
  --policy policies\conversion-analytics.v1.json `
  --ontology ..\..\packages\demand-contracts\ontology\personal-care.v1.json `
  --authorized-venue-id 00000000-0000-0000-0000-000000000000 `
  --output path\conversion-report.json
```

La CLI falla si el local autorizado no coincide. Los grupos con menos de treinta exposiciones o cinco
resultados de cada clase ocultan conteos, tasa e intervalo; las asociaciones nunca se presentan como
efectos causales.

El linaje MLOps se valida offline antes de registrar o promover:

```powershell
reserly-demand-validate-lineage lineage\end-to-end-lineage.v1.json `
  --repository-root ..\..
```

El manifiesto enlaza por SHA-256 dataset, features, ontología, embedding, configuración, modelo,
experimento y decisión de promoción. La CLI verifica el DAG y los ficheros `repo://`, imprime solo la
versión/digest canónicos y no registra ni promueve automáticamente.

La evidencia agregada de datos se valida antes de cada entrenamiento y promoción:

```powershell
reserly-demand-validate-data path\to\data-validation-evidence.json
```

La decisión cubre esquema, calidad, PSI, PII, leakage y sesgo. El código de salida es uno ante
cualquier gate fallido; su digest de evidencia solo admite la etapa y `datasetVersion` evaluados.
Los perfiles no deben incluir filas, muestras de valores, identidades ni atributos sensibles.

Evidently aporta un segundo diagnóstico de calidad y drift sobre una proyección Parquet minimizada:

```powershell
reserly-demand-report-evidently `
  --current path\current.parquet `
  --reference path\reference.parquet `
  --validation-evidence path\data-validation-evidence.json `
  --output-directory path\reports `
  --evaluated-at 2026-08-21T12:00:00+00:00
```

Ambos Parquet deben contener exactamente las mismas columnas allowlisted por
`evidently-report-v1`; identificadores, texto, vectores, categorías libres, NaN/infinito y columnas
desconocidas fallan cerrado. El comando vuelve a ejecutar `data-validation-v1`, guarda JSON/HTML
agregados y un manifiesto con hashes. `reviewRequired` es asesor y
`promotionAuthorized` siempre es `false`: el informe nunca sustituye el gate ni la aprobación humana.

El rollout de modelos usa `policies/model-rollout.v1.json` y el controlador `model_rollout.py`.
Shadow siempre responde con champion y evalúa el candidato en espejo. Canary asigna exclusivamente
por hash estable de `requestId`, avanza 1/5/10/25/50/100 % con comparación agregada y vuelve a
champion ante cualquier guardrail. Tras el último escalón aún se requiere `PromotionApproval` humana;
el cambio de aliases conserva `previous-champion`. Kill switch, registry no disponible o rollback
fallido activan `fallback-mvp-v1` sin inventar una versión de modelo.

El endpoint de scrape `GET /internal/demand/v1/metrics` publica exclusivamente series Prometheus
agregadas. No requiere el token funcional para permitir scraping desde la red interna, pero nunca debe
publicarse fuera de esa red. Las rutas se normalizan contra las plantillas FastAPI y todos los demás
labels usan allowlists cerradas; no se aceptan UUID, versiones libres, texto, payloads ni atributos
personales. `DemandMetrics` es la frontera tipada para que ranking, ingesta y jobs MLOps actualicen
latencia, errores, drift, calibración, cobertura, diversidad, exposición, rollout y valor.
