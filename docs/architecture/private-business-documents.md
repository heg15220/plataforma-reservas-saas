# Documentos empresariales privados

## Alcance

La tarea 1.10 implementa el pipeline interno de carga de alta censal 036/037, certificado censal,
licencia de actividad/apertura, documento administrativo equivalente u otro respaldo solicitado.
La tarea 1.19 lo conecta al propietario autenticado mediante una frontera REST y una pantalla
responsive, reutilizando el pipeline completo sin trasladar decisiones de seguridad al navegador.

## Frontera autenticada

`GET /api/venue/me/business-verification/document-request` devuelve la única solicitud abierta o
`204`. `POST /api/venue/me/business-verification/documents` acepta `documentRequestId`,
`documentType` y un único `file` multipart. Ambos endpoints heredan `venue_owner` del namespace.

`BusinessVerificationDocumentPortalService` resuelve `BusinessAccounts` por `ownerUserId`; el
cliente no envía `businessAccountId` ni `uploaderUserId`. La respuesta de consulta omite también el
check técnico de origen y cualquier identidad fiscal. La respuesta de carga contiene solo IDs
opacos, estado e instante, nunca hash, nombre original o localizador privado.

El límite servlet es 10 MiB por archivo y 11 MiB por request para admitir el overhead multipart.
Después del corte HTTP, el pipeline repite su límite y valida contenido real. Los errores esperados
se reducen a códigos `DOCUMENT_*` estables, sin propagar mensajes de ClamAV, S3 o PostgreSQL.

La pantalla `/panel/verificacion` consulta con la cookie HttpOnly, localiza motivos y tipos mediante
catálogos ES/EN, aplica una prevalidación de usabilidad y deja la validación autoritativa al backend.
El `FormData` es efímero; no usa almacenamiento web, no fija manualmente el boundary y no reintenta
automáticamente el POST.

## Flujo

1. Se normaliza el tipo contra `BusinessVerificationDocumentType`.
2. Se comprueba que la solicitud está abierta, pertenece a la cuenta en `pending_review`, admite el
   tipo y que el actor es su propietario con rol `venue_owner` o tiene rol `admin`.
3. El stream se lee una sola vez con límite `maxBytes + 1`, se cierra y se valida mediante magic
   bytes. Solo se admiten PDF, PNG y JPEG; no se conserva nombre ni extensión.
4. Se calcula SHA-256 sobre el contenido en claro y ClamAV lo analiza mediante `zINSTREAM`. Timeout,
   respuesta desconocida o indisponibilidad fallan de forma cerrada.
5. El contenido limpio se cifra con AES-256-GCM, nonce aleatorio de 96 bits y tag de 128 bits. El
   sobre binario es `RSY1 || nonce || ciphertext+tag`.
6. El sobre se almacena como `application/octet-stream` bajo una clave UUID del prefijo
   `business-verification/{accountId}/`. No se crea URL ni policy pública.
7. Una transacción nueva, con lock pesimista sobre la solicitud, repite autorización y estado para
   impedir TOCTOU; guarda metadatos y marca la solicitud `fulfilled`.
8. Si falla PostgreSQL después del `put`, se intenta borrar el objeto. La excepción de persistencia
   sigue siendo la principal y un fallo de compensación queda suprimido.

## Persistencia y privacidad

V8 añade a `"BusinessVerificationDocuments"` la solicitud satisfecha, MIME detectado, tamaño,
resultado e instante antivirus y el identificador de clave criptográfica. Una restricción exige
metadatos completos y resultado `clean`; un índice parcial impide satisfacer dos veces la misma
solicitud. Se mantienen la unicidad SHA-256 por cuenta y la prohibición de URLs públicas.

La base de datos no almacena binarios, claves criptográficas, amenazas detectadas, respuestas de
ClamAV, nombres originales ni URLs firmadas. `fileUrl` es el localizador interno del objeto.

## Configuración operativa

Local usa MinIO y ClamAV en Docker Compose. Staging y producción deben inyectar endpoint HTTPS,
bucket existente, credenciales restringidas, región y una clave AES de 32 bytes desde secretos.
Fuera de `local` y `test`, el arranque rechaza HTTP, creación automática de buckets y el
identificador de clave local.

La rotación se realiza cambiando `RESERLY_DOCUMENT_ENCRYPTION_KEY_ID` y
`RESERLY_DOCUMENT_ENCRYPTION_KEY_BASE64`; cada fila conserva el ID usado. El descifrado y la
rotación material quedan para el flujo administrativo de consulta documental.

`RESERLY_DOCUMENT_MAX_BYTES` configura tanto el corte multipart de fichero como el límite de
contenido real. `RESERLY_DOCUMENT_REQUEST_MAX_BYTES` debe ser ligeramente superior para incluir
campos y boundary sin ampliar el tamaño permitido del documento.

## Verificación

Las pruebas cubren límite y firmas MIME, SHA-256, sobre cifrado aleatorio, malware antes de
almacenar, metadatos internos, borrado compensatorio, derivación de ownership, proyección REST,
clasificación de errores, multipart cliente, estados de pantalla y autorización real del namespace.
La prueba de migraciones ejecuta Flyway V1–V8 sobre PostgreSQL/PostGIS real y Hibernate valida el
mapeo.
