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

Las cuatro hojas de contacto locales fueron inspeccionadas por el agente: no se observaron personas,
marcas o texto legible. Esto no equivale a revisión humana. El screening de personas por similitud de
prompts resultó no discriminativo —marcó 100/100— y se conserva como `inconclusive`, nunca como prueba
de presencia de personas ni como gate aprobado.

No se recomienda entrenar CLIP con imágenes generadas como única verdad: el modelo podría aprender
artefactos del generador y producir métricas artificialmente altas. Incluso tras materialización,
las imágenes solo podrán validar la infraestructura visual; el gate productivo exige imágenes
revisadas y etiquetadas por humanos, y nunca permite mutación automática del perfil del local.

## Límites de uso

- El dataset prueba contratos, reproducibilidad, leakage, cold-start y regresiones técnicas.
- No acredita accuracy, Recall@K, NDCG, conversión, causalidad ni rendimiento productivo.
- No permite revisión de promoción ni despliegue automático.
- Los IDs son sintéticos y solo agrupan observaciones; no deben convertirse en features directas.
- Los outcomes son simulados y ruidosos. Sus tasas no son objetivos comerciales.
