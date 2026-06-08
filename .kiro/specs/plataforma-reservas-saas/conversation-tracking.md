# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-06-08
- Tareas completadas en `tasks.md`: `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Siguiente tarea pendiente recomendada: `0.2. Crear repositorio, estructura base y convenciones de ramas.`
- Observación: el proyecto cuenta con especificación completa y stack definitivo seleccionado, pero todavía no hay implementación de producto.

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
