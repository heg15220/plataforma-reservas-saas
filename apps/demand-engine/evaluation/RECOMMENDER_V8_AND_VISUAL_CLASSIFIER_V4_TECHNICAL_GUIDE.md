# Guía técnica integral: recomendador contextual v8 y clasificador visual v4

## 1. Propósito, alcance y estado real

Este documento describe de extremo a extremo los dos componentes de aprendizaje automático más
recientes del motor de demanda de Reserly:

1. **Recomendador contextual por acciones v8**: versión final de esta línea de trabajo. Ordena
   locales para un usuario usando su intención reciente, preferencias consentidas, ubicación actual,
   fecha y hora solicitadas, disponibilidad, escasez alineada y señales de catálogo.
2. **Clasificador visual multirregión v4**: candidato de desarrollo que clasifica la familia de un
   local a partir de su imagen. Mejora el clasificador v3 combinando la imagen completa y un recorte
   central, pero aún no dispone de un holdout v4 independiente y, por tanto, **no está aprobado para
   producción**.

Los dos componentes resuelven problemas distintos. El recomendador v8 decide **qué local mostrar a
un usuario en un contexto concreto**. El clasificador v4 estima **qué familia visual representa una
imagen de un local**. El segundo puede proporcionar señales auxiliares al primero en una integración
futura, pero sus métricas no deben mezclarse ni interpretarse como una única prueba.

| Componente | Evidencia disponible | Resultado principal | Estado |
| --- | --- | --- | --- |
| Recomendador v8 | 5-fold temporal y test temporal independiente | Test top-1 90,625 % | Línea v8 cerrada; promoción automática deshabilitada |
| Clasificador visual v4 | 4-fold leave-one-view-out sobre development consumido | Accuracy media 83,268 % | Candidato congelado; holdout v4 pendiente |

Los experimentos usan datos sintéticos controlados. Demuestran contratos, comportamiento y ausencia
de regresiones claras, pero no sustituyen una evaluación con tráfico real, consentimiento, auditoría
de sesgos y supervisión de producto.

## 2. Mapa de artefactos

### 2.1 Recomendador v8

- Generación de datos: `src/reserly_demand_engine/recommendation_action_context_dataset.py`.
- Entrenamiento y evaluación: `src/reserly_demand_engine/recommendation_action_context_training.py`.
- Política inmutable: `policies/recommendation-action-context.v8.json`.
- Dataset: `evaluation/synthetic-marketplace-action-context-v8/`.
- Informe de desarrollo: `evaluation/results/recommendation-action-context-development.v8.json`.
- Resultado de test: `evaluation/results/recommendation-action-context.v8.json`.
- Modelo XGBoost: `models/contextual-recommender-action-context.v8.xgb.json`.
- Model card: `models/contextual-recommender-action-context.v8.model-card.json`.
- Tests de contrato: `tests/test_recommendation_action_context_v8.py`.

### 2.2 Clasificador visual v4

- Pipeline: `src/reserly_demand_engine/full_taxonomy_visual_multiregion_v4.py`.
- Manifiesto de las imágenes: `evaluation/synthetic-marketplace-full-taxonomy-visual-v3/generation-manifest.v3.json`.
- Embeddings globales y centrales: `evaluation/synthetic-marketplace-full-taxonomy-visual-v3/development-multiregion-embeddings.v4.json`.
- Informe final de desarrollo: `evaluation/results/full-taxonomy-visual-multiregion-robust-development.v4.json`.
- Modelo congelado: `models/full-taxonomy-visual-multiregion-classifier.v4.json`.
- Política del futuro holdout: `policies/full-taxonomy-visual-multiregion-holdout.v4.json`.
- Tests de contrato: `tests/test_full_taxonomy_visual_multiregion_v4.py`.
- Taxonomía: `packages/demand-contracts/catalog/venue-taxonomy.v1.json`.
- Contrato de CLIP: `models/clip-vit-b32-visual-evidence.v1.json`.

## 3. Recomendador contextual v8

### 3.1 Objetivo de negocio

El objetivo no es recomendar simplemente el local más popular. El sistema debe detectar la intención
actual del usuario y ordenar opciones que sean simultáneamente pertinentes y realizables. En
particular:

- una búsqueda o una secuencia reciente puede sustituir temporalmente preferencias históricas;
- la distancia se calcula desde la ubicación del usuario en ese instante;
- el día y la hora se comparan con cada candidato, no con una constante global;
- pocas plazas solo aumentan la oportunidad cuando existe afinidad de contenido, proximidad y radio
  compatible;
- un local nuevo o poco expuesto puede aparecer si encaja, sin recibir un impulso global inmerecido;
- un candidato sin capacidad o no elegible queda fuera antes del ranking.

La escasez es, por tanto, una señal condicional de utilidad y no una táctica de presión. Esto evita
recomendar indiscriminadamente locales casi llenos a usuarios sin interés.

### 3.2 Construcción reproducible de los datos

La semilla `8527` hace determinista el dataset `synthetic-marketplace-action-context-v8`. Su contrato
contiene:

- 100 locales;
- 40 perfiles de usuario;
- 3.200 sesiones;
- 25.600 candidatos, exactamente 8 por sesión;
- 17.596 eventos históricos;
- 10 tipos de acción;
- 6 familias y 28 tipos de local;
- 1.800 sesiones de entrenamiento, 600 de validación y 800 de test temporal.

#### Perfiles de usuario

Los 40 perfiles parten del dataset sintético base y varían idioma, tolerancia a distancia,
preferencias de servicios y atributos, sensibilidad al precio y patrones de uso. Las preferencias
persistentes solo se materializan cuando el perfil declara consentimiento. Si no existe ese permiso,
las features correspondientes se fijan a cero; no se infieren preferencias persistentes de forma
encubierta.

Cada sesión también tiene una intención efímera. Esto permite que la misma persona busque, por
ejemplo, deporte por la mañana y restauración por la tarde sin quedar atrapada en su historial.

#### Locales

Los 100 locales aportan tipo, familia, servicios, atributos, precio, calidad, popularidad/exposición,
antigüedad, coordenadas y capacidad por franja. El recomendador v8 usa 28 tipos de 6 familias, no las
254 clases visuales de v4. Esta cobertura es suficiente para probar el ranking contextual, pero no
significa que el recomendador ya haya sido validado sobre toda la taxonomía visual.

#### Acciones y recorridos

Cada sesión genera de 3 a 8 acciones anteriores al instante de recomendación. El vocabulario es:

1. `search`;
2. `category_filter`;
3. `venue_view`;
4. `service_view`;
5. `map_open`;
6. `availability_check`;
7. `save`;
8. `compare`;
9. `booking_start`;
10. `booking_complete`.

Las últimas cinco acciones pesan, de más reciente a más antigua, `0,42`, `0,26`, `0,17`, `0,10` y
`0,05`. Se crean recorridos directos y cambios de intención: algunas sesiones comienzan con una
familia antigua y terminan con señales recientes de otra. Así se comprueba que el modelo responde a
lo que el usuario quiere ahora y no solo a su perfil estático.

#### Ubicación puntual

Se escoge un local objetivo y se sitúa al usuario a una distancia aproximada de 0,15 a 6 km, con
rumbo variable. Para cada candidato se vuelve a calcular la distancia real mediante Haversine. Las
coordenadas crudas no entran en el modelo: solo se conservan proximidad, decaimiento de distancia y
pertenencia al radio preferido. El radio procede de preferencias permitidas o usa 12 km como valor
base, garantizando en la simulación que el objetivo pueda ser alcanzable.

#### Día, hora, disponibilidad y pocos huecos

Las afinidades temporales son específicas de cada local. Los días preferidos se distribuyen mediante
los desplazamientos `{i % 7, (i + 2) % 7, (i + 4) % 7}`. Según `i % 3`, el local recibe un patrón de
mañana, tarde o mixto. Esto corrige la debilidad detectada en v6, donde día y hora eran constantes y
no medían de verdad la sensibilidad temporal.

La capacidad total varía entre 6 y 20 plazas. En el 40 % de las sesiones objetivo se simulan 1 o 2
plazas restantes; en el resto hay al menos 3. Se derivan `availabilityRatio` y
`remainingSlotUrgency`. Antes de puntuar se aplica un filtro duro de capacidad.

La oportunidad por escasez es:

```text
alignedScarcityOpportunity =
    contentAffinity
  × currentLocationProximity
  × withinPreferredRadius
  × remainingSlotUrgency
```

Al ser multiplicativa, una urgencia alta no compensa una afinidad o proximidad nula.

#### Candidatos difíciles y etiqueta relevante

Cada grupo tiene exactamente ocho candidatos: el objetivo inicial, hasta tres negativos difíciles de
la misma familia y los locales cercanos necesarios para completar el grupo. La etiqueta no se fuerza
sobre el objetivo inicialmente elegido. Se calcula la utilidad puntual de todos los candidatos y se
adjudica como relevante el de mayor utilidad. Esto evita enseñar al modelo una contradicción entre
las features y la etiqueta, problema observado en v5.

Para no producir un benchmark artificialmente perfecto se introduce ruido de elección controlado:
aproximadamente 10 % en development y 5,875 % en test. En esas sesiones se elige una alternativa
plausible en vez del máximo matemático. La señal positiva recibe relevancia 3; un clic recibe 1 y dos
de cada tres sesiones positivas terminan en reserva. El test más limpio puede obtener mejor accuracy
que el OOF de desarrollo sin que esto implique fuga: la diferencia está declarada y la brecha queda
acotada.

### 3.3 Features

El vector tiene 23 variables, todas calculadas antes de conocer la elección:

| Grupo | Variables |
| --- | --- |
| Intención reciente | `recentActionTypeAffinity`, `recentActionFamilyAffinity`, `recentActionServiceAffinity`, `searchQueryAffinity`, `actionSequenceMomentum` |
| Preferencia consentida | `persistentPreferenceAffinity` |
| Catálogo | `taxonomyTypeAffinity`, `taxonomyFamilyAffinity`, `serviceAffinity`, `attributeAffinity`, `contentAffinity` |
| Ubicación | `currentLocationProximity`, `withinPreferredRadius`, `distanceDecayKm` |
| Tiempo y capacidad | `requestedDayAffinity`, `requestedHourAffinity`, `availabilityRatio`, `remainingSlotUrgency`, `alignedScarcityOpportunity` |
| Negocio acotado | `lowExposureAffinity`, `qualityScore`, `priceFit`, `isNewVenue` |

La afinidad agregada de contenido se construye como:

```text
0,30 × afinidad de tipo por acciones
+ 0,18 × afinidad de familia por acciones
+ 0,18 × afinidad de servicio
+ 0,12 × afinidad de tipo taxonómico
+ 0,08 × afinidad de atributo
+ 0,08 × afinidad de búsqueda
+ 0,06 × preferencia persistente consentida
```

Los identificadores de usuario/local, la posición previa, las coordenadas y los outcomes se excluyen.
Así el modelo no puede memorizar IDs, copiar el orden de entrada ni aprender del futuro.

### 3.4 Modelo y motivos de elección

Se utiliza `XGBRanker` con objetivo `rank:ndcg` y evaluación `ndcg@3`. Es un modelo de ranking por
grupos: aprende comparaciones entre los ocho locales de una misma sesión, que es el problema real,
en lugar de tratar cada fila como una clasificación binaria independiente.

XGBoost resulta apropiado porque:

- capta interacciones no lineales como afinidad × distancia × escasez;
- funciona bien con features tabulares heterogéneas y pocos miles de grupos;
- permite regularización, profundidad y muestreo explícitos;
- produce un artefacto JSON portable y auditable;
- evita la complejidad y el volumen de datos que exigiría una red neuronal entrenada de extremo a
  extremo.

La configuración final es conservadora: 24 árboles, profundidad máxima 3, learning rate 0,025,
`min_child_weight=45`, L2 90, L1 5, `subsample=0,68` y `colsample_bytree=0,76`. Esta combinación limita
la memorización y explica que el resultado OOF se mantenga por debajo de 90 %.

Tras la predicción del modelo se aplican únicamente priors de negocio pequeños y acotados. No
sustituyen la afinidad aprendida y no pueden saltarse capacidad, radio ni elegibilidad.

### 3.5 Selección 5-fold y test temporal

Se eligió 5-fold en lugar de 10-fold porque 3.200 sesiones permiten cinco bloques temporales con
suficiente tamaño, menor varianza y coste razonable. No se hace K-fold aleatorio. Se usa
**rolling-origin**: cada fold entrena con el pasado y valida en un bloque posterior. Esto imita el
despliegue y evita que una interacción futura ayude a predecir una anterior.

Solo se comparan tres configuraciones predeclaradas. La selección usa development. Después se
congela el modelo y se abre una única vez un test temporal independiente. Los ficheros de política,
lock y hashes impiden reajustar iterativamente contra ese test.

El valor de entrenamiento publicado, 87,35 %, es accuracy **out-of-fold**: cada predicción se genera
con un modelo que no vio ese bloque. No es el accuracy sobre las mismas filas usadas para ajustar los
árboles. Esta es la medida correcta para hablar de generalización durante el desarrollo.

### 3.6 Flujo de ejecución

```text
Perfil consentido + acciones recientes + contexto (ubicación/día/hora)
                              |
                              v
              Recuperación de candidatos elegibles
                              |
                              v
       Features por par sesión-local calculadas antes del outcome
                              |
                              v
         Filtro duro: capacidad, elegibilidad y restricciones
                              |
                              v
                 XGBRanker produce score base
                              |
                              v
           Priors de negocio acotados y contextuales
                              |
                              v
           Orden top-k + explicación de señales + fallback
```

Si el modelo o sus artefactos no están disponibles, el contrato mantiene el recomendador contextual
v4 y después el ranking determinista como fallbacks. La promoción automática está deshabilitada;
superar las métricas no equivale a autorización productiva.

### 3.7 Resultados v8

| Métrica | Development 5-fold OOF | Test temporal |
| --- | ---: | ---: |
| Accuracy top-1 | 87,35 % | 90,625 % |
| Error top-1 | 12,65 % | 9,375 % |
| Precision | 87,35 % | 90,625 % |
| Recall | 87,35 % | 90,625 % |
| F1 | 87,35 % | 90,625 % |
| Recall@3 | 99,95 % | 99,875 % |

Hay una sola etiqueta positiva y una sola predicción top-1 por sesión; por eso accuracy, precision,
recall y F1 top-1 coinciden. Esta métrica por sesión evita inflar el accuracy contando como aciertos
los numerosos verdaderos negativos a nivel de candidato.

La brecha absoluta entre OOF y test es 3,275 puntos. Los cortes conductuales del test obtienen:

- ubicación sensible: 92,879 %;
- pocos huecos con intención alineada: 99,387 %;
- cambio de intención: 89,300 %;
- horario vespertino: 91,25 %;
- local frío o de baja exposición: 89,630 %;
- pruebas contrafactuales: 10 de 10.

Estas cifras pasan las puertas sintéticas, pero `productionEvidence=false` y
`promotionAllowed=false`. La evidencia productiva requerirá datos reales independientes, definición
de objetivos online, guardrails y aprobación humana.

## 4. Clasificador visual multirregión v4

### 4.1 Objetivo y linaje de datos

V4 intenta predecir una de 23 familias a partir de una fotografía del establecimiento. Parte de las
1.016 imágenes ya existentes en v3:

- 254 tipos taxonómicos;
- 23 familias;
- 38 arquetipos visuales auxiliares;
- cuatro vistas independientes A, B, C y D por tipo;
- 254 imágenes por vista.

Las imágenes son PNG 4:3 y representan establecimientos distintos o encuadres independientes. Se
revisaron previamente por manifiesto, hashes, OCR, duplicados y aprobación humana. Las personas son
opcionales, pequeñas y no identificables en escenas no sensibles. Se excluyen menores, pacientes,
rostros reconocibles y usos biométricos. V4 **no generó nuevas imágenes**.

La vista D fue el holdout v3. Como ya se abrió y sus resultados se usaron para diagnosticar, dejó de
ser test. V4 trata A/B/C/D como development consumido. Ninguna de esas 1.016 imágenes puede volver a
presentarse como evidencia independiente.

### 4.2 Por qué usar CLIP congelado

El extractor es `openai/clip-vit-base-patch32`, revisión fijada
`fbf5e...`, mediante Transformers 4.56.2. Produce vectores visuales L2-normalizados de 512
dimensiones.

CLIP congelado se eligió porque ofrece representaciones visuales generales sin entrenar una red
convolucional desde cero con solo cuatro imágenes por tipo. Congelar el encoder reduce coste,
varianza y riesgo de memorizar el dataset. La revisión y versiones quedan fijadas para que una
actualización silenciosa no cambie las features.

No se usan prompts de texto como features finales. Los experimentos con similitud texto-imagen no
mejoraron el baseline. Tampoco entran el tipo, familia o arquetipo verdadero durante inferencia: solo
sirven como etiquetas de entrenamiento y evaluación.

### 4.3 Dos regiones por imagen

Cada PNG produce dos vistas numéricas:

1. **Global**: la imagen completa, 512 dimensiones.
2. **Centro 80 %**: recorte que elimina un 10 % de cada borde, 512 dimensiones.

El recorte se realiza en memoria con Pillow y nunca modifica el archivo fuente. Antes de procesar se
verifica el hash de cada PNG. El procesamiento usa lotes de 16 imágenes. La región global captura
fachada, distribución y contexto; la central reduce ruido de bordes, marcos, rótulos y elementos
periféricos y enfatiza el ambiente interior.

### 4.4 Cabeza híbrida: prototipos y LDA

El modelo final no reentrena CLIP. Aprende dos componentes ligeros encima de sus embeddings.

#### Prototipos por tipo

Para cada uno de los 254 tipos se calcula el centroide de sus embeddings. Ante una consulta se mide
la similitud coseno con los prototipos globales y centrales. Los scores de tipos pertenecientes a una
familia se agregan mediante máximo. Esto conserva detalle taxonómico: una familia puede ganar porque
la imagen se parece mucho a uno de sus tipos concretos.

Las dos ramas se estandarizan por consulta y se suman:

```text
prototypeScore = z(globalPrototypeScore) + z(centerPrototypeScore)
```

#### LDA con shrinkage

Los 1.024 valores global+centro se concatenan y normalizan. Una cabeza LDA regularizada con
`shrinkage=1` estima logits para las 23 familias. El shrinkage estabiliza la covarianza en un régimen
con muchas dimensiones y pocas muestras por clase.

Los logits también se estandarizan por consulta. El score final es:

```text
finalScore = z(prototypeScore) + 0,75 × z(ldaScore)
```

El modelo JSON conserva 254 prototipos globales, 254 centrales, las 23 clases y una matriz LDA de
23 × 1.024, además de parámetros, hashes y contrato de entrada.

### 4.5 Selección robusta 4-fold

La validación es leave-one-consumed-view-out: en cada fold se entrena con tres vistas completas y se
valida con la cuarta. Así nunca aparece otra fotografía del mismo tipo en el entrenamiento del fold
retenido. Los folds son A, B, C y D.

Se comparan diez fusiones predeclaradas y la selección no maximiza solo la media. El orden es:

1. mayor F1 macro del peor fold;
2. mayor accuracy del peor fold;
3. mayor F1 macro media;
4. mayor accuracy media.

Esta regla evita elegir un candidato que funciona muy bien en tres estilos visuales y falla en el
cuarto. El prototipo global+centro puro alcanzó 81,988 % de media, pero cayó a aproximadamente
72,44 % en D; se descartó por falta de robustez. Ganó
`global-center-prototype-lda-robust-0.75`.

### 4.6 Experimentos descartados

- Baseline v3 con cuatro vistas: 79,921 %.
- Similitud CLIP con texto: máximo 79,134 %; no aportó mejora.
- XGBoost sobre embeddings densos: 33,957 %; el tamaño muestral y la dimensionalidad no favorecían
  árboles por coordenada.
- SVM con embedding global: máximo 80,118 %.
- SVM global+centro: 81,988 % de media, con peor robustez por vista.
- Features clásicas de píxel: experimento cancelado antes de producir un fold o artefacto por su
  coste y bajo valor esperado frente a CLIP.

Conservar los resultados negativos es parte de la trazabilidad: impide repetir ensayos y evita
presentar solo los experimentos favorables.

### 4.7 Flujo de inferencia visual

```text
PNG + hash esperado
        |
        +--> imagen global --------> CLIP 512D --+
        |                                         |
        +--> recorte central 80 % -> CLIP 512D --+--> normalización
                                                   |
                   +-------------------------------+------------------+
                   |                                                  |
                   v                                                  v
        similitud con prototipos                           LDA regularizada
          de 254 tipos/familias                              23 logits
                   |                                                  |
                   +------------ fusión alpha=0,75 ------------------+
                                                   |
                                                   v
                                      familia top-1 y ranking top-k
```

En una integración operativa deben verificarse versión, dimensiones, hashes y esquema. Un fallo debe
cerrar la ruta visual y conservar el ranking determinista; nunca debe inventarse una categoría.

### 4.8 Resultados v4

| Métrica de development | Valor |
| --- | ---: |
| Accuracy media | 83,2677 % |
| Error | 16,7323 % |
| Precision macro | 85,6700 % |
| Recall macro | 81,3043 % |
| F1 macro | 81,5912 % |
| Recall@3 | 96,0630 % |
| Recall mínimo de clase | 37,5 % |
| Accuracy del peor fold | 78,3465 % |
| F1 del peor fold | 76,6883 % |

Por fold, la accuracy es A 88,583 %, B 85,827 %, C 80,315 % y D 78,346 %. La vista D mejora 3,543
puntos respecto a su evaluación v3, pero sigue siendo el caso más difícil.

Estas métricas son de development reutilizado. No prueban 90 % en datos nuevos. La política del
futuro holdout v4 exige, entre otras puertas, accuracy al menos 90 %, error como máximo 10 %, métricas
macro al menos 80 %, recall mínimo por clase al menos 70 % y brecha máxima de 10 puntos. El holdout
debe ser nuevo, revisado y abierto una sola vez después de congelar el candidato. Hasta entonces:

- `productionEvidence=false`;
- `promotionAllowed=false`;
- la tarea de holdout v4 permanece pendiente.

## 5. Librerías y decisiones de ingeniería

| Librería o módulo | Uso | Justificación |
| --- | --- | --- |
| XGBoost | `XGBRanker` de v8 | Ranking no lineal tabular, regularización explícita y artefacto JSON |
| NumPy | Métricas, prototipos, LDA y álgebra | Operaciones deterministas y eficientes sin introducir un runtime pesado en la cabeza visual |
| Transformers + PyTorch | Encoder CLIP congelado | Implementación mantenida, batching y ejecución CPU/GPU con revisión fijada |
| Pillow | Lectura y recorte central | Transformación segura en memoria sin alterar PNG originales |
| JSON, `pathlib`, `hashlib` | Contratos y sellado | Portabilidad, paths explícitos e integridad SHA-256 |
| pytest | Pruebas de contratos | Verifica fail-closed, reproducibilidad, ausencia de leakage y estados de promoción |

La cabeza visual final se serializa sin depender de scikit-learn en inferencia. SVM se usó solo en
exploración. En ambos componentes se prefieren artefactos legibles, versiones fijadas y validaciones
fail-closed frente a modelos opacos o estados mutables.

## 6. Reproducción y verificación

Desde `apps/demand-engine`, con el entorno del proyecto activado:

```powershell
$env:PYTHONPATH = "src"
pytest -q tests/test_recommendation_action_context_v8.py
pytest -q tests/test_full_taxonomy_visual_multiregion_v4.py
```

Los entry points instalables son:

```powershell
reserly-demand-generate-action-context-data
reserly-demand-train-action-context
reserly-demand-train-full-taxonomy-multiregion-v4
```

No deben ejecutarse destructivamente sobre los artefactos congelados. Los pipelines están diseñados
para fallar si el modelo, el lock o el resultado ya existen. Para una reproducción experimental se
deben usar rutas temporales o una versión nueva, conservar las semillas y no sobrescribir v8/v4. El
test temporal v8 ya fue consumido y las cuatro vistas visuales ya son development; reabrirlos para
ajustar hiperparámetros contaminaría la medición.

## 7. Integración conceptual de ambos componentes

El orden correcto de integración futura es:

1. validar la imagen y obtener, con v4 o un sucesor aprobado, probabilidades de familia/tipo;
2. convertirlas en señales de catálogo con procedencia y confianza, nunca en una verdad absoluta;
3. combinar esas señales con acciones, servicios y contexto antes de construir las features de v8;
4. mantener ubicación, capacidad y elegibilidad como restricciones independientes;
5. registrar la explicación: intención detectada, distancia, horario, disponibilidad y aportación
   visual;
6. recurrir al catálogo declarado o al ranking determinista si la señal visual no es fiable.

El recomendador v8 no se modifica mientras se mejora v4. Esta separación protege un componente que
ya supera su evaluación sintética y permite evolucionar el clasificador visual sin alterar el ranking
final ni atribuirle evidencia que todavía no posee.

## 8. Limitaciones y trabajo pendiente

- V8 cubre 100 locales, 40 usuarios y 28 tipos; necesita validación offline con eventos reales
  anonimizados y luego experimento online controlado.
- La mayor limpieza del test v8 respecto a development explica parte de su mejor resultado; futuras
  comparaciones deben mantener la política de ruido y dificultad fijada antes de abrir el test.
- El alto Recall@3 no elimina errores top-1 ni demuestra satisfacción, conversión o equidad.
- V4 tiene solo cuatro imágenes por tipo y fuerte solapamiento entre instalaciones visualmente
  parecidas; el recall mínimo de clase 37,5 % sigue siendo insuficiente.
- V4 requiere un holdout completamente nuevo. No se debe ajustar de nuevo con A/B/C/D ni reutilizar
  el holdout v3.
- Antes de producción hacen falta monitorización de drift, calibración, latencia, privacidad,
  explicabilidad, cobertura por clase y protocolo de rollback.

En resumen, v8 es la versión final de la línea contextual sintética y queda congelada con sus
fallbacks. V4 es una mejora técnica prometedora y trazable, pero su calidad productiva permanece
abierta hasta superar una evaluación verdaderamente independiente.
