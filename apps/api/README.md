# API de Reserly

Aplicación Spring Boot 4 con Java 21. Alojará la API REST y la lógica transaccional de Reserly como monolito modular.

## Organización

El paquete raíz es `com.reserly.platform`. Los contextos de negocio se separan en paquetes de primer nivel y deben exponer contratos explícitos antes de permitir dependencias desde otros contextos.

El esqueleto contiene el punto de entrada, la declaración documental de los contextos y la infraestructura transversal inicial. Seguridad y observabilidad se añadirán en sus tareas específicas.

La configuración se enlaza mediante `ReserlyProperties` y los perfiles `local`, `staging`, `production` y `test`. Staging y producción fallan si las URLs públicas no usan HTTPS, las cookies seguras están desactivadas o se intenta activar el pago real prematuramente.

PostgreSQL es la fuente de verdad. Flyway ejecuta las migraciones antes de que Hibernate valide el esquema; Hibernate nunca crea ni modifica tablas.

Redis se integra mediante Spring Data Redis y Spring Cache con TTL de cinco minutos, prefijo `reserly::` y valores nulos deshabilitados. Ningún dato transaccional puede depender exclusivamente de la caché.

El paquete `infrastructure.ratelimit` protege login, registro y recuperación por dirección remota,
y la verificación empresarial por cuenta. Usa contadores Lua atómicos con TTL y discriminadores
SHA-256; una cuota agotada devuelve `429` con `Retry-After` y Redis no disponible falla cerrado. El
contrato está en `docs/architecture/rate-limiting.md`.

RabbitMQ se integra mediante Spring AMQP. La topología base declara los exchanges `reserly.jobs.v1` y `reserly.jobs.dead-letter.v1`, además de una cola durable de aparcamiento. Cada contexto de negocio deberá declarar su propia cola y routing key.

Los textos configurables que se persistan en base de datos deben usar el contrato `LocalizedText` del paquete `localization` y columnas JSONB `lowerCamelCase` como `"descriptionI18n"`. El patrón completo está documentado en `docs/architecture/localized-data.md`.

Las convenciones de entidades, migraciones, DAOs, servicios, controladores, DTOs y conversores se validan con `npm run backend:conventions:check` desde la raíz. La guía completa está en `docs/architecture/backend-conventions.md`.

El contexto `identity.persistence` contiene la base de cuentas autenticadas, roles asignables, sesiones revocables y tokens de un solo uso. `AccountType` diferencia cuentas `customer`, `venue_business` y `admin` sin sustituir la autorización por roles. Los secretos de sesión, verificación y recuperación solo se persisten como hashes SHA-256. El modelo y sus invariantes están documentados en `docs/architecture/identity-persistence.md`.

Las contraseñas pasan exclusivamente por `PasswordHashingService`: BCrypt 2b con sal aleatoria,
coste 12–16, límite de 72 bytes UTF-8, comparación fail-closed con hash dummy y detección de rehash
para credenciales antiguas. Registro, login y recuperación consumen esta frontera.

`POST /api/auth/login` crea una sesión revocable de local y entrega su secreto solo en cookie
host-only `HttpOnly`, `SameSite=Strict` y `Secure` según entorno. PostgreSQL conserva SHA-256.
`POST /api/auth/logout` revoca por hash de forma idempotente y siempre elimina la cookie. El contrato
completo está en `docs/architecture/authentication-sessions.md`.

Spring Security valida esa cookie contra PostgreSQL en namespaces privados, construye un principal
sin secreto y exige `venue_owner` en `/api/venue/me/**` o `admin` en `/api/admin/**`. Sesiones
inválidas devuelven `401`; permisos insuficientes, `403`. No usa sesión HTTP, Basic ni formulario.
El contrato completo está en `docs/architecture/role-authorization.md`.

El registro crea además un desafío de verificación de email de 24 horas.
`POST /api/auth/email/verify` lo consume una sola vez y activa la cuenta pendiente;
`POST /api/auth/email/verification/request` rota el desafío con respuesta genérica. PostgreSQL
conserva solo SHA-256 y el trabajo de entrega se publica en RabbitMQ después del commit. El contrato
completo está en `docs/architecture/email-verification.md`.

`POST /api/auth/password/forgot` crea o rota un desafío sin enumerar cuentas.
`POST /api/auth/password/reset` consume el secreto, reemplaza el hash BCrypt y revoca todas las
sesiones anteriores. El trabajo de entrega se publica en una cola RabbitMQ independiente después
del commit. El contrato completo está en `docs/architecture/password-recovery.md`.

El contexto `businessverification.persistence` contiene identidades fiscales, historial mínimo de comprobaciones y metadatos de documentos privados. No persiste respuestas remotas completas, binarios ni URLs públicas. Su contrato de privacidad, auditoría e integridad está documentado en `docs/architecture/business-verification-persistence.md`.

El endpoint público `POST /api/auth/venues/register` crea atómicamente una cuenta
`venue_business`, su identidad empresarial no verificada y el rol `venue_owner`. La contraseña se
persiste únicamente como BCrypt, el cliente no controla privilegios y el alta no crea todavía el
perfil público del local. El contrato y sus límites están documentados en
`docs/architecture/venue-registration.md`.

El contexto `businessverification.validation` convierte identificadores fiscales a una clave
canónica y aplica estrategias locales por país antes de consultar unicidad. España incluye formato y
carácter de control para NIF, NIE y NIF de entidades, incluido el prefijo NIF-IVA `ES`. Los países
sin estrategia se normalizan sin declararse validados. El contrato se documenta en
`docs/architecture/business-tax-identifiers.md`.

El contexto `businessverification.remote` define el puerto de adaptadores por país/proveedor y un
gateway con selección determinista, timeouts, watchdog, reintentos acotados e idempotencia estable.
`businessverification.service` carga la identidad desde PostgreSQL y guarda evidencia mínima sin
mantener una transacción abierta durante la red ni cambiar todavía el estado de la cuenta. La
arquitectura se documenta en `docs/architecture/remote-business-verification.md`.

La política inicial España/UE consulta VIES por SOAP cuando el identificador es VAT ID. Para España
solo se usa VIES si el valor aportado incluía el prefijo `ES`; los NIF nacionales se resuelven de
forma inconclusa y sin red para su posterior revisión censal AEAT. VIES solo recibe país y número,
las respuestas XML tienen tamaño limitado y no se persisten, y las coincidencias de razón social y
dirección se calculan en memoria.

`BusinessVerificationStateService` aplica la evidencia con transacciones breves y lock pesimista
por cuenta. Un request activo correlaciona inicio y resultado para impedir respuestas tardías. Solo
una confirmación oficial con razón social y, si se aportó, dirección coherentes produce `verified`;
inconclusión, error o discrepancia produce `pending_review`, e invalidez oficial produce `rejected`.
Las aprobaciones caducan de forma configurable, 365 días por defecto.

Una transición a `pending_review` genera atómicamente un
`BusinessVerificationDocumentRequest`: conserva el check origen, un motivo cerrado y los tipos de
respaldo admitidos. No contiene fichero ni URL. La misma evidencia no duplica requerimientos y una
revalidación cancela el requerimiento abierto antes de consultar de nuevo.

`BusinessVerificationDocumentUploadService` acepta contenido únicamente para una solicitud abierta
y un propietario con rol `venue_owner` o un `admin`. Valida límite, MIME y firma binaria, exige
resultado limpio de ClamAV, cifra con AES-256-GCM y guarda el sobre en almacenamiento S3-compatible
privado. PostgreSQL conserva solo la clave interna, SHA-256 y metadatos mínimos. El contrato
completo está en `docs/architecture/private-business-documents.md`; el endpoint autenticado se
conectará cuando se implemente el middleware de seguridad.

`VenuePublicationEligibilityService` concentra la barrera empresarial de publicación. Bloquea si
falta verificar el email, el tipo no es `venue_business`, no existe identificador normalizado o no
hay verificación remota vigente ni aprobación administrativa. Solo devuelve motivos cerrados y
mantiene un lock de cuenta para que la operación de publicación de la Fase 2 pueda ejecutarlo en su
misma transacción. El contrato está en `docs/architecture/venue-publication-eligibility.md`.

## Ejecución

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verificación

```bash
mvn test -Dspring.profiles.active=test
```

Las pruebas de integración requieren un motor Docker disponible y crean instancias efímeras de PostGIS, Redis y RabbitMQ. Para aplicar el formato Java:

```bash
mvn spotless:apply
```
