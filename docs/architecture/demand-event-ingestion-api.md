# API interna de ingesta de eventos

`POST /api/internal/demand/v1/events` acepta un sobre `{ events: [...] }` de 1 a 100 eventos. El
namespace exige `X-Reserly-Service-Token`, se compara en tiempo constante y crea exclusivamente el
rol técnico `DEMAND_INGESTOR`. No acepta cookies de usuario ni se publica como API web.

La cuota distribuida usa el rate limiter Redis existente y discrimina por identificador técnico,
cuya clave se persiste como SHA-256. Redis caído falla cerrado. Tamaño máximo, retención,
identificador, token e interruptor se configuran por entorno; staging/producción deben obtener el
secreto de un gestor seguro y transportarlo mediante TLS.

La validación se ejecuta sobre todo el lote antes de la primera escritura: versión, catálogo de 22
eventos, productor canónico, finalidad, IDs permitidos, contexto por familia, tipos/rangos, pares
importe/moneda, bytes y consentimiento vigente. Los campos JSON desconocidos se rechazan globalmente.
`receivedAt` lo asigna el servidor y no puede ser anterior a `occurredAt`.

Cada evento se persiste con unicidad física de `eventId`. Reintentos devuelven `duplicate` y carreras
de unicidad recuperan la fila vencedora. La respuesta solo contiene eventId y estado. Un lote con
contrato inválido se rechaza antes de escribir; un error inesperado de infraestructura puede dejar
los eventos anteriores aceptados, que el productor reintenta de forma segura con los mismos IDs.

Los errores de contrato se reducen externamente a `EVENT_INVALID`, sin campo, valor, constraint o
mensaje de librería. La aplicación no registra body, contexto, identidad ni token. Micrometer cuenta
accepted/duplicate/rejected/disabled y rechazos por código interno de baja cardinalidad. Rate limit
usa sus contratos globales 429/503.

La configuración inicial admite un productor allowlisted. La rotación solapada y múltiples
credenciales se incorporarán cuando se despliegue Demand Engine en fase 20; hasta entonces rotar
requiere actualizar secreto y consumidor coordinadamente.
