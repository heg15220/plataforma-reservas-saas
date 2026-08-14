# Dashboard interno del motor de demanda

`GET /api/internal/demand/v1/observability/dashboard?hours=24` requiere autenticación de servicio y
rol `DEMAND_INGESTOR`. La respuesta no contiene identidades, locales, sesiones ni muestras.

## Lectura

- `persistedVolume` y `instrumentationCoveragePercent` usan la ventana UTC solicitada.
- `accepted`, `rejected`, `duplicates` y latencia son métricas desde el inicio del proceso; el campo
  `runtimeCounterScope=process_lifetime` evita confundirlas con una ventana persistida.
- `missingEventTypes` compara los tipos persistidos con el catálogo efectivo v1 de la ingesta.
- `quality` incorpora la auditoría de completitud, duplicidad, tiempo, consentimiento y PII.

## Alertas iniciales

- Cualquier `piiLeakageEvents`, `consentViolations` o `temporalOrderViolations` mayor que cero: crítica.
- Rechazos superiores al 5 % de aceptados durante 15 minutos: advertencia; 15 %: crítica.
- Duplicados superiores al 10 %: investigar reintentos o productor defectuoso, sin desactivar
  idempotencia.
- Latencia media de storage superior a 100 ms o máxima superior a 500 ms durante 15 minutos:
  revisar PostgreSQL, locks y backlog.
- Cobertura de eventos canónicos backend/web por debajo de la esperada durante un recorrido E2E:
  bloquear promoción. En entornos sin tráfico, ausencia no equivale automáticamente a incidente.

Los códigos de rechazo son opacos y de cardinalidad acotada. No deben añadirse IDs, excepciones,
payloads o valores de contexto como etiquetas de Micrometer.
