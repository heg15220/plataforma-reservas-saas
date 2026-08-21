# Mejora de calidad del recomendador semántico v2

## 1. Propósito

Este documento describe la mejora aplicada al componente de recuperación semántica del Demand
Engine, cómo se controla el riesgo de sobreajuste y subajuste, qué resultados se obtuvieron y qué
trabajo falta antes de permitir su promoción.

La finalidad no es conseguir métricas artificialmente perfectas sobre un fixture pequeño. La
finalidad es demostrar que una representación o modelo generaliza a consultas que no se utilizaron
para seleccionarlo, dentro de los límites de relevancia, latencia, privacidad y operación definidos
por Reserly.

El resultado actual es deliberadamente conservador:

- El challenger v2 mejora la representación multilingüe.
- Supera la puerta cross-locale en el holdout.
- Todavía falla otras puertas de calidad, generalización y latencia documental.
- Permanece en shadow y no participa en la recuperación online.
- Full-text/trigram continúa siendo el fallback autoritativo.

## 2. Problema observado en v1

La evidencia histórica está almacenada en:

`apps/demand-engine/evaluation/results/multilingual-e5-small.v1.windows-cpu.json`

| Métrica v1 | Resultado | Umbral | Estado |
| --- | ---: | ---: | --- |
| Recall@1 | 0,6875 | >= 0,80 | No pasa |
| Recall@3 | 0,8125 | >= 0,95 | No pasa |
| Mean Reciprocal Rank | 0,775521 | >= 0,85 | No pasa |
| Recall@3 cross-locale ES/EN | 0,625 | >= 0,90 | No pasa |
| Latencia warm de consulta p95 | 50,648 ms | <= 100 ms | Pasa |
| Latencia warm por documento p95 | 26,852 ms | <= 50 ms | Pasa |

El análisis consulta por consulta mostró que varios errores no procedían de una avería del encoder,
sino de una representación documental incompleta. Una consulta en español podía competir contra una
ficha disponible únicamente en inglés, o al contrario. Los fallos principales afectaban a corte,
coloración, peinado y cuidado facial.

V1 era útil como smoke test del contrato, pero solo contenía 16 consultas, 10 documentos y dos
negativos lejanos. Ajustar el sistema hasta obtener 100 % sobre ese conjunto habría producido una
señal optimista sin demostrar generalización.

## 3. Qué significa sobreajuste en este contexto

El encoder E5 no se reentrena durante esta iteración. Por tanto, el riesgo principal no es memorizar
pesos de entrenamiento locales, sino sobreajustar la selección del sistema al benchmark:

1. Se inspeccionan los fallos del test.
2. Se añaden exactamente las palabras que resuelven esos casos.
3. Se vuelve a ejecutar el mismo test.
4. Se presenta la mejora como si procediera de datos independientes.

Ese proceso contamina el test aunque el encoder permanezca congelado. También existe sobreajuste si
se prueban muchos modelos o configuraciones y se elige el mejor usando siempre el mismo holdout.

Para evitarlo, v2 separa:

- `development`: selección y diagnóstico de representación.
- `holdout`: puerta final sin ajuste posterior.

Una vez observado el holdout v2, queda consumido. No se permite corregir alias basándose en sus
errores y volver a declararlo independiente.

## 4. Qué significa subajuste

Existe subajuste cuando la representación o el modelo no captura suficiente señal del dominio. Sus
síntomas pueden ser:

- Recall@1 y MRR bajos incluso en desarrollo.
- Scores muy parecidos entre servicios diferentes.
- Ranking dominado por resultados genéricos.
- Incapacidad para distinguir corte, peinado, coloración y tratamiento capilar.
- Resultados estables pero poco adaptativos ante paráfrasis válidas.

Un modelo plano no se corrige reduciendo umbrales. Deben mejorarse los datos, la representación, los
negativos, la arquitectura de recuperación o el entrenamiento, y después medir sobre datos nuevos.

## 5. Solución v2 implementada

### 5.1 Representación documental multilingüe

Los documentos pueden incorporar `localizedTexts.es` y `localizedTexts.en` procedentes del catálogo
editorial gobernado. La composición incluye:

- Texto principal.
- Nombre y descripción del locale principal.
- Nombre, descripción y variantes publicadas del segundo locale.
- Eliminación determinista de duplicados tras normalizar espacios.
- Límite total de 4.000 caracteres.

No se realizan traducciones ni inferencias durante la petición. Las variantes deben existir antes en
el catálogo autorizado.

Las consultas mantienen un único texto y tienen prohibido enviar `localizedTexts`. Esto evita que el
cliente manipule la representación documental o aporte múltiples versiones optimizadas de su query.

### 5.2 Versionado y checksum

El challenger se identifica como `multilingual-e5-small-v2`. Mantiene:

- Modelo `intfloat/multilingual-e5-small`.
- Revisión Git `d1d99a1efae6779390caba937d92c54b5bc70e51`.
- 384 dimensiones.
- Prefijos E5 `query: ` y `passage: `.
- Normalización L2 y similitud coseno.
- `trust_remote_code=false`.

El checksum se calcula sobre la composición exacta enviada al encoder. Cambiar una localización
produce otro checksum y evita reutilizar silenciosamente un vector obsoleto.

El modo `raw` de v1 rechaza documentos con campos localizados. Así, una representación v2 no puede
publicarse accidentalmente con el identificador v1.

### 5.3 Benchmark ampliado

El dataset v2 se encuentra en:

`apps/demand-engine/evaluation/personal-care-retrieval.v2.json`

Incluye:

- 16 documentos.
- 32 consultas de desarrollo.
- 30 consultas holdout.
- Consultas en español e inglés.
- Paráfrasis y expresiones menos literales.
- Negativos próximos: cejas, pestañas, depilación, masaje, barba y bronceado.
- Negativos fuera del dominio: salud y restauración.

El evaluador rechaza:

- Corpus o conjunto de consultas vacíos.
- IDs duplicados.
- Documentos relevantes inexistentes.
- Splits incompletos o desconocidos.
- Texto normalizado idéntico entre desarrollo y holdout.
- Ausencia de casos cross-locale.

## 6. Puertas de generalización

Las métricas de promoción proceden exclusivamente del holdout. Las métricas de desarrollo se
publican para poder calcular la brecha:

```text
gap = max(0, métrica_desarrollo - métrica_holdout)
```

Las puertas adicionales son:

| Puerta | Máximo |
| --- | ---: |
| Brecha Recall@3 desarrollo/holdout | 0,10 |
| Brecha MRR desarrollo/holdout | 0,10 |

Solo se penaliza que desarrollo sea claramente mejor que holdout. Si holdout supera desarrollo, la
brecha se considera cero; no hay evidencia de sobreajuste de selección por ese indicador.

Estas puertas no prueban por sí solas ausencia de overfitting, pero evitan promover un cambio cuya
ganancia se concentre en las consultas utilizadas para desarrollarlo.

## 7. Resultados reales v2

La evidencia se encuentra en:

`apps/demand-engine/evaluation/results/multilingual-e5-small.v2.windows-cpu.json`

### 7.1 Desarrollo

| Métrica | Resultado |
| --- | ---: |
| Recall@1 | 0,875 |
| Recall@3 | 0,96875 |
| Mean Reciprocal Rank | 0,929688 |
| Recall@3 cross-locale | 0,9375 |

### 7.2 Holdout

| Métrica | Resultado | Umbral | Estado |
| --- | ---: | ---: | --- |
| Recall@1 | 0,70 | >= 0,80 | No pasa |
| Recall@3 | 0,866667 | >= 0,95 | No pasa |
| Mean Reciprocal Rank | 0,812222 | >= 0,85 | No pasa |
| Recall@3 cross-locale | 0,933333 | >= 0,90 | Pasa |

### 7.3 Generalización

| Métrica | Brecha | Máximo | Estado |
| --- | ---: | ---: | --- |
| Recall@3 | 0,102083 | <= 0,10 | No pasa |
| Mean Reciprocal Rank | 0,117465 | <= 0,10 | No pasa |

La diferencia es pequeña en Recall@3, pero sigue por encima del límite predeclarado. No se redondea
ni se aumenta el límite después de observar el resultado.

### 7.4 Latencia

| Métrica | Resultado | Umbral | Estado |
| --- | ---: | ---: | --- |
| Consulta warm p95 | 68,96 ms | <= 100 ms | Pasa |
| Documento bilingüe warm p95 por item | 109,86 ms | <= 50 ms | No pasa |

La primera medición de latencia v2 reutilizaba solo el texto base durante el benchmark. Se corrigió
para medir la composición bilingüe realmente codificada. El resultado correcto muestra una regresión
documental importante y bloquea la promoción.

## 8. Interpretación correcta de la comparación v1/v2

Los valores holdout v2 son numéricamente superiores a los de v1 en las cuatro métricas de
relevancia, especialmente cross-locale. Sin embargo, v2 contiene más consultas, más documentos y
negativos más difíciles. Por ello, la diferencia no debe presentarse como una comparación causal o
como porcentaje exacto de mejora.

La evidencia válida es:

- La nueva representación resuelve parte del problema multilingüe.
- El resultado cross-locale supera su puerta en el holdout v2.
- La caída entre desarrollo y holdout sigue siendo excesiva.
- La recuperación todavía no alcanza la calidad mínima global.
- La composición actual tampoco cumple latencia documental.

## 9. Decisión de promoción

El informe declara:

```text
qualityPassed=false
generalizationPassed=false
latencyPassed=false
promotionStatus=not_promoted
executionMode=shadow_only
fallback=full_text_trigram_only
```

El cambio no activa pgvector en tráfico real. Los filtros duros de publicación, permisos,
disponibilidad, capacidad, radio y categoría continúan aplicándose antes del ranking. El camino de
reserva nunca depende del componente semántico.

## 10. Por qué no se busca un resultado deliberadamente imperfecto

Una métrica perfecta en entrenamiento puede indicar memorización. Una métrica perfecta en un test
grande, representativo, aislado y nunca observado puede ser válida, aunque debe revisarse por posible
leakage o por un problema demasiado sencillo.

Por tanto, no se debe introducir ruido ni empeorar intencionadamente un buen resultado para que
parezca creíble. La protección correcta consiste en:

- Separar train, validación y test.
- Congelar el test antes de seleccionar el modelo.
- Buscar duplicados y leakage semántico.
- Incluir negativos difíciles.
- Usar cortes temporales y, cuando proceda, por local o usuario.
- Publicar intervalos de confianza.
- Comparar con baselines sobre los mismos candidatos.
- Validar estabilidad en varias semillas si existe entrenamiento.
- Medir shadow y valor online antes de producción.

## 11. Precision, recall, F1 y accuracy

Para recuperación ordenada se priorizan:

- Recall@K: si aparece algún relevante entre los primeros K.
- Precision@K: proporción de resultados relevantes dentro de K.
- MRR: penaliza que el primer relevante aparezca tarde.
- NDCG@K: evalúa el orden cuando existen grados de relevancia.
- MAP: útil cuando cada consulta tiene varios relevantes.
- Cobertura, diversidad y exposición: evitan un ranking útil pero concentrado.

Accuracy y F1 requieren convertir el ranking en una clasificación mediante un umbral. Son útiles
para componentes como intención, conversión o no-show, pero no describen bien por sí solas la calidad
de una lista ordenada.

Si se evalúa un clasificador, el umbral debe elegirse en validación y mantenerse congelado en test.
En datasets desbalanceados deben publicarse matriz de confusión, precision, recall, F1, PR-AUC,
ROC-AUC y calibración según el riesgo del caso de uso.

## 12. Próxima mejora válida

El holdout v2 ya no puede reutilizarse para seleccionar cambios. El siguiente ciclo debe:

1. Recopilar consultas y alternativas etiquetadas nuevas, con consentimiento y minimización.
2. Separar train, validación y test mediante ventanas temporales no solapadas.
3. Mantener un conjunto de test inaccesible durante el desarrollo.
4. Comparar sobre los mismos candidatos:
   - Full-text/trigram.
   - E5 con texto original.
   - E5 con representación multilingüe comprimida.
   - Recuperación híbrida.
5. Optimizar la composición documental sin usar los fallos del holdout v2 como etiquetas.
6. Medir latencia por longitud, tamaño de batch y hardware.
7. Añadir intervalos bootstrap por consulta y slices ES/EN/cross-locale.
8. Abrir shadow solo si pasan calidad, generalización, latencia, privacidad y restricciones duras.
9. Ejecutar A/B únicamente con tamaño muestral, guardrails y aprobación humana.

Un fine-tuning contrastivo puede estudiarse cuando exista suficiente evidencia real. Deberá usar
negativos difíciles, regularización, early stopping en validación y un test final nunca observado. No
debe entrenarse con el fixture v1/v2 y declararse después que esos mismos ejemplos prueban calidad.

## 13. Comandos de reproducción

Desde la raíz del repositorio:

```powershell
$env:PYTHONPATH='apps/demand-engine/src;packages/demand-contracts/src'
python -m unittest `
  apps/demand-engine/tests/test_embeddings.py `
  apps/demand-engine/tests/test_embedding_batch.py

npm run test:demand
```

Evaluación con el encoder real:

```powershell
Set-Location apps/demand-engine
$env:PYTHONPATH='src;../../packages/demand-contracts/src'
python -m reserly_demand_engine.embedding_evaluation
```

El último comando debe devolver código distinto de cero mientras alguna puerta falle. Eso no indica
una avería del CLI: es el comportamiento fail-closed esperado.

## 14. Archivos relacionados

- `apps/demand-engine/models/multilingual-e5-small.v2.json`.
- `apps/demand-engine/evaluation/personal-care-retrieval.v2.json`.
- `apps/demand-engine/evaluation/results/multilingual-e5-small.v2.windows-cpu.json`.
- `apps/demand-engine/src/reserly_demand_engine/embeddings.py`.
- `apps/demand-engine/src/reserly_demand_engine/embedding_batch.py`.
- `apps/demand-engine/src/reserly_demand_engine/embedding_evaluation.py`.
- `docs/architecture/demand-embedding-model-card.md`.
- `docs/demand-engine/evaluacion-recomendaciones-desarrollo.md`.

## 15. Conclusión

V2 es una mejora técnica y metodológica, pero no un modelo listo para producción. La representación
multilingüe funciona mejor para transferencia ES/EN, mientras el holdout evita convertir esa mejora
en una afirmación exagerada. La brecha de generalización y la latencia documental demuestran que el
sistema de puertas está detectando problemas reales.

La decisión correcta es conservar v2 en shadow, mantener full-text/trigram y obtener evidencia nueva
antes de continuar. El objetivo no es alcanzar un número visualmente atractivo, sino una mejora que
se mantenga fuera de los datos utilizados para desarrollarla y que respete los presupuestos
operativos del producto.
