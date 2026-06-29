# Elegibilidad para publicar locales

## Alcance

La tarea 1.11 implementa la barrera backend de RB-012 que puede evaluarse antes de crear el modelo
`Venues`. No crea una tabla ni un endpoint de publicación: ambas piezas pertenecen a la Fase 2.
`VenuePublicationEligibilityService` es una dependencia obligatoria del futuro caso de uso 2.9,
que añadirá la comprobación de datos mínimos del perfil.

## Condiciones

Una cuenta puede superar la barrera únicamente cuando se cumplen simultáneamente:

- `Users.emailVerifiedAt` no es nulo;
- `Users.accountType` es exactamente `venue_business`;
- `BusinessAccounts.businessTaxIdentifierNormalized` existe y no está vacío;
- existe aprobación empresarial por una de estas vías:
  - estado `verified` y `businessVerificationExpiresAt` estrictamente posterior al instante de
    evaluación;
  - `manualReviewStatus = approved`, decisión cuyo actor y fecha exige PostgreSQL.

Una aprobación que caduca exactamente en el instante evaluado ya no es válida. Los estados
`unverified`, `pending_remote_check`, `pending_review`, `rejected` y `expired` bloquean si no existe
la aprobación manual alternativa.

## Contrato y privacidad

`VenuePublicationEligibility` expone únicamente `allowed` y un conjunto inmutable de motivos
cerrados:

- `EMAIL_NOT_VERIFIED`;
- `ACCOUNT_TYPE_NOT_VENUE_BUSINESS`;
- `TAX_IDENTIFIER_NOT_NORMALIZED`;
- `BUSINESS_VERIFICATION_NOT_APPROVED`.

No incluye email, NIF/VAT, razón social, proveedor ni referencias. Una cuenta inexistente y una
cuenta no elegible se rechazan mediante `VenuePublicationNotAllowedException`; la futura capa REST
debe traducirla a un error genérico i18n y no usarla para enumerar cuentas.

## Consistencia transaccional

El DAO carga cuenta y propietario mediante `join fetch` y aplica `PESSIMISTIC_READ`. Las
transiciones empresariales existentes usan `PESSIMISTIC_WRITE` sobre la misma cuenta, por lo que no
pueden cambiar aprobación o caducidad durante la decisión.

`requireEligible` abre una transacción si no existe otra. Cuando la Fase 2 implemente la publicación,
debe llamarlo desde la misma transacción que actualice `Venues`; así el lock permanece hasta el
commit. Verificar primero y publicar en otra transacción introduciría una carrera y no está
permitido.

La verificación de email solo puede evolucionar de pendiente a confirmada en su flujo normal, por lo
que una confirmación concurrente puede mantener un rechazo conservador, pero no autorizar una
publicación indebida. Suspensión de cuentas, roles del actor y datos mínimos del perfil se validarán
en sus tareas específicas y no sustituyen esta barrera.

## Verificación

Las pruebas unitarias cubren aprobación remota vigente, vía manual, todos los bloqueos y frontera de
caducidad. Las pruebas de integración ejecutan el servicio contra PostgreSQL/PostGIS real, validan
el lock, el rechazo genérico y ambas vías de aprobación.
