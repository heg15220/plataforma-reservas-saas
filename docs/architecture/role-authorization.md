# Autenticación de sesión y autorización por roles

## Alcance

La tarea `1.17` incorpora Spring Security como frontera única de los namespaces privados:

| Namespace                            | Rol exigido   |
| ------------------------------------ | ------------- |
| `/api/venue/me` y `/api/venue/me/**` | `venue_owner` |
| `/api/admin` y `/api/admin/**`       | `admin`       |

El resto de rutas conserva acceso público hasta que su tarea funcional defina otra política. Los
prefijos son segmentos completos: una ruta como `/api/venue/mechanical` no pertenece al namespace
privado.

`account_type` describe la naturaleza de la cuenta, pero no concede permisos. Una cuenta solo
accede si la sesión es válida y `UserRoles` contiene la concesión explícita requerida. El rol
`employee_user` no recibe acceso general a `/api/venue/me/**`; sus permisos deberán modelarse de
forma más granular cuando se implemente el acceso de empleados.

## Flujo de autenticación

1. `SessionAuthenticationFilter` se ejecuta una vez en rutas privadas.
2. Exige como máximo una cookie `reserly_session`; duplicados se tratan como credencial no válida.
3. `SessionTokenService` valida exactamente 43 caracteres Base64 URL-safe y calcula SHA-256.
4. PostgreSQL busca el hash con `revokedAt IS NULL` y `expiresAt > now`, cargando la cuenta en la
   misma consulta.
5. Solo una cuenta `active`, o una `venue_business` pendiente de verificar email, puede continuar.
6. Los roles se consultan desde `UserRoles` en cada petición; revocar una concesión tiene efecto
   inmediato.
7. Se crea un `AuthenticatedAccount` sin secreto y authorities `ROLE_*` para Spring Security.
8. La cadena autoriza el namespace o responde sin ejecutar el controlador.

Una cuenta suspendida o deshabilitada provoca la revocación de la sesión observada. Una sesión
expirada, revocada, desconocida o malformada comparte el mismo resultado anónimo y no revela qué
comprobación falló.

## Actividad y caducidad

`expiresAt` es absoluto y nunca se renueva al usar la API. `lastSeenAt` solo se escribe cuando han
pasado al menos `RESERLY_SESSION_ACTIVITY_UPDATE_INTERVAL`, cinco minutos por defecto. El update
vuelve a exigir que la sesión continúe vigente y no revocada, evitando una escritura por petición y
carreras que revivan una sesión.

## Respuestas HTTP

Una ruta privada sin sesión admisible devuelve:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{"error":"AUTHENTICATION_REQUIRED"}
```

Una sesión válida sin el rol exigido devuelve:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{"error":"AUTHORIZATION_DENIED"}
```

No se incluye el rol esperado, los roles actuales, el estado de cuenta ni detalles de la sesión.
Los controladores pueden recibir `AuthenticatedAccount` con `@AuthenticationPrincipal` y deben usar
su `userId` como actor; nunca deben aceptar desde el cliente el propietario de un recurso privado.

## Configuración de Spring Security

La API usa `SessionCreationPolicy.STATELESS`; no crea `HttpSession`, no instala form login, HTTP
Basic, logout de framework ni request cache. También excluye el usuario y contraseña aleatorios de
autoconfiguración, porque no forman parte del modelo de identidad de Reserly.

CSRF se mantiene explícitamente desactivado hasta `16.3`. `SameSite=Strict` y cookies `Secure`
reducen exposición, pero no sustituyen tokens CSRF para operaciones mutables.

CORS permite credenciales únicamente desde los orígenes exactos de `RESERLY_ALLOWED_ORIGINS`.
Admite métodos REST conocidos y una lista cerrada de cabeceras, incluida `X-CSRF-Token` para el
endurecimiento futuro. Un preflight de otro origen se rechaza antes de autenticación.

## Persistencia y privacidad

No se añade migración. `AuthSessions`, `Users`, `UserRoles` y `Roles` ya contienen hash, vigencia,
revocación, estado y concesiones. La autenticación no guarda el token, email o roles en logs, Redis
ni respuestas de error. El principal vive solo durante la petición.

La consulta de sesión y la carga de roles permanecen en PostgreSQL para que revocaciones,
suspensiones y cambios de permisos sean inmediatos. Si el volumen lo exige, cualquier caché futura
deberá tener invalidación explícita y no podrá relajar la fuente de verdad.
