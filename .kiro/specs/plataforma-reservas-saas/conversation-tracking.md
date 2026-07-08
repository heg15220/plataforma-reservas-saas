# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-07-08
- Tareas completadas en `tasks.md`: `0.1` a `0.15`, `1.1` a `1.22`, `2.1` a `2.17`, `3.1` a
  `3.14` y `4.1` a `4.4`.
- Siguiente tarea pendiente recomendada: `4.5. Implementar generación automática de franjas por duración.`
- Observación: la Fase 4 ya dispone de migración base para horarios, franjas y bloqueos de
  disponibilidad, API privada de horario semanal, excepciones diarias y creación manual de franjas.

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
