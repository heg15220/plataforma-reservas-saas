# Adaptadores remotos de verificación empresarial

## Alcance

La tarea `1.6` crea la infraestructura ejecutable para consultar registros fiscales o mercantiles
sin acoplar el dominio a un proveedor. Incluye selección, límites temporales, reintentos,
idempotencia, clasificación de errores y auditoría mínima.

No incorpora todavía VIES, AEAT ni otro cliente de producción. Esos adaptadores y la política
España/UE pertenecen a `1.7`. Tampoco cambia `businessVerificationStatus`: traducir un resultado
técnico a `pending_remote_check`, `verified`, `pending_review`, `rejected` o `expired` pertenece a
`1.8`.

## Capas y contratos

El flujo interno es:

```text
RemoteBusinessVerificationService
  -> carga BusinessAccount desde PostgreSQL
  -> RemoteBusinessVerificationGatewayService
     -> RemoteBusinessVerificationAdapterRegistry
     -> RemoteBusinessVerificationAdapter
  -> guarda BusinessVerificationCheck
```

`RemoteBusinessVerificationAdapter` declara:

- código técnico estable;
- países soportados;
- prioridad;
- operación `verify`.

La prioridad menor gana en selección automática. Esto permite que un adaptador oficial y gratuito
tenga precedencia sobre uno comercial. Si se solicita un proveedor explícito, la ausencia o
incompatibilidad falla de forma controlada; nunca se cambia silenciosamente a otro.

El registro valida al arrancar:

- código en minúsculas y de 2 a 64 caracteres seguros;
- al menos un país ISO alpha-2 en mayúsculas;
- prioridad no negativa;
- ausencia de códigos duplicados.

## Request y minimización

El servicio recibe únicamente `requestId`, `businessAccountId` y proveedor preferido. País,
identificador canónico, razón social y dirección se cargan desde PostgreSQL para impedir que un
llamante interno sustituya datos fiscales.

El adaptador recibe:

- `requestId`;
- identificador interno de cuenta, que no debe enviarse fuera;
- país e identificador canónicos;
- razón social;
- dirección opcional.

El resultado admite estado `VERIFIED`, `INVALID` o `INCONCLUSIVE`, coincidencias opcionales,
referencia remota, instante y hash SHA-256 opcional. No admite cuerpo, mensaje libre ni payload del
proveedor.

## Timeouts y watchdog

Cada intento recibe:

- timeout de conexión;
- timeout de lectura;
- número de intento;
- clave idempotente;
- request de correlación.

El adaptador concreto debe configurar conexión y lectura en su cliente. Además, el gateway ejecuta
la llamada bloqueante en un hilo virtual y aplica un watchdog de duración
`connectTimeout + readTimeout`. Si vence, cancela la tarea y clasifica el intento como timeout.

No se mantiene una transacción PostgreSQL abierta durante la llamada remota.

## Reintentos

La taxonomía distingue:

- `NO_ADAPTER_CONFIGURED`;
- `PROVIDER_TIMEOUT`;
- `PROVIDER_UNAVAILABLE`;
- `PROVIDER_RATE_LIMITED`;
- `PROVIDER_AUTHENTICATION_ERROR`;
- `PROVIDER_PROTOCOL_ERROR`;
- `INVALID_PROVIDER_RESPONSE`.

Solo timeout, indisponibilidad y rate limit son reintentables. Autenticación, protocolo, respuesta
inválida o ausencia de adaptador terminan inmediatamente.

La política por defecto realiza hasta tres intentos con backoff exponencial de 250 ms, multiplicador
2 y máximo de 2 s. El esquema impide guardar más de cinco intentos. Una interrupción restaura el
flag del hilo y termina como indisponibilidad.

## Idempotencia

El llamante genera un UUID `requestId` por operación lógica. El gateway deriva
`SHA-256(providerCode + ":" + requestId)` y entrega la misma clave al adaptador en todos los
reintentos. El adaptador debe propagarla cuando el proveedor admita idempotency keys.

V5 añade a `"BusinessVerificationChecks"`:

- `"requestId"` UUID único;
- `"attemptCount"` entre 0 y 5;
- `"durationMs"` no negativo.

Antes de invocar la red, el servicio busca `requestId`. Si ya existe, devuelve la misma evidencia.
Reutilizarlo para otra cuenta se rechaza sin exponer identificadores. El índice único resuelve
carreras locales y la referencia remota única evita duplicar una respuesta estable del proveedor.

Los registros anteriores a V5 reciben su propio ID como `requestId`, preservando una identidad única
sin reconstruir eventos históricos.

## Errores y auditoría

Un fallo final se persiste con:

- proveedor o marcador `unavailable`;
- país e identificador canónicos;
- estado `error`;
- código controlado;
- clave i18n;
- instante;
- intentos;
- duración.

No se guarda excepción, URL, credencial, cuerpo ni mensaje remoto. Los identificadores tampoco se
incluyen en excepciones de aplicación.

Los resultados válidos guardan únicamente estado técnico, coincidencias, referencia y hash opcional.
Esta evidencia no autoriza publicación ni actualiza todavía el resumen de la cuenta.

## Extensión para un proveedor

Un adaptador de `1.7` deberá:

1. implementar `RemoteBusinessVerificationAdapter`;
2. declarar países y prioridad;
3. configurar autenticación y timeouts;
4. propagar idempotencia si el protocolo lo soporta;
5. validar tamaño, tipo y estructura de respuesta;
6. mapear fallos a la taxonomía cerrada;
7. calcular evidencia mínima;
8. no registrar secretos, identificadores ni cuerpos;
9. demostrar el contrato con un servidor simulado y fixtures controlados.
