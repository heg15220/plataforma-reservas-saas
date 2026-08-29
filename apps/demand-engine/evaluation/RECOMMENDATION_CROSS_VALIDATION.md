# Evaluación 5-fold del recomendador contextual

## Decisión metodológica

Se usa 5-fold rolling-origin. Con 40 perfiles, 10-fold dejaría aproximadamente cuatro perfiles por
fold y produciría estimaciones más variables. Cada fold entrena solo con sesiones anteriores y valida
en el bloque temporal siguiente. Popularidad, historial de categoría y patrones horarios se recalculan
con el train del fold; junio permanece como test y no participa en hiperparámetros.

El modelo es LambdaMART con un prior de negocio acotado. Solo ordena candidatos que ya pasaron
elegibilidad y capacidad. Utiliza afinidad de contenido/servicio/atributos, ambiente visual permitido,
disponibilidad, oportunidad por escasez, calidad, distancia, precio, baja exposición, historial,
horario común y cold-start. No usa IDs, outcomes futuros, posición ni atributos sensibles.

## Definición de métricas

Cada sesión madura con exactamente una alternativa relevante se convierte en ocho decisiones: una
positiva y siete negativas. El top-1 del modelo es la predicción positiva. Accuracy y error se miden
sobre esas ocho decisiones; precision, recall y F1 miden el acierto top-1 y evitan aprobar prediciendo
solo negativos. También se publican precision@3 y recall@3.

## Resultados reales

| Evidencia | Accuracy | Error | Precision | Recall | F1 | Recall@3 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Train final | 0,841280 | 0,158720 | 0,365120 | 0,365120 | 0,365120 | 0,700172 |
| Media 5-fold OOF | 0,825960 | 0,174040 | — | — | 0,303841 | 0,630761 |
| Test temporal junio | 0,818780 | 0,181220 | 0,275120 | 0,275120 | 0,275120 | 0,557416 |

Train queda por debajo de 0,90 y la brecha train-test es solo 0,022500, por lo que no hay evidencia de
sobreajuste. Test no alcanza accuracy 0,90, error <0,15 ni métricas 0,80. `qualityGatesPassed=false`.
La causa principal es que el generador selecciona entre candidatos mediante softmax con ruido gaussiano
y después aplica probabilidades estocásticas de clic/reserva; la alternativa observada no es una verdad
determinista de compatibilidad. Forzar 0,90 implicaría usar el outcome, memorizar IDs o redefinir el
positivo después de observar test.

## Escenarios de negocio

La suite contrafactual separada pasa 10/10, con accuracy/precision/recall/F1 1,00 y error 0,00:

1. alta afinidad, baja exposición y pocas plazas;
2. ambiente visual permitido acorde;
3. rango horario habitual;
4. local cercano compatible;
5. especialidad solicitada;
6. historial de reservas de categoría;
7. exploración cold-start;
8. calidad alta sin permitir que venza a una intención incompatible;
9. capacidad reservable;
10. balance de precio y distancia.

Estos escenarios prueban comportamiento del ranking, pero no sustituyen el test conductual. El
artefacto permanece candidato, promoción false y fallback determinista activo.
