# Mejora de la puerta visual v2 del Demand Engine

## Resultado

La confusión principal entre peluquería/estética e instalación municipal/interior genérico queda
corregida en la selección candidata v2 sin modificar CLIP ni rebajar los umbrales. La puerta
automática pasa en un holdout sintético nuevo, pero la puerta completa continúa bloqueada:

| Evidencia | Imágenes | Accuracy | Precision macro | Recall macro | F1 macro | Uso |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| v1 warm consumido | 70 | 0,700000 | 0,806537 | 0,752232 | 0,697821 | diagnóstico inicial |
| v2 warm desarrollo | 70 | 0,942857 | 0,937500 | 0,916667 | 0,912446 | ajuste visual |
| v1 validation-cold no modificado | 15 | 0,866667 | 0,791667 | 0,875000 | 0,825000 | contraste |
| v2 holdout nuevo | 24 | 1,000000 | 1,000000 | 1,000000 | 1,000000 | confirmación automática única |

`trainingAllowed=false`, `humanReviewCompleted=false` y `overallPassed=false`. El resultado perfecto
del holdout no acredita que el modelo sea perfecto ni demuestra sobreajuste de entrenamiento: CLIP
no se reentrenó. Sí revela que el test sintético, pequeño y con evidencia visual muy explícita, es
fácil. Por ello no debe utilizarse como evidencia productiva ni volver a abrirse para ajustar activos.

## Cambios de activos

`visual-selection-v2.json` sustituye de forma lógica 17 imágenes warm observadas como ambiguas:

- 11 peluquerías con sillas de corte, espejos, lavacabezas, carros y secadores claramente visibles;
- 6 instalaciones municipales con escenario, atril, filas de sillas y configuración cívica;
- los PNG originales permanecen en `images/` y los nuevos en `images-v2/`, ambos fuera de Git;
- cada inventario conserva SHA-256, dimensiones, categoría, cohorte y procedencia.

El warm v2 deja peluquería e instalación municipal en recall 1,00. Centro deportivo conserva recall
0,50 y evita presentar el conjunto de desarrollo como artificialmente perfecto.

## Holdout no contaminado

`visual-holdout-v2/definition.json` se congeló antes de inferencia y define 24 imágenes nuevas,
exactamente tres por cada una de las ocho categorías. Incluye variantes interiores/exteriores y
exclusiones explícitas de clases vecinas. La inspección estructural exige 24/24 PNG decodificables,
RGB/RGBA, resolución mínima 1024×768, relación declarada 4:3 o 3:2, balance 3×8, SHA-256 único y
distancia dHash mayor que 4.

El resultado real es cero violaciones, cero duplicados y distancia dHash mínima 18. La clasificación
se ejecutó una vez con `clip-vit-b32-visual-evidence-v1`, revisión
`fbf5e647b25f3514e526849b05cc0196b206d822`, Transformers 4.56.2 y CPU.

## Reproducción

Los píxeles se conservan localmente fuera de Git. Con ellos disponibles:

```powershell
$env:PYTHONPATH='apps/demand-engine/src;packages/demand-contracts/src'
python -m reserly_demand_engine.synthetic_visual_qa `
  --dataset apps/demand-engine/evaluation/synthetic-marketplace-v1 `
  --clip-manifest apps/demand-engine/models/clip-vit-b32-visual-evidence.v1.json `
  --selection apps/demand-engine/evaluation/synthetic-marketplace-v1/visual-selection-v2.json `
  --report-suffix .v2-development --evaluation-cohort warm

python -m reserly_demand_engine.synthetic_visual_qa `
  --dataset apps/demand-engine/evaluation/synthetic-marketplace-v1 `
  --holdout-definition apps/demand-engine/evaluation/synthetic-marketplace-v1/visual-holdout-v2/definition.json `
  --clip-manifest apps/demand-engine/models/clip-vit-b32-visual-evidence.v1.json
```

No debe repetirse la segunda orden para retocar la selección y volver a declarar el mismo conjunto
como test. Desde su primera ejecución, `visual-holdout-v2` es evidencia consumida.

## Trabajo pendiente

La subtarea 23.16.c.1 queda completada, pero 23.16.c.2 y 23.16 siguen pendientes. Una persona
independiente debe revisar originales, sustituciones y holdout para detectar categorías incorrectas,
texto/marcas, personas, artefactos y duplicados semánticos. Después se necesita un stress test con
imágenes reales consentidas o casos ambiguos, congelado sin ajustar contra el holdout v2.
