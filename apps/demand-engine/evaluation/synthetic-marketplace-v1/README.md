# Dataset sintético de marketplace v1

Este dataset sirve para desarrollar y comprobar pipelines de recomendación sin usar datos personales
ni presentar resultados sintéticos como evidencia productiva. Contiene exactamente 100 locales
ficticios, 40 perfiles pseudónimos, 2.400 sesiones temporales y 19.200 candidatos.

## Artefactos

- `venues.jsonl`: locales ficticios, descripciones ES/EN, servicios, atributos y ubicación aproximada
  sintética en diez zonas gallegas. No contiene direcciones ni negocios reales.
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

## Imágenes: estado bloqueado

Los 100 prompts permiten materializar posteriormente una imagen independiente por local mediante un
job autorizado. Git solo conserva la especificación. Cada imagen real deberá almacenarse fuera del
repositorio y registrar `objectKey`, SHA-256, modelo/revisión del generador, licencia/procedencia y
resultado de revisión humana. Hasta entonces `materialized=false` y `trainingAllowed=false`.

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
