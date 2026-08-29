# Dataset sintético de marketplace v1

Este dataset sirve para desarrollar y comprobar pipelines de recomendación sin usar datos personales
ni presentar resultados sintéticos como evidencia productiva. Contiene exactamente 100 locales
ficticios, 40 perfiles pseudónimos, 2.400 sesiones temporales y 19.200 candidatos. Cubre las ocho
categorías activas del catálogo: restaurante, peluquería, campo de fútbol, pista de pádel, instalación
municipal, centro deportivo, centro de estética y otros.

## Artefactos

- `venues.jsonl`: locales ficticios de las ocho categorías canónicas, descripciones ES/EN, servicios,
  atributos y ubicación aproximada sintética en diez zonas gallegas. No contiene direcciones ni
  negocios reales. Cada categoría aparece en warm, validation-cold y test-cold.
- `profiles.jsonl`: preferencias permitidas y consentimiento simulado. No contiene email, teléfono,
  edad, género, salud, pagos u otros atributos sensibles.
- `ranking-sessions.jsonl`: conjuntos completos de ocho candidatos, features anteriores al resultado
  y labels separados. Incluye ruido intencionado para impedir una relación perfecta.
- `image-prompts.jsonl`: 100 especificaciones visuales únicas. No son imágenes ni están autorizadas
  como input de entrenamiento.
- `manifest.json`: versión, semilla, cortes, recuentos, hashes y limitaciones.

## Splits y cold-start

| Split | Periodo | Sesiones | Entidades admitidas |
| --- | --- | ---: | --- |
| train | 2026-01-01 a 2026-04-30 | 1.400 | `warm` |
| validation | 2026-05-01 a 2026-05-31 | 400 | `warm`, `validationCold` |
| test | 2026-06-01 a 2026-06-30 | 600 | `warm`, `validationCold`, `testCold` |

Las entidades `validationCold` no aparecen en entrenamiento. Las `testCold` no aparecen en
entrenamiento ni validación. Las métricas deben publicarse por separado para cohortes warm y
cold-start; mezclar ambas oculta fallos de generalización.

## Regeneración y verificación

Desde la raíz del repositorio:

```powershell
$env:PYTHONPATH='apps/demand-engine/src;packages/demand-contracts/src'
python -m reserly_demand_engine.synthetic_marketplace `
  --output apps/demand-engine/evaluation/synthetic-marketplace-v1
python -m unittest apps/demand-engine/tests/test_synthetic_marketplace.py
```

Con semilla `1729`, los JSON/JSONL son reproducibles byte a byte. Cambiar semilla o generador exige
nueva versión de dataset; no debe sobrescribirse una versión utilizada por un experimento registrado.

## Imágenes: materializadas, pero todavía bloqueadas

Los 100 prompts se materializaron como una imagen PNG independiente de 1448×1086 por local mediante
el generador integrado. Los binarios viven en `images/`, ignorado por Git; el repositorio conserva
`image-assets.jsonl` con `objectKey` local, SHA-256, dimensiones, procedencia y estado de revisión.
El manifiesto declara `materializedImages=100`, pero mantiene `trainingAllowed=false` porque falta
revisión humana y la puerta de categoría visual no supera recall macro.

`visual-qa-report.json` contiene la ejecución real con CLIP fijado. La QA estructural pasa: 100/100
PNG válidos, RGB, 4:3, sin metadatos, sin duplicados exactos ni perceptuales y distancia dHash mínima
15. El diagnóstico de categoría obtiene accuracy 0,74, precision macro 0,819686, recall macro
0,772565 y F1 macro 0,746995; falla el umbral de recall 0,80. Validación-cold alcanza accuracy
0,866667 y test-cold 0,80, mientras warm queda en 0,70 por confusiones históricas entre peluquería y
estética y por interiores municipales genéricos.

### Mejora v2 sin reutilizar el test

`visual-selection-v2.json` selecciona 17 sustituciones versionadas únicamente a partir de errores de
la cohorte warm: once peluquerías y seis instalaciones municipales. Conserva los originales en
`images/` y los reemplazos en `images-v2/`; no rebaja el umbral ni modifica CLIP. En desarrollo warm,
el clasificador congelado pasa de accuracy 0,70/recall macro 0,752232 a
0,942857/0,916667. Peluquería e instalación municipal alcanzan recall 1,00; persiste una debilidad
visible en centro deportivo (recall 0,50), por lo que el resultado no se presenta como perfección.

La confirmación exploratoria se abrió una sola vez sobre `visual-holdout-v2`: 24 imágenes nuevas,
equilibradas 3×8, generadas independientemente y congeladas antes de ejecutar CLIP. La QA estructural
pasa 24/24, sin duplicados exactos/perceptuales y con dHash mínimo 18. El diagnóstico obtiene
accuracy, precision macro, recall macro y F1 macro 1,00. Esta cifra perfecta se interpreta como un
holdout sintético pequeño y fácil, no como sobreajuste demostrado ni como generalización productiva.
La validación-cold previa no modificada (recall 0,875) y el desarrollo v2 (0,916667) se conservan como
contraste menos optimista. `trainingAllowed=false`, `humanReviewCompleted=false` y
`overallPassed=false` permanecen invariables hasta revisión humana independiente y futuras pruebas
con imágenes reales o ambiguas.

Tras acordar el criterio de generalización, este holdout conserva
`automatedQualityPassed=true` solo para el diagnóstico de categoría histórico, pero el manifiesto
declara `benchmarkAdequacyPassed=false` y `benchmarkDifficultyReviewRequired=true`: 3 muestras por
categoría y un 1,00 sintético no satisfacen la puerta representativa.

El entrenamiento supervisado futuro usa `clip-linear-category-training-v1`: CLIP congelado y una
cabeza softmax con L2/early stopping elegidos solo en validación. Requiere 200 imágenes aprobadas y
disjuntas: 80 train, 40 validación y 80 test. La puerta real sube a accuracy test 0,90, error 0,10,
métricas macro 0,80, recall mínimo por categoría 0,70 y brecha train-test 0,10. Actualmente hay cero
activos autorizados, por lo que `visual-training-readiness.v1.json` declara
`actualTrainingExecuted=false`; el 0,916667 registrado corresponde solo al fixture contractual.

### Dataset provisional detenido en 120 imágenes

La generación del stress test se detuvo por decisión del usuario después de materializar 83 PNG
nuevos: 33 destinados inicialmente a validación y 50 destinados inicialmente a test. Sumados a los
100 activos de desarrollo disponibles, existen 183 candidatos físicos sin contar el holdout v2 ya
consumido. No se generarán más imágenes en esta iteración.

Para obtener un experimento honesto con lo disponible, `visual-training-dataset-v1/provisional-definition.json`
congela 120 imágenes y entidades únicas, exactamente 15 por cada categoría. El split provisional es
40 train (5 por categoría), 24 validación (3 por categoría) y 56 test (7 por categoría). No duplica
hashes ni venues entre splits, no usa el holdout consumido y conserva un único presupuesto de
apertura de test. Este reparto **no** satisface el contrato definitivo 80/40/80 ni permite completar
23.16.c.3.b.

`provisional-qa-report.json` confirma 120/120 activos materializados, 120 SHA-256 únicos, cero pares
perceptualmente duplicados, distancia dHash mínima 13 y cero violaciones estructurales. QA no ejecutó
inferencia: `testPredictionsObserved=false`. Los 120 activos continúan `pending`, con
`developmentTrainingAllowed=false`; primero deben aprobarse humanamente mediante
`HUMAN_REVIEW.md`. Solo después podrá construirse el dataset de embeddings, entrenarse una cabeza
provisional y abrirse su test una vez. Aun si alcanza 0,90, seguirá siendo evidencia sintética
provisional y no promoción productiva.

La revisión humana se completó el 29-08-2026 para los 120 activos provisionales. Después se generaron
embeddings CLIP 512-d normalizados y se entrenó la cabeza lineal con L2 elegido solo por validación.
El único test consumido obtiene 49/56: accuracy 0,875, error 0,125, precision macro 0,890625, recall
macro 0,875 y F1 macro 0,864541. Train obtiene 1,00 y validación 0,916667, por lo que la brecha
train-test 0,125 también falla. `otros` es la debilidad principal: recall 0,285714 y cinco de siete
casos confundidos con instalación municipal. `gatesPassed=false`, promoción continúa prohibida y no
se reabre el test para corregir el resultado.

### Stress test v2 independiente

El test provisional consumido se reclasifica íntegramente como desarrollo. Sus 120 activos aprobados
se redistribuyen antes del nuevo entrenamiento en 80 train y 40 validación, exactamente 10/5 por
categoría. `visual-training-dataset-v2` añade 80 imágenes de test completamente nuevas, diez por
categoría, con prompts, imageId y venueId congelados antes de materializar.

La QA v2 pasa 200/200: balance 80/40/80, 25 activos por categoría, 200 hashes únicos, cero clones
dHash <=4, distancia mínima 13 y `testPredictionsObserved=false`. Una imagen municipal generada con
banderas/escudo se sustituyó de forma versionada antes de inferencia; el original se conserva fuera
de Git. Los 120 activos de desarrollo están aprobados y los 80 test permanecen `pending`. El modelo
no se entrena ni el test se abre hasta completar `visual-training-dataset-v2/HUMAN_REVIEW.md`.

Las cuatro hojas de contacto locales fueron inspeccionadas por el agente: no se observaron personas,
marcas o texto legible. Esto no equivale a revisión humana. El screening de personas por similitud de
prompts resultó no discriminativo —marcó 100/100— y se conserva como `inconclusive`, nunca como prueba
de presencia de personas ni como gate aprobado.

La persona propietaria del workspace aprobó explícitamente las 80 imágenes nuevas. La autorización
v2 deja 200/200 activos aprobados únicamente para desarrollo. Se extrajeron embeddings CLIP 512-d
normalizados y se seleccionó L2 0,0001 usando exclusivamente train/validación. El único test
definitivo se abrió después de fijar el candidato y obtuvo 70/80: accuracy 0,875, error 0,125,
precision macro 0,878662, recall macro 0,875 y F1 macro 0,873369. Train/validación alcanzan
0,975/0,95 y la brecha train-test es 0,10.

La puerta de generalización pasa y las métricas macro superan 0,80, pero fallan accuracy >=0,90 y
error <=0,10. Las confusiones principales son municipal/otros (cinco errores combinados) y
estética/peluquería (dos). `gatesPassed=false`, `promotionAllowed=false` y el presupuesto de test
queda agotado. El resultado se conserva en
`evaluation/results/clip-linear-category-head.definitive-200.v1.json`; no debe reabrirse ni usarse
para seleccionar otra variante.

### Diagnóstico y candidato robusto posterior

El diagnóstico conservado no confirma un sobreajuste clásico excesivo: train-validación difiere solo
0,025. Sí detecta 4.104 parámetros para 80 filas (51,3 por fila), tres L2 empatados con desempate hacia
el más débil, 2.000 epochs y un cambio completo de fuente: las 80 imágenes test proceden de
`generated-independent-test-v2`, ausente en train/validación. Municipal/otros concentra seis errores
y mezcla titularidad pública —no observable solo en píxeles— con apariencia interior; estética y
peluquería añade dos errores. El margen medio de los errores es 0,4985 frente a 1,4305 en aciertos.

Tras consumir v2, sus 200 filas pueden usarse únicamente como desarrollo para la siguiente versión.
`clip-ridge-category-head-v2-development` ajusta PCA dentro de cada fold y selecciona conjuntamente
componentes y ridge mediante cinco folds estratificados y leave-one-source-out. La selección usa 16
componentes y ridge 0,01: reduce la cabeza efectiva de 4.104 a 136 parámetros, obtiene train 0,945,
out-of-fold 0,93, leave-one-source-out 0,91 y brecha interna 0,02375. Esto corrige la selección de
capacidad y la varianza observada, pero no es un test nuevo. El artefacto mantiene `testMetrics=null`,
`independentTestStatus=required` y promoción false hasta disponer de otro holdout disjunto.

No se recomienda entrenar CLIP con imágenes generadas como única verdad: el modelo podría aprender
artefactos del generador y producir métricas artificialmente altas. Incluso tras materialización,
las imágenes solo podrán validar la infraestructura visual; el gate productivo exige imágenes
revisadas y etiquetadas por humanos, y nunca permite mutación automática del perfil del local.

### Reetiquetado taxonómico sin imágenes nuevas

`venue-taxonomy.v1` incorpora 23 familias y 254 tipos candidatos, pero no reescribe los ocho labels
del dataset ni altera resultados consumidos. `taxonomy-relabel-worklist.v1.jsonl` proyecta los 200
activos aprobados como una cola nueva con revisión humana pendiente, `allowedUse` limitado a
`developmentRelabelingOnly`, `testEligible=false` y producción false. El manifiesto enlaza por
SHA-256 catálogo, definición v2 y worklist; registra 25 activos por etiqueta histórica, 80/40/80 por
split original y `newImagesGenerated=0`.

Las seis etiquetas con tipo físico reconocible reciben propuestas canónicas, nunca aprobación
automática. `instalacion-municipal` no recibe tipo: conserva `public-municipal` como atributo de
operador y exige revisar el espacio físico real. `otros` conserva propuestas parciales —coworking y
estudio fotográfico— y requiere reclasificar los casos de reunión o ensayo no representados por una
única clase. La revisión futura no puede presentar ninguno de estos 200 activos como holdout nuevo.

## Límites de uso

- El dataset prueba contratos, reproducibilidad, leakage, cold-start y regresiones técnicas.
- No acredita accuracy, Recall@K, NDCG, conversión, causalidad ni rendimiento productivo.
- No permite revisión de promoción ni despliegue automático.
- Los IDs son sintéticos y solo agrupan observaciones; no deben convertirse en features directas.
- Los outcomes son simulados y ruidosos. Sus tasas no son objetivos comerciales.

La evaluación integral del recomendador se documenta en
`../RECOMMENDATION_CROSS_VALIDATION.md`. Usa cinco folds temporales, el test de junio y diez escenarios
contrafactuales. Los contratos de negocio pasan 10/10, pero la relevancia observada test queda en
accuracy 0,818780 y F1 0,275120; no se promueve el candidato ni se oculta el fallo.
