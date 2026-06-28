# Registro de cuentas de local

## Alcance

`POST /api/auth/venues/register` crea la identidad autenticable y empresarial mínima de un
propietario de local. La operación no crea todavía un perfil público de local: las tablas y el CRUD
de locales pertenecen a la Fase 2. Esta separación permite completar la verificación de email y de
empresa antes de habilitar publicación o reservas.

La operación crea, en una única transacción:

1. un registro en `"Users"` con tipo `venue_business`;
2. un registro en `"BusinessAccounts"` enlazado al usuario;
3. una asignación en `"UserRoles"` al rol seed `venue_owner`.

No se crean sesiones, tokens de verificación, comprobaciones remotas ni documentos. Esas
capacidades corresponden a tareas posteriores de la Fase 1.

## Contrato HTTP

Request:

```json
{
  "account": {
    "email": "local@example.com",
    "password": "correct-horse-battery-staple",
    "preferredLocale": "es"
  },
  "business": {
    "taxCountry": "ES",
    "legalName": "Empresa de Prueba SL",
    "taxIdentifier": "B12345678",
    "registeredAddress": "Calle Ejemplo 1"
  },
  "acceptsLegalTerms": true
}
```

`preferredLocale` solo admite `es` o `en`. `taxCountry` debe ser un código ISO alpha-2 sintáctico.
La contraseña admite entre 12 y 72 caracteres y, adicionalmente, no puede superar 72 bytes UTF-8
por el límite de entrada de BCrypt.

Response `201 Created`:

```json
{
  "userId": "uuid",
  "businessAccountId": "uuid",
  "accountType": "venue_business",
  "businessVerificationStatus": "unverified",
  "emailVerificationRequired": true,
  "canPublishVenue": false
}
```

Errores públicos:

- `400 {"error":"REGISTRATION_INVALID"}` para JSON mal formado, validación de campos o una
  contraseña que excede el límite seguro de BCrypt;
- `409 {"error":"REGISTRATION_CONFLICT"}` para email o identidad fiscal duplicados.

El error de conflicto no identifica el campo duplicado. Esta decisión evita convertir el endpoint
en un oráculo de enumeración de emails o empresas. La respuesta nunca incluye contraseña, hash,
rol interno ni detalles de base de datos.

## Flujo transaccional

El controlador valida el DTO y un conversor lo transforma en un comando interno. El servicio:

1. valida el tamaño UTF-8 de la contraseña;
2. normaliza el email mediante `strip`, minúsculas y locale neutro;
3. aplica la normalización fiscal provisional `strip` y mayúsculas;
4. comprueba conflictos conocidos sin revelar cuál se produjo;
5. genera un hash BCrypt con coste 12 y sal aleatoria;
6. persiste usuario, cuenta empresarial y rol propietario;
7. devuelve únicamente identificadores y estados no sensibles.

La transacción cubre las tres escrituras. Cada inserción usa `saveAndFlush` para detectar
restricciones dentro del límite transaccional. Una `DataIntegrityViolationException` causada por
una carrera contra los índices únicos se traduce al mismo conflicto genérico y provoca rollback.
La precomprobación mejora la respuesta habitual, pero los índices de PostgreSQL siguen siendo la
autoridad frente a concurrencia.

## Estados y permisos iniciales

Los valores sensibles a privilegios no proceden del cliente:

- `Users.accountType = venue_business`;
- `Users.status = pending_email_verification`;
- `BusinessAccounts.businessVerificationStatus = unverified`;
- rol efectivo `venue_owner`;
- `canPublishVenue = false`.

Tipo de cuenta y rol son dimensiones diferentes. El primero activa invariantes empresariales; el
segundo será consumido por la autorización. Ninguno de ellos implica que la cuenta pueda publicar.
La regla `RB-012` exige además email verificado, identificador normalizado, verificación empresarial
aprobada y perfil mínimo completo.

## Normalización y límites deliberados

La normalización fiscal de esta iteración solo recorta espacios exteriores y convierte a mayúsculas.
No elimina separadores ni valida formato o dígito de control porque esas reglas dependen del país y
se implementan en `1.5`. Por tanto, el contrato actual no debe presentarse como una validación fiscal.

El registro usa BCrypt desde esta tarea para cumplir la invariante de no persistir secretos en claro.
La tarea `1.12` permanece pendiente: debe cerrar el contrato de verificación de hashes, política
configurable, rehash por aumento de coste y ciclo completo de credenciales.

También quedan fuera:

- envío y consumo de tokens de verificación de email (`1.14`);
- adaptación remota y máquina de estados empresarial (`1.6` a `1.8`);
- rate limiting (`1.16`);
- autorización HTTP por rol (`1.17`);
- formulario frontend y textos localizados (`1.18` y `1.21`);
- creación y publicación del perfil de local (Fase 2).

## Verificación

Las pruebas de integración arrancan Spring Boot contra PostgreSQL/PostGIS real mediante
Testcontainers. Comprueban el contrato HTTP, el contenido persistido, el hash verificable, el rol,
los estados iniciales, los conflictos genéricos, el rollback y la ausencia de escrituras ante
payloads inválidos. Una prueba aislada confirma además que dos hashes del mismo secreto son
distintos y ambos válidos, demostrando el uso de sal.
