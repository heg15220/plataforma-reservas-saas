# Persistencia de verificación empresarial

## Alcance

La migración `V4__create_business_verification_tables.sql` crea la base persistente para identidades empresariales, intentos de comprobación y documentos de respaldo. No implementa todavía registro, normalización por país, adaptadores remotos, transiciones de estado, carga de ficheros ni revisión administrativa.

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

La combinación `"taxCountry"` y `"businessTaxIdentifierNormalized"` es única. La normalización efectiva y el dígito de control se implementarán en `1.5`; la restricción ya impide duplicados una vez obtenido el valor canónico.

El estado inicial es `unverified`. El catálogo físico prepara `unverified`, `pending_remote_check`, `verified`, `pending_review`, `rejected` y `expired`; el enum y las transiciones autorizadas pertenecen a `1.8`.

Un estado `verified` exige `"businessVerifiedAt"`. Una decisión manual final exige revisor y fecha. Los revisores y propietarios no pueden eliminarse mientras una referencia empresarial auditada dependa de ellos.

## `"BusinessVerificationChecks"`

Registra evidencia mínima de cada intento:

- cuenta empresarial;
- proveedor y país;
- identificador comprobado;
- resultado técnico;
- coincidencia opcional de razón social y dirección;
- referencia remota;
- fecha;
- código de error y clave i18n controlada;
- hash SHA-256 opcional de la respuesta.

No existe columna para cuerpo JSON, payload o respuesta remota completa. Los estados técnicos son `pending`, `verified`, `invalid`, `inconclusive` y `error`. Un error exige código y clave de mensaje; los demás resultados no pueden conservar metadatos de error.

La combinación proveedor/referencia remota es única cuando existe. Esto prepara idempotencia y evita duplicar un mismo resultado externo.

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

Las relaciones se declaran sobre getters y usan carga lazy. Los DAOs no incorporan aún consultas de dominio. Las futuras consultas propias deberán usar `@Query`, acotar por propietario/cuenta y hacer explícitos estados, orden y locks.

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
- coherencia de revisión documental;
- rechazo de URLs públicas;
- restricción de borrado cuando existe evidencia.

`DatabaseMigrationIntegrationTests` exige Flyway V4 y el arranque valida los tres mapeos nuevos mediante Hibernate.
