# ADR-0001: límites entre el monolito y Demand Engine

- Estado: aceptada.
- Fecha: 2026-08-13.
- Propietarios: Backend Platform y Data/ML.
- Revisión obligatoria: antes de conceder acceso directo a PostgreSQL, introducir otro canal de
  transporte o permitir que el motor proponga mutaciones.

## Contexto

Reserly ya dispone de un monolito modular Spring Boot que protege publicación, permisos,
disponibilidad, recursos, capacidad, holds, reservas, pagos e incidencias. El motor de demanda
necesita Python y librerías de datos para calcular atributos, embeddings, predicciones y rankings,
pero no puede debilitar las invariantes transaccionales ni convertir una dependencia experimental en
un requisito para reservar.

La frontera debe permitir aprendizaje e inferencia sin duplicar ownership, filtrar datos personales
ni aceptar candidatos que el usuario no habría podido reservar.

## Decisión

`Demand Engine` será un servicio interno, desplegable de forma independiente y **consultivo**. El
monolito es el único punto de entrada para web/app, el único escritor del dominio operativo y la
última autoridad sobre elegibilidad y capacidad.

En la primera versión:

- el navegador nunca conoce ni llama la URL del motor;
- el motor no dispone de credenciales de escritura sobre tablas operativas;
- el motor no crea holds, confirma/cancela reservas, envía comunicaciones ni cambia precios;
- Spring genera candidatos aplicando restricciones duras antes de solicitar ranking;
- Spring valida de nuevo todos los candidatos y la disponibilidad después de recibir la respuesta;
- una caída, timeout o respuesta inválida activa una política determinista local;
- el camino de reserva funciona cuando el servicio, RabbitMQ, pgvector o MLflow no están disponibles.

## Ownership de datos y decisiones

| Agregado o decisión                             | Propietario                      | Acceso de Demand Engine                             |
| ----------------------------------------------- | -------------------------------- | --------------------------------------------------- |
| Usuarios, sesiones operativas y consentimientos | Spring/Identidad                 | Proyección seudónima mínima por contrato            |
| Locales, publicación, servicios y recursos      | Spring/Catálogo                  | Snapshot de candidatos o eventos versionados        |
| Horarios, franjas, capacidad, holds y reservas  | Spring/Disponibilidad y Reservas | Lectura de snapshot; nunca mutación                 |
| Pagos, penalizaciones, incidencias y documentos | Spring                           | Sin acceso salvo agregado explícito futuro          |
| Eventos canónicos, alternativas y atribución    | Spring/Demand boundary           | Consume eventos; solicita persistencia por contrato |
| Ontología y estados de gobernanza               | Spring/Administración            | Calcula propuestas; no publica atributos            |
| Features, embeddings, predicciones y rankings   | Demand Engine                    | Calcula; Spring persiste lo auditable               |
| Artefactos, parámetros y métricas de modelos    | Data/ML mediante MLflow          | Propietario técnico                                 |
| Elegibilidad final, hold y confirmación         | Spring                           | Sin autoridad                                       |

Mientras los fundamentos vivan en una única base, Flyway mantiene el esquema. Python no ejecuta
migraciones. Si el volumen exige un almacén analítico o una réplica, se aprobará otro ADR con
replicación, consistencia, borrado y ownership explícitos.

## Contrato síncrono de ranking

Spring expone hacia dentro un puerto `DemandRankingClient`; su adaptador HTTP llama:

```text
POST /internal/demand/v1/rankings
```

Petición conceptual:

```json
{
  "requestId": "uuid",
  "schemaVersion": 1,
  "policyVersion": "content-v1",
  "occurredAt": "UTC instant",
  "locale": "es|en",
  "context": {
    "category": "peluqueria",
    "zone": "coarse-zone-id",
    "requestedDate": "local-date"
  },
  "candidates": [
    {
      "venueId": "uuid",
      "serviceId": "uuid|null",
      "timeSlotId": "uuid|null",
      "distanceMeters": 500,
      "availableCapacity": 1,
      "visibleAttributeCodes": ["quiet", "nearby"]
    }
  ]
}
```

Respuesta conceptual:

```json
{
  "requestId": "same uuid",
  "schemaVersion": 1,
  "modelVersion": "baseline-1",
  "policyVersion": "content-v1",
  "rankedCandidates": [
    {
      "venueId": "uuid from request",
      "rank": 1,
      "score": 0.82,
      "components": { "affinity": 0.9, "proximity": 0.7 },
      "explanationCodes": ["nearby", "available_at_requested_time"]
    }
  ]
}
```

Invariantes del adaptador Spring:

- `requestId` debe coincidir y la versión debe estar soportada;
- la respuesta solo puede ser una permutación/subconjunto de candidatos enviados;
- candidato desconocido, UUID duplicado, score no finito, rank duplicado o explicación fuera de
  catálogo invalida la respuesta completa;
- omitir candidatos está permitido y Spring completa el orden con la política determinista;
- score y explicación nunca convierten un candidato inelegible en elegible;
- el frontend recibe razones localizadas desde catálogos Spring/Next, no texto libre de Python.

El contrato de conversión o demanda futuro seguirá el mismo sobre (`requestId`, `schemaVersion`,
versiones de política/modelo y timestamp) y nunca devolverá decisiones transaccionales.

## Contrato asíncrono

Los cambios confirmados se publicarán mediante outbox de PostgreSQL y RabbitMQ, no dentro de una
transacción distribuida. El sobre `DemandEventEnvelopeV1` contiene:

```text
eventId, schemaVersion, eventType, occurredAt, aggregateType,
aggregateId, correlationId, consentPurpose, payload tipado y minimizado
```

Reglas:

- el commit de negocio y la fila outbox son atómicos;
- entrega al menos una vez; el consumidor deduplica por `eventId`;
- orden solo garantizado por agregado cuando sea necesario y documentado;
- un evento incompatible va a dead-letter sin registrar el payload;
- email, teléfono, texto libre, tokens y documentos están prohibidos;
- RabbitMQ caído acumula outbox y no revierte una reserva confirmada.

Las tareas 19.5-19.11 implementarán catálogo, persistencia, ingestión, instrumentación y
reconciliación. Este ADR define el límite, no adelanta esos componentes.

## Presupuesto de latencia y resiliencia

Valores iniciales, externalizables por entorno:

- timeout de conexión: `50 ms`;
- timeout total de ranking: `200 ms`;
- presupuesto p95 añadido: `150 ms`;
- cero reintentos síncronos dentro de una búsqueda;
- circuit breaker abre durante `30 s` tras `>= 50 %` de fallos en una ventana mínima de 20 llamadas;
- máximo 100 candidatos por petición y 64 KiB de cuerpo serializado;
- bulkhead independiente para que ranking no agote threads de búsqueda/reserva.

Un timeout cancela la llamada y sirve fallback. No se encadenan llamadas a conversión, atributos y
ranking en la misma petición pública. Los valores deberán validarse con carga antes de activación.

## Fallback determinista

Spring conserva una implementación local y versionada:

1. elimina candidatos no publicados, incompatibles o sin disponibilidad;
2. ordena por coincidencia de filtros y texto;
3. aplica disponibilidad y cercanía cuando exista permiso;
4. usa valoración con muestra mínima y popularidad contextual;
5. desempata por identificador estable.

El fallback se activa ante circuito abierto, timeout, error HTTP, contrato inválido, modelo no
aprobado, versión incompatible o falta de confianza. La respuesta pública mantiene el mismo contrato
y señala internamente `rankingSource=deterministicFallback`; no muestra fallos técnicos al usuario.

## Seguridad y privacidad

- Comunicación de producción con identidad de workload y mTLS o mecanismo equivalente administrado;
  no se reutilizan cookies de usuario ni credenciales humanas.
- Desarrollo local usa un secreto específico no versionado y red privada de Compose.
- Autorización deny-by-default para `/internal/demand/**`; la API pública no reenvía sus cabeceras.
- Contexto geográfico agregado; ninguna coordenada habitual o domicilio se convierte en feature.
- Identidades solo seudónimas y consentidas; no se envía email en claro ni HMAC salvo necesidad
  explícita de una tarea posterior.
- Logs contienen `requestId`, versión, duración, resultado y código de fallback; no payloads,
  candidatos completos, vectores, texto libre o datos personales.
- La revocación y supresión se ejecutan en Spring y se propagan a artefactos derivados según su
  contrato de retención.

## Compatibilidad y despliegue

- Contratos versionados en ruta y `schemaVersion`.
- Cambios aditivos compatibles dentro de v1; campos obligatorios, semántica o tipos requieren v2.
- Spring soporta versión actual y anterior durante una ventana de despliegue.
- Demand Engine se despliega primero en shadow, después canary y finalmente como campeón.
- La configuración de campeón se cambia de forma atómica; rollback selecciona política/artefacto
  anterior sin desplegar Spring.
- La health readiness del motor comprueba artefacto cargado, pero no forma parte de la readiness del
  monolito.

## Observabilidad

Métricas mínimas: solicitudes, latencia, timeout, circuito, respuesta inválida, fallback por causa,
candidatos enviados/devueltos/descartados y versión. Las trazas propagan `traceparent` y
`correlationId` sin convertirlos en identidad de usuario. Alertas del motor nunca detienen reservas.

## Alternativas rechazadas

### Biblioteca Python embebida en JVM

Rechazada por acoplar ciclos de despliegue, dependencias nativas y recursos del proceso transaccional.

### Demand Engine como propietario de búsqueda y reserva

Rechazada porque duplicaría autorización/capacidad y haría crítica una dependencia probabilística.

### Acceso Python de escritura a tablas operativas

Rechazado por saltarse servicios, auditoría, locks y convenciones JPA/Flyway.

### Eventos exclusivamente desde frontend

Rechazado porque pueden perderse, manipularse y no demuestran el resultado transaccional.

### Reintentos síncronos y fallback dentro de Python

Rechazados porque amplifican latencia y ocultan a Spring qué política produjo el orden. El fallback
autoritativo vive en el monolito.

## Consecuencias

El diseño introduce contratos y cierta duplicación de snapshots, pero mantiene consistencia,
degradación segura y auditoría. La calidad del ranking puede evolucionar sin cambiar el flujo de
reserva. A cambio, los casos batch deben solicitar persistencia o consumir proyecciones autorizadas;
no pueden improvisar acceso a datos operativos.
