# Persistencia de verificación empresarial

## Alcance

La migración `V4__create_business_verification_tables.sql` crea la base persistente para identidades empresariales, intentos de comprobación y documentos de respaldo. V5 añade identidad idempotente y telemetría mínima a las ejecuciones remotas. V6 incorpora correlación de la operación activa y caducidad de aprobaciones. V7 separa el requerimiento documental del fichero que se cargará en `1.10`. La carga privada y revisión administrativa siguen perteneciendo a tareas posteriores.

El modelo aplica minimización desde el esquema: conserva datos fiscales necesarios, resultados estructurados y hashes de evidencia, pero no respuestas remotas completas ni binarios documentales.

## `"BusinessAccounts"`

Representa una identidad fiscal o registral gestionada por una cuenta autenticada.

Datos principales:

- propietario autenticado;
- país fiscal ISO alpha-2 en mayúsculas;
- razón social;
- identificador aportado;
- identificador normalizado;
- dirección opcional;
- resumen de verificación vigente;
- resumen de revisión manual;
- timestamps UTC.

La combinación `"taxCountry"` y `"businessTaxIdentifierNormalized"` es única. La normalización y el dígito de control se aplican mediante el módulo documentado en `business-tax-identifiers.md`.

El estado inicial es `unverified`. `BusinessVerificationStatus` implementa el catálogo cerrado `unverified`, `pending_remote_check`, `verified`, `pending_review`, `rejected` y `expired`.

V6 añade:

- `"activeVerificationRequestId"`: UUID obligatorio solo durante `pending_remote_check`;
- `"businessVerificationExpiresAt"`: fin de vigencia de una aprobación;
- restricción que exige inicio y fin de vigencia para `verified`;
- restricción que impide estado remoto pendiente sin request propietario;
- índice parcial por caducidad para cuentas verificadas.

Un estado `verified` exige una ventana positiva entre `"businessVerifiedAt"` y
`"businessVerificationExpiresAt"`. Las filas verificadas anteriores a V6 reciben una vigencia de
365 días desde su aprobación. `expired` conserva esa ventana como evidencia histórica, pero deja de
ser una aprobación vigente.

Una decisión manual final exige revisor y fecha. Los revisores y propietarios no pueden eliminarse mientras una referencia empresarial auditada dependa de ellos.

## `"BusinessVerificationChecks"`

Registra evidencia mínima de cada intento:

- cuenta empresarial;
- `requestId` UUID único de la operación lógica;
- proveedor y país;
- identificador comprobado;
- resultado técnico;
- coincidencia opcional de razón social y dirección;
- referencia remota;
- fecha;
- código de error y clave i18n controlada;
- hash SHA-256 opcional de la respuesta;
- número de invocaciones remotas;
- duración total del gateway en milisegundos.

No existe columna para cuerpo JSON, payload o respuesta remota completa. Los estados técnicos son `pending`, `verified`, `invalid`, `inconclusive` y `error`. Un error exige código y clave de mensaje; los demás resultados no pueden conservar metadatos de error.

`requestId` es único y evita volver a invocar al proveedor cuando la misma operación ya tiene
evidencia. La combinación proveedor/referencia remota también es única cuando existe. Los intentos
se limitan a cinco; un valor cero representa un error anterior a la red, como ausencia de adaptador.
La duración nunca puede ser negativa.

## `"BusinessVerificationDocuments"`

Guarda solo metadatos:

- cuenta empresarial;
- tipo documental;
- localizador privado;
- hash SHA-256 del binario;
- estado;
- usuario que carga;
- revisor, fecha y notas internas;
- timestamps UTC.

`"fileUrl"` mantiene el nombre del diseño histórico, pero su contrato es un object key o localizador interno. PostgreSQL rechaza valores que empiecen por `http://` o `https://`; una URL temporal de descarga deberá generarse después de autorizar cada petición y nunca persistirse.

Tipos iniciales:

- `census_registration_036_037`;
- `census_certificate`;
- `activity_or_opening_license`;
- `equivalent_administrative_document`;
- `other`.

Estados:

- `pending_review`;
- `accepted`;
- `rejected`;
- `needs_correction`.

Los estados finales exigen revisor y fecha. El mismo hash no puede registrarse dos veces para una cuenta. La eliminación de una cuenta con documentos queda restringida para obligar al futuro flujo de supresión a retirar primero los objetos privados.

## `"BusinessVerificationDocumentRequests"`

Representa una solicitud de respaldo, no un fichero. Se crea atómicamente cuando una evidencia
técnica deja la cuenta en `pending_review`.

Datos mínimos:

- cuenta empresarial;
- check que originó el requerimiento;
- motivo controlado;
- array cerrado de tipos documentales admitidos;
- estado `open`, `fulfilled` o `cancelled`;
- instante de solicitud y resolución;
- timestamps UTC.

Motivos: `no_automated_channel`, `provider_unavailable`, `insufficient_provider_data`,
`legal_name_unconfirmed` y `address_unconfirmed`.

Invariantes:

- un check origina como máximo una solicitud;
- una cuenta tiene como máximo una solicitud abierta;
- el array contiene entre uno y cinco tipos del catálogo conocido;
- una solicitud abierta no tiene fecha de resolución;
- una solicitud satisfecha o cancelada exige fecha de resolución;
- cuenta y check no pueden borrarse mientras exista el requerimiento.

España recibe alta 036/037, certificado censal, licencia de actividad/apertura, documento
administrativo equivalente y `other`. La licencia es evidencia complementaria; su presencia no
implica aprobación. Otros países reciben documento administrativo equivalente y `other` hasta
disponer de una política nacional.

V7 no almacena nombres de fichero, binarios, URLs, notas libres ni datos fiscales adicionales. V8
vincula el fichero cifrado con la solicitud, registra MIME, tamaño, análisis limpio e ID de clave y
garantiza una sola carga por solicitud. El pipeline se documenta en
`docs/architecture/private-business-documents.md`.

## Seguridad y privacidad

- No hay respuestas remotas completas.
- No hay binarios en PostgreSQL.
- No hay URLs públicas persistentes.
- Los hashes deben ser SHA-256 hexadecimales en minúsculas.
- Las entidades son internas y no deben exponerse por REST.
- Consultas y descargas futuras deben validar propietario o rol administrativo.
- El borrado requiere un flujo explícito que coordine metadatos, evidencias y almacenamiento privado.
- Las decisiones administrativas preservan actor y fecha.

## JPA y acceso a datos

El paquete `com.reserly.platform.businessverification.persistence` contiene:

- `BusinessAccountEntity` y `BusinessAccountDao`;
- `BusinessVerificationCheckEntity` y `BusinessVerificationCheckDao`;
- `BusinessVerificationDocumentEntity` y `BusinessVerificationDocumentDao`.
- `BusinessVerificationDocumentRequestEntity` y `BusinessVerificationDocumentRequestDao`.

Las relaciones se declaran sobre getters y usan carga lazy. `BusinessVerificationCheckDao` consulta
por `requestId` y por proveedor/referencia para resolver idempotencia y carreras. Las consultas
propias continúan usando `@Query`. La consulta de elegibilidad de publicación carga el propietario y
aplica lock pesimista para coordinarse con las transiciones de estado; su contrato se documenta en
`docs/architecture/venue-publication-eligibility.md`.

## Verificación

`BusinessVerificationPersistenceIntegrationTests` ejecuta PostgreSQL real y comprueba:

- tablas y repositorios;
- estado inicial seguro;
- unicidad fiscal por país;
- país ISO en mayúsculas;
- timestamp obligatorio al verificar;
- ausencia de columnas de respuesta remota completa;
- hash de respuesta válido;
- metadatos controlados de error;
- unicidad por request remoto;
- coherencia de revisión documental;
- rechazo de URLs públicas;
- restricción de borrado cuando existe evidencia.
- catálogo, cardinalidad e idempotencia de solicitudes documentales.

`DatabaseMigrationIntegrationTests` exige Flyway V8 y el arranque valida los mapeos mediante Hibernate.
