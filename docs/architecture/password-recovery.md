# Recuperación de contraseña

## Alcance

La tarea 1.15 implementa solicitud, rotación y consumo de recuperación para cuentas de local. El
proveedor transaccional, la plantilla ES/EN, el consumidor con reintentos y el outbox se completarán
en la Fase 8.

## Solicitud

`POST /api/auth/password/forgot` recibe:

```json
{
  "email": "local@example.com"
}
```

Todo email estructuralmente válido recibe `202` sin cuerpo. Bajo lock de usuario, solo una cuenta
`venue_business` no deshabilitada revoca desafíos anteriores y crea otro. La respuesta no distingue
cuenta inexistente, otro tipo o cuenta deshabilitada. Las cuentas pendientes y suspendidas pueden
renovar su contraseña, pero el flujo no cambia sus estados.

El secreto contiene 32 bytes CSPRNG y 43 caracteres Base64 URL-safe sin relleno. PostgreSQL almacena
solo SHA-256, propósito `password_reset`, emisión y caducidad. La vigencia predeterminada es 30
minutos y se valida entre 10 minutos y 24 horas.

Tras el commit, `PasswordResetEventRelay` publica un mensaje JSON persistente con `eventId`,
`userId`, email, locale, token y caducidad. La routing key es
`identity.password-reset.requested.v1` y la cola durable es
`reserly.identity.password-reset.v1`, con dead lettering compartido.

## Restablecimiento

`POST /api/auth/password/reset` recibe:

```json
{
  "token": "43-caracteres-Base64-URL-safe",
  "newPassword": "nueva-contraseña-segura"
}
```

La nueva contraseña debe tener al menos 12 caracteres y respetar el límite BCrypt de 72 bytes
UTF-8. El servicio rechaza el formato del token antes del lookup, calcula SHA-256 y bloquea el
desafío con su usuario. Solo continúa si el propósito es correcto, no se consumió ni revocó,
`expiresAt` sigue en el futuro y la cuenta es de local no deshabilitada.

Dentro de la misma transacción:

1. `PasswordHashingService` genera BCrypt 2b con sal y coste vigente.
2. Se actualizan `passwordHash` y `updatedAt`.
3. El desafío se marca consumido.
4. Los demás desafíos `password_reset` activos quedan revocados.
5. Todas las sesiones no revocadas de la cuenta reciben `revokedAt`.

No se alteran email, verificación de email, estado operativo, roles ni estado empresarial. Una cuenta
suspendida conserva la suspensión.

Token inexistente, expirado, revocado, usado, de otro propósito, cuenta no admisible o contraseña
fuera de política comparte `400 PASSWORD_RESET_INVALID`.

## Seguridad y límites

- El endpoint de solicitud no confirma existencia.
- El secreto no aparece en respuesta, log ni PostgreSQL.
- Cada solicitud invalida el enlace anterior.
- El consumo es de un solo uso y está protegido con lock pesimista.
- Cambiar la contraseña cierra todas las sesiones.
- El rate limiting corresponde a 1.16.
- La publicación `AFTER_COMMIT` evita entregar un token revertido, pero mantiene una ventana de
  pérdida hasta que la Fase 8 incorpore outbox y reintentos operativos.
- El payload RabbitMQ contiene el enlace sensible y exige TLS o red privada, permisos mínimos y
  retención acotada.

No se añadió migración: `"AuthTokens"` y `"AuthSessions"` de V2 ya contienen las restricciones,
índices y estados necesarios.
