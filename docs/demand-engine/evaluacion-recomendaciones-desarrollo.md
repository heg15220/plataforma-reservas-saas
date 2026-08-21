# Evaluación del sistema de recomendación en desarrollo

## 1. Objetivo

Esta guía explica cómo comprobar, desde el entorno de desarrollo, cuatro aspectos diferentes del
motor de recomendaciones de Reserly:

1. Que el servicio arranca y responde correctamente.
2. Que los contratos, restricciones y fallbacks funcionan.
3. Que los modelos alcanzan una calidad offline suficiente.
4. Que las recomendaciones producen valor real frente al baseline.

Estas comprobaciones no son equivalentes. Una suite de tests verde demuestra corrección técnica,
pero no demuestra que las recomendaciones sean útiles para usuarios reales. Del mismo modo, una
métrica offline alta no autoriza por sí sola un despliegue: también deben cumplirse privacidad,
sesgo, estabilidad, latencia, shadow/canary y revisión humana.

## 2. Estado actual conocido

La última evaluación real versionada del encoder semántico se encuentra en:

`apps/demand-engine/evaluation/results/multilingual-e5-small.v1.windows-cpu.json`

| Métrica | Resultado | Umbral | Estado |
|---|---:|---:|---|
| Recall@1 | 0,6875 | >= 0,80 | No pasa |
| Recall@3 | 0,8125 | >= 0,95 | No pasa |
| Mean Reciprocal Rank | 0,775521 | >= 0,85 | No pasa |
| Recall@3 cross-locale ES/EN | 0,625 | >= 0,90 | No pasa |
| Latencia warm de consulta p95 | 50,648 ms | <= 100 ms | Pasa |
| Latencia warm por documento p95 | 26,852 ms | <= 50 ms | Pasa |

El resultado guardado declara:

- `qualityPassed=false`.
- `latencyPassed=true`.
- `promotionStatus=not_promoted`.
- Fallback: `full_text_trigram_only`.

Por tanto, la evidencia actual indica que el componente semántico es suficientemente rápido, pero
todavía no alcanza la calidad exigida. El sistema continúa operativo mediante el ranking/fallback
determinista aprobado. Este comportamiento es intencionado: fallar una puerta de calidad no debe
dejar sin resultados al producto ni promover automáticamente un modelo deficiente.

El dataset de esta evaluación es sintético y pequeño: 16 consultas y 10 documentos de cuidado
personal en español e inglés. Sirve para validar el contrato y detectar regresiones claras, pero no
representa todo el catálogo ni demuestra rendimiento productivo.

## 3. Preparación del entorno

Todos los comandos de esta guía se ejecutan desde la raíz del repositorio:

```powershell
cd C:\Users\hugoe\Downloads\proyecto
```

Para ejecutar directamente módulos y pruebas Python:

```powershell
$env:PYTHONPATH="apps/demand-engine/src;packages/demand-contracts/src"
```

Si las dependencias de Machine Learning todavía no están instaladas:

```powershell
python -m pip install -e .\packages\demand-contracts -e ".\apps\demand-engine[ml]"
```

La evaluación real de embeddings puede necesitar descargar el modelo fijado si no está disponible
en la caché local. El manifiesto bloquea repositorio, revisión, licencia, dimensiones, idiomas y
prompts; no debe sustituirse la revisión por `main` ni por una versión mutable.

## 4. Nivel 1: comprobar que el servicio funciona

### 4.1 Arrancar infraestructura y motor

Debe existir un `.env.local` válido, incluyendo un
`RESERLY_DEMAND_ENGINE_SERVICE_TOKEN` de al menos 32 caracteres.

```powershell
npm run infra:up
npm run dev:demand
```

El segundo comando permanece ejecutándose. Las siguientes comprobaciones se realizan desde otra
consola PowerShell.

### 4.2 Comprobar health

```powershell
Invoke-RestMethod `
  http://127.0.0.1:8090/internal/demand/v1/health/live

Invoke-RestMethod `
  http://127.0.0.1:8090/internal/demand/v1/health/ready
```

`live` confirma que el proceso está vivo. `ready` confirma que puede atender tráfico según sus
dependencias y configuración. Ninguna de las dos pruebas mide la utilidad de las recomendaciones.

### 4.3 Consultar métricas

```powershell
Invoke-WebRequest `
  http://127.0.0.1:8090/internal/demand/v1/metrics |
  Select-Object -ExpandProperty Content
```

El endpoint expone métricas agregadas y con dimensiones cerradas:

- Latencia y solicitudes HTTP.
- Errores.
- Rankings y fallbacks.
- Distribución de scores.
- Drift y calibración.
- Cobertura, diversidad y exposición.
- Valor comercial agregado.
- Tráfico de rollout.
- Freshness de pipelines.
- Coste y saturación de capacidad.

No deben aparecer emails, UUID de usuarios, IDs de reservas, texto de consultas, URLs concretas,
versiones arbitrarias o valores de features como labels.

### 4.4 Iniciar Prometheus y Grafana

```powershell
npm run observability:config
npm run observability:up
npm run observability:status
```

Prometheus queda normalmente en `http://127.0.0.1:9090`. Grafana usa el puerto definido por
`RESERLY_GRAFANA_PORT`. Si la aplicación web ya ocupa el puerto 3000, debe configurarse otro puerto
local para Grafana, por ejemplo 3002, antes de iniciar el perfil.

El dashboard permite observar latencia, errores, drift, calibración, cobertura, diversidad,
exposición, valor, fallbacks, rollout, freshness, presupuesto y capacidad. Ver gráficas no sustituye
la evaluación offline ni el experimento controlado.

## 5. Nivel 2: ejecutar la validación técnica completa

### 5.1 Suite completa del Demand Engine

```powershell
npm run test:demand
```

La última ejecución verificada terminó con:

```text
Ran 259 tests
OK
```

La suite comprueba, entre otros aspectos:

- Contratos HTTP internos y autenticación.
- Ranking y fallback determinista.
- Exclusión previa por elegibilidad y restricciones duras.
- Reproducibilidad.
- Embeddings y matching semántico.
- Conversión, no-show y calibración.
- Learning-to-Rank.
- Exploración contextual.
- A/B testing.
- Drift, calidad, PII, leakage y sesgo.
- Promoción, shadow/canary y rollback.
- Diversidad y exposición de locales nuevos.
- Privacidad, linaje, auditoría y revisión humana.
- SLO, coste, capacidad, alertas y runbooks.

Una suite verde demuestra que las reglas implementadas se comportan como especifica el código. No
demuestra que el modelo generalice sobre tráfico real, especialmente cuando varios tests usan datos
sintéticos o dobles deterministas.

### 5.2 Suite focalizada de recomendación

```powershell
$env:PYTHONPATH="apps/demand-engine/src;packages/demand-contracts/src"

python -m unittest `
  apps/demand-engine/tests/test_mvp_acceptance.py `
  apps/demand-engine/tests/test_embeddings.py `
  apps/demand-engine/tests/test_promotion.py `
  apps/demand-engine/tests/test_learning_to_rank.py `
  -v
```

Esta ejecución es más rápida, pero debe tenerse en cuenta que
`test_embeddings.py` usa también un encoder falso y ortogonal para verificar el cálculo de las
métricas. Ese test demuestra que Recall/MRR se calculan correctamente; no mide la calidad del modelo
real descargado.

## 6. Nivel 3: ejecutar la evaluación offline del encoder real

```powershell
$env:PYTHONPATH="apps/demand-engine/src;packages/demand-contracts/src"

python -m reserly_demand_engine.embedding_evaluation

$LASTEXITCODE
```

También puede ejecutarse mediante el entrypoint instalado:

```powershell
reserly-demand-evaluate-embeddings
```

El evaluador carga:

- Modelo: `apps/demand-engine/models/multilingual-e5-small.v1.json`.
- Dataset: `apps/demand-engine/evaluation/personal-care-retrieval.v1.json`.
- Implementación: `apps/demand-engine/src/reserly_demand_engine/embedding_evaluation.py`.

La salida contiene únicamente métricas agregadas. No guarda embeddings ni reproduce textos en un
artefacto de resultados. El proceso devuelve código `0` cuando pasan calidad y latencia, y código
distinto de cero cuando falla alguna puerta.

Las puertas actuales son:

- Recall@1 >= 0,80.
- Recall@3 >= 0,95.
- MRR >= 0,85.
- Recall@3 cross-locale >= 0,90.
- Consulta warm p95 <= 100 ms.
- Documento warm p95 <= 50 ms por elemento.

Cada ejecución debe registrar, además de las métricas:

- Versión y revisión exacta del modelo.
- Versión del dataset.
- Fecha UTC.
- Sistema operativo, CPU/GPU y versiones de librerías.
- Estado de calidad y latencia.
- Commit productor y SHA-256 cuando el resultado pase al flujo MLOps.

## 7. Métricas apropiadas para recomendaciones

### 7.1 Por qué accuracy y F1 no bastan

Accuracy, precision, recall y F1 nacieron principalmente para clasificación. Un recomendador produce
una lista ordenada, por lo que también importa en qué posición aparece cada resultado relevante.

Si cada consulta tiene exactamente un único resultado correcto:

- La accuracy top-1 es prácticamente equivalente a Recall@1 o Hit Rate@1.
- Precision@3 será como máximo 1/3 incluso cuando la lista sea perfecta, porque solo existe un
  resultado etiquetado como relevante.
- F1 depende de un umbral o de una definición binaria del top K y pierde información del orden.

Por ello F1 no debe ser la métrica principal del ranking actual.

### 7.2 Recall@K

Mide la fracción de consultas cuyo resultado relevante aparece entre los K primeros.

```text
Recall@K = consultas con al menos un relevante en top K / consultas evaluables
```

En la implementación actual, al existir normalmente un relevante por consulta, esta medida también
se interpreta como Hit Rate@K.

Debe medirse al menos para:

- K=1: calidad de la primera recomendación.
- K=3: calidad de la zona más visible del carril.
- ES y EN por separado.
- Consultas cross-locale.
- Categorías/verticales permitidas, siempre con muestra mínima.

### 7.3 Precision@K

```text
Precision@K = resultados relevantes en top K / K
```

Es útil cuando una consulta admite varios resultados relevantes correctamente etiquetados. Para que
sea informativa, el dataset debe ampliar las etiquetas binarias actuales con varios candidatos
relevantes o relevancia graduada.

### 7.4 Mean Reciprocal Rank

```text
MRR = media de 1 / posición del primer resultado relevante
```

Premia que el primer resultado correcto aparezca pronto. Es especialmente útil cuando el usuario
necesita encontrar una primera opción adecuada.

### 7.5 NDCG@K

NDCG considera todo el orden y permite relevancia graduada. Es la métrica principal del challenger
Learning-to-Rank implementado.

El evaluador actual compara champion y challenger a K=3 mediante:

- NDCG@3.
- Conversión en top 3.
- Diversidad de categorías en top 3.
- Exposición de locales nuevos en top 3.
- Reproducibilidad de scores.

La política `learning-to-rank-evaluation-v1` exige:

- Ganancia NDCG >= 0,05.
- Ninguna pérdida de conversión.
- Ninguna regresión de diversidad.
- Ninguna regresión de exposición de locales nuevos.
- Diferencia de reproducibilidad <= 1e-8.

Una mejora NDCG que empeore conversión, diversidad o exposición no supera la puerta.

### 7.6 Métricas complementarias

También deben observarse:

- MAP@K si hay varios relevantes por consulta.
- Cobertura del catálogo y de candidatos elegibles.
- Porcentaje de solicitudes que caen al fallback.
- Diversidad intralista.
- Exposición de locales nuevos frente a establecidos.
- Tasa de restricciones o privacidad violadas, que debe ser cero.
- Latencia p50, p95 y p99.
- Error rate y disponibilidad.
- Drift PSI y estabilidad por ventana.

## 8. Métricas de modelos clasificadores

Conversión y no-show son modelos probabilísticos, no rankings puros. Para ellos sí son relevantes las
métricas de clasificación, pero deben interpretarse junto a calibración y desbalance.

La implementación actual calcula:

- ROC-AUC.
- Brier score.
- Log loss.
- Expected Calibration Error o ECE.
- Brecha Brier entre segmentos operativos permitidos.

Para conversión, los umbrales actuales son:

- ROC-AUC >= 0,70.
- Brier <= 0,22.
- ECE <= 0,15.

Precision, recall y F1 no se publican actualmente en el artefacto de conversión. Para añadirlos de
forma correcta se debe definir previamente un umbral de decisión que responda a un coste de negocio.
No debe elegirse 0,5 automáticamente.

Un informe de clasificación ampliado debería incluir:

- PR-AUC, especialmente con conversión/no-show poco frecuentes.
- Precision y recall para uno o varios umbrales aprobados.
- F1 o F-beta cuando exista un coste explícito de falsos positivos y falsos negativos.
- Matriz de confusión.
- Sensibilidad/especificidad.
- Curva de calibración.
- Métricas por segmento permitido con intervalos y muestra mínima.

Accuracy aislada puede ser engañosa: si solo el 5 % convierte, un clasificador que siempre predice
“no conversión” alcanza 95 % de accuracy y no aporta ninguna utilidad.

## 9. Construcción de un dataset de evaluación válido

Para medir calidad real deben conservarse exposiciones y resultados maduros con estas propiedades:

1. Cada consulta incluye el conjunto completo de alternativas elegibles mostrado en ese instante.
2. La disponibilidad y capacidad corresponden al instante de la recomendación.
3. Se conserva la posición, política/modelo y versión para evaluación, no como feature de training.
4. Se registra si hubo apertura, clic, hold, reserva, asistencia, cancelación y no-show.
5. El outcome debe haber madurado antes de incorporarse.
6. Train, calibración y evaluación se separan por tiempo, no mediante mezcla aleatoria que permita
   leakage futuro.
7. Identidad, email, teléfono, reserva, outcome futuro, posición y atributos sensibles no entran como
   features.
8. Las revocaciones de consentimiento se aplican antes de construir el dataset.
9. El dataset, features, política, modelo y resultados quedan versionados por digest.
10. Se conserva una población de control/baseline comparable.

Las métricas deben calcularse por consulta, no tratando cada candidato como una observación
independiente, porque los candidatos de una misma lista compiten entre sí.

## 10. Evaluación offline recomendada

Para cada versión candidata:

1. Congelar un cutoff temporal.
2. Construir train, calibración y holdout futuro.
3. Ejecutar primero el baseline determinista.
4. Ejecutar el candidato sobre exactamente los mismos conjuntos elegibles.
5. Calcular Recall@1/3, MRR, NDCG@3, conversión@3, diversidad y exposición.
6. Calcular intervalos mediante bootstrap por consulta cuando haya muestra suficiente.
7. Comparar diferencias candidato-baseline, no solo valores absolutos.
8. Ejecutar gates de PII, leakage, sesgo, drift, latencia y reproducibilidad.
9. Conservar el informe, dataset/policy/model versions y SHA-256.
10. Si pasa, abrir revisión humana; nunca desplegar automáticamente.

Los datos sintéticos permiten comprobar el evaluador y escenarios conocidos, pero
`productionEvidence=false` debe impedir que autoricen una promoción.

## 11. Evaluación online mediante shadow y A/B

La evaluación offline no mide cómo reaccionan usuarios reales. La secuencia segura es:

1. **Shadow:** champion produce la respuesta y challenger evalúa en espejo sin afectar al usuario.
2. **Canary:** una fracción determinista y pequeña del tráfico usa el candidato.
3. **A/B:** control y tratamiento mantienen asignación estable y criterios prerregistrados.
4. **Rollout:** solo tras muestra, potencia, guardrails, revisión humana y promoción atómica.

Las métricas online primarias deben incluir:

- Conversión de sesión/exposición a reserva.
- Reservas asistidas, generadas y recuperadas.
- Clientes nuevos y recurrentes.
- Ocupación de horas valle.
- Ingreso neto realizado.
- Coste de activación y coste incremental por cliente.
- Retorno incremental cuando el diseño causal lo permita.

Guardrails obligatorios:

- Cancelación.
- No-show.
- Errores y timeouts.
- Latencia p95/p99.
- Cobertura y fallback.
- Diversidad y exposición de locales nuevos.
- Restricciones duras y privacidad, ambas con tolerancia cero.

La política de promoción actual exige para pasar de piloto a rollout:

- Al menos 1.000 sesiones por variante.
- Al menos 100 reservas completadas.
- Muestra con potencia estadística.
- Confianza del 95 %.
- Periodo y asignación estables.
- Todas las métricas y guardrails requeridos.

No deben tomarse decisiones mirando repetidamente el experimento sin corrección estadística. La
política A/B existente define looks, potencia, MDE y criterios de parada.

## 12. Interpretación de resultados

### El servicio responde pero las métricas offline fallan

El sistema funciona técnicamente, pero el modelo no debe promoverse. Se mantiene el champion o el
fallback y se investiga dataset, modelo, prompts, etiquetado y cobertura.

### Las métricas offline pasan pero shadow falla

Puede existir drift, diferencia de infraestructura, latencia, errores, datos no representativos o
leakage en la evaluación. Se bloquea promoción y se conserva el champion.

### Offline y shadow pasan, pero A/B no mejora

El modelo ordena correctamente según las etiquetas offline, pero no produce valor incremental. Debe
mantenerse el baseline o declararse el experimento fútil; no se deben buscar segmentos a posteriori
para justificarlo.

### Mejora conversión pero empeora diversidad, exposición o no-show

No supera la puerta multimetric. El objetivo del sistema no es maximizar conversión ignorando
equidad, experiencia, seguridad o funcionamiento del marketplace.

### No existe muestra suficiente

El resultado es `insufficient` o inconcluso. No equivale a cero ni a éxito. Debe ampliarse el periodo
sin cambiar hipótesis, métricas o asignación durante el experimento.

## 13. Checklist mínimo de aceptación

Antes de afirmar que una versión “funciona”:

- [ ] Health y readiness correctos.
- [ ] Suite técnica completa verde.
- [ ] Dataset etiquetado, versionado y con holdout temporal.
- [ ] Recall@K, MRR y NDCG comparados con baseline.
- [ ] Conversión/calibración evaluadas cuando corresponda.
- [ ] Latencia p95/p99 dentro del presupuesto.
- [ ] Cero violaciones de privacidad y restricciones duras.
- [ ] Sin regresión de diversidad o exposición.
- [ ] Drift, estabilidad y reproducibilidad dentro de umbral.
- [ ] Shadow correcto.
- [ ] A/B con muestra, potencia, intervalos y guardrails.
- [ ] Revisión humana registrada.
- [ ] Rollback y fallback comprobados.

## 14. Conclusión aplicada al estado actual

El motor está implementado y protegido para degradar de forma segura. La suite técnica demuestra que
el pipeline, los contratos y los fallbacks funcionan conforme a diseño. Sin embargo, la última
evidencia versionada del encoder semántico no alcanza Recall@1, Recall@3, MRR ni recall cross-locale.
Por ello, actualmente no debe afirmarse que el modelo semántico esté listo para promoción.

El paso recomendado es ampliar el dataset etiquetado ES/EN con consultas y catálogos más
representativos, ejecutar de nuevo el encoder real y producir un informe comparativo único entre:

- Fallback full-text/trigram.
- Recuperación semántica por embeddings.
- Ranking MVP.
- Challenger Learning-to-Rank.

Ese informe debe incluir Recall@K, Precision@K cuando haya varios relevantes, MRR, NDCG@K,
diversidad, exposición, latencia, calibración cuando aplique y diferencias de negocio frente al
baseline. Solo después debe abrirse shadow y posteriormente un A/B con datos reales.
