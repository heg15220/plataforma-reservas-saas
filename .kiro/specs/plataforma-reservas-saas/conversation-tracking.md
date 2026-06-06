# Seguimiento de conversaciones y cambios

Este documento registra los cambios realizados en cada conversación respecto a los requisitos, diseño y tareas de la especificación del proyecto.

Fuente de verdad del avance:

- Requisitos: `requirements.md`
- Diseño técnico: `design.md`
- Tareas: `tasks.md`

## Estado actual

- Fecha de última actualización: 2026-06-06
- Tareas completadas en `tasks.md`: ninguna marcada con `[x]`.
- Siguiente tarea pendiente recomendada: `0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.`
- Observación: el proyecto cuenta con especificación completa, pero todavía no hay implementación de producto ni tareas cerradas.

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
