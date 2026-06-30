# Login, logout y sesiones revocables

## Contratos HTTP

`POST /api/auth/login` recibe:

```json
{
  "email": "owner@example.com",
  "password": "correct-horse-battery-staple"
}
```

El éxito responde `200` con `userId`, `accountType`, `preferredLocale`, `emailVerified` y
`sessionExpiresAt`. El secreto no aparece en JSON: se entrega en `Set-Cookie`.

Credencial, cuenta inexistente, tipo no empresarial y estado bloqueado responden el mismo:

```json
{ "error": "AUTHENTICATION_INVALID" }
```

El status es `401`; un payload malformado usa el mismo código con `400`.

`POST /api/auth/logout` no requiere body. Con cookie válida revoca la sesión; sin cookie, cookie
malformada, desconocida o ya revocada mantiene `204`. Siempre envía un tombstone para eliminarla.

## Flujo de login

1. Bean Validation limita forma y tamaño del payload.
2. El email se recorta, convierte a minúsculas con locale neutro y se consulta mediante `@Query`.
3. `PasswordHashingService.matches` se ejecuta incluso sin usuario mediante su hash BCrypt dummy.
4. Solo `venue_business` en estado `active` o `pending_email_verification` puede continuar.
5. Tras una coincidencia correcta, una variante/coste antiguo se actualiza dentro de la transacción.
6. Se generan 32 bytes con `SecureRandom`, codificados Base64 URL-safe sin padding.
7. PostgreSQL recibe SHA-256 hexadecimal, usuario, creación, última actividad inicial y expiración.
8. El adaptador HTTP crea la cookie y devuelve metadatos no sensibles.

Una cuenta pendiente de email puede entrar al panel para completar el flujo, pero RB-012 continúa
bloqueando publicación y reservas públicas.

## Cookie

Nombre estable: `reserly_session`.

- host-only, sin atributo `Domain`;
- `HttpOnly`;
- `Path=/`;
- `SameSite=Strict`;
- `Secure` según `RESERLY_SECURE_COOKIES`, obligatorio en staging/producción;
- `Max-Age` igual a `RESERLY_SESSION_LIFETIME`, 12 horas por defecto.

El middleware de `1.17` hashea la cookie, exige sesión no revocada/no expirada y cuenta operativa,
carga roles explícitos y actualiza `lastSeenAt` con frecuencia acotada. Protege los namespaces de
local y administración según `docs/architecture/role-authorization.md`. La tarea `16.3` añadirá la
defensa CSRF apropiada para operaciones con cookie.

## Logout y persistencia

El token recibido debe medir exactamente 43 caracteres Base64 URL-safe. Un valor diferente se
descarta antes de SHA-256 para limitar trabajo y memoria. `AuthSessionDao.revokeByTokenHash` ejecuta
un update directo solo cuando `revokedAt IS NULL`; repetir logout no cambia el instante original ni
revela existencia.

El secreto nunca se persiste, registra o devuelve en body. La cookie se construye después de que la
transacción de login haya completado; si persistir la sesión falla, no se emite.

## Configuración

`SessionProperties` valida una duración absoluta entre 5 minutos y 30 días y un intervalo de
escritura de actividad entre 1 minuto y 1 hora. No existe renovación deslizante. Aumentar la
duración amplía exposición ante robo de cookie; reducirla afecta experiencia y debe coordinarse con
el panel.

## Verificación

Las pruebas unitarias cubren entropía/formato, hash estable, rechazo de cookies no acotadas,
duración, frecuencia de actividad y atributos de cookie por entorno. La integración PostgreSQL
cubre login, error uniforme, cuentas pendientes, bloqueo por estado/tipo, rehash, secreto no
persistido, logout repetido, autorización por roles y payload inválido.
