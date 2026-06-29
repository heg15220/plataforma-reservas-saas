# Persistencia de identidad, roles, sesiones y tokens

## Alcance

La migración `V2__create_identity_role_session_and_token_tables.sql` establece el núcleo de identidad autenticada. No implementa todavía registro, login, hashing, emisión de credenciales ni autorización HTTP; esos casos de uso pertenecen a tareas posteriores de la Fase 1.

El cliente final del MVP permanece anónimo según `RB-001`. La tabla `"Users"` contiene únicamente cuentas que pueden autenticarse, inicialmente propietarios de locales y administradores.

## Modelo

### `"Users"`

Almacena email visible, email canónico, hash de contraseña, locale, verificación de email, estado y auditoría temporal.

- `"emailNormalized"` debe estar en minúsculas y es único.
- `"passwordHash"` nunca admite contraseñas en claro. `PasswordHashingService` usa BCrypt 2b,
  coste configurable validado entre 12 y 16 y una sal aleatoria por credencial.
- `"accountType"` admite `customer`, `venue_business` o `admin`; su default seguro es `customer`.
- `"preferredLocale"` admite `es` o `en`.
- `"status"` admite `pending_email_verification`, `active`, `suspended` o `disabled`.
- Todos los instantes son `timestamp with time zone` y se tratan como UTC en Java.

`accountType` clasifica la naturaleza de la cuenta, pero no concede permisos. El registro de local debe escribir `venue_business` explícitamente y completar verificación empresarial; la autorización efectiva depende de los roles de `"UserRoles"`. Las cuentas `admin` deben provisionarse mediante un flujo interno controlado.

### `"Roles"` y `"UserRoles"`

`"Roles"` es un catálogo cerrado con UUIDs estables:

- `venue_owner`;
- `admin`;
- `employee_user`, reservado para acceso futuro de empleados.

`anonymous` no es una asignación persistente: representa una petición sin identidad autenticada.

`"UserRoles"` impide duplicar el mismo rol para una cuenta. `"assignedByUserId"` permite conservar el actor de una concesión administrativa y admite `null` para seeds o procesos de sistema. El borrado de una cuenta elimina sus asignaciones; un rol asignado no puede eliminarse.

### `"AuthSessions"`

Representa sesiones revocables. El secreto entregado al cliente no se guarda. Solo se persiste un SHA-256 hexadecimal de 64 caracteres en `"tokenHash"`.

Una sesión operativa deberá cumplir simultáneamente:

- `"revokedAt"` es `null`;
- `"expiresAt"` es posterior al instante actual;
- el hash calculado del secreto coincide mediante comparación segura.

Los índices parciales por cuenta y expiración preparan lookup y limpieza sin indexar sesiones ya revocadas.

### `"AuthTokens"`

Representa credenciales de un solo uso para:

- `email_verification`;
- `password_reset`.

El esquema exige hash SHA-256 hexadecimal, expiración posterior a emisión y estados finales coherentes. Un token no puede figurar consumido y revocado a la vez. Las operaciones futuras de consumo deben ser transaccionales e impedir reutilización concurrente.

## Privacidad y seguridad

- No se guardan tokens ni contraseñas en claro.
- BCrypt limita la entrada a 72 bytes UTF-8 para evitar truncamiento silencioso.
- La verificación acepta hashes BCrypt 2a/2b/2y existentes y falla cerrada ante formato inválido.
- Un coste codificado superior a 16 se trata como inválido para impedir trabajo no acotado ante
  corrupción o manipulación de la base de datos.
- Un hash dummy con el coste vigente reduce diferencias temporales cuando el login no dispone de
  una credencial válida.
- Tras autenticar, las variantes anteriores o costes inferiores deben regenerarse con la política
  vigente; nunca se reduce un coste superior.
- No se incorporan IP, ubicación ni agente de usuario a las sesiones, aplicando minimización de datos.
- Las credenciales dependientes se eliminan al suprimir la cuenta.
- Los hashes de token son únicos para impedir que un secreto represente dos credenciales.
- Las restricciones críticas viven también en PostgreSQL y no dependen únicamente de validación de aplicación.

## Persistencia Java

Cada tabla tiene una entidad JPA y un DAO en `com.reserly.platform.identity.persistence`. Los IDs usan `GenerationType.UUID`; las relaciones se mapean sobre getters y las tablas/columnas usan nombres físicos explícitos.

`AccountType` modela el catálogo en Java y `AccountTypeConverter` lo traduce de forma estricta a los valores SQL en minúsculas. Un valor desconocido produce error en vez de degradarse silenciosamente.

Las entidades son internas y no deben devolverse desde controladores. Las consultas sensibles futuras deberán usar `@Query` y expresar todos sus filtros de vigencia, pertenencia, propósito y estado.

## Política de contraseñas

`PasswordHashingService` es la única frontera autorizada para operar contraseñas:

- `validate` comprueba que el secreto no sea nulo, vacío ni supere 72 bytes UTF-8;
- `hash` genera un hash autocontenido `$2b$` con coste y sal;
- `matches` realiza comparación BCrypt y usa el hash dummy si falta o está malformado;
- `requiresRehash` detecta variantes distintas de 2b y costes inferiores.

La longitud funcional mínima permanece en los DTO/casos de uso, actualmente 12 caracteres para
registro. La frontera criptográfica repite su propio límite para proteger también login,
recuperación y futuros flujos internos. Contraseña, hash y resultado detallado nunca se registran.

## Verificación

`IdentityPersistenceIntegrationTests` ejecuta el esquema sobre PostGIS efímero y comprueba:

- descubrimiento de los cinco DAOs;
- tablas físicas y catálogo de roles;
- unicidad de email normalizado;
- default `customer`, conversión de los tres tipos y rechazo de valores desconocidos;
- rechazo de secretos sin hash;
- cascada de asignaciones, sesiones y tokens al eliminar una cuenta.

`DatabaseMigrationIntegrationTests` verifica además que Flyway alcanza la versión `3` y Hibernate valida los mapeos contra el esquema real.
