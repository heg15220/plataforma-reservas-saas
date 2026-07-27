# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-07-27
- Tareas completadas en `tasks.md`: `0.1` a `0.15`, `1.1` a `1.22`, `2.1` a `2.17`, `3.1` a
  `3.14`, `4.1` a `4.14`, `5.1` a `5.12`, `6.1` a `6.12`, `7.1` a `7.16`, `8.1` a `8.14`,
  `9.1` a `9.10` y `10.1` a `10.15`.
- Siguiente tarea pendiente recomendada: `10.16. Crear traducciones ES/EN para incidencias,
  penalizaciones, advertencias y mensajes de restricción`.
- Observación: el escalado, el bloqueo de confirmación y la auditoría crítica disponen ya de
  cobertura específica de fronteras, privacidad, orden y ausencia de efectos laterales.

## Conversación 112 - Cobertura de escalado, bloqueo y auditoría crítica

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.13`, `10.14` y `10.15` en
    `phase/10-assistance-incidents-penalties`.
  - La cobertura del servicio de penalizaciones comprueba los contadores 1, 2, 3, 4 y 5 contra
    periodos de 7, 14, 21, 60 y 60 días, respectivamente.
  - Se añadieron casos para expirar una penalización activa exactamente en su frontera, reiniciar
    el contador desde el último tramo completo de 60 días e ignorar fronteras anteriores a la
    ventana operativa de 12 meses.
  - La confirmación verifica que un email penalizado se normaliza y se bloquea después de acreditar
    el hold, pero antes de bloquear la franja, validar formularios, persistir o publicar correos.
  - Los tests de auditoría exigen snapshots con claves cerradas, metadatos del actor y orden de
    escritura para reporte y cancelación, además de confirmar la frontera transaccional.
- Archivos modificados:
  - `PenaltyServiceTests`.
  - `ReservationConfirmationServiceTests`.
  - `NoShowReportServiceTests`.
  - `VenueReservationCancellationServiceTests`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-015`, `RF-020`, `RF-021` y `RF-023`.
  - `RB-001`, `RB-007` y `RB-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.13`, `10.14` y `10.15`.
- Siguiente tarea pendiente recomendada:
  - `10.16. Crear traducciones ES/EN para incidencias, penalizaciones, advertencias y mensajes de
    restricción`.
- Decisiones o aclaraciones relevantes:
  - El fin de una restricción es exclusivo: `endsAt == now` no bloquea una confirmación nueva.
  - Solo un tramo completado con contador operativo 4 o superior actúa como frontera de reinicio;
    una frontera anterior a la conservación de 12 meses se ignora.
  - Los contadores cero o fuera del rango entero y las incidencias no persistidas/no aplicables
    fallan antes de crear una penalización.
  - Un email o token inválido no consulta penalizaciones y, por tanto, no convierte el endpoint de
    confirmación en un oráculo de restricciones.
  - La auditoría del reporte se registra tras incidencia y reserva, pero antes de aplicar la
    penalización. La auditoría de cancelación se registra tras guardar la reserva y antes de
    publicar el evento de correo.
  - Evidencia focalizada: 39 tests correctos, 0 fallos, 0 errores y 0 omitidos, repartidos en
    bloques de 18, 13 y 8. Spotless se aplicó solo a cuatro tests Java.
  - La primera invocación Maven conjunta superó el límite durante compilación incremental. Se
    concedió una ventana adicional de diez segundos, se detuvo el proceso y se reutilizaron las
    clases compiladas con `surefire:test`; no se ejecutó la suite global ni módulos frontend.

## Conversación 111 - Cancelación preventiva y operación responsive de incidencias

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.10`, `10.11` y `10.12` en
    `phase/10-assistance-incidents-penalties`.
  - Se añadió la cancelación preventiva de una reserva futura confirmada por su propio local,
    exigiendo un motivo de hasta 500 caracteres, transición a `cancelled_by_venue`, liberación
    inmediata de capacidad, revocación del token seguro y auditoría minimizada.
  - La notificación al cliente se desacopló mediante un evento publicado después del commit y una
    cola RabbitMQ persistente, con reintentos, deduplicación y plantilla localizada.
  - El panel incorpora `/panel/incidencias` para editar las reglas de reserva y consultar el
    historial privado a partir de una reserva propia, sin pedir ni mostrar el email del cliente.
  - El detalle de reserva incorpora controles táctiles para marcar asistencia, preparar y confirmar
    un reporte de no asistencia, cancelar con motivo y abrir el historial relacionado.
  - La navegación móvil se concentró en cuatro destinos estables: inicio, reservas, calendario y
    más; este último da acceso directo a incidencias.
- Archivos modificados:
  - Migración `V29__store_reservation_customer_locale.sql` y persistencia de la locale del cliente.
  - Contrato, controlador, servicio, errores, auditoría, evento y mensajería de cancelación.
  - Tests focalizados de servicio, relay, consumidor y confirmación.
  - Cliente API web de reservas, nuevo cliente/dashboard de incidencias y acciones del detalle.
  - Página `/panel/incidencias`, shell responsive, catálogos `es`/`en` y tests web asociados.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del commit el cambio previo del usuario en `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-019`, `RF-020`, `RF-022` y `RF-023`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.10`, `10.11` y `10.12`.
- Siguiente tarea pendiente recomendada:
  - `10.13. Crear tests de escalado de penalizaciones`.
- Decisiones o aclaraciones relevantes:
  - La cancelación bloquea la reserva propia y solo admite reservas `confirmed` cuyo inicio todavía
    no ha llegado según el reloj de negocio; los estados terminales devuelven conflicto.
  - La locale se guarda al confirmar la reserva. Los registros históricos sin locale usan primero
    la locale por defecto del local y después `en`.
  - La liberación de aforo no necesita una escritura adicional: las consultas de ocupación ya
    excluyen `cancelled_by_venue`.
  - La vista de historial exige `reservationId` y reutiliza el contrato privado y minimizado de
    `10.9`; nunca transporta el email en URL, payload o respuesta.
  - Evidencia API focalizada: 14 tests, 0 fallos, 0 errores y 0 omitidos. Evidencia web focalizada:
    4 archivos y 12 tests, todos correctos. El typecheck limitado a siete entradas y sus
    dependencias terminó sin errores.
  - Los catálogos `es`/`en` son JSON válido y conservan 887 claves equivalentes. El validador global
    de texto sigue fallando por cuatro literales preexistentes fuera de estas tareas
    (`public-reservation-form`, `team-availability-manager` y `venue-reservations-dashboard`).
  - Checkstyle no dejó incidencias en el Java nuevo; su ejecución termina por 24 líneas largas
    preexistentes de las plantillas `.properties` en español e inglés.
  - No se ejecutaron suites globales, Docker, Testcontainers, PostgreSQL/Flyway reales ni pruebas
    end-to-end para respetar la validación acotada solicitada.

## Conversación 110 - Escalado, bloqueo de confirmación e historial profesional

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.7`, `10.8` y `10.9` en
    `phase/10-assistance-incidents-penalties`.
  - El reporte auditado calcula el número operativo de no asistencias, crea o recalcula una
    penalización global de 7, 14, 21 o 60 días y reinicia el contador después de completar un tramo
    de 60 días.
  - La confirmación autentica primero el hold y después bloquea la identidad normalizada para
    impedir que una penalización concurrente se omita.
  - Se añadió `GET /api/venue/me/incident-history?reservationId=...` con paginación acotada; la
    reserva propia acredita la consulta y el email nunca entra ni sale por HTTP.
  - Tanto el endpoint nuevo como el historial incluido en el detalle excluyen incidencias
    desestimadas o anteriores a la ventana operativa de 12 meses.
- Archivos modificados:
  - `PenaltyEntity`, `PenaltyDao`, `NoShowIncidentDao` y servicios de cálculo/restricción.
  - `NoShowReportServiceImpl` y su test para incorporar la penalización a la transacción existente.
  - `ReservationConfirmationServiceImpl`, handler, respuesta de restricción y tests dependientes.
  - Servicio, controlador, conversor, DTOs y errores del historial profesional.
  - `VenueReservationServiceImpl` y tests/consultas del detalle para aplicar conservación.
  - Tests focalizados de política, servicio, DAO, conversor, confirmación y detalle.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-015`, `RF-018`, `RF-020` y `RF-021`.
  - `RB-001` y `RB-007`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.7`, `10.8` y `10.9`.
- Siguiente tarea pendiente recomendada:
  - `10.10. Implementar cancelación preventiva por local con motivo`.
- Decisiones o aclaraciones relevantes:
  - La coordinación por email usa `pg_advisory_xact_lock(hashtextextended(...))`; el hash solo
    selecciona el lock y las consultas de negocio siguen comparando el email completo.
  - Una penalización activa se recalcula desde el instante del último reporte. Una fila activa cuyo
    fin ya pasó se marca `expired` antes de crear el siguiente tramo.
  - El contador incluye únicamente incidencias `no_show` en estado `reported` o `confirmed` dentro
    de 12 meses y desde el último bloqueo completado con contador 4 o superior.
  - El error público es `409 ACTIVE_BOOKING_RESTRICTION` y devuelve solo `restrictedUntil`; contador,
    incidencias, locales, actores y motivo interno no se exponen.
  - El historial independiente requiere `reservationId` propio. Devuelve únicamente tipo, fecha y
    estado, con páginas de 1 a 50 elementos y orden descendente estable.
  - Evidencia focalizada final: 39 tests, 0 fallos, 0 errores y 0 omitidos, divididos en bloques de
    16 y 23 para mantenerse bajo límites cortos. Compilaron 613 fuentes principales.
  - Spotless se aplicó y comprobó con una lista explícita de 33 Java afectados. Una primera
    resolución incorrecta del filtro reformateó 70 archivos adicionales; se identificaron y
    revirtieron inmediatamente porque al inicio no tenían cambios del usuario.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers, PostgreSQL real, Flyway real
    ni pruebas visuales.

## Conversación 109 - Asistencia automática y reporte auditado de no asistencia

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron `10.4`, `10.5` y `10.6` en
    `phase/10-assistance-incidents-penalties`.
  - Se añadió un job cada cinco minutos que marca como asistidas las reservas confirmadas,
    finalizadas y sin decisión manual, usando el periodo de `VenueBookingRules`.
  - Se añadió `POST /api/venue/me/reservations/{reservationId}/report-no-show`, que exige
    `confirmed=true`, propiedad acreditada y estado previo `no_show`.
  - El reporte crea `NoShowIncidents`, cambia la reserva a `reported` y añade una entrada
    minimizada en la nueva tabla `AuditLogs` dentro de la misma transacción.
  - Se añadió un `Clock` de negocio único con zona IANA configurable y default `Europe/Madrid`.
- Archivos modificados:
  - `ReservationDao`, `ReservationEntity`, `AttendanceService` y su test.
  - `DefaultAttendanceJob` y tests de job/consulta.
  - DTOs, controlador, servicio, errores y tests del reporte de no asistencia.
  - Migración `V28__create_audit_logs.sql`, entidad, DAO, servicio y tests de auditoría.
  - `BusinessClockConfiguration`, test y ejemplos de entorno local, staging y producción.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-019`, `RF-020` y preparación de `RF-021`.
  - `RB-006`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-005`, `RNF-006`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.4`, `10.5` y `10.6`.
- Siguiente tarea pendiente recomendada:
  - `10.7. Implementar cálculo de penalización 7, 14, 21 y 60 días`.
- Decisiones o aclaraciones relevantes:
  - Se corrige la semántica interna documentada en 10.3: `pending` conserva el estado físico
    `confirmed`, pero registra `attendanceMarkedAt` en vez de limpiarlo. Así queda como decisión
    manual y el job no puede sobrescribirla.
  - El job usa una única sentencia PostgreSQL condicional, `attendanceMarkedAt IS NULL` y el
    periodo `autoMarkAttendedAfterMinutes`; 120 minutos es el fallback para reglas ausentes.
  - La fecha y hora snapshot se interpretan en la zona de
    `RESERLY_BUSINESS_CLOCK_ZONE_ID`; el arranque falla si la zona no es válida.
  - El reporte solo acepta una reserva `no_show`; repetidos, estados `reported`, cancelados o
    asistidos producen conflicto sin escrituras.
  - La confirmación del operador no equivale a revisión administrativa: la incidencia se crea con
    estado `reported`. El cálculo de penalización queda deliberadamente en 10.7.
  - La auditoría guarda actor, rol, tipo/ID de entidad, acción, estados antes/después, IP directa y
    user-agent acotado. No guarda email, nombre, notas ni secretos.
  - Evidencia focalizada: 18 tests, 0 fallos, 0 errores y 0 omitidos; compilación correcta de 598
    fuentes principales y 135 fuentes de test; Spotless focalizado correcto.
  - No se ejecutaron suite global, frontend, Docker, Testcontainers, PostgreSQL real, Flyway real
    ni pruebas visuales.

## Conversación 108 - Esquema de penalizaciones, reglas de cancelación y asistencia manual

- Fecha: 2026-07-27.
- Resumen de la conversación:
  - Se completaron las tareas `10.1`, `10.2` y `10.3` en
    `phase/10-assistance-incidents-penalties`.
  - Se completó el esquema iniciado por V26 con `Penalties` y `VenueBookingRules`, incluyendo
    restricciones, índices operativos y migración de la antelación histórica de cada local.
  - Se añadieron `GET` y `PUT /api/venue/me/booking-rules`; el propietario procede exclusivamente
    de la sesión, la escritura se serializa y la política se aplica también al enlace público de
    cancelación.
  - Se añadió `POST /api/venue/me/reservations/{reservationId}/attendance` para marcar
    `attended`, `no_show` o `pending` sobre reservas propias finalizadas. La opción `pending`
    conserva `confirmed`; desde 10.4 registra una marca temporal para excluir la decisión manual
    del job automático.
- Archivos modificados:
  - Migración `V27__create_penalties_and_venue_booking_rules.sql`.
  - Entidades y DAOs de `Penalties` y `VenueBookingRules`; servicios, DTOs, conversor,
    controladores y errores del módulo `incidents`.
  - `ReservationEntity`, `ReservationDao`, política y servicio de gestión pública de reservas.
  - Tests focalizados de esquema, reglas, asistencia, cancelación pública y política.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó fuera del alcance el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-017`, `RF-019`, `RF-022`.
  - Preparación estructural de `RF-020` y `RF-021`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-007`, `RNF-008` y `RNF-011`.
- Tareas impactadas y completadas: `10.1`, `10.2` y `10.3`.
- Siguiente tarea pendiente recomendada:
  - `10.4. Implementar job para marcar asistida por defecto tras periodo configurado`.
- Decisiones o aclaraciones relevantes:
  - V26 sigue siendo la migración que creó `NoShowIncidents`; V27 completa 10.1 sin recrearla.
  - `VenueBookingRules` se siembra desde `Venues.cancellationNoticeMinutes`. Para locales creados
    después de V27, el servicio devuelve defaults compatibles y persiste la fila en el primer PUT.
  - El campo heredado de `Venues` se sincroniza al modificar reglas para que plantillas y contratos
    anteriores no observen una antelación distinta durante la transición.
  - Marcar `no_show` no crea incidencia ni penalización. El reporte confirmado y auditado se
    reserva para 10.5–10.7, evitando consecuencias automáticas por una marca operativa reversible.
  - Solo `confirmed`, `attended` y `no_show` admiten cambios manuales. Estados cancelados,
    expirados, holds y `reported` fallan sin mutación.
  - La finalización se calcula con el `Clock` y su zona de negocio; la igualdad con la hora de fin
    permite marcar asistencia.
  - Evidencia focalizada: 20 tests correctos, 0 fallos, 0 errores y 0 omitidos; compilación de 581
    fuentes principales y compilación de tests correctas; Spotless focalizado correcto.
  - Dos invocaciones Maven excedieron el límite externo de 60 segundos después de la compilación o
    después de escribir los primeros resultados. Los procesos se detuvieron y no se ejecutaron
    suite global, frontend, Docker, PostgreSQL real, Flyway real ni pruebas visuales.

## Conversación 101 - Entrega reintentable, errores persistidos y consulta por token

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron 8.7, 8.8 y 8.9 en `phase/8-emails-management`.
  - El consumidor transforma cada confirmación en emails de cliente y local, con idempotencia separada, tres intentos con backoff de 1/2 segundos y rechazo final a la DLQ existente.
  - Se creó `EmailDeliveries` para resultado mínimo, intentos y código técnico, sin destinatario, cuerpo ni token.
  - Se añadió `GET /api/public/reservations/manage/{token}`, que valida formato, consulta SHA-256 y devuelve solo la reserva asociada.
- Archivos modificados:
  - Migración V24, entidad/DAO de entrega, consumidor y pruebas de mensajería.
  - DAO de reservas, servicio/controlador/DTO/handler de gestión y pruebas focalizadas.
  - `tasks.md`, `conversation-tracking.md`, `technical-implementation.md` y documentación arquitectónica.
  - Se preservó fuera del commit `apps/web/next-env.d.ts`.
- Requisitos impactados: `RF-016`, `RF-017`, `RNF-002`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas: `8.7`, `8.8` y `8.9`.
- Siguiente tarea pendiente recomendada: `8.10. Implementar cancelación por token seguro`.
- Decisiones:
  - Cada destinatario se deduplica por `eventId + recipientKind`; un local fallido no reenvía al cliente ya entregado.
  - El backoff es deliberadamente finito y los payloads inválidos no se reintentan.
  - Los errores persistidos usan códigos cerrados y nunca almacenan PII, contenido o secretos.
  - Token inválido, ausente, expirado o revocado produce el mismo 404 y no consulta por token en claro.
  - Evidencia focalizada final: 8 tests correctos, 0 fallos, 0 errores y 0 omitidos, incluido el agotamiento de tres intentos. No se ejecutaron suites globales, frontend ni servicios reales.
## Conversación 100 - Avisos al local y cancelaciones ES/EN

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron 8.4, 8.5 y 8.6 en `phase/8-emails-management`.
  - Se añadieron contratos tipados y plantillas ES/EN para nueva reserva al local, cancelación por usuario al local y cancelación por local al usuario.
  - Cada familia ofrece asunto, texto y HTML, localiza agenda, escapa datos y usa fallback inglés.
  - La validación se limitó al paquete de notificaciones y dos clases de prueba.
- Archivos modificados:
  - `EmailTemplateType.java`, `LocalizedEmailTemplateService.java`, `LocalizedEmailTemplateServiceImpl.java` y tres records de datos.
  - Catálogos ES/EN, `ReservationLifecycleEmailTemplateTests.java`, documentación arquitectónica y los tres documentos de especificación.
  - Se preservó fuera del commit el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados: `RF-016`, `RF-023`, `RF-031`, `RNF-002`, `RNF-006` y `RNF-009`.
- Tareas impactadas y completadas: `8.4`, `8.5` y `8.6`.
- Siguiente tarea pendiente recomendada: `8.7. Implementar cola de envío con reintentos`.
- Decisiones o aclaraciones relevantes:
  - El local recibe datos operativos, nunca el token de gestión.
  - La cancelación del usuario no inventa motivo; la del local exige uno no vacío que el caso de uso deberá persistir y auditar. La redacción no atribuye no-show ni penalización al usuario.
  - Spotless comprobó 18 archivos; 7 tests focalizados pasaron sin fallos.
  - Checkstyle global encontró 44 incidencias preexistentes ajenas y no se usó como evidencia. No se ejecutaron suite completa, frontend, RabbitMQ, Mailpit, Brevo ni build global.

## Conversación 99 - Proveedor transaccional y primeras plantillas ES/EN

- Fecha: 2026-07-22.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 8.1, 8.2 y 8.3 en la rama `phase/8-emails-management`.
  - Se configuró un puerto de proveedor transaccional con adaptador SMTP: Mailpit local y Brevo
    cifrado en staging/producción, con credenciales externas, timeouts limitados y mensajes
    multipart/alternative UTF-8.
  - Se añadieron catálogos versionados ES/EN para verificación de email, recuperación de contraseña
    y confirmación de reserva, con fallback inglés, formatos localizados y escape estricto de HTML.
  - La validación se limitó a tres clases nuevas del módulo `notifications`.
- Archivos modificados:
  - `apps/api/pom.xml`, `apps/api/src/main/resources/application.yaml` y los tres ejemplos
    `.env.*.example`.
  - `infrastructure/compose.yaml` y `docs/architecture/transactional-email.md`.
  - El paquete `apps/api/src/main/java/com/reserly/platform/notifications`.
  - `apps/api/src/main/resources/email-templates/es.properties` y `en.properties`.
  - Tres clases de test bajo `apps/api/src/test/java/com/reserly/platform/notifications`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - Se preservó sin incluir en el commit el cambio previo de `apps/web/next-env.d.ts`.
- Requisitos impactados:
  - `RF-007`, `RF-008`, `RF-016`, `RF-031`, `RNF-002`, `RNF-005`, `RNF-006`,
    `RNF-009` y `RNF-012`.
- Tareas impactadas y completadas:
  - `8.1. Configurar proveedor de email transaccional`.
  - `8.2. Crear plantillas ES/EN de verificación de email y recuperación de contraseña`.
  - `8.3. Crear plantillas ES/EN de confirmación para usuario`.
- Siguiente tarea pendiente recomendada:
  - `8.7. Implementar cola de envío con reintentos`.
- Decisiones o aclaraciones relevantes:
  - El proveedor conserva una frontera sustituible; Brevo se usa por SMTP autenticado sobre
    SSL/TLS 465 y Mailpit solo escucha en loopback local.
  - El adaptador hace un intento y no implementa reintentos internos. Consumidores, idempotencia y
    reintentos persistentes permanecen en 8.7; errores persistidos en 8.8.
  - Las plantillas se mantienen en el repositorio, ofrecen texto y HTML y fallan si queda un
    marcador sin resolver. Todos los datos dinámicos HTML se escapan.
  - Evidencia final: 7 tests correctos, 0 fallos, 0 errores y 0 omitidos mediante
    `mvn -f apps/api/pom.xml "-Dtest=LocalizedEmailTemplateServiceTests,SmtpTransactionalEmailProviderTests,TransactionalEmailPropertiesTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`.
  - No se ejecutaron suite global, frontend, Testcontainers, RabbitMQ, Mailpit real, Brevo real,
    build global ni validaciones visuales.

## Conversación 98 - Tests de última plaza y hold expirado

- Fecha: 2026-07-21.
- Resumen de la conversación:
  - Se completaron conjuntamente `7.15` y `7.16`.
  - Se añadió cobertura al servicio de holds para proteger la última plaza: el primer competidor
    persiste un hold de capacidad 1 y el segundo se rechaza tras recomputar ocupación bajo el lock de
    franja.
  - Se añadió cobertura explícita al servicio de confirmación para un hold ya vencido, sin bloqueo de
    franja, consulta de capacidad, validación de formulario, guardado ni evento de email.
  - La validación se limitó a las dos clases de servicio modificadas.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/reservations/service/ReservationHoldServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/reservations/service/ReservationConfirmationServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014`, `RF-015`, `RNF-003`, `RB-003`, `RB-004` y `RB-005`.
- Tareas impactadas y completadas:
  - `7.15. Crear tests de concurrencia para última plaza`.
  - `7.16. Crear tests de confirmación de hold expirado`.
- Siguiente tarea pendiente recomendada:
  - `8.1. Configurar proveedor de email transaccional`.
- Decisiones o aclaraciones relevantes:
  - La prueba de última plaza es unitaria y determinista: modela la serialización que proporciona
    `PESSIMISTIC_WRITE` y protege la recomputación de capacidad que evita sobreventa.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=ReservationHoldServiceTests,ReservationConfirmationServiceTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`: 12 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - No se ejecutaron suite completa, frontend, build global, tests de integración ni validaciones
    transversales.

## Conversación 87 - Disponibilidad por recurso y cualquier profesional disponible

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se completaron conjuntamente `5.7` y `5.8` como las dos primeras tareas pendientes.
  - El cálculo público cruza ahora cada franja con su servicio activo, asociaciones compatibles,
    estado y visibilidad del recurso y cobertura completa del horario semanal.
  - Una franja con servicio y recursos asociados queda no reservable si ninguno es elegible.
  - Se añadió configuración por servicio para permitir o impedir `any_available` y se publican las
    opciones concretas disponibles por franja para preparar el selector de reserva.
  - La consulta de disponibilidad futura aplica las mismas reglas para evitar falsos estados de
    "próximamente disponible".
  - V20 añade `Services.allowsAnyAvailableResource` con `NOT NULL DEFAULT true`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V20__allow_any_available_resource_by_service.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/**`.
  - `apps/api/src/main/java/com/reserly/platform/resources/persistence/EmployeeResourceHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/availability/**`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-010`, `RF-026`, `RF-027` y `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-007` y `RNF-011`.
- Tareas impactadas y completadas:
  - `5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique`.
  - `5.8. Implementar opción "cualquier profesional disponible"`.
- Siguiente tarea pendiente recomendada:
  - `5.9. Implementar asignación automática simple por primera disponibilidad`.
- Decisiones o aclaraciones relevantes:
  - Un servicio sin asociaciones no exige recurso; una asociación no vacía activa el requisito.
  - Solo son elegibles recursos `active`, públicos y con horario que contenga toda la franja.
  - `allowsAnyAvailableResource=false` conserva las opciones concretas, pero oculta la delegación
    `any_available`.
  - La respuesta pública expone alias o nombre de pila, tipo y especialidad; no expone apellidos,
    notas internas, estados administrativos ni IDs de propietario/local.
  - La asignación efectiva queda para `5.9`; esta iteración calcula y publica candidatos.
  - Evidencia: suite focalizada 23/23; suite con migraciones 33/33; Flyway v20, Hibernate, Spotless y
    Checkstyle correctos.
## Conversación 86 - Horarios semanales de recursos y asociación servicio-recurso

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron `5.5` y `5.6` como las dos siguientes tareas pendientes.
  - Se implementó el modelo JPA y DAO de `EmployeeResourceHours` sobre la tabla ya creada en V19.
  - Se añadieron endpoints privados para consultar y reemplazar el horario semanal básico de un
    empleado, profesional o recurso bajo `/api/venue/me/team/{resourceId}/weekly-hours`.
  - Se implementó la asociación reemplazable entre servicios y recursos compatibles mediante la
    tabla `ServiceEmployeeResources`, con endpoint privado
    `PUT /api/venue/me/services/{serviceId}/resources`.
  - Se ajustó el validador de convenciones backend para reconocer anotaciones JPA multilínea entre
    una relación y su getter, como `@ManyToMany` seguido de `@JoinTable`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/resources/**`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/resources/**`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `scripts/validate-backend-conventions.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008`, `RF-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-007`, `RNF-011`.
- Tareas impactadas y completadas:
  - `5.5. Implementar horario semanal básico por empleado o recurso`.
  - `5.6. Implementar asociación entre servicios y empleados o recursos`.
- Siguiente tarea pendiente recomendada:
  - `5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique`.
- Decisiones o aclaraciones relevantes:
  - El horario semanal se reemplaza de forma completa para mantener una operación idempotente,
    ordenada y fácil de sincronizar desde el futuro panel.
  - Un día no disponible debe enviarse sin horas; un día disponible exige `startsAt < endsAt`.
  - La asociación servicio-recurso también se reemplaza de forma completa y solo acepta recursos no
    archivados del mismo propietario autenticado.
  - No se añade migración nueva porque V19 ya contenía `EmployeeResourceHours` y
    `ServiceEmployeeResources`.
  - Evidencia: Maven focalizado con migraciones, 29 tests, 0 fallos, Spotless y Checkstyle incluidos;
    convenciones backend, validación de español y `git diff --check` correctos.

## Conversación 85 - CRUD de equipo y estados MVP

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron `5.3` y `5.4` como las dos siguientes tareas pendientes.
  - Se implementó el módulo backend `resources` sobre la tabla `EmployeeResources`.
  - Se creó el CRUD privado de empleados, profesionales y recursos reservables bajo
    `/api/venue/me/team`.
  - Se implementaron los estados MVP `active`, `inactive`, `internal_only` y `archived`.
  - El estado `internal_only` y el archivado fuerzan `publicVisibility=false`; `archived` se trata
    como estado terminal que desaparece del listado y no se reabre desde este CRUD básico.
  - Se añadieron tests unitarios de servicio y controlador.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/resources/**`.
  - `apps/api/src/test/java/com/reserly/platform/resources/**`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007`, `RF-008`, `RF-010`.
  - `RNF-001`, `RNF-002`, `RNF-004`, `RNF-007`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `5.3. Implementar CRUD de empleados o recursos`.
  - `5.4. Implementar estados activo, inactivo, solo interno y archivado`.
- Siguiente tarea pendiente recomendada:
  - `5.5. Implementar horario semanal básico por empleado o recurso`.
- Decisiones o aclaraciones relevantes:
  - El contrato MVP usa `GET`, `POST` y `PATCH` en `/api/venue/me/team`, coherente con el diseño.
  - No se acepta `venueId`; el local se resuelve desde `AuthenticatedAccount.userId`.
  - El CRUD restringe la entrada a los cuatro estados MVP aunque la migración conserve valores
    futuros (`vacation`, `temporary_leave`) para iteraciones posteriores.
  - Evidencia: Maven focalizado con 23 tests, 0 fallos, Spotless y Checkstyle incluidos.

## Conversación 84 - Migración de recursos y CRUD básico de servicios

- Fecha: 2026-07-12.
- Resumen de la conversación:
  - Se continuó en la rama `phase/5-team-resources-MVP-services`.
  - Se completaron en paralelo funcional `5.1` y `5.2`.
  - Se creó la migración `V19__create_team_resource_and_service_tables.sql` con tablas físicas
    `Services`, `EmployeeResources`, `EmployeeResourceHours` y `ServiceEmployeeResources`.
  - Se conectaron `TimeSlots.serviceId`, `AvailabilityBlocks.serviceId` y
    `AvailabilityBlocks.employeeResourceId` con claves foráneas hacia el nuevo modelo.
  - Se implementó el CRUD privado básico de servicios: listado, creación y edición bajo
    `/api/venue/me/services`.
  - Se añadieron entidad JPA, DAO, DTOs, conversor, servicio transaccional, controlador REST,
    errores estables y tests enfocados.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V19__create_team_resource_and_service_tables.sql`.
  - `apps/api/src/main/java/com/reserly/platform/services/**`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/services/**`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-007`, `RF-008`, `RF-010`, `RF-031`.
  - `RNF-001`, `RNF-004`, `RNF-007`, `RNF-009`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `5.1. Crear migraciones de services, employee_resources, employee_resource_hours y service_employee_resources`.
  - `5.2. Implementar CRUD de servicios básicos`.
- Siguiente tarea pendiente recomendada:
  - `5.3. Implementar CRUD de empleados o recursos`.
- Decisiones o aclaraciones relevantes:
  - Los nombres físicos mantienen el patrón existente: tablas UpperCamelCase y columnas
    lowerCamelCase entrecomilladas.
  - El CRUD no acepta `venueId`; el local se resuelve desde `AuthenticatedAccount.userId` y
    `VenueDao`.
  - `PATCH /api/venue/me/services/{serviceId}` funciona como edición de campos básicos editables
    para mantener un contrato simple hasta que lleguen asociaciones y recursos.
  - Los campos localizados `nameI18n` y `descriptionI18n` quedan modelados como JSONB opcional para
    alinear el CRUD con el diseño multidioma.
  - Evidencia: Maven focalizado con 16 tests, 0 fallos, Spotless y Checkstyle incluidos.

## Conversación 83 - Calendario interno y tests de cálculo de disponibilidad

- Fecha: 2026-07-11.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se completaron `4.13` y `4.14`, cerrando la Fase 4 funcional de horarios, franjas y
    disponibilidad.
  - Se creó una vista interna semanal en `/panel/calendario` con navegación por semana, selector de
    fecha, resumen de capacidad, franjas disponibles, bloqueadas y detalle diario.
  - Se ampliaron las pruebas del cálculo público de disponibilidad para cubrir cierre semanal,
    reservas desactivadas por excepción, día completo, ausencia de huecos futuros y fallback de
    idioma.
  - Se añadieron traducciones ES/EN y tests UI para la nueva agenda interna.
- Archivos modificados:
  - `apps/web/src/features/availability/venue-internal-calendar.tsx`.
  - `apps/web/src/app/panel/calendario/page.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-006`, `RF-010`, `RF-011`, `RF-012`.
  - `RNF-001`, `RNF-004`, `RNF-007`, `RNF-009`, `RNF-011`, `RNF-012`.
- Tareas impactadas y completadas:
  - `4.13. Crear vista de calendario interno básica`.
  - `4.14. Crear tests de cálculo de disponibilidad`.
- Siguiente tarea pendiente recomendada:
  - `5.1. Crear migraciones de services, employee_resources, employee_resource_hours y service_employee_resources`.
- Decisiones o aclaraciones relevantes:
  - La vista interna reutiliza `GET /api/venue/me/time-slots` por fecha y consulta siete días en
    paralelo; no introduce un endpoint de rango hasta que las métricas lo justifiquen.
  - La agenda interna no envía `venueId`; todo aislamiento depende de la sesión y de los endpoints
    privados existentes.
  - El cálculo de capacidad sigue sin descontar reservas ni holds hasta Fase 7.
  - El navegador integrado se conectó, pero el servidor local no pudo mantenerse escuchando en
    segundo plano durante la comprobación; responsive y accesibilidad quedaron verificados por
    componentes, lint, tipos, tests y build.
  - Evidencia: backend focalizado con 8 tests, frontend focalizado con 6 tests, tipos, lint, i18n,
    español, build y whitespace correctos.

## Conversación 82 - Calendario público y panel privado de disponibilidad

- Fecha: 2026-07-11.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se completaron `4.11` y `4.12` como las dos primeras tareas pendientes.
  - La ficha pública incorpora una ventana responsive de siete días, navegación, selector de fecha,
    estados localizados, leyenda y franjas con capacidad.
  - Se creó `/panel/calendario` para gestionar horario semanal, excepciones, creación manual y
    automática de franjas, capacidad, bloqueo y reapertura.
  - Se añadió un cliente Zod, catálogos ES/EN y pruebas de UI y transporte HTTP.
- Archivos modificados:
  - `apps/web/src/features/availability/availability-api.ts`.
  - `apps/web/src/features/availability/availability-api.test.ts`.
  - `apps/web/src/features/availability/public-availability-calendar.tsx`.
  - `apps/web/src/features/availability/venue-availability-manager.tsx`.
  - `apps/web/src/features/availability/availability-ui.test.tsx`.
  - `apps/web/src/app/panel/calendario/page.tsx`.
  - `apps/web/src/features/public-venue/public-venue-profile.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-004`, `RF-006`, `RF-010`, `RF-011`, `RF-012`, `RF-031`.
  - `RNF-001`, `RNF-002`, `RNF-004`, `RNF-008`, `RNF-009`, `RNF-010`.
- Tareas impactadas y completadas:
  - `4.11. Crear calendario público de disponibilidad`.
  - `4.12. Crear panel privado de horarios y franjas`.
- Siguiente tarea pendiente recomendada:
  - `4.13. Crear vista de calendario interno básica`.
- Decisiones o aclaraciones relevantes:
  - Se usa una ventana de siete días porque el contrato backend consulta una fecha.
  - Las siete consultas son paralelas y cancelables con `AbortController`.
  - Reservar permanece deshabilitado hasta los holds de Fase 7.
  - El panel no envía IDs de local y usa la cookie HttpOnly.
  - El navegador integrado no estuvo disponible; responsive y accesibilidad se verificaron con
    componentes, lint, tipos y build.
  - Evidencia: 3 ficheros, 7 tests, 0 fallos; tipos, lint, i18n, español, build y whitespace correctos.

## Conversación 81 - Estado operativo y disponibilidad pública por fecha

- Fecha: 2026-07-09.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.9` y `4.10` como las dos siguientes tareas pendientes.
  - Se implementó `GET /api/public/venues/{slug}/availability?date=YYYY-MM-DD` como endpoint
    anónimo de disponibilidad pública de un local publicado.
  - Se añadió el caso de uso `PublicVenueAvailabilityService` para calcular estado operativo por
    fecha desde horario semanal, excepciones diarias y estado de franjas.
  - El cálculo devuelve estados `open`, `closed`, `unavailable`, `full` y `upcoming_available`, con
    labels localizados ES/EN y señal `bookingAvailable`.
  - Las franjas públicas exponen inicio, fin, capacidad total, capacidad disponible temporal,
    estado y si admiten reserva.
  - Se reutilizó la resolución pública de idioma de locales haciendo público
    `VenuePublicLocaleResolver`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicTimeSlotAvailabilityResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/PublicVenueAvailabilityResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/PublicVenueAvailabilityControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/PublicVenueAvailabilityServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.9. Implementar cálculo de estado del local`.
  - `4.10. Implementar endpoint de disponibilidad pública por local y fecha`.
  - Prepara `4.11`, `4.12`, `4.14` y el flujo de holds de Fase 7.
- Tareas completadas:
  - `4.9. Implementar cálculo de estado del local`.
  - `4.10. Implementar endpoint de disponibilidad pública por local y fecha`.
- Siguiente tarea pendiente recomendada:
  - `4.11. Crear calendario público de disponibilidad.`
- Decisiones o aclaraciones relevantes:
  - El contrato público usa `slug` porque la ficha pública y la búsqueda ya trabajan con slugs y no
    exponen IDs internos de local.
  - La capacidad disponible se iguala temporalmente a `capacity` cuando la franja está `available`;
    en Fase 7 se recalculará restando reservas confirmadas y holds vigentes.
  - `upcoming_available` se calcula cuando la fecha consultada no tiene huecos reservables pero
    existen franjas futuras `available`.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests,PublicVenueAvailabilityServiceTests,PublicVenueAvailabilityControllerTests" test`
    pasó con 27 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`, `npm run backend:conventions:check`,
    `npm run spanish:text:check` y `git diff --check`.

## Conversación 80 - Bloqueo manual de franjas y cierre operativo de día

- Fecha: 2026-07-09.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.7` y `4.8` como las dos siguientes tareas pendientes.
  - Se añadieron los endpoints privados `PATCH /api/venue/me/time-slots/{slotId}/block` y
    `PATCH /api/venue/me/time-slots/{slotId}/reopen`.
  - El bloqueo manual cambia la franja propia a `status=blocked` bajo bloqueo pesimista.
  - La reapertura manual solo permite volver a `available` desde `blocked` y rechaza la operación si
    el día tiene cierre completo o reservas desactivadas.
  - El cierre de día completo y la desactivación de reservas por día pasan a propagar efecto sobre
    `TimeSlots`, marcando como `unavailable` todas las franjas no bloqueadas de la fecha.
  - Al eliminar la excepción diaria y volver al horario semanal se restauran como `available` las
    franjas que estaban `unavailable`; las franjas `blocked` se conservan bloqueadas.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/AvailabilityDayServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.7. Implementar bloqueo y reapertura manual de franjas`.
  - `4.8. Implementar cierre de día completo`.
  - Prepara `4.9` y `4.10`.
- Tareas completadas:
  - `4.7. Implementar bloqueo y reapertura manual de franjas`.
  - `4.8. Implementar cierre de día completo`.
- Siguiente tarea pendiente recomendada:
  - `4.9. Implementar cálculo de estado del local.`
- Decisiones o aclaraciones relevantes:
  - El bloqueo manual se modela en `TimeSlots.status=blocked`; no crea una fila adicional en
    `AvailabilityBlocks` porque la franja ya tiene estado propio y versión.
  - El cierre diario se mantiene como excepción de día completo en `AvailabilityBlocks`, pero ahora
    también materializa el estado `unavailable` sobre franjas no bloqueadas para que las lecturas
    privadas y futuras lecturas públicas no muestren huecos reservables por accidente.
  - La reapertura de día no toca franjas `blocked`, preservando decisiones manuales previas del local.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 22 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`, `npm run backend:conventions:check`,
    `npm run spanish:text:check` y `git diff --check`.

## Conversación 79 - Generación automática y capacidad máxima de franjas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.5` y `4.6` como las dos siguientes tareas pendientes tras revisar el estado de
    `tasks.md` y los requisitos de gestión de franjas.
  - Se añadió el contrato privado `POST /api/venue/me/time-slots/generate` para generar franjas
    consecutivas de una fecha a partir de una duración fija y una capacidad inicial.
  - Se añadió el contrato privado `PATCH /api/venue/me/time-slots/{slotId}/capacity` para actualizar
    la capacidad máxima de una franja propia.
  - La generación valida local vigente, horario semanal abierto, reservas activas, ausencia de
    excepción diaria, duración entre 5 y 480 minutos, capacidad positiva y ausencia de solapes antes
    de persistir el lote.
  - La actualización de capacidad usa bloqueo pesimista sobre la franja propia para dejar preparada
    la consistencia transaccional que necesitarán reservas y holds.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotCapacityRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotGenerationRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-006 Calendario de disponibilidad`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.5. Implementar generación automática de franjas por duración`.
  - `4.6. Implementar capacidad máxima por franja`.
  - Prepara `4.7`, `4.10` y la validación de capacidad real de Fase 7.
- Tareas completadas:
  - `4.5. Implementar generación automática de franjas por duración`.
  - `4.6. Implementar capacidad máxima por franja`.
- Siguiente tarea pendiente recomendada:
  - `4.7. Implementar bloqueo y reapertura manual de franjas.`
- Decisiones o aclaraciones relevantes:
  - La generación automática no sobrescribe ni fusiona franjas existentes; si alguna candidata se
    solapa, se rechaza toda la operación para evitar resultados parciales ambiguos.
  - Las franjas generadas nacen con `status=available`, `createdByRule=true`, `serviceId=null` y
    capacidad positiva.
  - La capacidad solo se valida contra mínimo `1` porque todavía no existen reservas ni holds que
    consuman plazas; la restricción contra plazas confirmadas se incorporará cuando exista el modelo
    de reservas.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 18 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `mvn -f apps/api/pom.xml spotless:apply`.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.

## Conversación 78 - Excepciones diarias y creación manual de franjas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.3` y `4.4` como las dos siguientes tareas pendientes.
  - Se añadió la migración `V18__add_availability_block_kind.sql` para distinguir bloqueos manuales,
    días cerrados y días con reservas desactivadas dentro de `AvailabilityBlocks`.
  - Se implementaron entidades y DAOs para `AvailabilityBlocks` y `TimeSlots`.
  - Se añadió `GET/PUT /api/venue/me/availability-days` para consultar o sustituir la excepción de una fecha.
  - Se añadió `GET/POST /api/venue/me/time-slots` para listar y crear franjas manuales.
  - La creación manual valida local vigente, horario semanal, día no bloqueado, rango horario,
    capacidad positiva y ausencia de solapes.
  - Se añadieron tests unitarios de servicio y controlador para días cerrados, reservas inactivas y
    creación manual de franjas.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V18__add_availability_block_kind.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityDayControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/TimeSlotControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityDayRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityDayResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/TimeSlotResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/AvailabilityBlockEntity.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/TimeSlotEntity.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayInvalidException.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/AvailabilityDayServiceImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotInvalidException.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/TimeSlotServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/AvailabilityDayControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/TimeSlotControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/AvailabilityDayServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/TimeSlotServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día`.
  - `4.4. Implementar creación manual de franjas`.
  - Prepara `4.5`, `4.6`, `4.7`, `4.8`, `4.9` y `4.10`.
- Tareas completadas:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día`.
  - `4.4. Implementar creación manual de franjas`.
- Siguiente tarea pendiente recomendada:
  - `4.5. Implementar generación automática de franjas por duración.`
- Decisiones o aclaraciones relevantes:
  - Los días cerrados y los días con reservas inactivas se modelan como bloqueos de día completo en
    `AvailabilityBlocks` con `kind=closed_day` o `kind=reservations_disabled`.
  - Volver a `closed=false` y `reservationsEnabled=true` elimina la excepción y recupera el horario semanal.
  - Las franjas manuales nacen con `status=available`, `createdByRule=false`, `serviceId=null` y
    capacidad positiva.
  - No se permite crear franjas fuera del horario semanal ni en días cerrados o con reservas inactivas.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests,AvailabilityDayServiceTests,AvailabilityDayControllerTests,TimeSlotServiceTests,TimeSlotControllerTests" test`
    pasó con 13 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.
  - `DatabaseMigrationIntegrationTests` no pudo completar porque Testcontainers no encontró un Docker
    válido en el entorno actual.

## Conversación 77 - Migraciones y horario semanal de disponibilidad

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se inició la rama de fase `phase/4-horarios-franjas-disponibilidad`.
  - Se confirmaron `4.1` y `4.2` como las dos siguientes tareas pendientes tras revisar `tasks.md`,
    `requirements.md`, `design.md`, seguimiento e implementación técnica.
  - Se creó la migración `V17__create_availability_schedule_tables.sql` con las tablas físicas
    `VenueOpeningHours`, `TimeSlots` y `AvailabilityBlocks`.
  - Se implementó el módulo backend `availability` para configuración semanal privada del local.
  - Se añadió `GET /api/venue/me/opening-hours` para consultar el horario vigente.
  - Se añadió `PUT /api/venue/me/opening-hours` para sustituir de forma transaccional los siete días
    ISO de la semana.
  - Se validó que el payload no acepta IDs de local ni propietario y que el alcance procede del
    principal autenticado.
  - Se añadieron tests unitarios de servicio y controlador para reemplazo semanal, validaciones,
    propiedad por principal y errores estables.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V17__create_availability_schedule_tables.sql`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursController.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/OpeningHoursControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/controller/AvailabilityExceptionHandler.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/AvailabilityErrorResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHourResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/dto/OpeningHoursUpdateRequest.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/persistence/VenueOpeningHourEntity.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursInvalidException.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursService.java`.
  - `apps/api/src/main/java/com/reserly/platform/availability/service/OpeningHoursServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/controller/OpeningHoursControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/availability/service/OpeningHoursServiceTests.java`.
  - `package-info.java` de subpaquetes `availability`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-005 Estado público del local`.
  - `RF-006 Calendario de disponibilidad`.
  - `RF-010 Gestión de horarios`.
  - `RF-011 Gestión de franjas`.
  - `RF-012 Gestión de disponibilidad en tiempo real`.
  - `RNF-001 Seguridad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
  - `4.2. Implementar configuración de horario semanal`.
  - Prepara `4.3`, `4.4`, `4.5`, `4.6`, `4.7`, `4.8`, `4.9` y `4.10`.
- Tareas completadas:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
  - `4.2. Implementar configuración de horario semanal`.
- Siguiente tarea pendiente recomendada:
  - `4.3. Implementar días cerrados y reservas activas/inactivas por día.`
- Decisiones o aclaraciones relevantes:
  - La configuración semanal se reemplaza como snapshot completo de siete días para evitar estados
    parciales ambiguos.
  - Los días usan numeración ISO-8601: lunes `1`, domingo `7`.
  - Un día cerrado no puede tener horas ni reservas activas.
  - Un día abierto exige `opensAt < closesAt`; puede dejar `reservationsEnabled=false` para mantener
    horario operativo sin permitir reservas.
  - La migración crea `TimeSlots` y `AvailabilityBlocks` sin FKs a servicios o recursos todavía no
    existentes; las columnas quedan preparadas para fases posteriores.
  - Evidencia correcta: `mvn -f apps/api/pom.xml "-Dtest=OpeningHoursServiceTests,OpeningHoursControllerTests" test`
    pasó con 5 tests, 0 fallos, 0 errores y 0 omitidos, incluyendo Spotless y Checkstyle.
  - Evidencia correcta: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check`.
  - Evidencia parcial: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test`
    arrancó Testcontainers, validó 17 migraciones y aplicó Flyway hasta detectar una discrepancia de
    tipo entre SQL y JPA en `VenueOpeningHours.weekday`; se corrigió la migración de `smallint` a
    `integer`. El rerun posterior con Testcontainers no pudo completar porque el entorno actual no
    expuso un Docker válido para Testcontainers.

## Conversación 1 - Creación de especificación base

- Fecha: 2026-06-06
- Resumen: se generaron los documentos principales de spec build para la plataforma SaaS de reservas.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - Alcance MVP.
  - Usuarios finales, locales y administradores.
  - Búsqueda, filtros, ficha de local, disponibilidad, reservas y concurrencia.
  - Asistencia, no asistencia, penalizaciones y reseñas.
  - Estadísticas, suscripciones, RedSys, responsive móvil y equipo/recursos.
- Tareas impactadas:
  - Se creó el plan completo por fases desde Fase 0 hasta post-MVP.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Arquitectura recomendada: monolito modular con API REST, PostgreSQL, cache y cola de trabajos.
  - MVP centrado en reserva real, control de concurrencia y panel de local.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 2 - Internacionalización y verificación empresarial

- Fecha: 2026-06-06
- Resumen: se añadieron requisitos de internacionalización ES/EN y verificación empresarial para cuentas de locales.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-011 Resolución de idioma`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - Fase 0: infraestructura i18n.
  - Fase 1: `account_type`, `business_accounts`, verificación remota y bloqueo de publicación.
  - Fase 2: textos localizados en perfil/categorías.
  - Fase 8: plantillas de email ES/EN.
  - Fase 14: revisión administrativa de cuentas empresariales.
  - Fase 19: QA de idioma y verificación empresarial.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Si el idioma del navegador o app empieza por `es`, se usa español.
  - Cualquier otro idioma usa inglés.
  - Las cuentas de local requieren `account_type = venue_business`.
  - El identificador empresarial canónico es `business_tax_identifier`, con `tax_country` y `business_legal_name`.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 3 - Aclaración de APIs de verificación y documentos de respaldo

- Fecha: 2026-06-06
- Resumen: se aclaró que VIES no cubre todos los negocios locales españoles y se añadió flujo de verificación con documentos de respaldo.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - Fase 1: validación de formato y dígito de control, documentación de respaldo y subida privada.
  - Fase 14: revisión de documentos de respaldo.
  - Fase 19: QA de verificación con documentación.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Para España se debe aceptar NIF de autónomo, NIF de sociedad/entidad y NIF-IVA cuando aplique.
  - VIES solo debe usarse cuando aplique VAT ID intracomunitario.
  - Si la verificación automática no es concluyente, la cuenta queda en `pending_review`.
  - Documentos admitidos inicialmente: alta censal 036/037, certificado censal, licencia de actividad/apertura o documento administrativo equivalente.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 4 - Publicación inicial en GitHub

- Fecha: 2026-06-06
- Resumen: se inicializó el repositorio Git local, se creó el commit inicial y se subieron los documentos de especificación a GitHub.
- Archivos modificados:
  - Ningún archivo de especificación cambiado en esta conversación.
- Requisitos impactados:
  - Ninguno.
- Tareas impactadas:
  - Ninguna tarea de producto.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - Rama principal: `main`.
  - Repositorio remoto: `https://github.com/heg15220/plataforma-reservas-saas.git`.
  - Commit inicial: `fc00696 Add reservation SaaS specification`.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 5 - Instrucciones para agentes y seguimiento continuo

- Fecha: 2026-06-06
- Resumen: se añadió `AGENTS.md` y este documento de seguimiento para obligar a revisar `.kiro` al inicio de nuevas conversaciones y registrar cambios posteriores.
- Archivos modificados:
  - `AGENTS.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - Ningún requisito funcional de producto.
- Tareas impactadas:
  - Ninguna tarea de implementación de producto.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - `tasks.md` es la fuente de verdad para el avance.
  - `conversation-tracking.md` es el histórico obligatorio de cambios por conversación.
  - Cada agente debe revisar requisitos, diseño, tareas y seguimiento antes de actuar.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 6 - Documentación técnica obligatoria

- Fecha: 2026-06-06
- Resumen: se amplió `agents.md` para exigir documentación técnica profunda al finalizar cada tarea y documentación obligatoria de todo el código implementado.
- Archivos modificados:
  - `agents.md`
  - `technical-implementation.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - Ningún requisito funcional de producto.
- Tareas impactadas:
  - Todas las tareas de `tasks.md`, porque ninguna podrá marcarse como completada sin actualizar el documento técnico único.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Decisiones:
  - El documento técnico único será `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
  - Al cerrar cada tarea se documentará objetivo, requisitos, archivos, arquitectura, datos, APIs, seguridad, i18n, UI, tests, decisiones, riesgos y evidencia.
  - Todo código implementado deberá quedar documentado con el nivel necesario para entender responsabilidades, contratos, invariantes y efectos secundarios.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`

## Conversación 7 - Reseñas desde ficha y pestañas personalizadas del local

- Fecha: 2026-06-08
- Resumen: se incorporó a la especificación que la ficha pública del local debe incluir un botón para hacer reseñas desde los detalles del local y que el flujo debe solicitar email para validar si existe al menos una reserva pasada elegible en ese local. También se añadió que cada local puede configurar pestañas personalizadas dentro de su ficha, por ejemplo una pestaña de carta para restaurantes con menú completo, precios e información relacionada.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-024 Reseñas y valoraciones`.
  - `RF-031 Internacionalización de textos`.
  - `RB-013 Elegibilidad de reseñas por email y local`.
- Tareas impactadas:
  - Fase 2: se añadieron tareas para `venue_custom_tabs`, CRUD de pestañas personalizadas, renderizado público, permisos, sanitización e i18n.
  - Fase 11: se añadieron tareas para botón de reseña, comprobación de elegibilidad por email/local/reserva pasada y mensajes i18n de rechazo.
  - Fase 15: se añadió validación responsive del flujo móvil de pestañas y reseñas.
  - Fase 19: se añadieron validaciones de aceptación para reseñas desde ficha, rechazo sin reserva y pestañas personalizadas.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Decisiones o aclaraciones relevantes:
  - La reseña desde la ficha se valida en backend con `venue_id`, email normalizado y reserva confirmada/finalizada en el pasado.
  - Si no existe reserva elegible, o si todas las reservas elegibles ya tienen reseña, el sistema debe impedir la reseña con un mensaje claro e internacionalizado.
  - La respuesta pública de elegibilidad no debe devolver fechas, reservas ni historial asociado al email.
  - Las pestañas personalizadas se modelan como contenido público localizado, ordenable y sanitizado por local.

## Conversación 8 - Selección definitiva de stack y análisis de OverCut

- Fecha: 2026-06-08
- Resumen: se analizó el proyecto local `C:\Users\hugoe\Downloads\OverCut\overcut` para evaluar si su estructura, jerarquía, implementación y tecnologías son viables para la plataforma SaaS de reservas. Se decidió aprovechar la familia tecnológica Java/Spring para backend, pero no copiar OverCut tal cual. El frontend de OverCut basado en `react-scripts`/Create React App se descarta para proyecto nuevo y se selecciona Next.js con TypeScript. Se definió el stack definitivo por capa para la tarea `0.1`.
- Archivos modificados:
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Tareas completadas:
  - `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Stack seleccionado: Next.js + React + TypeScript para frontend; Spring Boot + Java 21 para backend; PostgreSQL + Hibernate/JPA + Flyway para persistencia; Redis para cache/rate limiting; RabbitMQ para cola; Quartz o scheduler con lock persistente para jobs; S3-compatible para archivos; Testcontainers, Playwright, Actuator, Micrometer y OpenTelemetry para verificación y operación.
  - OverCut confirma que Spring Boot/JPA es viable, pero su uso de H2, `schema.sql`, `data.sql`, `react-scripts`, `HashRouter`, token en `sessionStorage`, CORS abierto, CSRF desactivado, emails síncronos y ausencia de cola/cache/migraciones no cumple el nivel requerido para el SaaS de reservas.
  - La arquitectura debe organizarse como monolito modular por contextos de negocio, no solo por capas globales.

## Conversación 9 - Convenciones obligatorias de Java, Spring Boot, JPA y REST

- Fecha: 2026-06-16
- Resumen: se incorporaron a la planificación del proyecto convenciones obligatorias para nombres de tablas, clases Java, atributos, mapeos JPA, DAOs, servicios, controladores, DTOs y conversores REST. Estas reglas deben aplicarse en toda la implementación backend y en las migraciones de base de datos.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-001 Seguridad`, por separación de capas, DTOs y no exposición directa de entidades.
  - `RNF-003 Concurrencia y consistencia`, por obligación de DAOs, consultas explícitas y transacciones en servicios.
  - `RNF-005 Escalabilidad`, por separación de interfaces e implementaciones y monolito modular.
- Tareas impactadas:
  - Fase 0: se añadió `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores`.
  - Fase 1: se ajustó `1.1` para que las primeras tablas de identidad se creen aplicando nombres físicos `UpperCamelCase` y atributos/columnas `lowerCamelCase`.
  - Todas las tareas futuras que creen entidades, migraciones, DAOs, servicios, controladores, DTOs o conversores.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Las tablas físicas deben usar `UpperCamelCase`, por ejemplo `BusinessAccount` o `VenueCustomTab`.
  - En PostgreSQL se deberán usar identificadores entrecomillados en migraciones y mapeos JPA cuando sea necesario preservar mayúsculas.
  - Las clases Java deben usar `UpperCamelCase` y los atributos `lowerCamelCase`.
  - Las relaciones JPA se mapearán mediante anotaciones en los métodos `get` correspondientes, con setters existentes y consistentes para mantener invariantes.
  - Cada entidad tendrá DAO propio y las consultas de dominio se declararán con `@Query`.
  - Servicios y controladores separarán interfaz e implementación; otros módulos deberán depender de las interfaces.
  - La capa REST usará DTOs y conversores explícitos, sin exponer entidades JPA directamente desde controladores.

## Conversación 10 - Calidad ortográfica y codificación de textos españoles

- Fecha: 2026-06-16
- Resumen: se añadió un requisito transversal para que todo texto en español del proyecto conserve tildes, eñes, diéresis, signos de apertura, símbolos y caracteres especiales correctamente, sin problemas de codificación ni mojibake.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - Fase 0: se añadió `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles`.
  - Fase 19: se añadió `19.29. Validar que todo texto español visible conserva tildes, eñes, signos ¿/¡, caracteres especiales y codificación UTF-8 correcta`.
  - Criterios de salida del MVP: se añadió validación para detectar tildes omitidas, signos de apertura omitidos, caracteres especiales rotos o mojibake.
  - Todas las tareas futuras que creen UI, emails, errores públicos, estados, seeds, migraciones con texto visible, documentación de usuario o catálogos i18n.
- Tareas completadas:
  - Ninguna tarea de implementación marcada como completada.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Todo texto español debe guardarse y servirse en UTF-8.
  - No se aceptan textos visibles con mojibake ni caracteres sustitutos como `Ã`, `Â` o `�`.
  - Las normalizaciones técnicas sin tildes solo pueden usarse en campos internos no visibles, por ejemplo para búsqueda.
  - La versión visible al usuario debe conservar ortografía española correcta y caracteres especiales.

## Conversación 11 - Nombre comercial Reserly y sistema visual

- Fecha: 2026-06-18
- Resumen: se cerró la decisión pendiente de nombre comercial y sistema visual. El producto pasa a denominarse `Reserly` y se analizaron los prototipos de escritorio y móvil para formalizar la identidad, paleta, tipografía, geometría, componentes, estados, patrones responsive, accesibilidad e internacionalización que deberá aplicar la implementación.
- Archivos modificados:
  - `design.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - Pantallas mínimas del MVP para usuario final y local registrado.
- Tareas impactadas:
  - `0.7. Crear layout base responsive y sistema de componentes`.
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía`.
  - Todas las tareas posteriores que implementen interfaz pública, panel del local, formularios, calendarios, tablas, emails o recursos de marca.
- Tareas completadas:
  - Ninguna. La definición documental no completa `0.8`; faltan tokens, tema MUI, componentes y verificación visual implementados.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - El nombre comercial definitivo es `Reserly`.
  - `ReservaYa` queda limitado a los prototipos históricos y debe sustituirse por `Reserly` en la implementación.
  - La dirección visual usa superficies claras, azul como acción primaria, estados semánticos verde/ámbar/rojo/neutro, tipografía `Inter`, tarjetas de bordes suaves y navegación adaptada a escritorio y móvil.
  - El panel del local mantiene navegación lateral en escritorio y navegación inferior simplificada en móvil.
  - Los colores y estados deben implementarse con tokens semánticos, cumplir WCAG 2.2 AA y no depender únicamente del color.
  - Los prototipos son referencia visual y funcional, no una excepción a los requisitos de accesibilidad, responsive e i18n.

## Conversación 12 - Cierre de decisiones operativas con estrategia gratuita primero

- Fecha: 2026-06-18
- Resumen: se cerraron las decisiones pendientes de email, mapas/geocoding, comprobación española de NIF/CIF, revisión manual empresarial, PostGIS, RedSys, panel admin y conservación de incidencias. Se contrastaron condiciones vigentes con fuentes oficiales y se estableció como regla transversal priorizar opciones oficiales, libres o con plan gratuito compatible antes de contratar servicios.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
- Requisitos impactados:
  - `RF-028 Suscripción y RedSys`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-004 Rendimiento`.
  - `RNF-010 Verificación empresarial remota`.
  - `RB-007 Penalización global MVP`.
- Tareas impactadas:
  - Fase 1: validación local, VIES, AEAT y revisión documental.
  - Fase 3: geocodificación y búsqueda por radio.
  - Fase 8: proveedor de email transaccional.
  - Fase 10 y 16: conservación, anonimización, bloqueo y borrado.
  - Fase 13: RedSys preparado con simulador, sin cobro real en producción.
  - Fase 14: alcance cerrado del panel admin.
- Tareas completadas:
  - Ninguna. Son decisiones de especificación; la implementación y verificación siguen pendientes.
- Siguiente tarea pendiente recomendada:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Decisiones o aclaraciones relevantes:
  - Brevo Free es el proveedor inicial de email; Mailpit se usa en local.
  - LocationIQ Free y MapLibre son la combinación inicial para geocodificación y mapas, con atribución y adaptadores sustituibles.
  - PostGIS se activa desde el MVP.
  - La verificación española prioriza algoritmo local, VIES y AEAT. Si no existe canal AEAT automatizable confirmado, se usa revisión administrativa oficial y documental, no un proveedor comercial.
  - RedSys se prepara mediante interfaz, simulador y contratos, pero no se activa en producción hasta disponer de contrato bancario y credenciales.
  - Las incidencias y penalizaciones identificables se conservan operativamente hasta 12 meses; la evidencia mínima puede bloquearse hasta 3 años, sujeto a validación jurídica previa a producción.

## Conversación 13 - Repositorio base, monorepo y convenciones de ramas

- Fecha: 2026-06-22
- Resumen: se implementó la primera estructura ejecutable del producto sobre el repositorio Git existente. Se creó un monorepo con la API Spring Boot en `apps/api`, la aplicación Next.js en `apps/web`, documentación transversal en `docs` y un espacio reservado para infraestructura. También se formalizaron la estrategia de ramas cortas, Conventional Commits, el flujo de pull requests y los límites entre aplicaciones y contextos backend.
- Archivos modificados:
  - `.editorconfig`
  - `.gitattributes`
  - `.gitignore`
  - `README.md`
  - `CONTRIBUTING.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - Declaraciones `package-info.java` de los contextos backend iniciales.
  - `apps/api/src/main/resources/application.yaml`
  - `apps/web/README.md`
  - `apps/web/package.json`
  - `apps/web/package-lock.json`
  - `apps/web/next-env.d.ts`
  - `apps/web/next.config.ts`
  - `apps/web/tsconfig.json`
  - `apps/web/src/app/globals.css`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/page.tsx`
  - `docs/README.md`
  - `docs/architecture/monorepo.md`
  - `infrastructure/README.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-005 Escalabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
  - Se preparó la base para `0.3`, `0.4`, `0.5`, `0.6`, `0.9` y las futuras tareas de implementación por contexto, sin darlas por completadas.
- Tareas completadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Siguiente tarea pendiente recomendada:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
- Decisiones o aclaraciones relevantes:
  - Se adopta un monorepo con dos unidades desplegables: `apps/web` y `apps/api`.
  - `main` es la única rama permanente; el trabajo se realiza en ramas cortas `feature/`, `fix/`, `chore/`, `docs/`, `codex/` y, excepcionalmente, `release/`.
  - Se usará squash merge y Conventional Commits.
  - La web solo dependerá de contratos HTTP de la API.
  - Los contextos backend se representan como paquetes bajo `com.reserly.platform` y deberán colaborar mediante interfaces o eventos, sin consultar directamente la persistencia interna de otros contextos.
  - Se fijaron Spring Boot `4.1.0`, Next.js `16.2.9`, React `19.2.1` y TypeScript `5.9.2` para el esqueleto inicial.
  - Se forzó PostCSS `8.5.10` mediante `overrides` porque el árbol original de Next.js incorporaba una versión afectada por `GHSA-qx2v-qp2m-jg93`; tras regenerar el lockfile, `npm audit` informó cero vulnerabilidades.

## Conversación 14 - Publicación de la base y herramientas de calidad

- Fecha: 2026-06-22
- Resumen: se publicó la tarea `0.2` en la rama remota `main` mediante el commit `ae3f4f8`. A continuación se creó la rama `codex/task-0.3-quality-tooling` y se configuró una cadena de calidad completa para frontend y backend: ESLint, Prettier, Checkstyle, Spotless, Vitest, React Testing Library y JUnit, además de scripts raíz para iniciar las aplicaciones y ejecutar lint, formato, tipos, tests, builds y verificación integral.
- Archivos modificados:
  - `.gitignore`
  - `.prettierignore`
  - `.prettierrc.json`
  - `README.md`
  - `CONTRIBUTING.md`
  - `package.json`
  - `package-lock.json`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/config/checkstyle/checkstyle.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/web/README.md`
  - `apps/web/package.json`
  - `apps/web/eslint.config.mjs`
  - `apps/web/vitest.config.mts`
  - `apps/web/vitest.setup.ts`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/globals.css`
  - `apps/web/tsconfig.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - Se eliminó `apps/web/package-lock.json` al trasladar la instalación reproducible al lockfile raíz del workspace.
- Requisitos impactados:
  - `RNF-005 Escalabilidad`.
  - `RNF-007 Usabilidad`, mediante reglas frontend de Core Web Vitals.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
- Tareas impactadas:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
  - Se prepara la ejecución local y futura automatización de `0.9. Crear pipeline CI con tests y validación de estilo`.
- Tareas completadas:
  - `0.3. Configurar linters, formatter, test runner y scripts de desarrollo.`
- Siguiente tarea pendiente recomendada:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
- Decisiones o aclaraciones relevantes:
  - El frontend usa ESLint flat config con reglas de Next.js Core Web Vitals, React, hooks y TypeScript, y desactiva conflictos de formato mediante `eslint-config-prettier`.
  - Prettier formatea frontend y documentación operativa; `.kiro` y Java quedan excluidos para no reescribir la especificación ni competir con Spotless.
  - El backend usa Spotless con Google Java Format y Checkstyle con reglas estructurales y de nomenclatura.
  - Vitest `4.1.9` con jsdom y React Testing Library ejecuta pruebas de componentes síncronos; los Server Components asíncronos se reservarán para E2E.
  - JUnit mantiene una prueba de humo que verifica la creación del contexto Spring Boot sin infraestructura externa.
  - El lockfile se centraliza en la raíz mediante npm workspaces.
  - El árbol final de npm se auditó con cero vulnerabilidades conocidas.
  - `npm run verify` completó ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit y los builds de Next.js y Spring Boot.

## Conversación 15 - Configuración validada por entornos

- Fecha: 2026-06-22
- Resumen: se implementó la configuración por entornos `local`, `staging` y `production` en una rama apilada sobre la tarea `0.3`. Se añadieron plantillas dotenv sin secretos, perfiles Spring, propiedades Java tipadas y validadas, validación Zod durante el arranque/build de Next.js, scripts de desarrollo local y staging, y una comprobación automática que evita divergencias o exposición accidental de secretos en variables públicas.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `.gitignore`
  - `README.md`
  - `package.json`
  - `package-lock.json`
  - `scripts/validate-environment-examples.mjs`
  - `docs/configuration.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/ReserlyApplication.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyEnvironment.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/ReserlyProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/configuration/package-info.java`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-staging.yaml`
  - `apps/api/src/main/resources/application-production.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/test/java/com/reserly/platform/ReserlyApplicationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/ReserlyPropertiesTests.java`
  - `apps/web/README.md`
  - `apps/web/environment.ts`
  - `apps/web/environment.test.ts`
  - `apps/web/next.config.ts`
  - `apps/web/package.json`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-010 Verificación empresarial remota`, preparando secretos externos sin almacenarlos.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
- Tareas impactadas:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
  - Se preparan los contratos de `0.5`, `0.6`, `8.1`, `13.7` y las integraciones externas posteriores.
- Tareas completadas:
  - `0.4. Configurar variables de entorno por entorno: local, staging y producción.`
- Siguiente tarea pendiente recomendada:
  - `0.5. Configurar PostgreSQL local y migraciones.`
- Decisiones o aclaraciones relevantes:
  - Solo `NEXT_PUBLIC_APP_ENV` y `NEXT_PUBLIC_API_BASE_URL` pueden exponerse al navegador.
  - Los secretos reales se inyectarán desde el entorno de despliegue o un gestor de secretos; no se versionan ficheros `.env` reales.
  - Staging y producción exigen HTTPS público y cookies seguras.
  - Los pagos reales no pueden activarse todavía, ni siquiera mediante variable de entorno.
  - El perfil `test` es interno y usa valores aislados sin servicios externos.
  - PostgreSQL, Redis, RabbitMQ y S3 aparecen como contratos reservados en las plantillas, pero aún no son consumidos.
  - `npm run env:check` verifica paridad de claves, HTTPS, cookies seguras, pagos desactivados y ausencia de nombres potencialmente secretos bajo `NEXT_PUBLIC_`.
  - Las comprobaciones negativas confirmaron que Next.js rechaza HTTP en staging y Spring Boot rechaza producción con HTTP, cookies inseguras o pagos reales.

## Conversación 16 - PostgreSQL, PostGIS y Flyway

- Fecha: 2026-06-22
- Resumen: se configuró PostgreSQL 17 con PostGIS 3.5 para desarrollo local, se añadió persistencia JPA con Hikari y se estableció Flyway como único propietario del esquema. La migración inicial activa PostGIS, `pg_trgm` y `unaccent`. Se añadieron tests de integración con Testcontainers JDBC que crean una base efímera vacía, ejecutan Flyway y verifican extensiones, UTF-8 y UTC. También se verificó el flujo local real arrancando Compose y la API contra un volumen nuevo.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/configuration.md`
  - `infrastructure/compose.yaml`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/main/resources/db/migration/V1__enable_postgresql_extensions.sql`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones backend y persistencia`.
- Tareas impactadas:
  - `0.5. Configurar PostgreSQL local y migraciones.`
  - Prepara `1.1`, `2.1`, `3.5`, `4.1` y todas las tareas que añadan migraciones.
- Tareas completadas:
  - `0.5. Configurar PostgreSQL local y migraciones.`
- Siguiente tarea pendiente recomendada:
  - `0.6. Configurar cola de trabajos y cache.`
- Decisiones o aclaraciones relevantes:
  - Compose usa `postgis/postgis:17-3.5` fijada por digest y publica el puerto solo en `127.0.0.1`.
  - La autenticación local usa SCRAM-SHA-256 y un volumen persistente.
  - Flyway ejecuta migraciones antes de que Hibernate valide; `ddl-auto=validate` impide que Hibernate altere el esquema.
  - La migración `V1` no crea tablas de negocio: activa `postgis`, `pg_trgm` y `unaccent`.
  - Las conexiones Hikari ejecutan `SET TIME ZONE 'UTC'`. Esta medida se añadió porque la primera prueba real detectó que una sesión heredaba `Europe/Madrid` aunque el servidor estuviera en UTC.
  - Los tests backend requieren Docker y usan una base PostGIS efímera.
  - La verificación local sobre un volumen creado desde cero confirmó Flyway `V1`, las tres extensiones, UTF-8 y UTC.

## Conversación 17 - Redis, RabbitMQ y trabajos asíncronos

- Fecha: 2026-06-22
- Resumen: se configuraron Redis 8.8 y RabbitMQ 4.3 para desarrollo local y para la API Spring Boot. Redis queda disponible para caché, rate limiting y TTL auxiliares; RabbitMQ incorpora una topología compartida versionada, publisher confirms, publisher returns, reintentos de publicación y una cola durable de dead letters. Se añadieron tests de integración con contenedores reales y se verificó también el Compose local.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `package.json`
  - `scripts/validate-environment-examples.mjs`
  - `README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/cache-and-messaging.md`
  - `infrastructure/compose.yaml`
  - `infrastructure/README.md`
  - `apps/api/README.md`
  - `apps/api/pom.xml`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/application-local.yaml`
  - `apps/api/src/main/resources/application-test.yaml`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/CacheConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/cache/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingConfiguration.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/MessagingTopology.java`
  - `apps/api/src/main/java/com/reserly/platform/infrastructure/messaging/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-005 Escalabilidad`.
  - `RNF-006 Disponibilidad operativa`.
- Tareas impactadas:
  - `0.6. Configurar cola de trabajos y cache.`
  - Prepara `1.16`, `3.7`, `7.11`, `7.12`, `8.7`, `8.8`, `10.4`, `12.2` y las futuras tareas que requieran rate limiting, caché o ejecución asíncrona.
- Tareas completadas:
  - `0.6. Configurar cola de trabajos y cache.`
- Siguiente tarea pendiente recomendada:
  - `0.7. Crear layout base responsive y sistema de componentes.`
- Decisiones o aclaraciones relevantes:
  - Redis no es fuente de verdad y no puede decidir capacidad, permisos, penalizaciones ni pagos.
  - El TTL común de caché es de cinco minutos, los valores nulos están deshabilitados y las claves usan el prefijo `reserly::`.
  - RabbitMQ usa exchanges topic versionados y una cola de aparcamiento; cada módulo declarará su cola durable y routing key.
  - Los consumidores deberán ser idempotentes y los flujos que necesiten garantía entre PostgreSQL y RabbitMQ deberán implementar un outbox persistente.
  - La URI AMQP no debe incluir `/%2f` con Spring Boot 4.1; sin path se usa correctamente el vhost `/`.
  - Las imágenes oficiales de Redis y RabbitMQ están fijadas por versión y digest, protegidas con credenciales y publicadas solo en localhost.

## Conversación 18 - Layout responsive y componentes base

- Fecha: 2026-06-23
- Resumen: se integró Material UI con el App Router de Next.js 16 y se creó la base responsive reutilizable de Reserly. La experiencia pública dispone de cabecera de escritorio y navegación inferior móvil; el panel del local dispone de sidebar fijo en escritorio, cabecera compacta y navegación inferior móvil. Se añadieron primitivas de contenedor, encabezado, superficie y grid, una página de preview del panel, tests semánticos y validación visual en móvil, tablet y escritorio.
- Archivos modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/app/globals.css`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/providers.tsx`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/components/navigation-link.tsx`
  - `apps/web/src/components/layout/brand.tsx`
  - `apps/web/src/components/layout/index.ts`
  - `apps/web/src/components/layout/layout-system.test.tsx`
  - `apps/web/src/components/layout/page-container.tsx`
  - `apps/web/src/components/layout/page-heading.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/responsive-grid.tsx`
  - `apps/web/src/components/layout/surface.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/theme/base-theme.ts`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`, preparando un punto único para el idioma del documento.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
  - Pantallas mínimas de usuario final y local registrado.
- Tareas impactadas:
  - `0.7. Crear layout base responsive y sistema de componentes.`
  - Prepara `0.8`, `0.10`, todas las pantallas públicas, el panel del local y las validaciones responsive de la fase 15.
- Tareas completadas:
  - `0.7. Crear layout base responsive y sistema de componentes.`
- Siguiente tarea pendiente recomendada:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Decisiones o aclaraciones relevantes:
  - Se usan Material UI `9.1.2`, `@mui/material-nextjs` `9.1.1` y Emotion `11.14.x`.
  - `AppRouterCacheProvider` usa el adaptador específico `v16-appRouter` y capas CSS.
  - El breakpoint `md` de MUI, `900 px`, separa navegación móvil y escritorio.
  - El tema actual es deliberadamente estructural y provisional; `0.8` debe formalizar tokens, estados e iconografía.
  - La ruta `/panel-preview` es una demostración sin datos, se marca `noindex` y no sustituye al dashboard funcional.
  - El documento usa temporalmente `lang="es"` porque los textos visibles actuales están en español; la resolución dinámica se implementará en `0.11`.
  - La validación visual cubrió 320, 390, 768 y 1280 píxeles sin desbordamiento horizontal ni errores de consola.

## Conversación 19 - Adopción de GitFlow por fases

- Fecha: 2026-06-23
- Resumen: se sustituyó la estrategia anterior de ramas cortas por tarea por un GitFlow adaptado al plan del proyecto. A partir de esta decisión existirá una única rama de desarrollo por cada fase completa, `develop` será la rama permanente de integración y `main` quedará reservada para versiones promovidas a producción.
- Archivos modificados:
  - `requirements.md`
  - `design.md`
  - `tasks.md`
  - `conversation-tracking.md`
  - `technical-implementation.md`
- Requisitos impactados:
  - Nuevo `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RNF-005 Escalabilidad`, por la organización y trazabilidad del desarrollo.
- Tareas impactadas:
  - `0.2. Crear repositorio, estructura base y convenciones de ramas`, cuya decisión de ramas se corrige documentalmente sin cambiar su estado completado.
  - Todas las fases y tareas futuras, que deberán desarrollarse en la rama correspondiente a su fase.
- Tareas completadas:
  - Ninguna tarea nueva. Este cambio actualiza una convención transversal del proyecto.
- Siguiente tarea pendiente recomendada:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Decisiones o aclaraciones relevantes:
  - Ramas permanentes: `develop` para integración y `main` para producción.
  - Rama temporal principal de trabajo: `phase/<numero>-<descripcion>`, una por fase y creada desde `develop`.
  - Quedan prohibidas para el trabajo ordinario las ramas independientes por tarea, incluidas las variantes `task/*` y `codex/task-*`.
  - Las ramas de fase se integran en `develop` mediante pull request tras su verificación y documentación.
  - Las releases se promueven desde `develop` hacia `main`, opcionalmente mediante `release/<version>`.
  - Los hotfix parten de `main` y deben volver tanto a `main` como a `develop`.
  - Esta decisión sustituye expresamente la tomada en la conversación 13, donde `main` se había definido como única rama permanente.

## Conversación 20 - Sistema visual e implantación operativa de GitFlow

- Fecha: 2026-06-23
- Resumen: se implantó operativamente el GitFlow acordado creando y publicando `develop` y la rama única `phase/0-preparacion-proyecto`. En esa rama se completó la tarea `0.8` mediante tokens visuales semánticos, tema Material UI, estados accesibles, iconografía Lucide, un isotipo vectorial de Reserly y el catálogo vivo `/design-system`. La interfaz se validó en navegador real para móvil, tablet, escritorio y un viewport equivalente a zoom del 200 %.
- Archivos modificados:
  - `CONTRIBUTING.md`
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/README.md`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/app/design-system/page.tsx`
  - `apps/web/src/app/design-system/page.test.tsx`
  - `apps/web/src/components/layout/brand.tsx`
  - `apps/web/src/components/layout/page-container.tsx`
  - `apps/web/src/components/layout/page-heading.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/responsive-grid.tsx`
  - `apps/web/src/components/layout/surface.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/components/visual/index.ts`
  - `apps/web/src/components/visual/status-chip.tsx`
  - `apps/web/src/components/visual/status-chip.test.tsx`
  - `apps/web/src/theme/base-theme.ts`
  - `apps/web/src/theme/visual-tokens.ts`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `docs/architecture/visual-system.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-007 Usabilidad`.
  - `RNF-009 Internacionalización y localización`, al mantener contratos compatibles con textos localizados.
  - `RNF-012 Calidad lingüística y codificación UTF-8`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
  - Prepara todas las tareas posteriores de UI y la futura automatización de CI de `0.9`.
- Tareas completadas:
  - `0.8. Definir paleta, tipografía, estados visuales e iconografía.`
- Siguiente tarea pendiente recomendada:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
- Decisiones o aclaraciones relevantes:
  - `develop` se creó usando como punto inicial el estado integrado y verificado de las tareas `0.1` a `0.7`.
  - `phase/0-preparacion-proyecto` se creó desde `develop` y contendrá todas las tareas restantes de la Fase 0; no se crearán nuevas ramas por tarea.
  - `visual-tokens.ts` es la fuente de verdad para colores, radios, sombras y tipografía propios.
  - El tema MUI usa una escala base de `4 px`, controles de al menos `44 px` y overrides compartidos.
  - `StatusChip` comunica estado mediante texto, icono y color.
  - La iconografía se fija en `lucide-react 1.21.0`.
  - Los contrastes medidos de los chips visibles están entre `5.17:1` y `6.98:1`.
  - `/design-system` y `/panel-preview` son rutas internas `noindex`; no sustituyen pantallas funcionales.

## Conversación 21 - Pipeline CI con calidad, frontend y backend

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.9` creando el workflow de GitHub Actions para integración continua. El pipeline se ejecuta en pull requests hacia `develop` y `main`, en pushes a ramas GitFlow (`develop`, `main`, `phase/**`, `release/**`, `hotfix/**`) y manualmente. Se separaron los checks en `Quality`, `Frontend` y `Backend integration`, se añadió un script local `ci:check` para proteger el contrato mínimo del workflow y se documentaron las reglas de seguridad, caché, branch protection y validación local.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `scripts/validate-ci-workflow.mjs`
  - `docs/continuous-integration.md`
  - `package.json`
  - `README.md`
  - `CONTRIBUTING.md`
  - `docs/README.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-005 Escalabilidad`, por automatizar una integración reproducible del monorepo.
  - `RNF-006 Disponibilidad operativa`, por impedir integraciones sin build y tests.
  - `RNF-011 Convenciones backend y persistencia`, por ejecutar Checkstyle, Spotless y tests de migraciones.
  - `RNF-012 Calidad lingüística y codificación UTF-8`, por mantener Prettier y validaciones documentales dentro de la cadena.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`, por alinear eventos y checks con `develop`, `main` y ramas de fase.
- Tareas impactadas:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
  - Prepara la protección operativa de las tareas `0.10` a `0.15` y de las fases posteriores.
- Tareas completadas:
  - `0.9. Crear pipeline CI con tests y validación de estilo.`
- Siguiente tarea pendiente recomendada:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
- Decisiones o aclaraciones relevantes:
  - El workflow usa permisos mínimos `contents: read` y `persist-credentials: false`.
  - No se usan `pull_request_target`, `workflow_run`, secretos ni entornos reales de staging/producción.
  - `Quality` concentra formato, lint y contratos de configuración; `Frontend` concentra TypeScript, Vitest y build Next.js; `Backend integration` concentra JUnit, Flyway, Testcontainers y build Spring Boot.
  - `npm run verify` incluye ahora `npm run ci:check` para detectar cambios peligrosos o incompletos en el workflow antes de abrir PR.
  - La protección de ramas debe configurarse en GitHub con los checks `Quality`, `Frontend` y `Backend integration` como obligatorios cuando el repositorio remoto lo permita.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 22 - Infraestructura i18n con catálogos ES/EN

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.10` creando la infraestructura i18n frontend con `next-intl`, catálogos versionados `es` y `en`, configuración request-scoped, proveedor en el layout raíz, tipos de mensajes/locales, documentación operativa y tests de paridad de claves. Las páginas actuales `/`, `/design-system` y `/panel-preview`, junto con los shells público y de panel, consumen ya textos desde catálogos. La resolución dinámica por preferencia, parámetro seguro, navegador/app y fallback queda preparada para `0.11`.
- Archivos modificados:
  - `apps/web/package.json`
  - `package-lock.json`
  - `apps/web/next.config.ts`
  - `apps/web/README.md`
  - `apps/web/locales/es.json`
  - `apps/web/locales/en.json`
  - `apps/web/src/global.d.ts`
  - `apps/web/src/i18n/config.ts`
  - `apps/web/src/i18n/request.ts`
  - `apps/web/src/i18n/messages.test.ts`
  - `apps/web/src/test-utils/render-with-intl.tsx`
  - `apps/web/src/app/layout.tsx`
  - `apps/web/src/app/providers.tsx`
  - `apps/web/src/app/page.tsx`
  - `apps/web/src/app/page.test.tsx`
  - `apps/web/src/app/design-system/page.tsx`
  - `apps/web/src/app/design-system/page.test.tsx`
  - `apps/web/src/app/panel-preview/page.tsx`
  - `apps/web/src/components/layout/public-shell.tsx`
  - `apps/web/src/components/layout/venue-shell.tsx`
  - `apps/web/src/components/layout/layout-system.test.tsx`
  - `docs/README.md`
  - `docs/architecture/frontend-layout.md`
  - `docs/architecture/internationalization.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`, por continuar el trabajo dentro de `phase/0-preparacion-proyecto`.
- Tareas impactadas:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
  - Prepara `0.11`, `0.12`, `0.15`, `1.21`, `3.14`, `8.14`, `10.16` y las futuras pantallas con textos localizados.
- Tareas completadas:
  - `0.10. Crear infraestructura i18n con catálogos es y en.`
- Siguiente tarea pendiente recomendada:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
- Decisiones o aclaraciones relevantes:
  - Se usa `next-intl 4.13.0`, instalado con versión exacta y cero vulnerabilidades conocidas en `npm audit`.
  - Los catálogos viven en `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `src/i18n/request.ts` usa locale estático `es` temporalmente para no mezclar el alcance de `0.10` con la resolución dinámica de `0.11`.
  - El fallback operativo declarado es `en`.
  - `src/global.d.ts` augmenta `next-intl` con locales y mensajes tipados.
  - `messages.test.ts` valida que ambos catálogos tienen las mismas claves y que el catálogo español conserva caracteres críticos como `É`, `á`, `ó`, `ñ` y `Más`.
  - La detección automática de textos hardcodeados queda para `0.12`; la validación profunda de mojibake y calidad lingüística queda para `0.15`.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 23 - Resolución dinámica de idioma

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.11` implementando la resolución dinámica de idioma del frontend. La web deja de usar locale estático y calcula el idioma efectivo mediante cookie de preferencia `reserly-locale`, parámetro público seguro `locale`/`lang`, cabecera de app `x-reserly-app-locale`, cabecera `Accept-Language` y fallback `en`. Se añadió un `proxy.ts` de Next.js para normalizar parámetros y persistir la preferencia, un módulo puro de resolución con tests unitarios, y documentación operativa actualizada.
- Archivos modificados:
  - `apps/web/proxy.ts`
  - `apps/web/README.md`
  - `apps/web/src/i18n/config.ts`
  - `apps/web/src/i18n/locale-resolution.ts`
  - `apps/web/src/i18n/locale-resolution.test.ts`
  - `apps/web/src/i18n/messages.test.ts`
  - `apps/web/src/i18n/request.ts`
  - `docs/architecture/internationalization.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RB-011 Resolución de idioma`.
- Tareas impactadas:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
  - Prepara `0.12`, `0.15`, `19.6`, `19.7` y `19.8`.
- Tareas completadas:
  - `0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback en.`
- Siguiente tarea pendiente recomendada:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Decisiones o aclaraciones relevantes:
  - `defaultLocale` pasa a `en` para que el fallback operativo coincida con el contrato de producto.
  - Las preferencias persistidas solo aceptan valores exactos `es` o `en`.
  - Los parámetros públicos `locale` y `lang` aceptan únicamente tags acotados, con longitud máxima y caracteres permitidos; variantes `es-*` resuelven a `es` y el resto a `en`.
  - `Accept-Language` se interpreta respetando calidad `q` y orden; si el idioma preferido no empieza por `es`, el resultado es `en`.
  - No se añadió selector visual de idioma; el cambio manual queda disponible mediante URL hasta que exista UI específica.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot. La primera ejecución sin permisos elevados falló al descargar dependencias de Maven por bloqueo de red del sandbox; la ejecución aprobada con red pasó correctamente.

## Conversación 24 - Push obligatorio a GitHub al cerrar cada tarea

- Fecha: 2026-06-23
- Resumen: se actualizó la especificación `.kiro` para exigir que, al terminar cada tarea, los cambios se consoliden en un commit trazable y se suban al repositorio remoto de GitHub en la rama de fase correspondiente antes de iniciar la siguiente tarea.
- Archivos modificados:
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
- Requisitos impactados:
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - Todas las tareas futuras de `tasks.md`, porque el criterio de cierre operativo ahora incluye commit y push a GitHub.
- Tareas completadas:
  - Ninguna; es una actualización de proceso y documentación, no el cierre de una tarea funcional del plan.
- Siguiente tarea pendiente recomendada:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Decisiones o aclaraciones relevantes:
  - La rama de trabajo sigue siendo la rama de fase, no una rama por tarea.
  - Cada tarea cerrada debe dejar la rama local alineada con `origin/<rama-de-fase>`.
  - Si el push falla por autenticación, red o permisos, debe tratarse como bloqueo operativo y resolverse antes de empezar la siguiente tarea.

## Conversación 25 - Validación automática i18n y textos hardcodeados

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.12` añadiendo `npm run i18n:check`, un validador AST de interfaz que comprueba la paridad de claves entre `apps/web/locales/es.json` y `apps/web/locales/en.json`, detecta texto visible hardcodeado en componentes TSX y verifica claves estáticas usadas con `useTranslations` y `getTranslations`. El check se integró en `npm run verify`, en el workflow de GitHub Actions y en el contrato local `ci:check`.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `README.md`
  - `apps/api/src/test/java/com/reserly/platform/infrastructure/InfrastructureServicesIntegrationTests.java`
  - `apps/web/src/components/layout/brand.tsx`
  - `docs/architecture/internationalization.md`
  - `docs/continuous-integration.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-i18n.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
  - Prepara `0.13`, `0.15`, `1.21`, `3.14`, `8.14`, `10.16`, `16.14` y las validaciones de aceptación i18n de la fase 19.
- Tareas completadas:
  - `0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.`
- Siguiente tarea pendiente recomendada:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node/TypeScript AST reutilizable para ejecutarse localmente, en `verify` y en CI sin depender de un plugin ESLint propio todavía.
  - El alcance del detector de hardcoded UI cubre `.tsx` no test bajo `apps/web/src`; emails, backend, seeds, migraciones y plantillas quedan para validaciones futuras, especialmente `0.15`.
  - La marca visible `Reserly` en `brand.tsx` pasa a leerse desde `Brand.name`, evitando una excepción manual al contrato i18n.
  - El test de integración de infraestructura backend se estabilizó con espera acotada al leer la caché Redis para eliminar una condición intermitente observada durante `npm run verify`.
  - Evidencia de verificación final: `npm run verify` completó correctamente `ci:check`, `env:check`, `i18n:check`, ESLint, Checkstyle, Prettier, Spotless, TypeScript, Vitest, JUnit con Testcontainers y los builds de Next.js y Spring Boot.

## Conversación 26 - Patrón de textos localizados persistidos

- Fecha: 2026-06-23
- Resumen: se completó la tarea `0.13` definiendo el patrón de datos localizados en base de datos para textos configurables y visibles fuera de catálogos estáticos. El patrón usa columnas conceptuales `*_i18n` traducidas físicamente a `lowerCamelCase`, JSONB con `sourceLocale` y `values.es`/`values.en`, reglas de publicación, fallback controlado y el value object backend `LocalizedText`.
- Archivos modificados:
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
  - `apps/api/README.md`
  - `apps/api/src/main/java/com/reserly/platform/localization/LocalizedText.java`
  - `apps/api/src/main/java/com/reserly/platform/localization/SupportedLocale.java`
  - `apps/api/src/main/java/com/reserly/platform/localization/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/localization/LocalizedTextTests.java`
  - `docs/README.md`
  - `docs/architecture/internationalization.md`
  - `docs/architecture/localized-data.md`
- Requisitos impactados:
  - `RF-031 Internacionalización de textos`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
  - Prepara `2.3`, `2.5`, `2.14`, `2.15`, `3.14`, `6.11`, `6.12`, `8.2` a `8.6`, `10.16`, `13.2`, `14.10` y las validaciones i18n de aceptación.
- Tareas completadas:
  - `0.13. Definir patrón para textos localizados en base de datos mediante campos *_i18n o JSON { es, en }.`
- Siguiente tarea pendiente recomendada:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
- Decisiones o aclaraciones relevantes:
  - El JSONB canónico guarda `sourceLocale` y `values` para cumplir el requisito de almacenar idioma origen y traducciones `es`/`en`.
  - La convención `*_i18n` queda como nombre conceptual; las columnas físicas deben usar `lowerCamelCase`, por ejemplo `"descriptionI18n"`.
  - La publicación de contenido público debe exigir traducciones completas `es` y `en`, salvo fallback explícito documentado.
  - El fallback visible se resuelve como locale solicitado, `en` y después `sourceLocale`.
  - `LocalizedText` no traduce automáticamente; valida y resuelve contenido ya aportado.
  - Evidencia de verificación final: se ejecutó `LocalizedTextTests` correctamente; la verificación completa del repositorio se ejecutó tras iniciar Docker Desktop para habilitar Testcontainers.

## Conversación 27 - Convenciones backend automatizadas

- Fecha: 2026-06-24
- Resumen: se completó la tarea `0.14` incorporando una validación automática de convenciones backend mediante `npm run backend:conventions:check`. El nuevo validador revisa clases Java `UpperCamelCase`, entidades JPA con tablas `UpperCamelCase`, columnas y atributos persistidos `lowerCamelCase`, relaciones JPA declaradas en getters con setter correspondiente, DAOs con consultas propias anotadas con `@Query`, separación de interfaces e implementaciones para servicios/controladores, DTOs REST con sufijos explícitos, conversores y migraciones Flyway con identificadores físicos entrecomillados. El check se integró en `npm run verify`, en GitHub Actions y en el contrato `ci:check`.
- Archivos modificados:
  - `.github/workflows/ci.yml`
  - `README.md`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/continuous-integration.md`
  - `docs/architecture/backend-conventions.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-backend-conventions.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RNF-001 Rendimiento y escalabilidad`, por reforzar convenciones de persistencia previsibles para futuras consultas.
  - `RNF-003 Seguridad`, por mantener contratos backend explícitos y revisables.
- Tareas impactadas:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
  - Prepara `1.1`, `1.2`, `1.3`, `1.4`, `1.6`, `2.3`, `2.5`, `3.1`, `3.2`, `6.1`, `8.1`, `10.1`, `13.1`, `14.1` y cualquier tarea futura que cree entidades, migraciones, DAOs, servicios, controladores, DTOs o conversores.
- Tareas completadas:
  - `0.14. Definir y automatizar convenciones backend: tablas UpperCamelCase, clases Java UpperCamelCase, atributos lowerCamelCase, JPA por getters/setters, DAOs con @Query, interfaces separadas de servicios/controladores, DTOs REST y conversores.`
- Siguiente tarea pendiente recomendada:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node estático para ejecutarse de forma rápida en local y CI sin introducir dependencias nuevas.
  - Las entidades JPA deben usar `Entity` como sufijo y declarar `@Table(name = "\"UpperCamelCase\"")`.
  - Las relaciones JPA se validan sobre getters, permitiendo anotaciones intermedias como `@JoinColumn` antes del método `get*`.
  - Las columnas `@Column` y `@JoinColumn` deben declararse con nombres físicos quoted `lowerCamelCase`.
  - Los servicios y controladores concretos deben terminar en `ServiceImpl` y `ControllerImpl`, con interfaz hermana `Service` o `Controller`.
  - Los DAOs propios deben expresar consultas mediante `@Query` para evitar métodos derivados opacos en lógica sensible.
  - Evidencia de verificación final: `npm run backend:conventions:check`, `npm run ci:check`, `npm run format:check:web`, `git diff --check` y `npm run verify` se ejecutaron correctamente antes de commit y push.

## Conversación 28 - Validación UTF-8 y calidad de textos españoles

- Fecha: 2026-06-24
- Resumen: se completó la tarea `0.15` añadiendo `npm run spanish:text:check`, un validador de codificación UTF-8 estricta, mojibake, signos de apertura y tildes frecuentes en textos españoles versionados. El check cubre catálogos, documentación, `.kiro`, plantillas de entorno, recursos backend y futuras rutas de plantillas, seeds o fixtures. Se corrigió el mojibake real detectado en `.env.local.example` y el nuevo check quedó integrado en `npm run verify`, en GitHub Actions y en el contrato `ci:check`.
- Archivos modificados:
  - `.env.local.example`
  - `.github/workflows/ci.yml`
  - `README.md`
  - `docs/README.md`
  - `docs/continuous-integration.md`
  - `docs/architecture/internationalization.md`
  - `docs/architecture/spanish-text-quality.md`
  - `package.json`
  - `scripts/validate-ci-workflow.mjs`
  - `scripts/validate-spanish-text.mjs`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RNF-009 Internacionalización y localización`.
  - `RNF-012 Calidad lingüística, acentos y codificación de textos en español`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
  - Prepara `1.21`, `2.3`, `3.14`, `8.2` a `8.6`, `10.16`, `14.10`, `16.14`, `19.8` y `19.29`.
- Tareas completadas:
  - `0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.`
- Siguiente tarea pendiente recomendada:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
- Decisiones o aclaraciones relevantes:
  - La validación se implementa como script Node sin dependencias nuevas para ejecutarse rápido en local y CI.
  - El script usa `TextDecoder` con `fatal: true` para detectar bytes no UTF-8.
  - La detección de mojibake ignora ejemplos documentales dentro de código inline Markdown para permitir explicar secuencias inválidas sin romper el check.
  - La revisión de tildes es conservadora y cubre palabras frecuentes del proyecto; no sustituye revisión humana de textos finales.
  - La Fase 0 queda completada en `tasks.md`; la siguiente tarea recomendada inicia la Fase 1.
  - Evidencia de verificación final: `npm run spanish:text:check`, `npm run ci:check`, `npm run format:check:web`, `git diff --check` y `npm run verify` se ejecutaron correctamente antes de commit y push.

## Conversación 29 - Persistencia base de identidad, roles, sesiones y tokens

- Fecha: 2026-06-28
- Resumen: se completó la tarea `1.1` creando mediante Flyway las tablas físicas `"Users"`, `"Roles"`, `"UserRoles"`, `"AuthSessions"` y `"AuthTokens"`, con columnas `lowerCamelCase`, UUIDs, claves foráneas, checks, índices únicos y parciales, cascadas y catálogo inicial de roles. Se añadieron cinco entidades JPA, cinco DAOs, pruebas de integración sobre PostgreSQL 17 y documentación operativa profunda. Los secretos de sesión, verificación y recuperación se almacenan únicamente como hashes SHA-256 hexadecimales. `accountType` queda expresamente reservado para `1.2`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V2__create_identity_role_session_and_token_tables.sql`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserRoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthSessionDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AuthTokenDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/identity-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-001 Identidad del usuario final`.
- Tareas impactadas:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
  - Prepara `1.2`, `1.12`, `1.13`, `1.14`, `1.15` y `1.17`.
- Tareas completadas:
  - `1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos UpperCamelCase y atributos/columnas lowerCamelCase.`
- Siguiente tarea pendiente recomendada:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
- Decisiones o aclaraciones relevantes:
  - `anonymous` no se persiste porque representa ausencia de autenticación y el usuario final del MVP no necesita cuenta.
  - Los roles asignables iniciales son `venue_owner`, `admin` y `employee_user`.
  - Rol y tipo de cuenta son conceptos separados; `"accountType"` se implementará en `1.2`.
  - Sesiones y tokens solo almacenan hashes SHA-256 hexadecimales, nunca secretos en claro.
  - No se recopilan IP ni user agent de sesión sin una necesidad funcional y legal definida.
  - Los índices parciales cubren exclusivamente credenciales activas.
  - Flyway alcanza la versión `2` y Hibernate valida las cinco entidades.
  - Evidencia de cierre: `npm run verify` correcto; 22 tests frontend y 19 tests backend sin fallos, migración sobre PostgreSQL 17, integración con Redis/RabbitMQ y builds de Next.js/Spring Boot correctos.

## Conversación 30 - Tipo de cuenta cerrado y tipado

- Fecha: 2026-06-28
- Resumen: se completó la tarea `1.2` añadiendo `"accountType"` a `"Users"` mediante Flyway V3, con valores cerrados `customer`, `venue_business` y `admin`, nulabilidad prohibida y `customer` como default seguro. Se creó el enum de dominio `AccountType`, un conversor JPA estricto y pruebas unitarias e integración que validan el catálogo, el default, la escritura/lectura JPA y el rechazo PostgreSQL de valores desconocidos.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V3__add_account_type_to_users.sql`
  - `apps/api/src/main/java/com/reserly/platform/identity/AccountType.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/AccountTypeConverter.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserEntity.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/AccountTypeTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/AccountTypeConverterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/persistence/IdentityPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/architecture/identity-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
  - Prepara `1.3`, `1.4`, `1.11`, `1.17`, `2.9` y `14.1`.
- Tareas completadas:
  - `1.2. Implementar account_type con valores customer, venue_business y admin.`
- Siguiente tarea pendiente recomendada:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
- Decisiones o aclaraciones relevantes:
  - `customer` es el default fail-closed; nunca se asignan privilegios empresariales o administrativos por omisión.
  - El registro de local deberá establecer `venue_business` explícitamente desde el caso de uso, no confiar en un valor arbitrario del cliente.
  - Las cuentas `admin` deberán provisionarse mediante un flujo interno restringido y auditable.
  - Tipo de cuenta y rol son independientes: el tipo activa invariantes y verificaciones; los roles autorizan acciones.
  - Los valores técnicos son canónicos y no se traducen; las etiquetas visibles futuras sí usarán i18n.
  - Evidencia de cierre: suite dirigida con 14 tests correcta; `npm run verify` correcto con 22 tests frontend y 26 tests backend, Flyway V3 sobre PostgreSQL 17, integración Redis/RabbitMQ y ambos builds.

## Conversación 31 - Persistencia de verificación empresarial y documentos privados

- Fecha: 2026-06-28
- Resumen: se completó `1.3` creando Flyway V4 con `"BusinessAccounts"`, `"BusinessVerificationChecks"` y `"BusinessVerificationDocuments"`. El esquema aplica unicidad fiscal por país, estados iniciales seguros, evidencia remota mínima, hashes SHA-256, referencias idempotentes, localizadores documentales privados, actor/fecha en decisiones y borrado restringido. Se añadieron tres entidades JPA, tres DAOs, diez pruebas empresariales de integración y documentación profunda.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V4__create_business_verification_tables.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/business-verification-persistence.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
  - Prepara `1.4` a `1.11`, `1.19`, `1.22`, `2.9`, `14.6` a `14.8` y `14.14`.
- Tareas completadas:
  - `1.3. Crear tablas business_accounts, business_verification_checks y business_verification_documents.`
- Siguiente tarea pendiente recomendada:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
- Decisiones o aclaraciones relevantes:
  - El estado inicial empresarial es `unverified`; la máquina de estados se implementará en `1.8`.
  - La unicidad se aplica sobre país e identificador normalizado; la normalización real queda para `1.5`.
  - Los checks no guardan respuestas remotas completas, solo resultado, referencia y hash opcional.
  - `"fileUrl"` conserva el nombre histórico pero es un object key privado; PostgreSQL rechaza URLs HTTP persistentes.
  - PostgreSQL no almacena binarios documentales.
  - Propietarios, revisores y cuentas con evidencias no pueden eliminarse mediante una operación parcial.
  - Evidencia de cierre: suite dirigida con 12 tests correcta; `npm run verify` correcto con 22 tests frontend y 36 tests backend, Flyway V4 sobre PostgreSQL 17, Redis/RabbitMQ y ambos builds.

## Conversación 32 - Registro transaccional de cuentas de local

- Fecha: 2026-06-28
- Resumen: se completó `1.4` implementando `POST /api/auth/venues/register`. El caso de uso valida el payload, fija privilegios en backend, normaliza email, aplica normalización fiscal provisional, genera BCrypt con coste 12 y crea en una transacción el usuario, la identidad empresarial y el rol propietario. Los conflictos son genéricos, la publicación queda bloqueada y el contrato se separa expresamente de la creación del perfil de local de la Fase 2.
- Archivos modificados:
  - `apps/api/pom.xml`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/RoleDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/persistence/UserDao.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/VenueRegistrationController.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/VenueRegistrationControllerImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/RegistrationExceptionHandler.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/controller/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/converter/VenueRegistrationConverter.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/converter/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationCommand.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/VenueRegistrationResponse.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/RegistrationErrorResponse.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/dto/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/PasswordHashingService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/PasswordHashingServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/RegistrationConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/RegistrationValidationException.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/service/PasswordHashingServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/service/package-info.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/venue-registration.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
  - Prepara `1.5`, `1.6`, `1.8`, `1.11`, `1.12`, `1.14`, `1.16`, `1.17`, `1.18`, `1.21`, `1.22` y la Fase 2.
- Tareas completadas:
  - `1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.`
- Siguiente tarea pendiente recomendada:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
- Decisiones o aclaraciones relevantes:
  - El registro de Fase 1 crea cuenta e identidad empresarial, no un perfil `Venue`; ese modelo comienza en Fase 2.
  - `accountType`, rol y estados se fijan en backend y no pueden ser elegidos por el cliente.
  - El usuario arranca en `pending_email_verification` y la empresa en `unverified`; no se declara una comprobación remota que aún no ocurrió.
  - La publicación queda cerrada mediante `canPublishVenue=false`.
  - BCrypt con coste 12 se incorpora como mínimo seguro para no almacenar secretos en claro; `1.12` sigue pendiente para completar verificación, política configurable y rehash.
  - La normalización fiscal de `1.4` es deliberadamente provisional: trim y mayúsculas. Formato, separadores y dígito de control pertenecen a `1.5`.
  - Los duplicados de email o identidad fiscal responden el mismo `409 REGISTRATION_CONFLICT`, incluidos conflictos de carrera detectados por PostgreSQL.
  - Evidencia de cierre: 6 pruebas dirigidas correctas; `npm run verify` correcto con 22 tests frontend y 42 tests backend, Flyway V4 sobre PostgreSQL 17, Redis/RabbitMQ y ambos builds.

## Conversación 33 - Normalización y validación local de identificadores empresariales

- Fecha: 2026-06-28
- Resumen: se completó `1.5` creando una frontera de dominio extensible para normalizar identificadores fiscales y aplicar reglas locales por país antes de consultar unicidad. La estrategia española reconoce DNI/NIF, NIE, NIF especiales `K/L/M`, NIF de entidades y la representación NIF-IVA con prefijo `ES`; valida formato y carácter de control y produce una clave nacional sin separadores. Los países sin estrategia aplican canonicalización segura, pero declaran explícitamente que formato y control no han sido comprobados. El registro usa únicamente esta clave para precheck y persistencia, por lo que variantes visuales equivalentes colisionan contra el índice único existente.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierScheme.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/NormalizedBusinessTaxIdentifier.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/CountryBusinessTaxIdentifierValidator.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/SpanishBusinessTaxIdentifierValidator.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/validation/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationService.java`
  - `apps/api/src/main/java/com/reserly/platform/identity/service/VenueRegistrationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/validation/BusinessTaxIdentifierValidationServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/validation/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/architecture/business-tax-identifiers.md`
  - `docs/architecture/venue-registration.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
  - Prepara `1.6`, `1.7`, `1.8`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.`
- Siguiente tarea pendiente recomendada:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
- Decisiones o aclaraciones relevantes:
  - La canonicalización común usa NFKC, mayúsculas con locale neutro y solo elimina espacios, guion, punto y barra; cualquier otra puntuación o carácter no ASCII se rechaza para evitar colisiones ambiguas.
  - `taxCountry` forma parte de la clave única. En España el prefijo NIF-IVA `ES` se acepta como representación, pero se elimina del valor nacional canónico.
  - La validación española cubre persona física, NIE, NIF especiales y entidades; no acredita emisión, titularidad, alta censal ni ROI/VIES.
  - Los países sin estrategia no se bloquean: se normalizan con esquema `GENERIC` y garantías locales en `false`, manteniendo la cuenta en `unverified`.
  - El índice único de V4 continúa siendo la autoridad concurrente. No se añade V5 porque la columna canónica y la restricción ya existen, y el checksum nacional debe tener una sola implementación versionable en dominio.
  - Los errores no contienen el identificador aportado y el endpoint conserva el contrato genérico `400 REGISTRATION_INVALID`.
  - Se corrigieron fixtures y ejemplos previos que usaban `B12345678`, cuyo control es inválido, por `B12345674`.
  - Evidencia de cierre: 22 pruebas unitarias específicas y 6 de integración de registro correctas; `npm run verify` correcto con 22 tests frontend y 65 backend, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 34 - Infraestructura de adaptadores remotos por país y proveedor

- Fecha: 2026-06-28
- Resumen: se completó `1.6` implementando el puerto de adaptadores remotos, un registro validado con selección determinista por país/proveedor, un gateway con timeouts entregados al adaptador, watchdog total sobre hilos virtuales, backoff exponencial y reintentos solo para errores transitorios. El caso de uso interno carga los datos fiscales desde PostgreSQL, evita mantener transacciones abiertas durante la red y persiste evidencia mínima. Flyway V5 añade `requestId`, número de intentos y duración; repetir el mismo request reutiliza el check y usarlo para otra cuenta se rechaza. No se conecta aún a VIES/AEAT ni se cambia el estado empresarial.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/db/migration/V5__add_remote_verification_execution_metadata.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationCheckDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapterRegistry.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationResult.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationAttemptContext.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationCallExecutor.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationErrorCode.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecution.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationExecutionException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationInvocation.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationSleeper.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteVerificationStatus.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/ThreadRemoteVerificationSleeper.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/VirtualThreadRemoteVerificationCallExecutor.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/NoRemoteVerificationAdapterException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessAccountNotFoundException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationCommand.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationOutcome.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteVerificationRequestConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
  - Prepara `1.7`, `1.8`, `1.9`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.`
- Siguiente tarea pendiente recomendada:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
- Decisiones o aclaraciones relevantes:
  - La tarea implementa la infraestructura ejecutable y el contrato de adaptadores; VIES, AEAT y la selección España/UE concreta permanecen en `1.7`.
  - Los adaptadores declaran código, países y prioridad. La selección automática favorece la prioridad menor y una preferencia explícita nunca cae silenciosamente a otro proveedor.
  - Cada intento recibe timeouts de conexión y lectura; un watchdog adicional limita el total a la suma de ambos.
  - Solo timeout, indisponibilidad y rate limit se reintentan. Autenticación, protocolo, respuesta inválida y ausencia de adaptador terminan inmediatamente.
  - La clave idempotente es SHA-256 de proveedor y `requestId`, estable entre reintentos y opaca para el tercero.
  - V5 hace `requestId` único, limita intentos a cinco y exige duración no negativa. Filas históricas reciben su propio ID como request.
  - El servicio carga país, identificador, razón social y dirección desde PostgreSQL; el comando no permite sustituir datos fiscales.
  - No se mantiene una transacción abierta durante la red y no se actualiza aún `businessVerificationStatus`.
  - No se guardan cuerpos, mensajes remotos, URLs, credenciales ni excepciones; solo códigos y claves i18n controladas.
  - Evidencia de cierre: pruebas dirigidas con gateway, migración, persistencia y servicio correctas; `npm run verify` correcto con 22 tests frontend y 75 backend, Flyway V5, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 35 - Validación inicial de empresas de España y la UE

- Fecha: 2026-06-28
- Resumen: se completó `1.7` con un adaptador SOAP real para VIES, selección semántica entre NIF español nacional y NIF-IVA, comparación tolerante de razón social y dirección y una degradación segura para la comprobación censal AEAT. VIES recibe solo país y número VAT; su XML se limita, analiza de forma segura y reduce a evidencia mínima. Un NIF español sin prefijo `ES` no se presupone inscrito en ROI y produce resultado inconcluso sin automatizar la sede electrónica. La tarea no cambia todavía el estado empresarial.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/matching/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationAdapterRegistry.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationRequest.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/aeat/AeatCensusManualReviewAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/aeat/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/ViesBusinessVerificationAdapter.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/ViesProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/remote/vies/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/EuropeanVatIdentifierPolicy.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/matching/BusinessIdentityMatchingServiceTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/matching/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/RemoteBusinessVerificationGatewayTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/aeat/AeatCensusManualReviewAdapterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/aeat/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/vies/ViesBusinessVerificationAdapterTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/remote/vies/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/EuropeanVatIdentifierPolicyTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/README.md`
  - `docs/configuration.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
  - Prepara `1.8`, `1.9`, `1.11`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.`
- Siguiente tarea pendiente recomendada:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
- Decisiones o aclaraciones relevantes:
  - En España, solo un identificador aportado con prefijo explícito `ES` se enruta a VIES; la canonicalización local elimina el prefijo, por lo que la política consulta también el valor original persistido.
  - Un NIF español nacional ya validado localmente se enruta a `aeat-census-manual`, que no realiza red y devuelve `INCONCLUSIVE`. No se raspa ni automatiza la sede electrónica.
  - Los demás territorios VIES soportados se tratan inicialmente como VAT ID mientras no exista un adaptador registral nacional específico. Grecia se traduce de `GR` a `EL` en el límite SOAP.
  - VIES recibe exclusivamente código de país y número VAT. Razón social, dirección e ID interno no salen de Reserly.
  - El parser XML rechaza DTD y entidades externas, limita el cuerpo y valida país, número y booleano devueltos.
  - La respuesta remota no se persiste. Solo se guardan resultado técnico, coincidencias opcionales y hash SHA-256.
  - Las comparaciones toleran diacríticos y puntuación mediante similitud Levenshtein configurable; dato ausente produce `null`, no una coincidencia.
  - Los faults transitorios VIES se integran con los reintentos de `1.6`; protocolos o respuestas inválidas no se reintentan.
  - No se envía una cabecera de idempotencia inventada: VIES no la documenta y la protección local por `requestId` permanece activa.
  - `1.7` no actualiza `businessVerificationStatus`; esa política se implementará en `1.8`.
  - Evidencia de cierre: pruebas focalizadas correctas y `npm run verify` correcto con 22 tests frontend y 88 backend, Flyway V1–V5, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 36 - Máquina de estados de verificación empresarial

- Fecha: 2026-06-28
- Resumen: se completó `1.8` implementando una máquina de estados transaccional para la identidad empresarial. Cada comprobación reserva la cuenta con `pending_remote_check` y un `requestId` activo, ejecuta la red fuera de transacción y aplica únicamente evidencia correlacionada. Una confirmación oficial coherente produce `verified`; invalidez produce `rejected`; inconclusión, error o discrepancia producen `pending_review`. V6 añade caducidad persistida y un índice para convertir aprobaciones vencidas en `expired`.
- Archivos modificados:
  - `.env.local.example`
  - `.env.staging.example`
  - `.env.production.example`
  - `apps/api/src/main/resources/application.yaml`
  - `apps/api/src/main/resources/db/migration/V6__add_business_verification_state_metadata.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStatus.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateProperties.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateSnapshot.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationInProgressException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateConflictException.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationOutcome.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceImpl.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/configuration.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
  - Prepara `1.9`, `1.11`, `1.21`, `1.22`, `2.9` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.8. Implementar estados pending_remote_check, verified, pending_review, rejected y expired.`
- Siguiente tarea pendiente recomendada:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
- Decisiones o aclaraciones relevantes:
  - `BusinessVerificationStatus` es el catálogo de dominio; los valores persistidos permanecen en minúsculas.
  - `activeVerificationRequestId` es obligatorio únicamente en `pending_remote_check`. Cuenta, request activo, request del check y estado deben coincidir para cerrar la operación.
  - Un lock pesimista serializa cada transición. `REQUIRES_NEW` limita la transacción al inicio o al cierre; VIES nunca se ejecuta con una transacción o lock abiertos.
  - Una segunda comprobación mientras existe otra activa falla de forma controlada y sin exponer IDs.
  - Para verificar se exige `matchedLegalName = true`; si existe dirección aportada también se exige `matchedAddress = true`. Ausencia o discrepancia deriva a revisión.
  - `invalid` se traduce a `rejected`; `inconclusive` y `error` se traducen a `pending_review`.
  - La vigencia automática predeterminada es 365 días y se valida entre 1 y 730 días.
  - V6 migra aprobaciones históricas a 365 días desde `businessVerifiedAt`, exige una ventana positiva y crea un índice parcial de caducidad.
  - `expireDueVerifications` realiza un update en bloque sin cargar identificadores fiscales. La futura planificación periódica queda como integración operativa posterior.
  - Los tests de estado confirman commits reales y limpian por IDs creados para no contaminar otras clases.
  - Evidencia de cierre: pruebas focalizadas conjuntas correctas con 28 tests; `npm run verify` correcto con 22 tests frontend y 93 backend, Flyway V1–V6, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.
  - Tras la suite integral se añadió una regresión para limpiar actor y fecha de una decisión manual anterior al revalidar; las 9 pruebas focalizadas del servicio pasaron. No se repitió `npm run verify` porque la ejecución adicional no fue autorizada.

## Conversación 37 - Solicitud automática de documentación de respaldo

- Fecha: 2026-06-29
- Resumen: se completó `1.9` creando un requerimiento documental persistente, idempotente y separado de los ficheros. V7 añade `"BusinessVerificationDocumentRequests"` con check origen, motivo cerrado, tipos admitidos, estado y timestamps. La máquina de estados lo crea en la misma transacción que `pending_review`; `verified` y `rejected` no generan solicitudes. Una revalidación cancela la solicitud abierta antes de consultar de nuevo. No se implementa aún carga, almacenamiento ni endpoint público.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V7__create_business_verification_document_requests.sql`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentRequestEntity.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessVerificationDocumentRequestDao.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/package-info.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentType.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestReason.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestSnapshot.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestService.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestPolicy.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/BusinessVerificationStateServiceImpl.java`
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/BusinessVerificationDocumentRequestPolicyTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/RemoteBusinessVerificationServiceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/businessverification/persistence/BusinessVerificationPersistenceIntegrationTests.java`
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`
  - `apps/api/README.md`
  - `docs/architecture/business-verification-persistence.md`
  - `docs/architecture/remote-business-verification.md`
  - `.kiro/specs/plataforma-reservas-saas/design.md`
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
  - Prepara `1.10`, `1.19`, `1.21`, `1.22` y `14.6` a `14.8`.
- Tareas completadas:
  - `1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.`
- Siguiente tarea pendiente recomendada:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de actividad/apertura o documento equivalente.`
- Decisiones o aclaraciones relevantes:
  - Solicitud y documento son agregados diferentes; V7 no incluye binario, object key ni URL.
  - Cada check puede originar una sola solicitud y cada cuenta solo puede mantener una abierta.
  - Los motivos son `no_automated_channel`, `provider_unavailable`, `insufficient_provider_data`, `legal_name_unconfirmed` y `address_unconfirmed`.
  - España admite los cinco tipos iniciales; para otros países se ofrecen documento administrativo equivalente y `other` hasta añadir políticas nacionales.
  - La licencia de actividad/apertura es admisible como evidencia complementaria, pero no implica aprobación por sí sola.
  - Motivo y tipos se derivan exclusivamente en servidor; no se acepta texto libre ni una selección del cliente.
  - `ensureRequested` exige la transacción de la máquina de estados para que solicitud y `pending_review` sean atómicos.
  - Una revalidación cancela y fecha el requerimiento abierto. Si vuelve a ser inconclusa, el nuevo check origina otro.
  - Docker Desktop estaba detenido al primer intento de integración; se inició en segundo plano y las pruebas se repitieron correctamente.
  - Evidencia de cierre: 4 pruebas unitarias de política y 25 focalizadas de integración correctas; `npm run verify` correcto con 22 tests frontend y 100 backend, Flyway V1–V7, PostgreSQL 17/PostGIS, Redis, RabbitMQ y ambos builds.

## Conversación 38 - Subida cifrada de documentación empresarial privada

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.10` con un pipeline interno fail-closed para cargar los tipos admitidos por una
    solicitud documental abierta.
  - El contenido se limita y valida por MIME y magic bytes, se analiza mediante ClamAV `zINSTREAM`,
    se cifra con AES-256-GCM y se almacena en un bucket privado S3-compatible.
  - PostgreSQL conserva únicamente object key interno, SHA-256 del original y metadatos mínimos de
    seguridad. La solicitud se satisface bajo lock pesimista y la autorización se repite después del
    almacenamiento para impedir TOCTOU.
  - MinIO y ClamAV quedan disponibles en Compose local; staging y producción exigen endpoint HTTPS,
    bucket precreado y secretos externos.
  - No se expone todavía un endpoint HTTP: el servicio queda preparado para conectarse al contexto
    autenticado cuando se implementen las tareas de seguridad/API.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example`.
  - `apps/api/pom.xml`, `apps/api/README.md`, `apps/api/src/main/resources/application.yaml`.
  - `apps/api/src/main/resources/db/migration/V8__add_private_document_upload_metadata.sql`.
  - Entidad y DAOs de documento, solicitud documental y roles.
  - Nuevo paquete `businessverification.document` con propiedades, validación, ClamAV, cifrado y
    almacenamiento MinIO.
  - Contratos e implementaciones de carga y persistencia en `businessverification.service`.
  - Pruebas de contenido, cifrado, pipeline y migración V8.
  - `infrastructure/compose.yaml`, `infrastructure/README.md`.
  - `docs/configuration.md`, `docs/architecture/business-verification-persistence.md` y
    `docs/architecture/private-business-documents.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de
    actividad/apertura o documento equivalente`.
  - Prepara `1.11`, `1.17`, `1.19`, `1.21`, `1.22`, `14.6`, `14.8` y `14.14`.
- Tareas completadas:
  - `1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de
    actividad/apertura o documento equivalente`.
- Siguiente tarea pendiente recomendada:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
- Decisiones o aclaraciones relevantes:
  - Solo se admiten `application/pdf`, `image/png` e `image/jpeg`, confirmados por firma binaria; no
    se confía en nombre ni extensión y no se conservan.
  - El límite predeterminado es 10 MiB y la lectura usa `maxBytes + 1` para detectar exceso sin
    aceptar streams ilimitados.
  - Cualquier error, timeout o respuesta desconocida de ClamAV bloquea la carga.
  - El sobre cifrado versionado es `RSY1 || nonce(12) || ciphertext+tag`; solo se persiste el ID de
    clave para permitir rotación futura.
  - `fileUrl` sigue siendo un localizador interno y nunca una URL pública o firmada.
  - La autorización depende de roles explícitos: propietario de la cuenta con `venue_owner` o
    `admin`. `accountType` no concede permisos.
  - La cuenta debe estar `pending_review`, la solicitud debe seguir `open` y el tipo debe haber sido
    solicitado. Las comprobaciones se realizan antes del trabajo costoso y de nuevo bajo lock.
  - Si falla la transacción tras almacenar, se intenta borrar el objeto; una futura reconciliación
    operativa deberá detectar residuos si también falla esa compensación.
  - Evidencia de cierre: 7 pruebas nuevas focalizadas, migración V8 real y `npm run verify` correcto
    con 22 tests frontend y 107 backend, cero fallos, builds Next.js y Spring Boot correctos.

## Conversación 39 - Barrera empresarial de publicación

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.11` mediante una política backend reutilizable que bloquea la publicación cuando
    falta verificar el email, la cuenta no es `venue_business`, no existe identificador fiscal
    normalizado o la verificación empresarial no está aprobada.
  - La aprobación se reconoce por verificación remota `verified` todavía vigente o por revisión
    administrativa `approved`.
  - El servicio devuelve únicamente motivos cerrados y no expone email, identificador, razón social
    ni evidencia remota.
  - La lectura usa lock pesimista sobre `BusinessAccounts`; el futuro caso de uso de `2.9` deberá
    invocarla en la misma transacción que publique el perfil y añadir la validación de datos mínimos
    de `Venues`, tabla que todavía no existe.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/businessverification/persistence/BusinessAccountDao.java`.
  - `VenuePublicationBlocker`, `VenuePublicationEligibility`,
    `VenuePublicationEligibilityContext`, `VenuePublicationEligibilityPolicy`,
    `VenuePublicationEligibilityService`, `VenuePublicationEligibilityServiceImpl` y
    `VenuePublicationNotAllowedException`.
  - `VenuePublicationEligibilityPolicyTests` y
    `VenuePublicationEligibilityServiceIntegrationTests`.
  - `apps/api/src/main/java/com/reserly/platform/businessverification/service/package-info.java`.
  - `apps/api/README.md`, `docs/architecture/business-verification-persistence.md` y
    `docs/architecture/venue-publication-eligibility.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro y gestión de local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
  - `RB-012 Publicación de cuentas de local`.
- Tareas impactadas:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
  - Prepara `1.14`, `1.17`, `2.4`, `2.9`, `2.13`, `7.6` y `14.7`.
- Tareas completadas:
  - `1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados`.
- Siguiente tarea pendiente recomendada:
  - `1.12. Implementar hashing seguro de contraseñas`.
- Decisiones o aclaraciones relevantes:
  - La tarea no crea `Venues` ni un endpoint ficticio: ambos pertenecen a la Fase 2. Cierra la
    barrera previa sobre identidad y verificación empresarial.
  - Una aprobación automática requiere estado `verified` y caducidad estrictamente futura. En el
    instante exacto de expiración ya bloquea.
  - La aprobación administrativa es una vía alternativa; el esquema existente garantiza actor y
    fecha para `manualReviewStatus = approved`.
  - `accountType` forma parte de la regla de negocio, pero no sustituye los roles que autorizarán al
    actor en `1.17`.
  - `PESSIMISTIC_READ` se coordina con el `PESSIMISTIC_WRITE` de la máquina empresarial. El método
    usa una transacción escribible porque PostgreSQL prohíbe `SELECT FOR SHARE` en una transacción
    declarada read-only.
  - La primera integración permitió detectar y corregir esa incompatibilidad y tipar como
    `Timestamp` los `Instant` de fixtures JDBC.
  - Evidencia de cierre: 7 pruebas focalizadas correctas y `npm run verify` correcto con 22 tests
    frontend y 114 backend, cero fallos; Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds
    Next.js/Spring Boot correctos.

## Conversación 40 - Política completa de hashing de contraseñas

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.12` ampliando la protección BCrypt que el registro había adelantado en `1.4`.
  - `PasswordHashingService` centraliza validación criptográfica, generación BCrypt 2b con sal,
    comparación fail-closed y detección de rehash.
  - El coste se configura mediante `RESERLY_PASSWORD_BCRYPT_STRENGTH`, con rango de arranque 12–16.
  - Los hashes ausentes, malformados o con coste no acotado se comparan contra un hash dummy de la
    política vigente para reducir diferencias temporales y evitar trabajo arbitrario.
  - El registro ya delega también el límite de 72 bytes UTF-8 en esta frontera común.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example`.
  - `apps/api/src/main/resources/application.yaml`.
  - `PasswordHashingService`, `PasswordHashingServiceImpl`, `PasswordHashingProperties` y
    `PasswordHashingValidationException`.
  - `VenueRegistrationServiceImpl` y el `package-info` de servicios de identidad.
  - `PasswordHashingServiceTests` y `PasswordHashingPropertiesTests`.
  - `apps/api/README.md`, `docs/configuration.md`, `docs/architecture/identity-persistence.md` y
    `docs/architecture/venue-registration.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro y gestión de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.12. Implementar hashing seguro de contraseñas`.
  - Prepara `1.13` y `1.15` para verificar y actualizar credenciales sin duplicar lógica.
- Tareas completadas:
  - `1.12. Implementar hashing seguro de contraseñas`.
- Siguiente tarea pendiente recomendada:
  - `1.13. Implementar login y logout de locales`.
- Decisiones o aclaraciones relevantes:
  - Se conserva BCrypt por compatibilidad con los hashes ya emitidos y por ser una función
    adaptativa con sal; las nuevas credenciales usan explícitamente variante 2b.
  - La política funcional de registro mantiene mínimo 12 caracteres. La frontera criptográfica
    aplica además no nulo, no vacío y máximo 72 bytes UTF-8 para todos los casos de uso.
  - `matches` admite sintácticamente hashes 2a, 2b y 2y para migración, pero nunca registra
    contraseña, hash ni resultado detallado.
  - Un hash de variante anterior o coste menor requiere rehash tras autenticar; un coste superior
    válido no se reduce.
  - Costes codificados fuera de 4–16 se tratan como hash inválido, evitando un cálculo controlado por
    datos corruptos.
  - La primera continuación quedó bloqueada por el límite temporal de herramientas; no se cerró ni
    commiteó la tarea. Al retomarla, las pruebas unitarias pasaron.
  - Docker Desktop estaba detenido al primer intento de integración. Se inició y las pruebas se
    repitieron correctamente.
  - Evidencia de cierre final: 6 pruebas focalizadas correctas; integración de registro y arranque
    correcta; `npm run verify` correcto con 22 tests frontend y 119 backend, cero fallos; Flyway
    V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 41 - Login y logout seguro de cuentas de local

- Fecha: 2026-06-29.
- Resumen de la conversación:
  - Se completó `1.13` con los contratos `POST /api/auth/login` y `POST /api/auth/logout`.
  - El login normaliza el email, verifica BCrypt mediante la política central de `1.12`, admite
    exclusivamente cuentas de local activas o pendientes de verificar el email y renueva hashes
    antiguos después de autenticar correctamente.
  - Cada login crea una sesión absoluta configurable, entrega el secreto únicamente en una cookie
    host-only `HttpOnly`, `SameSite=Strict` y persiste solo su SHA-256.
  - El logout es idempotente: revoca la sesión conocida si existe, elimina siempre la cookie y no
    revela si el token era válido.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `AuthenticationController`, `AuthenticationControllerImpl`, `AuthenticationExceptionHandler`,
    `SessionCookieFactory` y el `package-info` de controladores de identidad.
  - `AuthenticationConverter`, `AuthenticationErrorResponse`, `LoginCommand`, `LoginRequest` y
    `LoginResponse`.
  - `AuthenticationService`, `AuthenticationServiceImpl`, `InvalidAuthenticationException`,
    `LoginOutcome`, `SessionProperties`, `SessionTokenService` y `SessionTokenServiceImpl`.
  - `AuthSessionDao` y `UserDao`.
  - `AuthenticationIntegrationTests`, `SessionCookieFactoryTests`, `SessionPropertiesTests` y
    `SessionTokenServiceTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/authentication-sessions.md` y
    `docs/architecture/identity-persistence.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Login y logout de local`.
  - `RF-007 Registro y gestión de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-007 Internacionalización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.13. Implementar login y logout de locales`.
  - Prepara `1.14`, `1.15`, `1.16`, `1.17` y `16.3`.
- Tareas completadas:
  - `1.13. Implementar login y logout de locales`.
- Siguiente tarea pendiente recomendada:
  - `1.14. Implementar verificación de email`.
- Decisiones o aclaraciones relevantes:
  - Una cuenta de local `pending_email_verification` puede autenticarse para completar su
    configuración, pero la barrera de publicación de `1.11` continúa bloqueándola.
  - Cuentas suspendidas, deshabilitadas, de cliente, inexistentes o con contraseña errónea reciben
    el mismo error genérico y consumen una comparación BCrypt.
  - El secreto de sesión tiene 256 bits aleatorios, 43 caracteres Base64 URL-safe sin relleno y
    nunca aparece en DTO, respuesta JSON ni base de datos.
  - La duración predeterminada es absoluta de 12 horas y se valida entre 5 minutos y 30 días.
  - No se añadió migración: `auth_sessions` de V2 ya incluye hash único, fechas, caducidad y
    revocación necesarias.
  - La autorización de peticiones y actualización de `lastSeenAt` pertenecen a `1.17`; la
    protección CSRF adicional pertenece a `16.3`.
  - La primera prueba de integración usó espacios alrededor del email, rechazados correctamente
    por `@Email`; el caso se corrigió para comprobar la normalización de mayúsculas.
  - El primer `npm run verify` detectó formato Markdown pendiente. Tras corregirlo, un segundo
    intento sufrió un timeout transitorio de workers Vitest; la suite web aislada y la ejecución
    integral final pasaron.
  - Evidencia de cierre final: pruebas focalizadas de sesión y autenticación correctas;
    `npm run verify` correcto con 22 tests frontend y 132 backend, cero fallos; Flyway V1–V8,
    PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 42 - Verificación transaccional de email

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.14` conectando el registro de local con un desafío de verificación de email de un
    solo uso.
  - Se añadieron los contratos `POST /api/auth/email/verify` y
    `POST /api/auth/email/verification/request`.
  - El consumo bloquea el token, valida propósito, vigencia y estados finales, fija
    `emailVerifiedAt`, activa solo cuentas pendientes y revoca desafíos hermanos.
  - El reenvío responde de forma genérica para evitar enumeración y rota el desafío anterior bajo
    lock de usuario.
  - La entrega se representa mediante un evento posterior al commit y una cola RabbitMQ durable,
    aislada y versionada; proveedor, plantillas, consumidor con reintentos y outbox permanecen en la
    Fase 8.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `VenueRegistrationServiceImpl`, `UserDao`, `AuthTokenDao` y los `package-info` de identidad.
  - `EmailVerificationController`, `EmailVerificationControllerImpl`,
    `EmailVerificationExceptionHandler` y `EmailVerificationConverter`.
  - DTOs de verificación, solicitud y error.
  - `EmailVerificationService`, `EmailVerificationServiceImpl`, propiedades, resultado, excepción,
    evento y servicio criptográfico de tokens de un solo uso.
  - Topología, configuración y relay RabbitMQ del contexto de identidad.
  - Pruebas de integración de verificación y registro, pruebas unitarias de token, propiedades y
    relay, y ampliación de la prueba real de infraestructura.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/email-verification.md`, `identity-persistence.md` y
    `cache-and-messaging.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Internacionalización`.
  - `RNF-008 Observabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.14. Implementar verificación de email`.
  - Prepara `1.15`, `1.16`, `1.17`, `2.9`, `8.1`, `8.2`, `8.7` y `8.8`.
- Tareas completadas:
  - `1.14. Implementar verificación de email`.
- Siguiente tarea pendiente recomendada:
  - `1.15. Implementar recuperación de contraseña`.
- Decisiones o aclaraciones relevantes:
  - El secreto contiene 256 bits CSPRNG, usa 43 caracteres Base64 URL-safe y PostgreSQL conserva
    solo SHA-256.
  - La vigencia predeterminada es 24 horas y se valida entre 15 minutos y 7 días.
  - Una cuenta suspendida puede probar la dirección sin quedar reactivada; una deshabilitada no
    consume el desafío.
  - Token inexistente, malformado, expirado, revocado o usado comparte
    `EMAIL_VERIFICATION_INVALID`.
  - Cuenta inexistente, ya verificada, suspendida o deshabilitada comparte `202` en la solicitud de
    otro desafío.
  - No se añadió migración: `AuthTokens` de V2 ya incluye todas las columnas, restricciones e
    índices requeridos.
  - La publicación `AFTER_COMMIT` evita entregar un token revertido, pero no cierra la ventana entre
    PostgreSQL y RabbitMQ; el outbox y los reintentos operativos corresponden a `8.7`.
  - El primer intento focalizado detectó que Spring Boot 4 expone `tools.jackson.databind` en lugar
    del `ObjectMapper` legado; se corrigió antes de repetir 15 pruebas focalizadas.
  - El primer cierre integral quedó aplazado por cuota de herramientas. Al retomarlo, un intento
    sufrió un timeout transitorio al arrancar workers Vitest y otro encontró Docker Desktop
    detenido; la suite web aislada pasó, Docker se inició y la ejecución integral final fue
    correcta.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 142 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ, topología de verificación y builds
    Next.js/Spring Boot correctos.

## Conversación 43 - Recuperación segura de contraseña

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.15` con `POST /api/auth/password/forgot` y
    `POST /api/auth/password/reset`.
  - La solicitud responde siempre `202` para emails válidos y solo rota desafíos de cuentas de
    local no deshabilitadas.
  - El restablecimiento consume bajo lock un token `password_reset`, aplica la política BCrypt,
    revoca desafíos hermanos y cierra todas las sesiones de la cuenta.
  - El mensaje de entrega se publica después del commit en una cola RabbitMQ durable, aislada y
    versionada.
  - Se limitó Vitest a dos workers para eliminar timeouts recurrentes al crear siete procesos jsdom
    simultáneos en el entorno disponible.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example`, `.env.production.example` y
    `apps/api/src/main/resources/application.yaml`.
  - `PasswordResetController`, implementación, manejador, DTOs, servicio, propiedades, excepción y
    evento.
  - `AuthSessionDao` y `UserDao`.
  - Topología, configuración y relay RabbitMQ de recuperación.
  - `PasswordResetIntegrationTests`, pruebas de propiedades y relay, e infraestructura RabbitMQ.
  - `apps/web/vitest.config.mts`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/password-recovery.md`, `identity-persistence.md` y
    `cache-and-messaging.md`.
  - `.kiro/specs/plataforma-reservas-saas/requirements.md`,
    `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Internacionalización`.
  - `RNF-008 Observabilidad`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.15. Implementar recuperación de contraseña`.
  - Se amplió `8.2` para incluir plantillas ES/EN de recuperación.
  - Prepara `1.16`, `1.20`, `1.21`, `8.1`, `8.2`, `8.7` y `8.8`.
- Tareas completadas:
  - `1.15. Implementar recuperación de contraseña`.
- Siguiente tarea pendiente recomendada:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
- Decisiones o aclaraciones relevantes:
  - El token tiene 256 bits CSPRNG, propósito independiente y vigencia predeterminada de 30 minutos,
    validada entre 10 minutos y 24 horas.
  - PostgreSQL solo conserva SHA-256; secreto y email solo coinciden en el transporte de entrega.
  - Las cuentas pendientes o suspendidas pueden renovar la credencial sin cambiar su estado; las
    deshabilitadas no emiten ni consumen desafíos.
  - Cambiar la contraseña no modifica email, verificación, roles ni estado empresarial.
  - Token, cuenta o contraseña no admisibles comparten `PASSWORD_RESET_INVALID`.
  - La nueva contraseña conserva mínimo funcional de 12 caracteres y máximo criptográfico de 72
    bytes UTF-8.
  - Se revocan todas las sesiones no revocadas, incluidas las ya expiradas, para fallar cerrado ante
    cualquier lectura futura defectuosa.
  - No se añadió migración: `AuthTokens` y `AuthSessions` de V2 ya contienen el modelo requerido.
  - La publicación posterior al commit mantiene la ventana sin outbox documentada para `8.7`.
  - El primer intento focalizado no ejecutó integración porque Docker Desktop estaba detenido; tras
    iniciarlo, 8 pruebas focalizadas pasaron.
  - El primer `npm run verify` volvió a agotar el timeout al arrancar siete workers Vitest sin
    ejecutar tests. Se fijó `maxWorkers = 2`; la suite web aislada y la ejecución integral final
    pasaron.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 150 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ, colas de verificación/recuperación y builds
    Next.js/Spring Boot correctos.

## Conversación 44 - Rate limiting distribuido de identidad y verificación empresarial

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se completó `1.16` con ventanas fijas atómicas sobre Redis para login, registro, solicitud y
    consumo de recuperación de contraseña, y verificaciones empresariales remotas.
  - Los endpoints anónimos se discriminan por la dirección remota observada por la aplicación; las
    comprobaciones empresariales se aíslan por UUID de cuenta.
  - Las claves contienen SHA-256 del discriminador y nunca almacenan IP, email ni UUID empresarial
    en claro.
  - El contador y su TTL se crean mediante un script Lua indivisible entre todas las instancias.
  - Una cuota agotada devuelve `429 RATE_LIMIT_EXCEEDED` con `Retry-After`; Redis no disponible
    devuelve `503 RATE_LIMIT_UNAVAILABLE` para fallar cerrado.
  - Las respuestas empresariales ya persistidas conservan su idempotencia y no consumen una nueva
    unidad; una comprobación nueva agota cuota antes de cambiar estado o llamar al proveedor.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example` y `.env.production.example`.
  - `apps/api/src/main/resources/application.yaml` y `application-test.yaml`.
  - Nuevo paquete `apps/api/src/main/java/com/reserly/platform/infrastructure/ratelimit` con
    propiedades, scopes, servicio Redis, interceptor MVC, configuración, excepciones y contrato
    HTTP.
  - `RemoteBusinessVerificationServiceImpl`.
  - `InfrastructureServicesIntegrationTests`,
    `RemoteBusinessVerificationRateLimitTests`, `SensitiveEndpointRateLimitInterceptorTests` y
    `RateLimitExceptionHandlerTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/cache-and-messaging.md` y `docs/architecture/rate-limiting.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-008 Observabilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RNF-013 Flujo GitFlow y promoción entre ramas`.
- Tareas impactadas:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
  - Prepara `16.6` para ampliar la misma frontera a reservas y enlaces públicos.
  - Prepara `17.3` y `17.4` para métricas y alertas operativas de rechazos.
- Tareas completadas:
  - `1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial`.
- Siguiente tarea pendiente recomendada:
  - `1.17. Implementar middleware de autorización por rol`.
- Decisiones o aclaraciones relevantes:
  - Cuotas iniciales: login 10/5 min; registro 5/h; solicitud de recuperación 5/15 min; consumo de
    recuperación 10/15 min; verificación empresarial 5/h por cuenta.
  - Las ventanas admiten entre 1 segundo y 24 horas y los máximos entre 1 y 10.000.
  - La aplicación no confía directamente en `X-Forwarded-For`; el proxy de producción debe sanear
    y normalizar la dirección verificada.
  - El perfil `test` desactiva la barrera para suites funcionales no relacionadas; la prueba de
    infraestructura la activa explícitamente contra Redis real.
  - No se añadió migración porque los contadores son efímeros y no forman parte de la fuente de
    verdad transaccional.
  - Riesgos pendientes: ráfaga en borde de ventana fija, usuarios tras NAT compartiendo cuota,
    configuración confiable del proxy y ausencia de métricas específicas hasta la Fase 17.
  - Evidencia focalizada: 8 pruebas correctas, incluidas Redis 8 real, TTL, hash de clave,
    enrutamiento de endpoints, `Retry-After` y bloqueo previo del proveedor.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 156 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 45 - Middleware de autorización por rol

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se inició `1.17` incorporando Spring Security stateless sobre la cookie opaca ya existente.
  - Se implementó resolución de sesión vigente, carga de roles desde PostgreSQL, principal
    inmutable, revocación al observar cuentas bloqueadas y actualización acotada de `lastSeenAt`.
  - Se definieron `venue_owner` para `/api/venue/me/**` y `admin` para `/api/admin/**`, con errores
    JSON uniformes `401` y `403`.
  - Se añadió CORS con credenciales limitado a `allowedOrigins`, métodos y cabeceras cerrados.
  - La primera prueba no incorporaba la cadena Security al `MockMvc` manual; tras conectarla con el
    soporte oficial, 13 pruebas focalizadas pasaron.
  - Al retomar, se añadió CORS estricto para credenciales, se excluyó la cuenta aleatoria de Spring
    Boot, se verificaron límites exactos de namespace y se completó la suite integral.
- Archivos modificados:
  - `.env.local.example`, `.env.staging.example` y `.env.production.example`.
  - `apps/api/pom.xml`, `ReserlyApplication`, `AuthSessionDao`, `UserRoleDao`,
    `SessionProperties`, `application.yaml` y pruebas de propiedades/cookie.
  - Nuevo paquete `apps/api/src/main/java/com/reserly/platform/identity/security`.
  - Nueva integración `RoleAuthorizationIntegrationTests`.
  - `apps/api/README.md`, `docs/configuration.md`,
    `docs/architecture/authentication-sessions.md` y
    `docs/architecture/role-authorization.md`.
  - `.kiro/specs/plataforma-reservas-saas/design.md` y
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`,
    `.kiro/specs/plataforma-reservas-saas/tasks.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-030 Administración de plataforma`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas:
  - `1.17. Implementar middleware de autorización por rol`.
  - Prepara `1.19`, `2.4`, `2.12`, `9.1`, `14.1` y `16.3`.
- Tareas completadas:
  - `1.17. Implementar middleware de autorización por rol`.
- Siguiente tarea pendiente recomendada:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
- Decisiones o aclaraciones relevantes:
  - `account_type` no concede permisos; la autorización depende de `UserRoles`.
  - `employee_user` no recibe acceso global al namespace propietario.
  - La sesión sigue teniendo caducidad absoluta; `lastSeenAt` no renueva `expiresAt`.
  - CSRF permanece pendiente de `16.3`.
  - No se añadió migración: V2 ya contiene sesiones, usuarios, roles y asignaciones requeridas.
  - La primera repetición tras la pausa encontró Docker Desktop detenido; se arrancó y Testcontainers
    continuó sobre infraestructura real.
  - Una aserción comparaba un instante Java con el redondeo microsegundo de PostgreSQL; se corrigió
    para comprobar la invariancia real de `expiresAt` antes y después de la petición.
  - Evidencia focalizada final: 14 pruebas correctas, con 9 casos de integración de seguridad.
  - Evidencia final: `npm run verify` correcto con 22 tests frontend y 166 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 46 - Pantalla pública de registro empresarial

- Fecha: 2026-06-30.
- Resumen de la conversación:
  - Se implementó la ruta pública `/locales/registro` con composición responsive, explicación del
    proceso y formulario de cuenta e identidad empresarial.
  - El formulario envía exactamente el contrato existente de `POST /api/auth/venues/register`:
    email, contraseña, locale, país fiscal, razón social, identificador, dirección registral
    opcional y consentimiento legal.
  - Se añadieron validación contextual, límite BCrypt por caracteres y bytes, foco en el primer
    error, mostrar/ocultar contraseña, bloqueo durante el envío, cancelación al desmontar y estados
    genéricos de conflicto, petición inválida, rate limit e indisponibilidad.
  - El éxito informa de la verificación de email y del bloqueo de publicación hasta aprobar también
    el negocio, sin fingir que el perfil `Venue` exista todavía.
  - Se corrigió el enlace global de acceso para usar la ruta especificada `/locales/acceso`.
  - Se validó visualmente la pantalla a 1265 px y 390 px: una cuadrícula de dos columnas pasa a una
    columna, no existe overflow horizontal, el idioma del documento es `es`, el título es
    `Registro de local | Reserly` y los landmarks/nombres accesibles están presentes.
- Archivos modificados:
  - `apps/web/src/app/locales/registro/page.tsx`.
  - `apps/web/src/features/venue-registration/venue-registration-form.tsx`.
  - `apps/web/src/features/venue-registration/venue-registration-schema.ts`.
  - `apps/web/src/features/venue-registration/venue-registration-api.ts`.
  - Tests unitarios y de componente de los tres módulos anteriores.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `apps/web/src/components/layout/public-shell.tsx` y
    `apps/web/src/components/layout/layout-system.test.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`,
    `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md` y
    `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
- Tareas impactadas:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
  - Prepara `1.19`, `1.20`, `1.21`, `1.22` y el perfil de local de la Fase 2.
- Tareas completadas:
  - `1.18. Crear pantalla de registro de local con campos empresariales`.
- Siguiente tarea pendiente recomendada:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
- Decisiones o aclaraciones relevantes:
  - La Fase 1 no solicita nombre comercial, categoría, imagen, descripción u horarios porque el
    modelo `Venue` se crea en la Fase 2. La pantalla lo comunica y no descarta esos requisitos.
  - La validación cliente es de usabilidad; formato fiscal, normalización, control, unicidad y
    verificación remota siguen siendo autoridad exclusiva del backend.
  - Un `409` nunca indica si colisionó el email o el identificador fiscal.
  - No se persisten credenciales ni datos fiscales en almacenamiento del navegador y no se
    registran payloads.
  - Los textos imprescindibles de esta pantalla se incorporan ya en ES/EN para cumplir la
    infraestructura i18n; `1.21` conserva el alcance transversal de login, errores y estados.
  - No se añadió migración ni se modificó el backend.
  - Evidencia focalizada: 13 tests nuevos correctos.
  - Evidencia integral: `npm run verify` correcto con 35 tests frontend y 166 backend, cero fallos;
    Flyway V1–V8, PostgreSQL/PostGIS, Redis, RabbitMQ y builds Next.js/Spring Boot correctos.

## Conversación 47 - Portal privado de documentación empresarial

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.19` como primera tarea pendiente y se detectó que el pipeline seguro de `1.10`
    todavía no tenía una frontera HTTP consumible por una pantalla.
  - Se añadieron consulta y carga multipart autenticadas bajo
    `/api/venue/me/business-verification`, derivando cuenta y actor de la sesión.
  - Se implementó `/panel/verificacion` con estados de carga, ausencia de solicitud, sesión
    caducada, error recuperable, solicitud abierta, validación de archivo y confirmación.
  - La interfaz muestra exclusivamente motivos y tipos cerrados devueltos por el backend; no recibe
    NIF, razón social, dirección, check técnico, hash ni URL privada.
  - Se configuraron límites multipart de 10 MiB por documento y 11 MiB por request.
  - Se actualizaron diseño, arquitectura documental, configuración y catálogos ES/EN.
- Archivos modificados:
  - Nuevos paquetes backend `businessverification/controller`, `converter` y `dto`.
  - Nuevos `BusinessVerificationDocumentPortalService` e implementación.
  - `BusinessAccountDao`, `application.yaml` y plantillas de entorno.
  - Nuevas pruebas de portal y controlador.
  - `apps/web/src/app/panel/verificacion/page.tsx`.
  - Nuevo feature `apps/web/src/features/business-documents`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
  - `docs/architecture/private-business-documents.md` y `docs/configuration.md`.
- Requisitos impactados:
  - `RF-032 Verificación empresarial de cuentas de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-006 Disponibilidad operativa`.
  - `RNF-007 Compatibilidad e internacionalización`.
  - `RNF-011 Convenciones de implementación backend y persistencia`.
- Tareas impactadas:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
  - Prepara `1.20`, `1.21`, `1.22`, `14.6`, `14.8` y `16.3`.
- Tareas completadas:
  - `1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes`.
- Siguiente tarea pendiente recomendada:
  - `1.20. Crear pantalla de acceso para locales`.
- Decisiones o aclaraciones relevantes:
  - `GET .../document-request` devuelve `204` si no existe solicitud abierta.
  - `POST .../documents` no acepta `businessAccountId` ni `uploaderUserId`; ambos se derivan del
    principal autenticado.
  - Los errores documentales se reducen a códigos `DOCUMENT_*` sin mensajes de ClamAV, S3,
    PostgreSQL o datos de propiedad.
  - La prevalidación cliente de PDF/JPEG/PNG y 10 MiB es solo usabilidad; backend vuelve a validar
    límite, MIME, magic bytes, ownership, tipo, antivirus y almacenamiento.
  - No se añadió migración: V7 y V8 ya contienen solicitudes, metadatos, restricciones e índices.
  - Evidencia frontend: 21 tests focalizados y 56 tests totales correctos; build incluye
    `/panel/verificacion`.
  - Evidencia backend: 13 tests documentales correctos, Checkstyle/Spotless correctos y JAR
    generado.
  - La suite Testcontainers no pudo abrir el pipe de Docker dentro del sandbox y la validación
    visual con navegador no pudo iniciar procesos por límite operativo del entorno. No se
    atribuyeron estos bloqueos al código ni se ocultaron; las pruebas nuevas no dependen de Docker.
  - `git add` fue rechazado por el límite de uso del entorno. No se creó commit ni se ejecutó push;
    los cambios permanecieron íntegros y se retomaron posteriormente para completar el cierre Git.

## Conversación 48 - Pantalla de acceso para locales

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.20` como primera tarea pendiente sobre una rama limpia y sincronizada.
  - Se implementó `/locales/acceso` con una composición responsive alineada con el registro
    empresarial y el sistema visual existente.
  - El formulario autentica mediante `POST /api/auth/login`, solicita entrega de cookie con
    `credentials: include` y valida únicamente los metadatos no sensibles de la respuesta.
  - Los errores `400` y `401` se presentan con el mismo mensaje para no revelar si existe el email,
    si la contraseña es incorrecta, si el tipo de cuenta no corresponde o si la cuenta está
    suspendida.
  - Se añadieron validación contextual, foco en el primer error, mostrar/ocultar contraseña,
    cancelación al desmontar, bloqueo de doble envío y estados diferenciados de rate limit e
    indisponibilidad.
  - Tras el éxito se navega a `/panel` con el locale de cuenta como parámetro seguro; el proxy lo
    normaliza y el nuevo punto de entrada redirige a `/panel/verificacion` mientras se construye el
    resumen operativo definitivo.
  - Se añadieron enlaces explícitos a registro y recuperación de contraseña.
  - La pantalla se verificó visualmente a 1280 px y 390 × 844 px, sin overflow horizontal y con
    jerarquía, landmarks, nombres accesibles y errores de campo correctos.
- Archivos modificados:
  - `apps/web/src/app/locales/acceso/page.tsx`.
  - `apps/web/src/app/panel/page.tsx`.
  - Nuevo feature `apps/web/src/features/venue-login` con API, esquema, formulario y pruebas.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
- Tareas impactadas:
  - `1.20. Crear pantalla de acceso para locales`.
  - Prepara `1.21`, `1.22`, `15.8` y `16.3`.
- Tareas completadas:
  - `1.20. Crear pantalla de acceso para locales`.
- Siguiente tarea pendiente recomendada:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
- Decisiones o aclaraciones relevantes:
  - La pantalla no implementa autenticación propia: consume el contrato seguro cerrado en `1.13`.
  - La contraseña no se recorta, persiste, incluye en URL ni registra; el cliente solo aplica el
    límite de 72 bytes de BCrypt como ayuda.
  - `userId` y `sessionExpiresAt` se validan pero no se muestran ni almacenan.
  - El secreto de sesión permanece inaccesible a JavaScript porque backend lo entrega como cookie
    HttpOnly.
  - `/panel` es ya el destino estable del login; su redirect temporal evita acoplar el formulario a
    una subsección que cambiará al aparecer el dashboard.
  - El enlace de recuperación queda preparado en `/locales/recuperar-contrasena`; su pantalla no
    forma parte de `1.20`, aunque el endpoint backend ya existe desde `1.15`.
  - Los textos imprescindibles del login se añaden en ES/EN para que la pantalla sea utilizable.
    `1.21` permanece pendiente por su revisión transversal de registro, login, errores y todos los
    estados de verificación.
  - Evidencia focalizada: 16 tests correctos.
  - Evidencia integral: 16 archivos y 72 tests frontend correctos con un worker; build Next.js,
    TypeScript, ESLint, Prettier, i18n y calidad de español correctos.

## Conversación 49 - Contrato ES/EN de identidad y verificación

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `1.21` como primera tarea pendiente y se auditó la cobertura creada durante
    registro, carga documental y login.
  - Se creó el namespace compartido `Verification` con títulos y descripciones ES/EN para email,
    cuenta empresarial, revisión manual, documentos y barreras de publicación.
  - Se añadieron ocho categorías seguras de error para identidad y verificación sin mensajes
    internos, proveedores ni pistas de enumeración.
  - Se implementaron listas cerradas y mapas TypeScript exhaustivos entre valores persistidos,
    claves de catálogo y tonos visuales.
  - La respuesta de registro pasó de un cast abierto a validación Zod; un estado empresarial
    desconocido ahora falla cerrado.
  - El éxito del registro muestra por separado la confirmación del correo y la identidad
    empresarial mediante un resumen accesible que no expone códigos `snake_case`.
  - Se verificaron catálogos y componentes tanto en español como en inglés.
  - La inspección visual española pasó en 1280 px y 390 × 844 px sin overflow. El navegador
    integrado retuvo su preferencia española al solicitar inglés; la variante inglesa se verificó
    en prueba de componente y la matriz visual completa permanece en `15.15`.
- Archivos modificados:
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - Nuevo feature `apps/web/src/features/verification`.
  - `apps/web/src/features/venue-registration/venue-registration-api.ts`.
  - `apps/web/src/features/venue-registration/venue-registration-form.tsx`.
  - Pruebas de API y formulario de registro.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-005 Usabilidad y responsive`.
  - `RNF-007 Compatibilidad e internacionalización`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
  - Prepara `1.22`, `2.9`, `14.6`, `14.8` y `15.15`.
- Tareas completadas:
  - `1.21. Crear textos ES/EN para registro, login, errores y estados de verificación`.
- Siguiente tarea pendiente recomendada:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Decisiones o aclaraciones relevantes:
  - El estado técnico remoto (`verified`, `invalid`, `inconclusive`, `error`) no se muestra al
    propietario; la UI usa el estado agregado de la cuenta.
  - Los mapas no construyen claves i18n a partir de valores arbitrarios del servidor.
  - Revisión manual y documentos tienen vocabularios separados porque `approved` y `accepted` no
    son intercambiables en persistencia.
  - Los bloqueos de publicación ya disponen de texto, aunque la pantalla que los consumirá
    corresponde a `2.9`.
  - El resumen usa texto, icono y color; nunca depende solo del tono.
  - No se modificaron backend, base de datos, migraciones ni contratos HTTP.
  - Evidencia focalizada final: 5 archivos y 23 tests correctos, incluidos estado desconocido y
    render inglés.
  - Evidencia integral: 18 archivos y 82 tests correctos; build, TypeScript, ESLint, Prettier, i18n
    y español correctos.

## Conversación 50 - Cierre integrado de identidad, verificación y permisos

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se auditó la cobertura existente de registro, login, verificación de email, verificación
    empresarial, documentos y seguridad.
  - Se conservaron las suites focalizadas ya existentes y se cerró la carencia transversal con
    dos recorridos HTTP autenticados sobre PostgreSQL real.
  - El primer recorrido registra un local, verifica su email mediante token de un solo uso, inicia
    sesión, reutiliza la cookie HttpOnly y consulta su solicitud documental privada.
  - El segundo recorrido demuestra que el endpoint requiere autenticación y que otro propietario
    no puede consultar ni adjuntar un archivo a una solicitud ajena.
  - Se verificó que el intento cruzado devuelve `403 DOCUMENT_UPLOAD_FORBIDDEN` y no persiste
    ningún documento.
  - La verificación integral del repositorio terminó correctamente.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/identity/controller/VenueRegistrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-007 Registro de local`.
  - `RF-008 Acceso y panel privado del local`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Integridad y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
- Tareas impactadas:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Tareas completadas:
  - `1.22. Crear tests de registro, login, verificación de email, verificación empresarial,
    documentación de respaldo y permisos`.
- Siguiente tarea pendiente recomendada:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
- Decisiones o aclaraciones relevantes:
  - La prueba transversal vive en la suite de registro porque parte del contrato público y cruza
    todos los límites relevantes hasta el recurso privado; no replica la lógica de los servicios.
  - Los tokens de verificación se insertan como fixtures con hash conocido. El consumo, la
    invalidación y el cambio de estado se ejercitan a través del endpoint real.
  - Las sesiones se obtienen exclusivamente desde `Set-Cookie`; los tests no inyectan un principal
    artificial en los recorridos que validan autenticación.
  - El aislamiento se comprueba con dos cuentas persistidas y dos cookies reales. Una consulta
    ajena no filtra la existencia de la solicitud y una escritura ajena falla de forma explícita.
  - No se modificaron contratos productivos, migraciones ni modelos de datos.
  - Evidencia focalizada: 8 tests en `VenueRegistrationIntegrationTests`, cero fallos.
  - Evidencia integral: `npm run verify` correcto, incluidas suites web/API, lint, formato,
    TypeScript, i18n, calidad de español y builds de producción.

## Conversación 51 - Esquema base de locales, categorías e imágenes

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.1` como primera tarea pendiente y el inicio de la Fase 2 en la rama
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V9` con las tablas físicas `Categories`, `Venues` y
    `VenueImages`, siguiendo `UpperCamelCase` para tablas y `lowerCamelCase` para columnas.
  - Se añadieron claves foráneas, vocabularios cerrados, validación de slugs, email, país,
    timestamps, JSONB localizado, publicación, coordenadas y orden de galería.
  - La pertenencia del local se protege mediante una clave foránea compuesta que obliga a que
    `businessAccountId` y `ownerUserId` correspondan a la misma cuenta empresarial.
  - Se añadió una ubicación PostGIS generada desde latitud/longitud y un índice GiST, además de
    índices de búsqueda por nombre, categoría, estado y ubicación textual.
  - No se incluyeron categorías iniciales ni datos semilla; corresponden a `2.2`.
  - La prueba de migración se amplió para verificar versión, columnas, índices e invariantes sobre
    PostgreSQL/PostGIS real. La primera prueba de coordenadas detectó que un `CHECK` SQL podía
    evaluar a `NULL`; se corrigió exigiendo explícitamente ambos componentes.
  - La verificación integral expuso que el validador de convenciones delimitaba `CREATE TABLE`
    mediante una expresión regular y confundía SQL multilínea con columnas. Se sustituyó por un
    lector acotado que respeta paréntesis, literales y comentarios.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V9__create_venue_category_and_image_tables.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `scripts/validate-backend-conventions.mjs`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
  - Prepara `2.2` a `2.13`, `3.1` a `3.6` y `14.2`.
- Tareas completadas:
  - `2.1. Crear migraciones de venues, categories y venue_images`.
- Siguiente tarea pendiente recomendada:
  - `2.2. Crear seed de categorías iniciales`.
- Decisiones o aclaraciones relevantes:
  - `Categories` se crea vacía deliberadamente; la semilla y sus traducciones se cierran en las
    tareas `2.2` y `2.3`.
  - Una categoría es obligatoria al crear `Venues`, pero la migración no crea automáticamente el
    perfil durante el registro empresarial.
  - `mainImageUrl` pertenece al perfil y `VenueImages` representa solo la galería ordenada.
  - La columna `location` es derivada y no constituye una segunda fuente de verdad.
  - El borrado de categoría y cuenta empresarial queda restringido; las imágenes usan cascada solo
    cuando se elimina físicamente su local.
  - Evidencia focalizada: 5 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V9 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras código y documentación, con 82 tests web y
    188 tests API.

## Conversación 52 - Semilla inicial de categorías

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.2` como primera tarea pendiente sobre la rama limpia y sincronizada
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V10` con las ocho categorías iniciales requeridas: restaurante,
    peluquería, campo de fútbol, pista de pádel, instalación municipal, centro deportivo, centro de
    estética y otros.
  - Cada categoría usa un UUID reservado y estable, un slug sin tildes apto para URL, nombre
    canónico en español correcto y estado activo.
  - `V9` exige que toda categoría tenga un `nameI18n` ES/EN válido. La semilla respeta esa
    invariante desde su inserción; la auditoría específica de traducciones, fallback y completitud
    permanece en `2.3`.
  - Se amplió la prueba de migración para comprobar Flyway V10 y el contenido exacto de las ocho
    filas sobre PostgreSQL/PostGIS real, sin bloquear futuras categorías adicionales.
  - Se corrigió en `design.md` la ubicación de las restricciones de `Categories`, que durante
    `2.1` habían quedado accidentalmente bajo `venue_custom_tabs`.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V10__seed_initial_venue_categories.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.2. Crear seed de categorías iniciales`.
  - Prepara `2.3`, `2.4`, `3.3` y `14.2`.
- Tareas completadas:
  - `2.2. Crear seed de categorías iniciales`.
- Siguiente tarea pendiente recomendada:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
- Decisiones o aclaraciones relevantes:
  - Los UUID `20000000-0000-0000-0000-000000000001` a
    `20000000-0000-0000-0000-000000000008` quedan reservados para el catálogo base.
  - El slug es la identidad semántica estable de URL y filtros; no se deriva en tiempo de ejecución
    del texto traducido.
  - Los nombres canónicos conservan tildes y ortografía española; solo los slugs son ASCII.
  - No se añaden descripciones, iconos, orden editorial ni endpoints porque no forman parte de
    `2.2`.
  - La migración no usa `ON CONFLICT`: Flyway garantiza una única aplicación y cualquier colisión
    debe fallar de forma visible, no ocultarse.
  - Evidencia focalizada: 6 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V10 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 182 tests API.

## Conversación 53 - Traducciones completas de categorías

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.3` como primera tarea pendiente sobre la rama limpia y sincronizada
    `phase/2-locales-categorias-perfil`.
  - Se creó la migración Flyway `V11` para completar las ocho categorías iniciales con
    descripciones naturales en español e inglés.
  - Se conservó el nombre y la descripción canónica en español, mientras `nameI18n` y
    `descriptionI18n` actúan como fuentes localizadas estructuradas.
  - Se endureció `ckCategoriesDescriptionI18n`: una descripción localizada, cuando existe, debe
    incluir valores ES/EN no vacíos.
  - La prueba de migración carga los JSONB persistidos mediante `LocalizedText`, comprueba el
    contenido exacto, la completitud, la resolución ES/EN y el fallback inglés.
  - Se verificó además que PostgreSQL rechaza intentar dejar una categoría inicial con descripción
    solo en español.
- Archivos modificados:
  - Nuevo
    `apps/api/src/main/resources/db/migration/V11__complete_initial_category_translations.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
  - Prepara `2.4`, `3.3`, `3.14` y `14.2`.
- Tareas completadas:
  - `2.3. Crear traducciones ES/EN para categorías iniciales`.
- Siguiente tarea pendiente recomendada:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
- Decisiones o aclaraciones relevantes:
  - Se añadieron descripciones localizadas porque `V10` ya contenía los nombres ES/EN mínimos
    exigidos por `V9`; `V11` cierra el contenido completo y su auditoría.
  - No se editó `V10`: la evolución es forward-only y conserva checksums Flyway publicados.
  - `descriptionI18n` puede ser `NULL` para una categoría futura aún sin contenido, pero no puede
    contener un documento parcial.
  - Los slugs y UUID no se traducen ni cambian con el locale.
  - No se implementó endpoint de categorías; la futura API deberá resolver el texto antes de
    responder y no exponer JSONB.
  - Evidencia focalizada: 7 tests correctos en `DatabaseMigrationIntegrationTests`, con Flyway V1 a
    V11 sobre PostgreSQL 17.5/PostGIS.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 183 tests API.

## Conversación 54 - CRUD privado del perfil del local

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.4` como primera tarea pendiente sobre la rama
    `phase/2-locales-categorias-perfil`.
  - Se implementaron entidades JPA y DAOs documentados para `Venues` y `Categories`, con acceso por
    propiedades y consultas explícitas por propietario.
  - Se añadió el servicio transaccional de creación, lectura, actualización y archivo del perfil
    singular asociado al principal autenticado.
  - Se expusieron `GET /api/venue/me`, `POST`, `PATCH` y `DELETE /api/venue/me/profile`, con interfaz
    de controlador, implementación, DTOs, conversor y errores estables.
  - El payload no admite IDs de propietario/cuenta, slug, estado, publicación, imagen ni
    disponibilidad manual.
  - La creación genera slug seguro, estado `draft` y disponibilidad `automatic`; actualizar
    conserva identidad/slug/estado y archivar realiza borrado lógico.
  - `V12` incorpora unicidad parcial para un único perfil no archivado por propietario y permite
    recreación tras archivo.
  - Se añadieron pruebas de adaptador REST, errores, ciclo CRUD real, aislamiento básico,
    normalización, coordenadas, categoría y migración.
- Archivos modificados:
  - Nuevo módulo productivo bajo `apps/api/src/main/java/com/reserly/platform/venues` con paquetes
    `controller`, `converter`, `dto`, `persistence` y `service`.
  - Nueva migración
    `apps/api/src/main/resources/db/migration/V12__enforce_single_current_venue_per_owner.sql`.
  - Nuevas pruebas bajo `apps/api/src/test/java/com/reserly/platform/venues`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad y protección de datos`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
  - Prepara `2.5` a `2.13`, `3.1` y `14.3`.
- Tareas completadas:
  - `2.4. Implementar CRUD de perfil del local para propietario`.
- Siguiente tarea pendiente recomendada:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
- Decisiones o aclaraciones relevantes:
  - El contrato MVP gestiona un perfil vigente por propietario. Los perfiles archivados permanecen
    como historial y no bloquean una nueva alta.
  - El `PATCH` es sustitutivo para los campos editables; permite limpiar opcionales con `null`.
  - La imagen principal no se acepta como URL arbitraria; corresponde a la carga segura de `2.7`.
  - Los campos localizados se reservan para `2.5`; `2.4` conserva la descripción canónica simple.
  - Crear un borrador no exige verificación empresarial aprobada. La barrera se aplica al publicar
    en `2.9`.
  - El aislamiento exhaustivo por HTTP permanece en `2.12`; esta iteración ya demuestra que el
    servicio usa solo `ownerUserId` y que otro propietario no puede leer el perfil.
  - Evidencia focalizada: 11 tests correctos entre migración, controlador y servicio.
  - Evidencia integral: `npm run verify` correcto tras implementación y documentación, con 82
    tests web y 187 tests API.

## Conversación 55 - Textos públicos localizados del perfil

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.5` como primera tarea pendiente y se auditó el CRUD privado de `2.4`.
  - Se creó `V13` con `servicesI18n`, `rulesI18n` y `publicTextI18n` JSONB.
  - La migración transforma descripciones canónicas preexistentes en `descriptionI18n` usando el
    locale por defecto, evitando pérdida de contenido en despliegues incrementales.
  - Se mapearon los cuatro documentos JSONB de `VenueEntity` al value object `LocalizedText`.
  - Se añadió `LocalizedTextDto` al request/response privado; solo admite claves `es` y `en`, exige
    idioma fuente con valor visible y permite limpiar documentos con `null`.
  - El servicio deriva `description` del idioma fuente para impedir divergencia entre la columna
    canónica y `descriptionI18n`.
  - Se adaptó la serialización de `SupportedLocale` para persistir etiquetas minúsculas estables.
  - Se verificaron roundtrip JSON, persistencia Hibernate, fallback, traducciones parciales y
    limpieza de los cuatro campos.
- Archivos modificados:
  - Nueva migración
    `apps/api/src/main/resources/db/migration/V13__add_localized_public_venue_texts.sql`.
  - Nuevo `apps/api/src/main/java/com/reserly/platform/venues/dto/LocalizedTextDto.java`.
  - `SupportedLocale`, entidad, DTOs, conversor y servicio del perfil.
  - Pruebas de localización, migración, controlador y servicio del perfil.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
  - Prepara `2.6`, `2.9`, `2.10` y `2.11`.
- Tareas completadas:
  - `2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos
    configurables`.
- Siguiente tarea pendiente recomendada:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
- Decisiones o aclaraciones relevantes:
  - Los borradores permiten documentos parciales siempre que exista el idioma fuente. La
    publicación decidirá si exige ambas traducciones o fallback aprobado.
  - `description` permanece como proyección canónica del idioma fuente; no es una segunda entrada
    editable.
  - Los campos son texto plano. No se acepta HTML ni rich text en este contrato.
  - La vista privada expone los documentos completos para edición; la futura ficha pública debe
    resolver el locale y devolver strings.
  - La validación de 350 palabras no se adelanta y permanece exclusivamente en `2.6`.
  - Evidencia focalizada: 17 tests correctos entre migración, localización, controlador y servicio.
  - Evidencia integral: `npm run verify` correcto tras código y documentación.

## Conversación 56 - Límite de descripción por idioma

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.6` como primera tarea pendiente después de revisar la especificación completa.
  - Se creó una política de dominio que limita cada traducción presente de `descriptionI18n` a un
    máximo inclusivo de 350 palabras.
  - Se definió un conteo Unicode determinista: letras y números forman palabras, apóstrofes y
    guiones internos permanecen unidos, y puntuación, símbolos y emojis actúan como separadores.
  - La política se ejecuta en altas y actualizaciones antes de consultar o modificar el perfil.
  - El error se expone como HTTP `422`, código `VENUE_DESCRIPTION_TOO_LONG` y metadatos seguros del
    idioma y del límite, sin devolver contenido escrito por el usuario.
  - Se verificaron límites 350/351, descripción ausente, idiomas independientes, Unicode,
    adaptación REST e integración real con PostgreSQL.
- Archivos modificados:
  - Nuevos `VenueDescriptionService`, `VenueDescriptionServiceImpl` y
    `VenueDescriptionTooLongException`.
  - Nuevo DTO `VenueDescriptionLimitErrorResponse`.
  - `VenueProfileServiceImpl` y `VenueProfileExceptionHandler`.
  - Nuevas pruebas unitarias y ampliaciones de pruebas de controlador e integración del perfil.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
- Tareas impactadas:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
  - Prepara `2.9` y la futura interfaz de edición del perfil.
- Tareas completadas:
  - `2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado`.
- Siguiente tarea pendiente recomendada:
  - `2.7. Implementar carga segura de imagen principal del local`.
- Decisiones o aclaraciones relevantes:
  - El límite se aplica al guardar tanto borradores como perfiles ya existentes, no solo en el
    futuro cambio de estado a publicado.
  - Se valida cada traducción presente aunque no sea el idioma fuente.
  - No se añade constraint SQL: no reproduciría con fidelidad la semántica léxica Unicode aplicada
    por la API.
  - Servicios, reglas y texto público conservan su límite técnico de payload y no reciben el límite
    editorial de descripción.
  - Evidencia focalizada: 9 tests correctos, cero fallos, errores u omitidos.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 193 tests API y ambos builds.

## Conversación 57 - Carga segura de imagen principal

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.7` como primera tarea pendiente y se auditó el esquema y la carga privada previa.
  - Se implementó carga multipart limitada al propietario autenticado.
  - JPEG/PNG se validan por decodificación real, MIME, tamaño, dimensiones, píxeles y frame único.
  - Toda imagen aceptada se vuelve a codificar para retirar metadatos.
  - Se creó almacenamiento MinIO en bucket privado independiente y entrega pública mediada por API
    únicamente para locales publicados.
  - La sustitución compensa en rollback y limpia el objeto anterior después del commit.
  - V14 añade clave interna, MIME, tamaño y dimensiones con integridad todo-o-nada.
- Archivos modificados:
  - Nueva migración `V14__add_secure_venue_main_image_metadata.sql`.
  - Nuevo paquete `venues.image`, controladores, servicio y DTOs de imagen principal.
  - Entidad, DAO, respuesta/conversor de perfil y advice de errores.
  - Configuración API, plantillas de entorno y pruebas de imagen/migración.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-003`, `RF-004`, `RF-008`, `RF-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008`, `RNF-011`.
- Tareas impactadas:
  - `2.7. Implementar carga segura de imagen principal`.
  - Prepara `2.9`, `2.10` y `3.1`.
- Tareas completadas:
  - `2.7. Implementar carga segura de imagen principal`.
- Siguiente tarea pendiente recomendada:
  - `2.8. Implementar galería opcional`.
- Decisiones o aclaraciones relevantes:
  - El bucket permanece privado; la API exige estado publicado antes de entregar bytes.
  - La galería y su orden permanecen en `2.8`.
  - No se aceptan GIF, SVG, WebP ni URLs arbitrarias.
  - El nombre original nunca se persiste ni construye claves.
  - Evidencia focalizada: 20 tests correctos, cero fallos, errores u omitidos.
  - Verificación transversal correcta para CI, entornos, i18n, español, convenciones, lint,
    formato, TypeScript, 82 tests web y ambos builds.
  - La repetición integral de tests API quedó impedida por un error 500 del motor Linux de Docker
    Desktop. No fue un fallo de aserción: Testcontainers informó que no encontraba un entorno
    Docker. La integración específica ya había pasado antes de la incidencia.

## Conversación 58 - Galería opcional del local

- Fecha: 2026-07-01.
- Resumen de la conversación:
  - Se confirmó `2.8` como primera tarea pendiente.
  - Se implementó listado, carga, reordenación, borrado y lectura pública de galería.
  - Se fijó máximo MVP de ocho imágenes, alt text obligatorio y posiciones contiguas.
  - Se reutilizaron recodificación y bucket privado de `2.7`.
  - V15 añade metadatos seguros y unicidad de posición diferible.
- Archivos modificados:
  - Nueva migración `V15__secure_venue_gallery_images.sql`.
  - Nuevos entidad/DAO, DTOs, controlador y servicio de galería.
  - Advice, pruebas de controlador/servicio/migración y documentación `.kiro`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-006`, `RNF-008`, `RNF-011`.
- Tareas impactadas y completadas:
  - `2.8. Implementar galería opcional`.
- Siguiente tarea pendiente recomendada:
  - `2.9. Implementar publicación de local solo con email verificado, verificación empresarial
    aprobada y datos mínimos`.
- Decisiones o aclaraciones relevantes:
  - Ocho imágenes equilibra experiencia MVP, almacenamiento y renderizado.
  - Alt text es obligatorio para accesibilidad.
  - Reordenar exige el snapshot completo de IDs propios.
  - Las imágenes no se convierten en principales automáticamente.
  - Evidencia focalizada: 13 tests unitarios y 7 tests de migración correctos.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 206 tests API y ambos builds.

## Conversación 59 - Publicación condicionada del local

- Fecha: 2026-07-01.
- Resumen:
  - Se confirmó `2.9` como primera tarea pendiente.
  - Se reutilizó la barrera empresarial creada en `1.11` dentro de la transacción de publicación.
  - Se añadió validación de categoría, traducciones, imagen, dirección y coordenadas mínimas.
  - La transición a `published` fija `publishedAt` y es idempotente.
  - Los rechazos agregan requisitos seguros y accionables.
- Archivos modificados:
  - Nuevos servicio, implementación, enum, excepción y DTO de publicación.
  - Controlador/advice del perfil y pruebas unitarias, REST e integración.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`, `RF-031`, `RF-032`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-008`.
- Tareas impactadas y completadas:
  - `2.9. Implementar publicación de local solo con email verificado, verificación empresarial
    aprobada y datos mínimos`.
- Siguiente tarea pendiente recomendada:
  - `2.10. Crear ficha pública inicial del local con textos vía i18n`.
- Decisiones:
  - Descripción requiere ES/EN; documentos opcionales requieren ambos solo cuando existen.
  - Los datos mínimos geográficos son dirección, ciudad, país y coordenadas.
  - Imagen principal es obligatoria; galería no.
  - No se crea migración: V9 ya soporta estado y `publishedAt`.
  - Evidencia focalizada: 12 tests de elegibilidad/publicación y 9 tests de transición/regresión.
  - Evidencia integral: `npm run verify` correcto, con 82 tests web, 210 tests API y ambos builds.

## Conversación 60 - Ficha pública inicial localizada

- Fecha: 2026-07-01.
- Resumen:
  - Se confirmó `2.10` como primera tarea pendiente.
  - Se creó lectura anónima por slug limitada en SQL a perfiles `published`.
  - Categoría y textos dinámicos se resuelven en ES/EN sin exponer JSONB.
  - `showPhone` y `showEmail` se aplican antes de serializar.
  - `/locales/[slug]` usa SSR, metadata, Zod y diseño responsive.
  - Reservas y valoraciones futuras se comunican sin simular disponibilidad.
- Archivos modificados:
  - Nuevos controlador, servicio, DTOs y pruebas públicas en `venues`.
  - Mapeo de categoría, consultas de local/galería y advice.
  - Nueva ruta y feature `public-venue`, pruebas y catálogos ES/EN.
  - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004`, `RF-008`, `RF-009`, `RF-031`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-004`, `RNF-008`, `RNF-011`.
- Tareas impactadas y completadas:
  - `2.10. Crear ficha pública inicial del local con textos vía i18n`.
- Siguiente tarea pendiente recomendada:
  - `2.11. Crear panel de edición de perfil`.
- Decisiones:
  - Un perfil no publicado es indistinguible de un slug inexistente.
  - Locale no soportado cae a inglés; el contenido conserva fallback al idioma fuente.
  - El alt text actual es neutro; localizarlo exige evolución de modelo.
  - Se usa `no-store` hasta disponer de invalidación editorial.
  - No se inventan horarios, puntuaciones ni disponibilidad.
  - Evidencia: 5 tests backend y 5 web focalizados, 87 tests web completos, lint, formato, tipos y
    ambos builds correctos.
  - `npm run verify` agotó el límite durante Testcontainers; no registró fallos de aserción y las
    comprobaciones de esta tarea pasaron aisladas.

## Conversación 61 - Panel de edición de perfil

- Fecha: 2026-07-07.
- Resumen:
  - Se confirmó `2.11` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica reciente.
  - Se creó la ruta privada `/panel/perfil` con `VenueShell`, metadatos no indexables y componente
    cliente para operar con cookie `HttpOnly`.
  - Se implementó el editor de perfil con secciones de identidad, textos localizados ES/EN,
    ubicación, contacto visible, imagen principal, galería y publicación.
  - Se añadió cliente frontend para `GET/POST/PATCH /api/venue/me`, publicación, imagen principal,
    galería y resolución de URLs de assets.
  - Se añadió parser de formulario con normalización de blancos, país, coordenadas, visibilidad y
    documentos localizados sin duplicar reglas de dominio sensibles.
  - Se incorporó un endpoint de categorías activas `GET /api/public/categories` para alimentar el
    selector sin hardcodear seeds en la UI.
  - La navegación del panel sustituyó `Más` por `Perfil` como sección principal en desktop y móvil.
- Archivos modificados:
  - Nuevos backend:
    - `VenueCategoryController` y `VenueCategoryControllerImpl`.
    - `VenueCategoryService` y `VenueCategoryServiceImpl`.
    - `VenueCategoryResponse`.
    - `VenueCategoryServiceTests` y `VenueCategoryControllerTests`.
  - Backend modificado:
    - `CategoryDao` con consulta de categorías activas ordenadas.
  - Nuevos frontend:
    - `apps/web/src/app/panel/perfil/page.tsx`.
    - `features/venue-profile/venue-profile-api.ts`.
    - `features/venue-profile/venue-profile-schema.ts`.
    - `features/venue-profile/venue-profile-editor.tsx`.
    - Tests de API, schema y editor.
  - Frontend modificado:
    - `VenueShell`, catálogos `es`/`en` y `messages.test.ts`.
  - Documentación:
    - `design.md`, `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-008 Gestión de imágenes del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RF-032 Verificación empresarial para publicación de locales`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.11. Crear panel de edición de perfil`.
  - Prepara `2.12`, `2.13` y las futuras pestañas personalizadas `2.15`.
- Tareas completadas:
  - `2.11. Crear panel de edición de perfil`.
- Siguiente tarea pendiente recomendada:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
- Decisiones o aclaraciones relevantes:
  - El panel no replica reglas críticas de backend: propiedad, límites editoriales, publicación e
    imágenes seguras siguen siendo autoridad del API.
  - `GET /api/public/categories` se limita a categorías activas y nombres resueltos; no sustituye al
    futuro CRUD admin de categorías.
  - La creación del primer perfil queda soportada por el cliente API mediante `POST`; el componente
    usa los mismos campos y la validación cliente no añade estado ni propietario.
  - Las subidas de imagen se ofrecen después de existir perfil, porque los endpoints de imagen
    requieren un perfil vigente del propietario.
  - `npm run test:web` completo volvió a agotar el timeout de dos tests antiguos de UI en Vitest
    durante carga MUI/jsdom; no hubo fallo de aserción del perfil. Se ejecutaron suites focalizadas
    correctas y build web/API correcto.

## Conversación 62 - Tests de permisos entre propietarios de locales

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.12` como primera tarea pendiente tras revisar el estado de `tasks.md` y el
    contexto reciente de especificación y seguimiento.
  - Se añadieron pruebas backend para demostrar que un local autenticado no puede leer, actualizar,
    archivar ni operar imágenes de un perfil perteneciente a otro propietario.
  - Se cubrió el servicio transaccional de perfil con una prueba de integración sobre PostgreSQL
    Testcontainers, verificando además que el perfil original conserva su estado y datos tras
    intentos cruzados.
  - Se cubrieron los servicios de imagen principal y galería con pruebas unitarias que fuerzan la
    ausencia de local editable para el propietario autenticado y validan que no se escriben objetos
    ni entidades cuando falla la autorización por propiedad.
  - No se modificaron contratos REST, migraciones ni modelos de dominio; la iteración endurece la
    red de regresión sobre reglas de propiedad ya implementadas.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueMainImageServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueGalleryServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-011 Convenciones de nomenclatura`.
  - Refuerzo indirecto de `RF-004`, `RF-008` y `RF-009`, porque los tests protegen edición de
    perfil, imagen principal y galería.
- Tareas impactadas:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
  - Prepara la siguiente cobertura de publicación `2.13`.
- Tareas completadas:
  - `2.12. Crear tests de permisos para que un local no edite datos de otro`.
- Siguiente tarea pendiente recomendada:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
- Decisiones o aclaraciones relevantes:
  - La autorización entre locales se valida desde los servicios, no desde datos enviados por cliente:
    todas las operaciones usan `ownerUserId` derivado de la sesión.
  - Un intento cruzado se representa como `VenueProfileNotFoundException`, preservando privacidad al
    no revelar si el recurso existe para otro propietario.
  - Las pruebas de imagen validan explícitamente ausencia de efectos secundarios en almacenamiento y
    persistencia cuando no hay local editable para el propietario autenticado.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=VenueProfileServiceIntegrationTests,VenueMainImageServiceTests,VenueGalleryServiceTests" test`
    correcto con 13 tests, 0 fallos, Spotless y Checkstyle correctos dentro del ciclo Maven.

## Conversación 63 - Tests de bloqueo de publicación por verificación empresarial

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.13` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se añadió cobertura unitaria directa en la política de elegibilidad para demostrar que
    `pending_remote_check`, `pending_review` y `rejected` bloquean la publicación cuando no existe
    revisión manual aprobada.
  - Se añadió cobertura de integración en `VenueProfileServiceIntegrationTests` para demostrar que
    un perfil editorialmente completo, con email verificado, imagen principal, descripción ES/EN,
    dirección y coordenadas, permanece en `draft` si la verificación empresarial está pendiente o
    rechazada.
  - Se ajustaron fixtures de integración para respetar el constraint real de
    `pending_remote_check`: ese estado exige `activeVerificationRequestId`.
  - No se modificaron servicios productivos, contratos REST, migraciones ni modelos; la iteración
    refuerza la red de regresión sobre la barrera de publicación existente.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/businessverification/service/VenuePublicationEligibilityPolicyTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueProfileServiceIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-032 Verificación empresarial para publicación de locales`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-010 Verificación empresarial remota`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
  - Prepara `2.14`, porque las futuras pestañas personalizadas deben respetar la misma barrera antes
    de exposición pública.
- Tareas completadas:
  - `2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada`.
- Siguiente tarea pendiente recomendada:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
- Decisiones o aclaraciones relevantes:
  - Se interpreta "pendiente" en sentido amplio: `pending_remote_check` y `pending_review` deben
    bloquear publicación salvo aprobación manual explícita.
  - `rejected` también queda cubierto como estado no aprobatorio definitivo.
  - El error público sigue siendo `VENUE_PUBLICATION_REJECTED` con requisito cerrado
    `BUSINESS_VERIFICATION_NOT_APPROVED`; no se exponen proveedor, identificador fiscal ni evidencia.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicationEligibilityPolicyTests,VenuePublicationEligibilityServiceIntegrationTests,VenueProfileServiceIntegrationTests,VenuePublicationServiceTests" test`
    correcto con 21 tests, 0 fallos, Spotless y Checkstyle correctos dentro del ciclo Maven.

## Conversación 64 - Migración de pestañas personalizadas del local

- Fecha: 2026-07-07.
- Resumen de la conversación:
  - Se confirmó `2.14` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica reciente.
  - Se creó la migración Flyway `V16__create_venue_custom_tabs.sql` con tabla física
    `VenueCustomTabs`, siguiendo la convención `UpperCamelCase`.
  - La tabla incorpora `venueId`, `position`, `isActive`, `titleI18n`, `contentI18n`,
    `contentFormat`, `createdAt` y `updatedAt`.
  - Se añadieron constraints para pertenencia al local, rango de orden, unicidad diferible por
    local/posición, estructura i18n, traducciones ES/EN obligatorias en pestañas activas, formato
    `safe_html`, timestamps coherentes y bloqueo de patrones HTML peligrosos evidentes.
  - Se amplió `DatabaseMigrationIntegrationTests` para esperar versión Flyway 16, auditar columnas,
    comprobar índices y validar restricciones de orden, i18n y contenido inseguro.
  - Se documentó el modelo inicial en `design.md`.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V16__create_venue_custom_tabs.sql`.
  - `apps/api/src/test/java/com/reserly/platform/configuration/DatabaseMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/design.md`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
  - Prepara `2.15`, `2.16` y `2.17`.
- Tareas completadas:
  - `2.14. Crear migración de venue_custom_tabs con orden, estado activo, contenido seguro y campos localizados`.
- Siguiente tarea pendiente recomendada:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
- Decisiones o aclaraciones relevantes:
  - `venue_custom_tabs` se materializa físicamente como `VenueCustomTabs`.
  - Se permite borrador inactivo con idioma fuente, pero una pestaña activa exige ES/EN.
  - `contentFormat` queda fijado a `safe_html`; el saneador profundo se implementará en el CRUD,
    mientras la base aplica defensa adicional contra patrones peligrosos evidentes.
  - La unicidad de posición es diferible para permitir reordenaciones atómicas.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests" test` correcto
    con 8 tests, 0 fallos, Spotless y Checkstyle correctos.

## Conversación 65 - CRUD privado de pestañas personalizadas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.15` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se implementó el CRUD privado de pestañas personalizadas del local autenticado: listado,
    creación, edición, reordenación exacta, activación/desactivación y borrado con compactación.
  - Se añadieron entidad JPA, DAO con consultas explícitas por propietario, servicio transaccional,
    saneador HTML conservador, DTOs, conversor y controlador REST bajo `/api/venue/me/custom-tabs`.
  - El servicio deriva siempre el local desde el `ownerUserId` autenticado; no acepta `venueId` desde
    el cliente y responde como no encontrado ante accesos fuera de propiedad.
  - Se normalizan títulos a texto plano, se sanea contenido HTML con allowlist sin atributos y se
    exige ES/EN antes de activar una pestaña.
  - Se mantienen posiciones contiguas `0..n-1`, límite de 16 pestañas y formato persistido
    `safe_html`.
- Archivos modificados:
  - Nuevos backend:
    - `VenueCustomTabEntity`, `VenueCustomTabDao`.
    - `VenueCustomTabService`, `VenueCustomTabServiceImpl`,
      `VenueCustomTabHtmlSanitizer`, `VenueCustomTabInvalidException`,
      `VenueCustomTabLimitException`.
    - `VenueCustomTabController`, `VenueCustomTabControllerImpl`.
    - `VenueCustomTabConverter`.
    - DTOs `VenueCustomTabRequest`, `VenueCustomTabResponse`, `VenueCustomTabOrderRequest`,
      `VenueCustomTabLocalizedTextDto` y `VenueCustomTabCommand`.
    - Tests `VenueCustomTabServiceTests` y `VenueCustomTabControllerTests`.
  - Modificados:
    - `CategoryDao`, ajustado para que la query explícita sea compatible con el validador de
      convenciones y Checkstyle.
    - `VenueProfileExceptionHandler`.
    - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
    - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
    - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
  - Prepara `2.16` porque deja lectura privada, orden, activación y contenido saneado listos para
    proyectarse en la ficha pública.
  - Prepara `2.17` porque ya existen pruebas base de permisos, orden, sanitización e i18n del CRUD.
- Tareas completadas:
  - `2.15. Implementar CRUD de pestañas personalizadas del local para propietario`.
- Siguiente tarea pendiente recomendada:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
- Decisiones o aclaraciones relevantes:
  - En esta iteración no se implementa lectura pública; queda para `2.16`.
  - El contenido HTML se sanea de forma conservadora: solo etiquetas editoriales simples sin
    atributos (`p`, `br`, `ul`, `ol`, `li`, `strong`, `em`, `b`, `i`). Cualquier texto queda
    escapado.
  - No se añade dependencia externa de sanitización para evitar ampliar superficie y descargas; si se
    requieren enlaces, tablas o menús estructurados, deberá definirse allowlist específica.
  - El error REST de validación es `VENUE_CUSTOM_TAB_INVALID`; el límite usa
    `VENUE_CUSTOM_TAB_LIMIT_REACHED`.
  - Evidencia: `mvn -f apps/api/pom.xml "-Dtest=DatabaseMigrationIntegrationTests,VenueCustomTabServiceTests,VenueCustomTabControllerTests" test`
    correcto con 15 tests, 0 fallos, Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run spanish:text:check` y `npm run backend:conventions:check`
    correctos.

## Conversación 66 - Pestañas personalizadas activas en ficha pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.16` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se extendió la proyección pública `GET /api/public/venues/{slug}` para incluir únicamente
    pestañas activas de locales publicados.
  - Se añadió DTO público localizado de pestañas con `title`, `content`, `position` y
    `contentFormat`.
  - Se añadió consulta pública ordenada por `position` sobre `VenueCustomTabs`, filtrando
    `isActive = true` y `venue.status = 'published'`.
  - Se actualizó la ficha Next.js `/locales/[slug]` para renderizar las pestañas dentro del bloque
    principal de detalles, usando el HTML seguro ya saneado por backend.
  - Se actualizó el esquema Zod público para rechazar contratos alterados y exigir
    `contentFormat = safe_html`.
- Archivos modificados:
  - Backend:
    - `VenuePublicCustomTabResponse`.
    - `VenuePublicProfileResponse`.
    - `VenueCustomTabDao`.
    - `VenuePublicProfileServiceImpl`.
    - `VenuePublicProfileServiceTests`.
    - `VenuePublicProfileControllerTests`.
  - Frontend:
    - `public-venue-api.ts`.
    - `public-venue-api.test.ts`.
    - `public-venue-profile.tsx`.
    - `public-venue-profile.test.tsx`.
  - Documentación:
    - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
    - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
    - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-007 Usabilidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
  - Prepara `2.17`, que deberá ampliar cobertura de permisos, orden, publicación, sanitización e
    i18n alrededor de pestañas personalizadas.
- Tareas completadas:
  - `2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local`.
- Siguiente tarea pendiente recomendada:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Decisiones o aclaraciones relevantes:
  - La respuesta pública no expone IDs de pestañas, `venueId`, propietario ni documentos JSONB
    completos.
  - La UI renderiza `safe_html` con `dangerouslySetInnerHTML` solo porque el contenido ya fue saneado
    por backend en `2.15` y el contrato público exige ese formato.
  - No se muestran pestañas inactivas ni pestañas de perfiles no publicados.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicProfileServiceTests,VenuePublicProfileControllerTests" test`
    correcto con 5 tests, 0 fallos, Spotless y Checkstyle correctos.
  - Evidencia frontend: `npm run test --workspace @reserly/web -- public-venue-api.test.ts public-venue-profile.test.tsx`
    correcto con 5 tests, 0 fallos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check`,
    `npm run format:check:web` y `npm run typecheck --workspace @reserly/web` correctos.
  - Evidencia de build UI: `npm run build:web:test` correcto.

## Conversación 67 - Tests de pestañas personalizadas

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `2.17` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se ampliaron los tests unitarios del servicio privado de pestañas para cubrir permisos sobre
    pestañas ajenas, rechazo de reordenaciones no exactas con IDs duplicados y rechazo de contenido
    HTML sin texto visible tras sanitización.
  - Se reforzó el test unitario de la ficha pública para comprobar que, si el slug no corresponde a
    un local publicado, no se consulta el DAO de pestañas.
  - Se añadió una prueba de integración con Spring Boot, Flyway, JPA y PostgreSQL Testcontainers que
    crea un propietario, verifica email y cuenta empresarial, crea un local publicable, crea pestañas
    activas e inactivas, reordena, publica y comprueba la respuesta pública en ES/EN.
- Archivos modificados:
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicProfileServiceTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenueCustomTabPublicationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-004 Ficha pública del local`.
  - `RF-009 Gestión de perfil público`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Tareas completadas:
  - `2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas`.
- Siguiente tarea pendiente recomendada:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
- Decisiones o aclaraciones relevantes:
  - No se modificó código productivo; la iteración se limita a cobertura automatizada y documentación.
  - La publicación de pestañas se valida desde la ruta pública real, no solo mediante mocks, para
    cubrir la consulta `findAllPublishedActiveByVenueId` contra el esquema migrado.
  - La prueba de sanitización pública verifica que el HTML expuesto no conserva `<script>`,
    `onclick` ni `javascript:` después de persistir contenido creado por el servicio.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenueCustomTabServiceTests,VenuePublicProfileServiceTests,VenueCustomTabPublicationIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.

## Conversación 68 - Endpoint base de búsqueda pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.1` como primera tarea pendiente tras revisar `tasks.md`, requisitos, diseño,
    seguimiento e implementación técnica.
  - Se implementó `GET /api/public/venues/search` como endpoint público base de descubrimiento.
  - Se añadió una respuesta paginada con tarjetas mínimas de locales publicados: slug, nombre,
    categoría localizada, descripción breve, imagen principal y ubicación pública.
  - Se centralizó la resolución de idioma pública para reutilizarla entre ficha y búsqueda.
  - Se añadieron tests unitarios de servicio y controlador.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicLocaleResolver.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicProfileControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-004 Ficha pública del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
  - Prepara `3.2`, `3.3`, `3.4`, `3.5`, `3.6` y `3.7`, que ampliarán búsqueda textual,
    filtros, radio, ordenación y estado resumido.
- Tareas completadas:
  - `3.1. Implementar endpoint GET /api/public/venues/search`.
- Siguiente tarea pendiente recomendada:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
- Decisiones o aclaraciones relevantes:
  - Esta iteración no implementa aún parámetros `q`, categoría, ciudad, radio ni ordenación por
    relevancia; esos comportamientos quedan para las tareas específicas de Fase 3.
  - El endpoint solo devuelve locales con `status = 'published'`.
  - La respuesta no expone IDs internos, propietario, cuenta empresarial, datos fiscales ni contacto
    directo.
  - La paginación pública normaliza `page < 0` a `0`, `size <= 0` a `20` y limita `size` a `50`.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests" test`
    correcto con 6 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check` y `npm run spanish:text:check`
    correctos.

## Conversación 70 - Filtros públicos por categoría

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.3` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional repetible `category`.
  - Se implementó el filtrado estructurado por slug público de categoría, independiente del texto
    libre y combinable con `q`.
  - Se normalizan los slugs recibidos eliminando blancos, convirtiendo a minúsculas y deduplicando
    en orden de llegada antes de consultar persistencia.
  - Se añadieron consultas DAO específicas para listado y conteo con categorías, y para la
    intersección entre búsqueda textual y categorías.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para validar el
    filtro por `restaurante`, `pista-de-padel` y la combinación `q=padel&category=restaurante`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.3. Añadir filtros por categoría`.
  - Prepara `3.10`, que añadirá el panel visual de filtros desktop y móvil sobre este contrato.
- Tareas completadas:
  - `3.3. Añadir filtros por categoría`.
- Siguiente tarea pendiente recomendada:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
- Decisiones o aclaraciones relevantes:
  - El contrato usa `category` como parámetro repetible por slug público, por ejemplo
    `/api/public/venues/search?category=restaurante&category=pista-de-padel`.
  - Los slugs de categoría son identificadores públicos estables; no se exponen IDs internos.
  - `category` vacío, nulo o solo con blancos se ignora y conserva el comportamiento base.
  - Cuando hay `q` y `category`, ambos filtros se cruzan con `AND`; no se mezclan como palabras clave.
  - La respuesta no añade campos nuevos y conserva los textos visibles localizados.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 10 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 76 - Cierre de tests y traducciones de búsqueda pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.13` y `3.14`.
  - Se amplió la cobertura del cliente `searchPublicVenues` para filtros vacíos, paginación positiva,
    `size` y errores HTTP.
  - Se amplió la cobertura de `PublicSearchResultsView` para vacío genérico sin `q`, vacío específico
    de local no encontrado, filtros, tarjetas y carriles de descubrimiento vacíos.
  - Se añadió un test explícito de contrato de traducciones para `HomePage` y `PublicSearch` en ES/EN,
    cubriendo buscador, filtros, resultados, estados vacíos, tarjetas, categorías, ordenación y
    carriles.
  - Se cerró la Fase 3 dejando `4.1` como siguiente tarea recomendada.
- Archivos modificados:
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/src/features/public-search/public-search-translations.test.ts`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-030 Recomendaciones`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.13. Crear tests de búsqueda y filtros`.
  - `3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas`.
- Tareas completadas:
  - `3.13. Crear tests de búsqueda y filtros`.
  - `3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas`.
- Siguiente tarea pendiente recomendada:
  - `4.1. Crear migraciones de venue_opening_hours, time_slots y availability_blocks`.
- Decisiones o aclaraciones relevantes:
  - Las traducciones de búsqueda quedan verificadas con un test específico, además del validador
    global de catálogos.
  - El test de API cubre que filtros en blanco no se envían y que errores HTTP se propagan con un
    mensaje controlado.
  - El test de vista cubre que el estado vacío genérico no ofrece registro de local, mientras el vacío
    con `q` sí lo ofrece.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx src/features/public-search/public-search-translations.test.ts --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 3 ficheros, 9 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 75 - Carriles de descubrimiento y vacío de local no encontrado

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.11` y `3.12`.
  - Se amplió `/explorar` para cargar carriles iniciales de descubrimiento mediante llamadas simples
    al endpoint público: recomendados, destacados y cercanos.
  - Se añadió `size` al cliente de búsqueda pública para limitar las peticiones auxiliares de
    carriles.
  - Se mostró un bloque "También puedes explorar" con tres secciones y enlaces compactos a fichas de
    local.
  - Se mejoró el estado vacío para distinguir búsquedas con texto: cuando hay `q` y no hay resultados,
    la pantalla comunica "No encontramos ese local" y ofrece limpiar filtros o registrar el local.
  - Se actualizaron traducciones ES/EN y pruebas focalizadas.
- Archivos modificados:
  - `apps/web/src/app/explorar/page.tsx`.
  - `apps/web/src/features/public-search/public-search-api.ts`.
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-030 Recomendaciones`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
  - `3.12. Crear estado vacío para local no encontrado`.
  - Prepara `3.13`, que consolidará tests de búsqueda y filtros.
- Tareas completadas:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
  - `3.12. Crear estado vacío para local no encontrado`.
- Siguiente tarea pendiente recomendada:
  - `3.13. Crear tests de búsqueda y filtros`.
- Decisiones o aclaraciones relevantes:
  - `recommended` usa `sort=availability` y `size=3`.
  - `featured` usa `sort=rating` y `size=3`; el backend mantiene fallback estable hasta que existan
    reseñas o criterio editorial.
  - `nearby` usa la ubicación textual actual cuando existe; sin ubicación, cae a una selección simple
    por disponibilidad.
  - No se solicita geolocalización ni se persiste ubicación del usuario.
  - El estado vacío específico solo se activa cuando hay texto de búsqueda `q`, para no confundir un
    vacío por filtros amplios con un local concreto no encontrado.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto; Next compila `/explorar`.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 74 - Resultados públicos con tarjetas y filtros responsive

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.9` y `3.10`.
  - Se creó la ruta pública `/explorar` como pantalla server-side de resultados.
  - Se añadió un cliente de búsqueda pública validado con Zod para consumir
    `GET /api/public/venues/search` desde Next.js sin reenviar cookies.
  - Se creó la vista de resultados con tarjetas de local que muestran imagen principal, nombre,
    categoría, ubicación aproximada, estado, valoración pendiente, descripción breve y disponibilidad
    resumida.
  - Se añadió panel de filtros desktop como lateral y panel móvil como bloque desplegable, ambos con
    filtros soportados por backend: texto, ubicación, categoría y ordenación.
  - Se añadieron traducciones ES/EN y tests focalizados del cliente API y de la vista.
- Archivos modificados:
  - `apps/web/src/app/explorar/page.tsx`.
  - `apps/web/src/features/public-search/public-search-api.ts`.
  - `apps/web/src/features/public-search/public-search-api.test.ts`.
  - `apps/web/src/features/public-search/public-search-results.tsx`.
  - `apps/web/src/features/public-search/public-search-results.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.9. Crear pantalla de resultados con tarjetas`.
  - `3.10. Crear panel de filtros desktop y móvil`.
  - Prepara `3.11`, que añadirá secciones de recomendados, destacados y cercanos.
- Tareas completadas:
  - `3.9. Crear pantalla de resultados con tarjetas`.
  - `3.10. Crear panel de filtros desktop y móvil`.
- Siguiente tarea pendiente recomendada:
  - `3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple`.
- Decisiones o aclaraciones relevantes:
  - Se consolida `/explorar` como ruta pública de resultados, coherente con la home ya implementada.
  - La pantalla usa renderizado server-side y `fetch(..., { cache: "no-store" })` hasta definir
    invalidación de caché para resultados públicos.
  - El panel móvil se implementa con `details/summary` para evitar estado cliente y mantener una
    interacción accesible y ligera.
  - No se añade filtro por disponibilidad real ni valoración mínima porque el backend todavía no
    expone contratos para esos filtros; solo se permite ordenar por modos ya soportados.
  - Las tarjetas comunican valoraciones como próximas para no simular métricas inexistentes.
  - Evidencia frontend: `npm exec vitest -- run src/features/public-search/public-search-api.test.ts src/features/public-search/public-search-results.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests y 0 fallos.
  - Evidencia build: `npm run build:web:test` correcto; Next compila `/explorar` como ruta dinámica.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run lint:web`,
    `npm run i18n:check`, `npm run spanish:text:check`, `npm exec prettier -- --check ...` y
    `git diff --check` correctos.

## Conversación 73 - Estado resumido de resultados e inicio con buscador

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron como siguientes tareas pendientes `3.7` y `3.8`.
  - Se amplió la tarjeta pública devuelta por `GET /api/public/venues/search` con estado resumido
    del local: `statusCode`, `statusLabel`, `availabilitySummary` y `bookingAvailable`.
  - El estado se deriva de `manualAvailabilityStatus` como aproximación inicial:
    `available`, `unavailable` y `availability_pending`.
  - Se mantuvo explícita la limitación de que la disponibilidad real por horarios, capacidad y
    franjas pertenece a la Fase 4.
  - Se sustituyó la home de demostración por una pantalla pública funcional con el mensaje
    "¿Dónde quieres pedir cita hoy?", buscador principal, campos `q` y `location`, botón de envío y
    accesos rápidos por categorías.
  - Se añadieron traducciones ES/EN para el nuevo buscador y se eliminó el texto visible hardcodeado
    de la home.
  - Se ajustaron nombres técnicos `_es` y `_en` del editor de perfil para que el validador i18n no
    los trate como texto visible.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/dto/VenueSearchItemResponse.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `apps/web/src/app/page.tsx`.
  - `apps/web/src/app/page.test.tsx`.
  - `apps/web/locales/es.json`.
  - `apps/web/locales/en.json`.
  - `apps/web/src/features/venue-profile/venue-profile-editor.tsx`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-010 Accesibilidad`.
- Tareas impactadas:
  - `3.7. Añadir estado resumido de local en resultados`.
  - `3.8. Crear pantalla de inicio con buscador y mensaje principal`.
  - Prepara `3.9`, que creará la pantalla de resultados con tarjetas.
- Tareas completadas:
  - `3.7. Añadir estado resumido de local en resultados`.
  - `3.8. Crear pantalla de inicio con buscador y mensaje principal`.
- Siguiente tarea pendiente recomendada:
  - `3.9. Crear pantalla de resultados con tarjetas`.
- Decisiones o aclaraciones relevantes:
  - `manualAvailabilityStatus = available` se expone como `statusCode = available`,
    `bookingAvailable = true` y etiqueta localizada.
  - `manualAvailabilityStatus = unavailable` se expone como `statusCode = unavailable` y
    `bookingAvailable = false`.
  - `automatic`, valores desconocidos o nulos caen en `availability_pending` para no prometer
    disponibilidad real antes de implementar horarios y franjas.
  - La home envía el formulario por `GET` a `/explorar` con parámetros `q` y `location`, compatible
    con la futura pantalla de resultados.
  - Los accesos rápidos enlazan a `/explorar?category=...` usando slugs estables ya soportados por el
    backend.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 13 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia frontend: `npm exec --workspace @reserly/web vitest -- run src/app/page.test.tsx src/features/venue-profile/venue-profile-editor.test.tsx --pool=threads --maxWorkers=1 --testTimeout=20000`
    correcto con 2 ficheros, 3 tests, 0 fallos.
  - Evidencia adicional: `npm run typecheck --workspace @reserly/web`, `npm run i18n:check`,
    `npm run spanish:text:check`, `npm run backend:conventions:check`, `npm run lint:web`,
    `npm exec prettier -- --check ...` y `git diff --check` correctos.
  - La suite web completa con `vitest run --pool=threads --maxWorkers=1 --testTimeout=20000` no
    emitió resultados antes del timeout del comando; se verificaron los tests afectados de forma
    focalizada.

## Conversación 72 - Radio geográfico y ordenación pública

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmaron `3.5` y `3.6` como las dos siguientes tareas pendientes tras revisar `tasks.md`,
    seguimiento e implementación técnica.
  - Se amplió `GET /api/public/venues/search` con `latitude`, `longitude`, `radiusKm` y `sort`.
  - Se implementó filtro por radio usando PostGIS sobre la columna generada `Venues.location` y el
    índice GiST existente.
  - Se consolidó la búsqueda pública en un método DAO nativo avanzado con filtros opcionales para
    texto, categoría, ubicación textual, radio y ordenación controlada.
  - Se añadieron ordenaciones públicas por `relevance`, `distance`, `availability`, `rating` y
    `newest`.
  - Se documentó que `rating` queda como modo contractual con fallback estable hasta que exista el
    modelo de reseñas; `availability` usa `manualAvailabilityStatus` como señal disponible en el
    modelo actual hasta que lleguen franjas y disponibilidad real.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para radio,
    ordenación por cercanía y ordenación por disponibilidad manual.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-005 Estado público del local`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
  - `3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad`.
  - Prepara `3.7`, que añadirá estado resumido de local en resultados.
- Tareas completadas:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
  - `3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad`.
- Siguiente tarea pendiente recomendada:
  - `3.7. Añadir estado resumido de local en resultados`.
- Decisiones o aclaraciones relevantes:
  - El filtro por radio solo se activa con `latitude`, `longitude` y `radiusKm` válidos.
  - El radio se limita a un máximo de 500 km para evitar consultas públicas desproporcionadas.
  - `sort=distance` ordena por `ST_Distance` cuando hay coordenadas; sin coordenadas cae al orden
    estable.
  - `sort=relevance` prioriza coincidencias en nombre, categoría y descripción cuando hay `q`.
  - `sort=availability` usa `manualAvailabilityStatus` como aproximación inicial hasta implementar
    disponibilidad por franjas.
  - `sort=rating` queda aceptado y estable, pero sin ranking real hasta que existan reseñas.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 71 - Filtro público por ubicación textual

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se continuó en la rama de fase `phase/3-busqueda-publica-descubrimiento`.
  - Se confirmó `3.4` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional `location`.
  - Se implementó el filtrado por ciudad, zona/provincia, dirección, código postal o país mediante
    comparación normalizada sin tildes ni mayúsculas.
  - Se combinó `location` con los filtros previos `q` y `category` usando intersección.
  - Se añadieron consultas DAO de listado y conteo para ubicación sola, ubicación con categoría,
    ubicación con texto y ubicación con texto más categoría.
  - Se añadieron pruebas unitarias y de integración con PostgreSQL Testcontainers para validar
    Madrid, València sin tilde y dirección `Xàtiva` buscada como `xativa`.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-002 Filtros avanzados`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-009 Gestión de perfil público`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
  - Prepara `3.5`, que añadirá filtro por radio con coordenadas.
- Tareas completadas:
  - `3.4. Añadir filtros por ciudad, zona o dirección normalizada`.
- Siguiente tarea pendiente recomendada:
  - `3.5. Añadir filtro por radio si hay coordenadas`.
- Decisiones o aclaraciones relevantes:
  - El contrato usa `location` como parámetro textual único para ciudad, zona/provincia, dirección,
    código postal o país.
  - `location` vacío o en blanco se ignora y conserva el comportamiento previo.
  - La comparación usa `lower(unaccent(...)) LIKE :locationPattern ESCAPE '\'`.
  - `location` se cruza con `q` y `category` mediante `AND`; no se mezcla con la búsqueda textual
    general como palabra clave.
  - No se persiste ni procesa ubicación precisa de usuario en esta tarea; el radio queda para `3.5`.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 12 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check`, `npm run spanish:text:check` y
    `git diff --check` correctos.

## Conversación 69 - Búsqueda pública por nombre y palabras clave

- Fecha: 2026-07-08.
- Resumen de la conversación:
  - Se confirmó `3.2` como primera tarea pendiente tras revisar `tasks.md`, seguimiento e
    implementación técnica.
  - Se amplió `GET /api/public/venues/search` con el parámetro opcional `q`.
  - Se añadió búsqueda textual sobre locales publicados por nombre, descripción canónica y categoría
    como palabras clave públicas.
  - Se normalizó el término de búsqueda en servicio para comparar sin mayúsculas ni tildes mediante
    `unaccent`, escapando comodines de `LIKE`.
  - Se mantuvo intacta la respuesta visible: los textos públicos siguen saliendo con tildes y sin
    normalización destructiva.
  - Se añadieron tests unitarios para propagación de `q`, normalización de `Café` a patrón `cafe` y
    uso de consultas DAO específicas.
  - Se añadió una prueba de integración con PostgreSQL Testcontainers para validar `unaccent` y la
    consulta JPA real sobre locales publicados.
- Archivos modificados:
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchController.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerImpl.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/persistence/VenueDao.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchService.java`.
  - `apps/api/src/main/java/com/reserly/platform/venues/service/VenuePublicSearchServiceImpl.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/controller/VenuePublicSearchControllerTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchIntegrationTests.java`.
  - `apps/api/src/test/java/com/reserly/platform/venues/service/VenuePublicSearchServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-001 Buscador principal`.
  - `RF-003 Resultados de búsqueda`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-001 Seguridad`.
  - `RNF-002 Privacidad`.
  - `RNF-004 Rendimiento`.
  - `RNF-008 Calidad y mantenibilidad`.
  - `RNF-009 Internacionalización y localización`.
  - `RNF-011 Convenciones de nomenclatura`.
- Tareas impactadas:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
  - Prepara `3.3`, que añadirá filtros por categoría sin mezclarlo con el texto libre.
- Tareas completadas:
  - `3.2. Añadir búsqueda por nombre y palabras clave`.
- Siguiente tarea pendiente recomendada:
  - `3.3. Añadir filtros por categoría`.
- Decisiones o aclaraciones relevantes:
  - `q` vacío o en blanco conserva el listado base de `3.1`.
  - La búsqueda textual no filtra todavía por ciudad, zona, radio, disponibilidad ni valoración.
  - La comparación usa `lower(unaccent(...)) LIKE :queryPattern ESCAPE '\'`.
  - Se escapan `%`, `_` y `\` para evitar que el texto del usuario actúe como comodín no solicitado.
  - Evidencia backend: `mvn -f apps/api/pom.xml "-Dtest=VenuePublicSearchServiceTests,VenuePublicSearchControllerTests,VenuePublicProfileControllerTests,VenuePublicSearchIntegrationTests" test`
    correcto con 8 tests, 0 fallos, 0 errores y 0 omitidos; Spotless y Checkstyle correctos.
  - Evidencia transversal: `npm run backend:conventions:check` y `npm run spanish:text:check`
    correctos.

## Conversación 70 - Asignación automática y panel de equipo

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó la política interna de asignación por primera disponibilidad para el futuro hold.
  - Se creó `/panel/equipo` para recursos, horarios, servicios, asociaciones y opción delegada.
  - Se añadieron contratos Zod, estados de error y traducciones ES/EN.
  - La validación se limitó expresamente a los módulos de esta conversación.
- Archivos modificados:
  - Servicios y tests de asignación en el módulo backend de disponibilidad.
  - `apps/web/src/app/panel/equipo/page.tsx`.
  - `apps/web/src/features/team/team-api.ts`, gestor y sus tests.
  - `apps/web/src/components/layout/venue-shell.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - Los tres documentos de seguimiento de `.kiro`.
- Requisitos impactados:
  - `RF-026`, `RF-027`, `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-003`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas:
  - `5.9. Implementar asignación automática simple por primera disponibilidad`.
  - `5.10. Crear sección "Equipo y disponibilidad" en panel`.
- Siguiente tarea pendiente recomendada:
  - `5.11. Mostrar selector de servicio y profesional en reserva cuando el local lo configure`.
- Decisiones o aclaraciones relevantes:
  - No se inventó persistencia: la política se integrará en el futuro hold.
  - La primera disponibilidad toma el primer candidato del orden estable y recalcula elegibilidad.
  - El panel usa exclusivamente endpoints privados `/api/venue/me/...`.
  - Evidencia backend: 5 tests correctos de asignación.
  - Evidencia frontend: 4 tests correctos y ESLint focalizado correcto.
  - No hubo validación completa, typecheck/build global, migraciones, suite visual ni comprobaciones
    transversales por instrucción expresa del usuario.

## Conversación 71 - Cierre de fase 5 e inicio de formularios

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se cerraron los selectores públicos y la matriz de disponibilidad de 5.11/5.12.
  - La verificación focalizada descubrió y corrigió un NPE en franjas sin servicio.
  - Se creó V21 con campos personalizados y snapshots de respuestas.
  - Se implementó el catálogo inmutable de cinco campos base obligatorios.
- Archivos modificados:
  - `PublicVenueAvailabilityServiceImpl.java`.
  - `V21__create_reservation_form_tables.sql`.
  - `ReservationBaseFieldDefinition.java` y `ReservationBaseFieldCatalog.java`.
  - `ReservationBaseFieldCatalogTests.java` y `ReservationFormMigrationIntegrationTests.java`.
  - `DatabaseMigrationIntegrationTests.java`.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-006`, `RF-013`, `RF-026`, `RF-027` y `RB-010`.
  - `RNF-001`, `RNF-002`, `RNF-008` y `RNF-009`.
- Tareas impactadas y completadas:
  - `5.11`, `5.12`, `6.1` y `6.2`.
- Siguiente tarea pendiente recomendada:
  - `6.3. Implementar CRUD de campos personalizados`.
- Decisiones o aclaraciones:
  - La FK de `reservationId` se difiere hasta crear Reservations en fase 7.
  - `fieldId` usa SET NULL y se guardan key/label para conservar histórico.
  - Los cinco campos base no son configuración editable ni filas custom.
  - Evidencia 5.11/5.12: 14 tests backend focalizados, 7 frontend y ESLint focalizado.
  - Evidencia 6.1/6.2: 6 tests focalizados, Flyway V21 y PostgreSQL/PostGIS correctos.
  - No se ejecutaron suites completas, build, typecheck ni validaciones transversales.

## Conversación 72 - CRUD y tipos de campos personalizados

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó el CRUD privado de campos personalizados para el local autenticado.
  - Se modelaron los ocho tipos de RF-013 con códigos estables en REST, Java y PostgreSQL.
  - Se mantuvo la coherencia de optionsJson al entrar o salir del tipo selector.
  - La validación se limitó expresamente a las pruebas del módulo forms.
- Archivos modificados:
  - Nuevos paquetes controller, converter, dto, persistence y service bajo
    apps/api/src/main/java/com/reserly/platform/forms.
  - ReservationFormFieldServiceTests.java y ReservationFormFieldControllerTests.java.
  - Normalización de formato final en los Java existentes del mismo módulo forms.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva personalizado.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad,
    RNF-008 Calidad y mantenibilidad, RNF-009 Internacionalización y localización.
- Tareas impactadas:
  - 6.3. Implementar CRUD de campos personalizados.
  - 6.4. Implementar tipos: texto corto, texto largo, número, selector, checkbox, fecha,
    teléfono y email.
- Tareas completadas:
  - 6.3 y 6.4.
- Siguiente tarea pendiente recomendada:
  - 6.5. Implementar obligatoriedad y orden.
- Decisiones o aclaraciones relevantes:
  - Los endpoints usan /api/venue/me/reservation-form/fields y derivan el local solo de
    AuthenticatedAccount.
  - Creación, edición y eliminación usan bloqueos de escritura y consultas acotadas a propiedad.
  - La edición de 6.3 no permite aún alterar obligatoriedad, posición ni opciones.
  - Los select nacen con opciones vacías; su configuración se difiere a 6.6.
  - La eliminación es física y el histórico futuro se preserva mediante snapshots y SET NULL.
  - Evidencia: 6 tests focalizados correctos, sin fallos, errores ni omitidos.
  - Spotless se aplicó solo a forms; sus gates globales y Checkstyle global se omitieron porque
    detectaron deuda previa fuera del alcance.
  - No se ejecutaron suite completa, build, typecheck, migraciones ni validaciones transversales.

## Conversación 88 - Obligatoriedad, orden y opciones de formulario

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se añadió obligatoriedad editable a los campos personalizados.
  - Se implementó reordenación completa y transaccional del formulario propio.
  - Se implementaron opciones normalizadas y validadas para campos selector.
  - La validación se limitó al módulo forms.
- Archivos modificados:
  - DTOs ReservationFormFieldRequest, ReservationFormFieldCommand y nuevo
    ReservationFormFieldOrderRequest.
  - ReservationFormFieldConverter.
  - ReservationFormFieldDao.
  - ReservationFormFieldService y ReservationFormFieldServiceImpl.
  - ReservationFormFieldController y ReservationFormFieldControllerImpl.
  - ReservationFormFieldServiceTests y ReservationFormFieldControllerTests.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva configurable.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad y
    RNF-008 Calidad y mantenibilidad.
- Tareas impactadas:
  - 6.5. Implementar obligatoriedad y orden.
  - 6.6. Implementar opciones para campos selector.
- Tareas completadas:
  - 6.5 y 6.6.
- Siguiente tarea pendiente recomendada:
  - 6.7. Implementar previsualización del formulario.
- Decisiones o aclaraciones relevantes:
  - El orden se reemplaza mediante PUT /api/venue/me/reservation-form/fields/order.
  - La petición debe ser una permutación completa de los campos activos propios.
  - La reordenación bloquea el local y el conjunto de campos antes de validar y escribir.
  - required se configura solo en campos custom; los cinco campos base siguen siendo obligatorios.
  - Un select exige de 1 a 50 opciones únicas sin distinguir caja, de hasta 160 caracteres.
  - Otros tipos persisten optionsJson nulo y rechazan opciones no vacías.
  - optionsI18nJson continúa reservado para 6.11.
  - Evidencia: 8 tests focalizados correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless se aplicó únicamente a forms; los gates globales se omitieron.
  - No se ejecutaron suite completa, migraciones, build independiente, typecheck ni validaciones
    transversales.
## Conversación 89 - Preview y validación backend del formulario

- Fecha: 2026-07-13.
- Resumen de la conversación:
  - Se implementó la previsualización privada del formulario completo.
  - Se combinaron campos base inmutables y campos custom activos en un esquema ordenado.
  - Se implementó validación y normalización backend de respuestas por tipo.
  - La validación ejecutada se limitó al módulo forms.
- Archivos modificados:
  - ReservationFormFieldController, implementación y prueba.
  - Nuevos DTOs ReservationFormPreviewFieldResponse y ReservationFormPreviewResponse.
  - Nuevos servicios ReservationFormPreviewService y ReservationFormPreviewServiceImpl.
  - Nuevos DTOs ReservationFormAnswerCommand y ValidatedReservationFormAnswer.
  - Nuevos servicios y errores ReservationFormResponseValidator,
    ReservationFormResponseValidatorImpl, ReservationFormResponseInvalidException y
    ReservationFormResponseViolation.
  - Nuevas pruebas ReservationFormPreviewServiceTests y
    ReservationFormResponseValidatorTests.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013 Formulario de reserva configurable.
  - RNF-001 Seguridad, RNF-002 Privacidad, RNF-003 Integridad,
    RNF-008 Calidad y mantenibilidad y RNF-009 Internacionalización.
- Tareas impactadas:
  - 6.7. Implementar previsualización del formulario.
  - 6.8. Implementar validación backend de respuestas.
- Tareas completadas:
  - 6.7 y 6.8.
- Siguiente tarea pendiente recomendada:
  - 6.9. Crear UI de configuración del formulario.
- Decisiones o aclaraciones relevantes:
  - GET /api/venue/me/reservation-form/preview deriva el local de la cuenta autenticada.
  - El preview coloca primero cinco campos base y después los custom en orden contiguo.
  - Los campos base usan labelKey i18n; los custom conservan label canónico hasta 6.11.
  - El validador es un servicio interno reutilizable y no adelanta endpoints de reserva.
  - Se rechazan claves desconocidas/duplicadas, obligatorios ausentes y valores incompatibles.
  - La salida normalizada conserva snapshots y tipos JSON para la futura persistencia de fase 7.
  - Evidencia final: 10 tests focalizados correctos, 0 fallos, 0 errores y 0 omitidos.
  - La primera ejecución tuvo un único fallo de expectativa en un fixture de label; se corrigió.
  - Spotless se aplicó exclusivamente a forms; los gates globales se omitieron.
  - No se ejecutaron suite completa, migraciones, build independiente, typecheck ni validaciones
    transversales.
## Conversaci?n 90 - Configurador y tests frontend del formulario

- Fecha: 2026-07-13.
- Resumen: se implement? /panel/formulario con CRUD, orden, preview, navegaci?n e i18n ES/EN.
- Archivos modificados: ruta, cliente API, manager, tests, VenueShell, cat?logos y documentos .kiro.
- Requisitos impactados: RF-013, RNF-001, RNF-002, RNF-003, RNF-008 y RNF-009.
- Tareas completadas: 6.9 y 6.10.
- Siguiente tarea recomendada: 6.11.
- Decisiones:
  - El editor reconcilia cat?logo y preview despu?s de cada mutaci?n.
  - Los campos base son obligatorios e inmutables.
  - Vitest focalizado agot? 30 segundos; no se ejecut? validaci?n completa.

## Conversaci?n 91 - Localizaci?n y publicaci?n del formulario

- Fecha: 2026-07-13.
- Resumen de la conversaci?n:
  - Se implementaron labels y opciones custom con idioma origen y valores ES/EN.
  - Se a?adi? publicaci?n transaccional con bloqueo por traducciones incompletas.
  - Se a?adi? aprobaci?n expl?cita de fallback y despublicaci?n autom?tica al editar.
  - Se adapt? el editor, preview, cat?logos y tests focalizados.
  - La validaci?n final se interrumpi? por petici?n expresa del usuario para proceder al commit.
- Archivos modificados:
  - V22__localize_and_publish_reservation_forms.sql.
  - Entidades VenueEntity y ReservationFormFieldEntity.
  - DTOs, conversor, servicios, controlador y handler del m?dulo forms.
  - Tests forms backend y tests del configurador frontend.
  - reservation-form-api.ts, reservation-form-manager.tsx y cat?logos ES/EN.
  - tasks.md, conversation-tracking.md y technical-implementation.md.
- Requisitos impactados:
  - RF-013, RNF-001, RNF-003, RNF-009 y RNF-012.
- Tareas impactadas y completadas:
  - 6.11 y 6.12.
- Siguiente tarea pendiente recomendada:
  - 7.1. Crear migraci?n de reservations.
- Decisiones o aclaraciones relevantes:
  - Los can?nicos se derivan del idioma origen; los JSONB conservan ES/EN.
  - Cada opci?n localizada se alinea por ?ndice con optionsJson.
  - La publicaci?n exige traducciones completas o fallback aprobado expl?citamente.
  - Cualquier mutaci?n del formulario invalida su publicaci?n.
  - La API devuelve 409 estable cuando la publicaci?n est? bloqueada.
  - Evidencia parcial: 3 tests del cliente API correctos; el import Surface detectado se corrigi?.
  - La ejecuci?n final fue interrumpida y no se hicieron m?s validaciones por orden del usuario.
## Conversación 92 - Migración de reservas y endpoint público de holds

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente las tareas 7.1 y 7.2 en la rama
    `phase/7-reservations-holds-concurrency`.
  - Se creó la migración Flyway V23 con el agregado `Reservations`, sus relaciones, restricciones,
    índices y la FK física desde `ReservationFormResponses`.
  - Se implementó `POST /api/public/reservations/holds` con contrato DTO, separación
    controlador/interfaz, servicio/interfaz, DAO JPA y consulta pública específica de franja.
  - El hold inicial guarda snapshots de franja, asignación de recurso y únicamente el hash SHA-256
    del token opaco; no recopila todavía datos personales.
  - La validación se limitó a los módulos de reservas y migraciones directamente afectados.
- Archivos modificados:
  - `apps/api/src/main/resources/db/migration/V23__create_reservations.sql`.
  - Paquetes `controller`, `dto`, `persistence` y `service` bajo
    `apps/api/src/main/java/com/reserly/platform/reservations`.
  - Tests focalizados bajo `apps/api/src/test/java/com/reserly/platform/reservations`.
  - `apps/api/src/test/java/com/reserly/platform/forms/ReservationFormMigrationIntegrationTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014 Bloqueo temporal de reserva`.
  - `RF-015 Confirmación de reserva`, como preparación del agregado y token de proceso.
  - `RNF-001 Seguridad`, `RNF-002 Privacidad`, `RNF-003 Concurrencia y consistencia`.
  - `RNF-006 Disponibilidad operativa`, `RNF-008 Observabilidad` y
    `RNF-011 Convenciones de implementación backend y persistencia`.
  - `RB-003 Capacidad de franja`, `RB-004 Bloqueo temporal`, `RB-005 Prioridad de reserva` y
    `RB-010 Disponibilidad con equipo o recursos`.
- Tareas impactadas:
  - `7.1. Crear migración de reservations`.
  - `7.2. Implementar endpoint POST /api/public/reservations/holds`.
  - Prepara `7.3`, `7.4` y `7.5`, sin cerrar sus garantías.
- Tareas completadas:
  - `7.1. Crear migración de reservations`.
  - `7.2. Implementar endpoint POST /api/public/reservations/holds`.
- Siguiente tarea pendiente recomendada:
  - `7.3. Implementar hold temporal de 5 minutos`.
- Decisiones o aclaraciones relevantes:
  - `Reservations` usa nombre físico UpperCamelCase y columnas lowerCamelCase.
  - La migración prepara todos los estados y campos futuros de confirmación, gestión, cancelación y
    asistencia, aunque la entidad JPA inicial solo mapea lo necesario para crear el hold.
  - El endpoint devuelve HTTP 201, `reservationId`, `holdToken`, `expiresAt` y
    `remainingSeconds`; el token original solo se entrega una vez.
  - Se reutiliza `OneTimeTokenService` para CSPRNG de 256 bits y SHA-256.
  - El endpoint exige local publicado, franja disponible y futura, coincidencia de servicio,
    capacidad bruta suficiente y asignación válida de recurso.
  - El bloqueo pesimista de franja, el descuento de reservas/holds vigentes y la prioridad
    transaccional quedan expresamente para 7.4 y 7.5.
  - La vigencia inicial se materializa con una expiración de cinco minutos para respetar el contrato,
    pero 7.3 permanece pendiente hasta aplicar esa política en todos los flujos que consumen holds.
  - Evidencia final:
    `mvn -f apps/api/pom.xml "-Dtest=ReservationHoldServiceTests,ReservationHoldControllerTests,ReservationMigrationIntegrationTests,ReservationFormMigrationIntegrationTests" "-Dspotless.check.skip=true" "-Dcheckstyle.skip=true" test`:
    8 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless focalizado sobre reservations y el test de formularios afectado: correcto.
  - No se ejecutaron suite completa, frontend, build global, tests de concurrencia ni validaciones
    transversales.

## Conversación 93 - Vigencia de holds y bloqueo pesimista de franjas

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente las tareas 7.3 y 7.4 en la rama
    `phase/7-reservations-holds-concurrency`.
  - Se centralizó la vigencia de los holds en una política de dominio con duración exacta de cinco
    minutos, límite superior exclusivo y segundos restantes nunca negativos.
  - La creación del hold consume esa política tanto para persistir `expiresAt` como para responder
    `remainingSeconds`, evitando duplicar reglas temporales.
  - La lectura de la franja pasó a adquirir `PESSIMISTIC_WRITE`; el servicio la invoca dentro de la
    misma transacción que valida, asigna recurso y persiste la reserva.
  - La verificación se limitó al módulo `reservations` y a tres clases de tests relacionadas.
- Archivos modificados:
  - `ReservationHoldExpirationPolicy.java` y `ReservationHoldExpirationPolicyImpl.java`.
  - `ReservationHoldServiceImpl.java`.
  - `ReservationTimeSlotDao.java`.
  - `ReservationHoldExpirationPolicyTests.java`.
  - `ReservationTimeSlotDaoLockTests.java`.
  - `ReservationHoldServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-014 Bloqueo temporal de reserva`.
  - `RNF-003 Concurrencia y consistencia`.
  - `RB-004 Bloqueo temporal` y `RB-005 Prioridad de reserva`.
- Tareas impactadas:
  - `7.3. Implementar hold temporal de 5 minutos`.
  - `7.4. Implementar transacción con bloqueo de franja o control optimista`.
  - Se prepara `7.5`, sin implementar aún el cálculo agregado de capacidad.
- Tareas completadas:
  - `7.3. Implementar hold temporal de 5 minutos`.
  - `7.4. Implementar transacción con bloqueo de franja o control optimista`.
- Siguiente tarea pendiente recomendada:
  - `7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes`.
- Decisiones o aclaraciones relevantes:
  - Un hold está vigente solo cuando `now < expiresAt`; en el instante exacto de expiración deja de
    estarlo.
  - La duración canónica es `Duration.ofMinutes(5)` y el cálculo usa `Instant` UTC inyectable.
  - Se eligió bloqueo pesimista de escritura sobre `TimeSlots`, conforme al flujo 5.2 del diseño.
  - El bloqueo se mantiene hasta el commit porque la consulta se ejecuta desde un método
    `@Transactional`; no se abre una transacción independiente en el DAO.
  - La exclusión serializa competidores por franja, pero el descuento de reservas confirmadas y
    holds vigentes corresponde a 7.5 y la prueba concurrente de última plaza a 7.15.
  - Evidencia final: compilación de main y tests más ejecución exclusiva de
    `ReservationHoldExpirationPolicyTests`, `ReservationTimeSlotDaoLockTests` y
    `ReservationHoldServiceTests`: 8 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - Spotless se aplicó exclusivamente al módulo `reservations`; no se ejecutaron la suite completa,
    frontend, integraciones, tests de concurrencia ni validaciones transversales.

## Conversación 94 - Capacidad efectiva y endpoint de confirmación

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se implementaron conjuntamente 7.5 y 7.6 en
    `phase/7-reservations-holds-concurrency`.
  - La creación de holds calcula ocupación después de bloquear la franja y antes de asignar
    recursos: suma reservas confirmadas del ciclo de vida y holds con expiración estrictamente
    posterior al reloj transaccional.
  - Se añadió `POST /api/public/reservations/{reservationId}/confirm` con DTOs validados,
    controlador separado, servicio transaccional y error público no enumerable.
  - La confirmación bloquea reserva y franja, verifica propiedad mediante hash de token en tiempo
    constante, conserva `partySize`, normaliza identidad y consume el secreto del hold.
  - Las respuestas personalizadas no se ignoran: hasta 7.9, una lista no vacía se rechaza.
- Archivos modificados:
  - `ReservationDao.java`, `ReservationTimeSlotDao.java` y `ReservationEntity.java`.
  - `ReservationHoldServiceImpl.java` y `ReservationHoldServiceTests.java`.
  - DTOs `ReservationConfirmRequest`, `ReservationConfirmFormResponse` y
    `ReservationConfirmResponse`.
  - `ReservationConfirmationController`, implementación y manejador de errores.
  - `ReservationConfirmationService`, implementación y excepción de dominio.
  - `ReservationCapacityDaoTests`, `ReservationConfirmationServiceTests` y
    `ReservationConfirmationControllerTests`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - RF-014 y RF-015.
  - RNF-001, RNF-002 y RNF-003.
  - RB-003, RB-004 y RB-005.
- Tareas impactadas:
  - 7.5 y 7.6.
  - Se preparan 7.7, 7.8, 7.9 y 7.10 sin marcarlas como completadas.
- Tareas completadas:
  - `7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes`.
  - `7.6. Implementar endpoint POST /api/public/reservations/{id}/confirm`.
- Siguiente tarea pendiente recomendada:
  - `7.7. Validar hold vigente antes de confirmar`.
- Decisiones o aclaraciones relevantes:
  - Consumen capacidad `confirmed`, `attended`, `no_show` y `reported`; las canceladas y expiradas
    no consumen plazas.
  - Un hold consume capacidad solo con `holdExpiresAt > now`; el límite exacto ya está libre.
  - La consulta agregada no bloquea por sí sola: su contrato exige que el servicio posea antes el
    lock de `TimeSlots`.
  - La confirmación inicial incluye comprobaciones defensivas de estado, expiración y ocupación,
    pero 7.7 y 7.8 permanecen abiertas hasta completar sus contratos de error y tests específicos.
  - No se genera aún token de gestión ni se encolan emails; corresponden a 7.10 y 7.11.
  - Evidencia focalizada: 11 tests, 0 fallos, 0 errores y 0 omitidos en cinco clases exclusivas de
    `reservations`; Spotless se aplicó solo a ese módulo.
  - No se ejecutaron suite completa, frontend, Testcontainers, tests de concurrencia ni módulos
    ajenos.

## Conversación 95 - Vigencia y capacidad real durante confirmación

- Fecha: 2026-07-14.
- Resumen de la conversación:
  - Se completaron conjuntamente 7.7 y 7.8 en
    `phase/7-reservations-holds-concurrency`.
  - La confirmación reutiliza `ReservationHoldExpirationPolicy` y considera vencido el hold en el
    instante exacto `holdExpiresAt`.
  - Estado, token y partySize se acreditan antes de devolver una causa específica, evitando revelar
    la expiración de reservas ajenas.
  - Tras bloquear la franja, la capacidad se recalcula excluyendo explícitamente el propio hold y
    exige que ocupación ajena más partySize quepan en la capacidad actual.
  - Hold expirado y capacidad insuficiente se traducen a HTTP 409 con códigos estables y sin datos
    internos.
- Archivos modificados:
  - `ReservationConfirmationService.java` y `ReservationConfirmationServiceImpl.java`.
  - `ReservationHoldExpiredException.java`.
  - `ReservationCapacityUnavailableException.java`.
  - `ReservationConfirmationExceptionHandler.java`.
  - `ReservationDao.java`.
  - `ReservationConfirmationServiceTests.java`.
  - `ReservationCapacityDaoTests.java`.
  - `ReservationConfirmationExceptionHandlerTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - RF-014 y RF-015.
  - RNF-003.
  - RB-003, RB-004 y RB-005.
- Tareas impactadas:
  - 7.7 y 7.8.
  - Se prepara 7.16 con cobertura unitaria del límite, sin cerrar su prueba específica posterior.
- Tareas completadas:
  - `7.7. Validar hold vigente antes de confirmar`.
  - `7.8. Validar capacidad real antes de confirmar`.
- Siguiente tarea pendiente recomendada:
  - `7.9. Validar respuestas del formulario`.
- Decisiones o aclaraciones relevantes:
  - Vigencia significa `now < holdExpiresAt`; igualdad y fechas posteriores se rechazan.
  - Un token inválido conserva `RESERVATION_CONFIRMATION_INVALID` aunque el hold esté vencido.
  - Solo el poseedor acreditado recibe `RESERVATION_HOLD_EXPIRED`.
  - La capacidad de confirmación se expresa como
    `occupiedByOthers <= slotCapacity - reservationPartySize` para evitar contar dos veces el hold.
  - El estado no cambia a `expired` en el camino de error; la materialización periódica sigue siendo
    responsabilidad de 7.12.
  - Evidencia focalizada: 15 tests, 0 fallos, 0 errores y 0 omitidos en cinco clases de
    `reservations`; Spotless se limitó a ese módulo.
  - No se ejecutaron suite global, frontend, Testcontainers, tests concurrentes ni módulos ajenos.

## Conversación 96 - Formulario, credencial de gestión y trabajo de confirmación

- Fecha: 2026-07-20.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 7.9, 7.10 y 7.11 en la rama
    `phase/7-reservations-holds-concurrency`.
  - La confirmación valida las respuestas contra el formulario publicado vigente, persiste snapshots
    históricos en la misma transacción y devuelve un error público estable sin revelar el esquema.
  - Cada reserva confirmada recibe un secreto de gestión CSPRNG; PostgreSQL conserva solo su hash
    SHA-256 y su caducidad, mientras el secreto original se limita al trabajo de correo.
  - La confirmación publica un evento dentro de la transacción y un relay `AFTER_COMMIT` lo convierte
    en un mensaje JSON persistente de RabbitMQ con routing key y cola versionadas.
  - El aviso del local usa el email operativo no vacío y, si falta, el email de la cuenta propietaria.
  - Las validaciones se limitaron a cinco clases de formularios, reservas y mensajería.
- Archivos modificados:
  - `ReservationFormFieldDao.java`, `ReservationFormFieldAnswer.java`,
    `ReservationFormResponseDao.java` y `ReservationFormResponseEntity.java`.
  - `ReservationFormConfirmationService.java` y `ReservationFormConfirmationServiceImpl.java`.
  - `ReservationConfirmRequest.java`, `ReservationEntity.java` y
    `ReservationConfirmationServiceImpl.java`.
  - `ReservationFormAnswersInvalidException.java` y
    `ReservationConfirmationExceptionHandler.java`.
  - `ReservationManagementTokenPolicy.java` y `ReservationManagementTokenPolicyImpl.java`.
  - `ReservationConfirmationEmailAnswer.java`,
    `ReservationConfirmationEmailRequestedEvent.java` y el paquete
    `reservations/messaging` con topología, configuración y relay.
  - Tests dirigidos de formularios, confirmación, token, handler y mensajería.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-013 Formulario de reserva configurable`.
  - `RF-015 Confirmación de reserva`.
  - `RF-016 Emails de reserva`.
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia` y
    `RNF-008 Observabilidad`.
- Tareas impactadas:
  - `7.9. Validar respuestas del formulario`.
  - `7.10. Generar token seguro de gestión de reserva`.
  - `7.11. Encolar emails de confirmación`.
  - Se preparan 8.3, 8.4, 8.7, 8.8 y 8.9 sin completarlas.
- Tareas completadas:
  - `7.9. Validar respuestas del formulario`.
  - `7.10. Generar token seguro de gestión de reserva`.
  - `7.11. Encolar emails de confirmación`.
- Siguiente tarea pendiente recomendada:
  - `7.12. Implementar job de expiración de holds`.
- Decisiones o aclaraciones relevantes:
  - Solo se aceptan IDs pertenecientes al esquema custom activo de un formulario publicado; se
    rechazan duplicados, campos desconocidos, valores inválidos y ausencias obligatorias.
  - `formResponses` tiene límite HTTP de 100 elementos y el adaptador rechaza listas mayores que el
    esquema publicado antes de construir comandos de validación.
  - Las respuestas conservan snapshots de clave, label y JSON normalizado para no depender de
    futuras ediciones o eliminaciones del campo.
  - El token de gestión no se devuelve por HTTP ni se almacena en claro en PostgreSQL. La caducidad
    inicial se fija treinta días después del final de la cita.
  - El evento se publica tras commit para impedir emails de transacciones revertidas. El mensaje es
    durable y usa DLQ compartida, pero el outbox, reintento, registro persistente de fallos y
    consumidor idempotente siguen en 8.7 y 8.8.
  - El payload RabbitMQ contiene PII y el token necesario para crear el enlace; no debe registrarse.
    TLS, ACL, retención e idempotencia por destinatario quedan como requisitos operativos de fase 8.
  - Evidencia focalizada final:
    `mvn '-Dtest=ReservationFormConfirmationServiceTests,ReservationManagementTokenPolicyTests,ReservationConfirmationServiceTests,ReservationConfirmationExceptionHandlerTests,ReservationConfirmationEmailEventRelayTests' '-Dspotless.check.skip=true' '-Dcheckstyle.skip=true' test`:
    14 tests correctos, 0 fallos, 0 errores y 0 omitidos.
  - No se ejecutaron suite completa, frontend, Testcontainers, tests de concurrencia, validaciones
    visuales ni chequeos globales de estilo. El chequeo global existente está bloqueado por 47
    archivos ajenos a estas tareas y se omitió deliberadamente en la evidencia final.
## Conversación 97 - Expiración de holds y cierre del flujo público de reserva

- Fecha: 2026-07-21.
- Resumen de la conversación:
  - Se completaron en paralelo las tareas 7.12, 7.13 y 7.14 en la rama `phase/7-reservations-holds-concurrency`.
  - Se añadió un job transaccional, idempotente y configurable que materializa como `expired` los holds vencidos cada minuto mediante una única actualización masiva.
  - El calendario público enlaza las franjas reservables con un formulario que obtiene únicamente el esquema publicado, fija el número de personas antes de crear el hold y muestra su cuenta atrás real.
  - Tras confirmar, el navegador conserva temporalmente el resumen validado en `sessionStorage` y presenta una pantalla responsive sin volver a exponer el token del hold ni consultar datos personales por UUID.
- Archivos modificados:
  - `ReserlyApplication.java`, `ReservationDao.java` y `ReservationHoldExpirationJob.java`.
  - `PublicReservationFormController.java`, `PublicReservationFormControllerImpl.java`, `PublicReservationFormResponse.java`, `PublicReservationFormService.java` y `PublicReservationFormServiceImpl.java`.
  - `public-availability-calendar.tsx`, la ruta `locales/[slug]/reservar`, el módulo `features/public-reservation` y sus tests.
  - La ruta `reservas/[id]/confirmacion`, el módulo `features/reservation-booking` y su test.
  - `apps/web/locales/es.json`, `apps/web/locales/en.json` y tests focalizados del job.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-013 Formulario de reserva configurable`, `RF-014 Hold temporal`, `RF-015 Confirmación de reserva` y `RF-016 Emails de reserva`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`, `RNF-004 Responsive`, `RNF-007 Internacionalización` y `RNF-008 Observabilidad`.
- Tareas impactadas:
  - 7.12, 7.13 y 7.14.
- Tareas completadas:
  - `7.12. Implementar job de expiración de holds`.
  - `7.13. Crear formulario público de reserva con contador visible`.
  - `7.14. Crear pantalla de confirmación`.
- Siguiente tarea pendiente recomendada:
  - `7.15. Crear tests de concurrencia para última plaza`.
- Decisiones o aclaraciones relevantes:
  - La expiración materializada usa la frontera estricta `holdExpiresAt < now`; el job no reabre estados y puede repetirse sin efectos laterales.
  - El aforo se elige antes de crear el hold y queda bloqueado después para que hold y confirmación compartan el mismo valor.
  - Los campos custom opcionales vacíos se omiten; los obligatorios y consentimientos siguen validándose en servidor.
  - La confirmación se transporta solo dentro de la sesión de la pestaña. Si falta o no valida, se muestra un estado neutro sin consultar por UUID.
  - Evidencia focalizada API: 3 tests, 0 fallos, 0 errores y 0 omitidos; compilación de 511 fuentes y Checkstyle correctos.
  - Evidencia focalizada web: 6 tests distintos correctos en los tres archivos nuevos; se usó un único worker y no se ejecutaron suite global, lint, build ni typecheck global.
## Conversación 102 - Cancelación segura, plazo por local y gestión pública

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron las tareas 8.10, 8.11 y 8.12 en `phase/8-emails-management`.
  - El enlace seguro permite consultar y cancelar una reserva confirmada mediante una operación
    transaccional serializada; el token queda revocado tras el éxito y la capacidad se libera al
    salir la reserva del conjunto de estados ocupantes.
  - Cada local dispone de una antelación persistente, con 24 horas por defecto, y el servidor
    calcula una frontera inclusiva usando la zona del reloj de negocio.
  - Se añadió la pantalla responsive `/reservas/gestionar/[token]`, con diálogo de confirmación,
    estados de carga, plazo vencido, enlace inválido y cancelación completada, en español e inglés.
- Archivos modificados:
  - Entidades `ReservationEntity`, `VenueEntity`, `ReservationDao` y migración
    `V25__add_venue_cancellation_notice.sql`.
  - Contratos, servicio, política, controlador y manejador de errores de gestión de reservas.
  - Tests dirigidos de servicio, política, controlador y cliente HTTP web.
  - Ruta y módulo `features/reservation-management`, además de catálogos ES/EN.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RNF-002 Seguridad y privacidad`, `RNF-003 Concurrencia y consistencia`,
    `RNF-004 Responsive` y `RNF-007 Internacionalización`.
- Tareas impactadas y completadas: 8.10, 8.11 y 8.12.
- Siguiente tarea pendiente recomendada:
  - `8.13. Crear tests de token inválido, expirado y cancelación válida`.
- Decisiones o aclaraciones relevantes:
  - `cancellationNoticeMinutes` admite de 0 a 525600 minutos y usa 1440 por defecto; cero permite
    cancelar hasta el instante de inicio.
  - La frontera es inclusiva. Un intento posterior devuelve `409
    RESERVATION_CANCELLATION_DEADLINE_PASSED`; enlaces inexistentes, caducados, revocados o estados
    no cancelables conservan el error opaco 404.
  - La cancelación escribe actor `customer`, razón técnica `customer_request`, instante y estado
    `cancelled_by_user`; no almacena ni registra el secreto.
  - Evidencia focalizada API: 12 tests correctos en tres clases, sin fallos ni errores. Evidencia web:
    2 tests correctos en un archivo con un worker. La compilación API fue correcta.
  - El typecheck web global sigue bloqueado por errores anteriores de MUI/i18n fuera del alcance;
    también se omitió Spotless global porque reporta 43 archivos históricos no relacionados.
## Conversación 103 - Cobertura de enlace seguro e idioma por destinatario

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron las tareas 8.13 y 8.14 en `phase/8-emails-management`.
  - Se amplió la cobertura de cancelación para credenciales malformadas, caducadas y válidas,
    verificando ausencia de consultas o mutaciones cuando corresponde.
  - La revisión de 8.14 detectó que el evento usaba el locale del local para los dos destinatarios.
    El contrato ahora transporta por separado el idioma del cliente y el del local.
  - El formulario público envía el locale activo; el aviso del local conserva su preferencia
    guardada y el consumidor selecciona cada plantilla de forma independiente.
- Archivos modificados:
  - Contrato de confirmación, servicio, evento y consumidor de email.
  - Cliente y formulario público de reserva.
  - Tests de gestión segura, confirmación, consumidor y relay.
  - `tasks.md`, `conversation-tracking.md` y `technical-implementation.md`.
- Requisitos impactados:
  - `RF-017 Consulta y cancelación por enlace seguro`.
  - `RF-031 Internacionalización de textos`.
  - `RNF-002 Seguridad y privacidad` y `RNF-007 Internacionalización`.
- Tareas impactadas y completadas: 8.13 y 8.14.
- Siguiente tarea pendiente recomendada:
  - `9.1. Implementar endpoint GET /api/venue/me/reservations`.
- Decisiones o aclaraciones relevantes:
  - `locale` del contrato público acepta exclusivamente `es` o `en`; cualquier valor ausente o no
    permitido se rechaza por Bean Validation.
  - Las reglas de reserva incluidas en el email del cliente se resuelven con su locale, mientras el
    aviso del negocio usa `Venue.defaultLocale`.
  - Evidencia focalizada API: 20 tests, 0 fallos, 0 errores y 0 omitidos.
  - Vitest no pudo iniciar el worker del único test web en el límite de 60 segundos; no hubo test
    ejecutado ni fallo de aserción. No se repitió para evitar validaciones interminables.
  - El cambio previo `apps/web/next-env.d.ts` se mantuvo fuera del trabajo.

## Conversación 107 - Cobertura focalizada de permisos y filtros del panel

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completó la tarea 9.10 y, con ella, la fase 9 del panel de reservas.
  - Se añadió una prueba HTTP aislada con MockMvc y Spring Security que verifica 401 sin sesión,
    403 para un administrador y acceso para `ROLE_VENUE_OWNER`.
  - La prueba acredita que el controlador usa exclusivamente el `userId` del principal tanto para
    listar como para abrir un detalle, y conserva 404 opaco para una reserva ajena o inexistente.
  - Se amplió la cobertura del servicio para todos los estados visibles y para rechazos de periodo,
    usuario sobredimensionado y propietario ausente antes de consultar persistencia.
- Archivos modificados:
  - `VenueReservationPermissionTests.java`.
  - `VenueReservationServiceTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas y completadas:
  - `9.10. Crear tests de permisos y filtros`.
- Siguiente tarea pendiente recomendada:
  - `10.1. Crear migraciones de no_show_incidents, penalties y venue_booking_rules`.
- Decisiones o aclaraciones relevantes:
  - La prueba HTTP reproduce de forma focalizada la política central
    `/api/venue/me/** -> ROLE_VENUE_OWNER` y usa los manejadores JSON reales de 401/403.
  - La capa HTTP se ejecuta sin Spring Boot ni base de datos; las pruebas ya existentes de DAO
    protegen que propietario, identidad y filtros permanezcan en la consulta JPQL.
  - Un primer intento de integración real compiló 558 fuentes principales y 126 de test, pero los
    cinco casos no llegaron a ejecutarse porque Docker no estaba disponible. La clase temporal se
    descartó y no se reintentó Testcontainers.
  - Evidencia final: 3 tests HTTP de permisos y 8 tests de servicio, todos correctos; Spotless
    focalizado correcto para los dos archivos afectados.
  - No se ejecutaron suite completa, tests frontend, Docker, PostgreSQL, Flyway, lint, build ni
    validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se conserva fuera del trabajo y del commit.

## Conversación 106 - Agenda diaria, detalle responsive y actualización automática

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completaron las tareas 9.7, 9.8 y 9.9 en `phase/9-panel-reservations`.
  - Se creó `/panel/reservas` como entrada principal del panel privado, con selector de fecha,
    navegación entre días, métricas, estados de carga/error/vacío y listado responsive.
  - Se creó `/panel/reservas/{id}` con composición adaptativa para escritorio y móvil, incluyendo
    cliente, cita, respuestas del formulario, recurso asignado e historial de incidencias.
  - La agenda actualiza en segundo plano cada 30 segundos únicamente mientras la pestaña está
    visible, al recuperar el foco, al volver a ser visible y mediante una acción manual.
  - La implementación consume exclusivamente los endpoints privados ya entregados en 9.1–9.6.
- Archivos modificados:
  - `apps/web/src/features/venue-reservations/venue-reservations-api.ts`.
  - `apps/web/src/features/venue-reservations/venue-reservations-dashboard.tsx`.
  - `apps/web/src/features/venue-reservations/venue-reservation-detail-panel.tsx`.
  - Tests y fixtures focalizados de `venue-reservations`.
  - `apps/web/src/app/panel/reservas/page.tsx`.
  - `apps/web/src/app/panel/reservas/[id]/page.tsx`.
  - `apps/web/src/app/panel/page.tsx`.
  - `apps/web/locales/es.json` y `apps/web/locales/en.json`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-003 Usabilidad y accesibilidad`.
  - `RNF-004 Internacionalización`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.7, 9.8 y 9.9.
- Tareas completadas:
  - `9.7. Crear pantalla de reservas del día`.
  - `9.8. Crear detalle de reserva desktop y móvil`.
  - `9.9. Añadir actualización tras nueva reserva`.
- Siguiente tarea pendiente recomendada:
  - `9.10. Crear tests de permisos y filtros`.
- Decisiones o aclaraciones relevantes:
  - La vista diaria solicita como máximo 100 reservas. Si el total es superior, lo comunica sin
    iniciar paginaciones automáticas ilimitadas.
  - La actualización automática se suspende con la pestaña oculta y cada cambio de fecha cancela
    la petición anterior; un contador de secuencia impide que una respuesta antigua sobrescriba
    el día actual.
  - Se usa `credentials: include`, `cache: no-store` y validación Zod en el límite HTTP. Los cuerpos
    de error no se conservan ni se presentan al usuario.
  - Los estados desconocidos reciben una etiqueta segura; los datos personales se muestran solo
    dentro del shell privado y no se persisten en almacenamiento del navegador.
  - Evidencia focalizada: 2 archivos Vitest, 6 tests, 0 fallos; TypeScript acotado sin errores; 82
    claves `VenueReservations` coincidentes entre español e inglés.
  - ESLint fue cancelado dos veces al alcanzar 60 segundos, incluso limitado a cinco archivos. No
    se repitió ni se ejecutaron lint, build o suites globales para evitar validaciones
    interminables y fuera del alcance solicitado.
  - No se realizó despliegue ni validación visual con navegador; el alcance solicitado termina en
    commit y push. El cambio previo `apps/web/next-env.d.ts` se conserva fuera del commit.

## Conversación 104 - Listado, filtros y detalle privado de reservas

- Fecha: 2026-07-24.
- Resumen de la conversación:
  - Se completaron conjuntamente las tareas 9.1, 9.2 y 9.3 en
    `phase/9-panel-reservations`.
  - Se implementó el listado paginado `GET /api/venue/me/reservations` con orden estable por fecha,
    hora de inicio e instante de creación descendentes.
  - El listado admite periodos de calendario `day`, `week` y `month`, fecha ancla, franja, estado,
    búsqueda por nombre/email, página y tamaño limitado.
  - Se implementó `GET /api/venue/me/reservations/{reservationId}` con una consulta que combina
    UUID y propietario autenticado, sin distinguir entre reserva inexistente y ajena.
  - Los contratos no exponen hashes de hold, tokens de gestión ni caducidades de secretos.
  - Las validaciones se limitaron al módulo API y a tres clases nuevas del paquete `reservations`.
- Archivos modificados:
  - `ReservationDao.java`.
  - `VenueReservationController.java`, `VenueReservationControllerImpl.java` y
    `VenueReservationExceptionHandler.java`.
  - `VenueReservationConverter.java`.
  - DTOs `VenueReservationSummaryResponse`, `VenueReservationListResponse`,
    `VenueReservationDetailResponse` y `VenueReservationErrorResponse`.
  - `VenueReservationService.java`, `VenueReservationServiceImpl.java`,
    `VenueReservationPeriod.java`, `VenueReservationFilterInvalidException.java` y
    `VenueReservationNotFoundException.java`.
  - `VenueReservationControllerTests.java`, `VenueReservationServiceTests.java` y
    `VenueReservationDaoTests.java`.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-018 Panel de reservas del local`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.1, 9.2 y 9.3.
  - Se preparan los contratos de detalle para 9.4 y 9.5 sin completar respuestas del formulario ni
    presentación del empleado/recurso.
- Tareas completadas:
  - `9.1. Implementar endpoint GET /api/venue/me/reservations`.
  - `9.2. Implementar filtros por día, semana, mes, franja, estado y usuario`.
  - `9.3. Implementar endpoint de detalle de reserva`.
- Siguiente tarea pendiente recomendada:
  - `9.4. Mostrar respuestas del formulario en detalle`.
- Decisiones o aclaraciones relevantes:
  - Una fecha sin `period` se interpreta como un día; un `period` sin fecha se rechaza con 400 para
    evitar resultados dependientes del reloj del servidor.
  - La semana usa lunes inclusivo y el lunes siguiente exclusivo; el mes usa su primer día
    inclusivo y el primer día del mes siguiente exclusivo.
  - Los holds y expiraciones anónimas no forman parte del panel: solo se consultan filas con email
    confirmado. Los estados visibles son `confirmed`, `cancelled_by_user`,
    `cancelled_by_venue`, `attended`, `no_show` y `reported`.
  - El filtro de usuario se normaliza con `Locale.ROOT`, escapa `%`, `_` y `\`, y se aplica contra
    nombre en minúsculas y email normalizado.
  - La paginación admite páginas 0..100000 y tamaños 1..100; el contrato usa 0 y 25 por defecto.
  - Reserva inexistente, ajena o anónima devuelve el mismo 404
    `VENUE_RESERVATION_NOT_FOUND`.
  - Evidencia focalizada final: 10 tests, 0 fallos, 0 errores y 0 omitidos en tres clases; 551
    fuentes principales y 124 fuentes de test compilaron correctamente.
  - Spotless se aplicó y comprobó solo sobre `VenueReservation*.java` y `ReservationDao.java`.
    Checkstyle se ejecutó durante Maven; no se ejecutaron suite global, frontend, Testcontainers,
    migraciones ni validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se conservó fuera del trabajo y del commit.

## Conversación 105 - Respuestas, recurso e historial de incidencias en el detalle

- Fecha: 2026-07-26.
- Resumen de la conversación:
  - Se completaron las tareas 9.4, 9.5 y 9.6 en `phase/9-panel-reservations`.
  - El detalle privado carga los snapshots históricos de respuestas con clave, etiqueta y valor
    JSON tal como fueron confirmados.
  - El recurso asignado se resuelve por propietario aunque haya sido archivado después de la cita,
    preservando el significado histórico de la reserva.
  - Se creó `NoShowIncidents` como fuente persistente del historial profesional y se muestra un
    máximo de 50 incidencias recientes asociadas al email normalizado.
  - El historial minimiza datos: no devuelve local, reserva, actor, email ni notas de los reportes.
  - Se mantuvo el mismo endpoint privado de detalle y no se añadieron rutas públicas.
- Archivos modificados:
  - Migración `V26__create_no_show_incidents.sql`.
  - `NoShowIncidentEntity.java` y `NoShowIncidentDao.java`.
  - `ReservationFormResponseDao.java` y `EmployeeResourceDao.java`.
  - `VenueReservationService.java`, `VenueReservationServiceImpl.java` y
    `VenueReservationDetail.java`.
  - `VenueReservationConverter.java` y `VenueReservationDetailResponse.java`.
  - DTOs `VenueReservationFormAnswerResponse`,
    `VenueReservationAssignedResourceResponse`, `VenueReservationIncidentResponse` y
    `VenueReservationIncidentHistoryResponse`.
  - Tests focalizados de servicio, controlador, consultas y persistencia de incidencias.
  - `.kiro/specs/plataforma-reservas-saas/tasks.md`.
  - `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`.
  - `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Requisitos impactados:
  - `RF-008 Acceso y panel privado del local`.
  - `RF-013 Formulario de reserva configurable`.
  - `RF-018 Panel de reservas del local`.
  - `RF-020 Reporte de no asistencia`.
  - `RNF-002 Seguridad y privacidad`.
  - `RNF-005 Rendimiento`.
  - `RNF-006 Mantenibilidad`.
- Tareas impactadas:
  - 9.4, 9.5 y 9.6.
  - 10.1 queda parcialmente preparada por la tabla `NoShowIncidents`, pero continúa pendiente
    porque aún no existen las migraciones de `Penalties` y `VenueBookingRules`.
- Tareas completadas:
  - `9.4. Mostrar respuestas del formulario en detalle`.
  - `9.5. Mostrar empleado o recurso asignado`.
  - `9.6. Mostrar historial de incidencias asociado al email`.
- Siguiente tarea pendiente recomendada:
  - `9.7. Crear pantalla de reservas del día`.
- Decisiones o aclaraciones relevantes:
  - Las respuestas se ordenan por `createdAt` e `id` ascendentes y usan snapshots para no depender
    de ediciones o eliminaciones posteriores del campo.
  - El recurso histórico conserva nombre, apellido, alias, tipo, especialidad y estado, pero no
    expone notas internas, descripción, visibilidad pública ni datos del local.
  - Un `employeeResourceId` inexistente o ajeno se trata como detalle no encontrado para no devolver
    una referencia inconsistente.
  - El historial solo puede consultarse después de acreditar una reserva propia; no existe
    búsqueda HTTP por email arbitrario.
  - El historial devuelve `totalElements`, `truncated` e `items`; `items` está limitado a 50 y
    ordenado por fecha de reporte e identificador descendentes.
  - La migración V26 impone una incidencia por reserva, email canónico, tipos/estados cerrados,
    relaciones auditables e índices por email y local. Los flujos de escritura permanecen en fase
    10.
  - Evidencia final: 14 tests focalizados, 0 fallos, 0 errores y 0 omitidos. Compilaron 558 fuentes
    principales y 125 fuentes de test.
  - Spotless se aplicó y comprobó solo sobre los archivos afectados. Checkstyle focalizado detectó
    una línea nueva de 102 caracteres, que se corrigió; el comando también volvió a incluir 25
    incidencias históricas fuera del alcance en plantillas y una clase previa, por lo que no se
    repitió como chequeo global.
  - No se ejecutaron suite global, frontend, Testcontainers, migraciones reales, Docker ni
    validaciones visuales.
  - El cambio previo `apps/web/next-env.d.ts` se mantuvo fuera del trabajo.
