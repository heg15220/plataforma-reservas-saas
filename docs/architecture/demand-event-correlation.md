# Correlación de eventos de demanda

## Contrato de propagación

La cabecera `X-Reserly-Correlation-Id` contiene un UUID de recorrido. La web crea una correlación
al comenzar una búsqueda y la mantiene en `sessionStorage` para clic, ficha, disponibilidad, hold,
confirmación y reseña dentro de la misma pestaña. No es cookie, identidad, credencial ni prueba de
causalidad.

Las API web públicas propagan la cabecera. `DemandCorrelationFilter` solo actúa sobre
`/api/public/**`: conserva UUID válido, sustituye ausente o inválido y expone el valor normalizado en
respuesta. CORS limita la cabecera a orígenes configurados. Un valor controlado por cliente nunca
participa en autenticación, autorización, consultas de objeto ni rate limiting.

`DemandOperationalTelemetryAspect` captura el UUID ya validado antes de publicar el evento async.
Por ello `bookingStarted` y `bookingCompleted`, aun ocurriendo en requests distintos, pueden unirse
con el recorrido web que originó la reserva. Fuera de un request HTTP se genera un UUID local seguro.

## Reconciliación

`BehaviorEventDao.findByRequestIdOrdered` usa la columna e índice de V46 y conserva orden estable por
`occurredAt,eventId`. `DemandEventReconciliationService` separa productores `web` y `spring` y
clasifica:

- `matched`: existen observaciones de ambos productores;
- `frontend_only`: el navegador informó actividad sin resultado backend aceptado;
- `backend_only`: existe resultado canónico sin observación web, por ejemplo integración o bloqueo
  de telemetría cliente;
- `not_found`: no hay eventos aceptados para el UUID.

La proyección devuelve solo `eventId`, tipo, productor e instante. Excluye contexto, sesión,
identidades, sujetos operativos y cualquier dato personal. El backend es autoridad del resultado;
una observación frontend no convierte un hold en reserva ni reemplaza asistencia/cancelación.

## Límites operativos

La correlación es observacional y best-effort: bloqueo de JavaScript, cierre de pestaña o entrega
async fallida pueden producir trazas parciales. Los estados hacen visible esa ausencia sin inventar
enlaces por email, IP o fingerprint. 19.20 medirá cobertura y 19.19 validará orden y completitud.
