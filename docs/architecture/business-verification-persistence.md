# Persistencia de verificación empresarial

## Alcance

La migración `V4__create_business_verification_tables.sql` crea la base persistente para identidades empresariales, intentos de comprobación y documentos de respaldo. V5 añade identidad idempotente y telemetría mínima a las ejecuciones remotas. Las transiciones del estado empresarial, carga de ficheros y revisión administrativa siguen perteneciendo a tareas posteriores.

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

El estado inicial es `unverified`. El catálogo físico prepara `unverified`, `pending_remote_check`, `verified`, `pending_review`, `rejected` y `expired`; el enum y las transiciones autorizadas pertenecen a `1.8`.

Un estado `verified` exige `"businessVerifiedAt"`. Una decisión manual final exige revisor y fecha. Los revisores y propietarios no pueden eliminarse mientras una referencia empresarial auditada dependa de ellos.

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

Las relaciones se declaran sobre getters y usan carga lazy. `BusinessVerificationCheckDao` consulta
por `requestId` y por proveedor/referencia para resolver idempotencia y carreras. Las consultas
propias continúan usando `@Query`.

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

`DatabaseMigrationIntegrationTests` exige Flyway V5 y el arranque valida los mapeos mediante Hibernate.
