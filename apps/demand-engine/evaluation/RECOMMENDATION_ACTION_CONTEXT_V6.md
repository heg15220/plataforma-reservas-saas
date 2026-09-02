# Recomendador contextual por acciones, ubicación y escasez v6

Esta iteración evalúa un ranking consultivo sobre 100 locales, 40 perfiles, 3.200 sesiones,
25.600 candidatos y 17.596 acciones sintéticas de diez tipos. La ubicación procede del instante
de la solicitud y se transforma a distancia Haversine, proximidad y ajuste al radio; las
coordenadas crudas no son features del modelo.

## Contrato de comportamiento

- La intención reciente combina búsquedas, filtros, vistas, mapas, disponibilidad, guardados,
  comparaciones e inicios/finalizaciones de reserva con decaimiento por recencia.
- La preferencia persistente solo aporta señal cuando existe consentimiento.
- Un local con pocas plazas recibe oportunidad únicamente mediante
  `contentAffinity * currentLocationProximity * withinPreferredRadius * remainingSlotUrgency`.
- Capacidad positiva, apertura, servicio reservable y elegibilidad son filtros anteriores al ranking.
- Calidad, popularidad o urgencia nunca deben anular una incompatibilidad de intención o ubicación.

## Protocolo y resultados

Se seleccionó LambdaMART con cinco folds `rolling-origin` sobre 2.400 sesiones de desarrollo. El
holdout posterior de 800 sesiones se selló antes de seleccionar y se abrió una sola vez.

| Métrica | Desarrollo 5-fold | Test temporal |
| --- | ---: | ---: |
| Accuracy | 88,35 % | 91,25 % |
| Error | 11,65 % | 8,75 % |
| Precision | 88,35 % | 91,25 % |
| Recall | 88,35 % | 91,25 % |
| F1 | 88,35 % | 91,25 % |
| Recall@3 | 99,95 % | 100 % |

La brecha absoluta train-test es 2,9 puntos. Los diez escenarios contrafactuales pasan. En test,
los cortes obtienen 99,34 % para escasez alineada, 91,74 % para casos sensibles a ubicación y
92 % para cambios de intención. Las elecciones claras alcanzan 96,55 %; las 47 elecciones
estocásticas declaradas como ambiguas se publican por separado.

## Límites

El resultado pasa las puertas offline, pero es evidencia sintética: no acredita causalidad,
conversión ni seguridad de promoción. `promotionAllowed=false`. Producción debe aplicar
consentimiento, TTL y minimización de ubicación, observación shadow, A/B y fallback determinista.
