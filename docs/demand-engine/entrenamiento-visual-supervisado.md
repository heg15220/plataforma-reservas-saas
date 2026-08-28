# Entrenamiento visual supervisado

## Estado

El pipeline está implementado y probado, pero el entrenamiento real no se ha ejecutado. Los activos
actuales mantienen `humanReviewStatus=pending` y `developmentTrainingAllowed=false`; utilizarlos
incumpliría el contrato de datos. El informe reproducible es
`evaluation/synthetic-marketplace-v1/visual-training-readiness.v1.json`.

## Modelo

`clip-linear-category-head-v1` es una cabeza softmax multiclase sobre embeddings de
`clip-vit-b32-visual-evidence-v1`. CLIP permanece congelado. Solo se entrenan una matriz 8×512 y ocho
sesgos, lo que reduce varianza frente a ajustar todo el backbone con pocos datos.
La optimización CPU utiliza PyTorch 2.8.0 fijado en el extra `ml`.

La política `clip-linear-category-training.v1.json` exige:

- ocho categorías canónicas;
- 10 imágenes por categoría en train, 5 en validación y 10 en test;
- 80/40/80 imágenes y 200 en total;
- revisión humana `approved` y `developmentTrainingAllowed=true` en cada fila;
- UUID, SHA-256 y venueId únicos/disjuntos entre splits;
- embedding L2 normalizado de la revisión CLIP fijada;
- selección de L2 y early stopping únicamente con validación;
- apertura de test después de congelar el candidato.

## Puerta de aceptación

| Métrica | Umbral |
| --- | ---: |
| Accuracy test | >= 0,90 |
| Error test | <= 0,10 |
| Precision macro | >= 0,80 |
| Recall macro | >= 0,80 |
| F1 macro | >= 0,80 |
| Recall mínimo por categoría | >= 0,70 |
| Brecha absoluta train-test | <= 0,10 |

Un test sintético con accuracy >=0,98 exige revisar la dificultad y bloquea la puerta aunque las
métricas pasen. No existe límite superior artificial para train; el control de sobreajuste usa la
brecha, validación, regularización y early stopping.

## Pruebas ejecutadas

El fixture contractual usa datos separables con dos hard negatives intercambiados en test. Obtiene:

- train: accuracy 1,00;
- validación: accuracy 1,00;
- test: accuracy 0,916667 y error 0,083333;
- precision/recall/F1 macro test: 0,916667;
- brecha train-test: 0,083333;
- `gatesPassed=true`, pero `promotionAllowed=false` por evidencia sintética.

Este resultado prueba la implementación, no la calidad del futuro modelo real. Pruebas adicionales
verifican determinismo, rechazo de activos pendientes/no autorizados, leakage de venue entre splits,
fallo con accuracy inferior a 0,90, bloqueo de test sintético perfecto y que test no participa en la
selección de hiperparámetros.

## Ejecución futura

Cuando exista un dataset aprobado:

```powershell
$env:PYTHONPATH='apps/demand-engine/src;packages/demand-contracts/src'
python -m reserly_demand_engine.visual_training `
  --policy apps/demand-engine/policies/clip-linear-category-training.v1.json `
  --dataset <dataset-aprobado.json> `
  --output <artefacto-candidato.json>
```

El resultado de test debe conservarse aunque sea inferior a 0,90. Si falla, se crea una nueva versión
de dataset/test antes de otra iteración; nunca se reajusta contra el mismo test consumido.
