# Verificación de email

## Alcance

La tarea 1.14 conecta el registro de locales con un desafío verificable y añade los contratos para
consumirlo o solicitar otro. No implementa todavía el proveedor Brevo, las plantillas ES/EN, el
consumidor con reintentos ni el outbox operativo; corresponden a la Fase 8.

## Emisión

`VenueRegistrationServiceImpl` crea la identidad, empresa, rol y desafío en una sola transacción.
`OneTimeTokenService` obtiene 32 bytes de `SecureRandom` y los codifica como 43 caracteres Base64
URL-safe sin relleno. `"AuthTokens"` recibe únicamente:

- usuario y propósito `email_verification`;
- SHA-256 hexadecimal del secreto;
- emisión y caducidad absoluta.

La vigencia predeterminada es 24 horas y se valida al arrancar entre 15 minutos y 7 días.

Después del commit, `EmailVerificationEventRelay` serializa un trabajo JSON persistente con
`eventId`, `userId`, email, locale, token y caducidad. Lo publica en `reserly.jobs.v1` mediante
`identity.email-verification.requested.v1`; la cola durable propia es
`reserly.identity.email-verification.v1` y envía rechazos definitivos al dead letter compartido.
Email y token son datos sensibles del transporte y nunca se registran.

## Consumo

`POST /api/auth/email/verify` recibe:

```json
{
  "token": "43-caracteres-Base64-URL-safe"
}
```

El servicio rechaza el formato antes de consultar datos. Después calcula SHA-256 y obtiene mediante
lock pesimista el token de propósito correcto y su usuario. Solo continúa si:

- no está consumido ni revocado;
- `expiresAt` es estrictamente posterior al instante actual;
- pertenece a una cuenta `venue_business`;
- la cuenta no está deshabilitada.

El consumo fija `consumedAt`. Si el email seguía pendiente, fija `emailVerifiedAt`, cambia
`pending_email_verification` a `active` y actualiza `updatedAt`. Una suspensión se conserva: poder
probar el control del email no revoca una decisión operativa. Finalmente se revocan desafíos
hermanos activos.

La respuesta correcta contiene únicamente `emailVerified`, `emailVerifiedAt` y `accountStatus`.
Todos los tokens inexistentes, malformados, expirados, revocados o usados comparten
`400 EMAIL_VERIFICATION_INVALID`.

## Nueva solicitud

`POST /api/auth/email/verification/request` recibe un email válido y responde siempre `202` sin
cuerpo. Solo una cuenta de local pendiente genera trabajo: bajo lock de usuario revoca desafíos
activos y crea uno nuevo. Cuenta inexistente, ya verificada, suspendida o deshabilitada produce el
mismo contrato público.

El endpoint no sustituye el rate limiting de la tarea 1.16.

## Consistencia y límites

PostgreSQL es la fuente de verdad del desafío. El evento se publica en fase `AFTER_COMMIT`, evitando
entregar un token cuya creación finalmente se revierta. Todavía existe una ventana entre commit y
RabbitMQ: si el broker falla, se registra solo `eventId` y el usuario puede pedir un desafío nuevo.
La cola con reintentos, entrega idempotente, almacenamiento de errores y outbox se completará en
8.7–8.8.

No se añadió migración: V2 ya contiene propósito cerrado, hash único, vigencia, consumo, revocación,
restricciones de estados finales e índices parciales.
