# AGENTS.md

## Instrucciones obligatorias al iniciar una conversación

Antes de responder a cualquier petición nueva sobre este proyecto, el agente debe revisar el estado real de la especificación en `.kiro`.

Archivos fuente de verdad:

- `.kiro/specs/plataforma-reservas-saas/requirements.md`
- `.kiro/specs/plataforma-reservas-saas/design.md`
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`
- `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`

## Checklist inicial

En cada nueva conversación:

1. Leer `tasks.md`.
2. Identificar las tareas marcadas como completadas con `[x]`.
3. Identificar la primera tarea pendiente con `[ ]`; esa es la siguiente tarea recomendada salvo que el usuario indique otra prioridad.
4. Leer las secciones relevantes de `requirements.md` y `design.md` antes de modificar requisitos, arquitectura o tareas.
5. Leer `conversation-tracking.md` para entender cambios recientes, decisiones tomadas y trabajo pendiente.
6. Informar brevemente del estado actual antes de ejecutar cambios si el trabajo depende del avance de tareas.

## Reglas de actualización

- No marcar una tarea como completada en `tasks.md` si no se ha implementado y verificado realmente.
- Si se completa una tarea, actualizar su checkbox de `[ ]` a `[x]` y registrar la evidencia en `conversation-tracking.md`.
- Si se completa una tarea, documentar de forma muy profunda y detallada la implementación técnica de esa iteración en el documento técnico único: `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.
- Si una conversación cambia requisitos, diseño o tareas, actualizar `conversation-tracking.md` en la misma conversación.
- Si se añaden nuevas tareas, deben colocarse en la fase correspondiente de `tasks.md` y mencionarse en el registro.
- Si una decisión sustituye una decisión anterior, registrar la decisión nueva y la razón.
- Mantener el lenguaje del proyecto en español.

## Documentación técnica obligatoria por tarea

Al finalizar cada tarea de `tasks.md`, el agente debe añadir una entrada nueva en `.kiro/specs/plataforma-reservas-saas/technical-implementation.md`.

Esta entrada debe ser profunda, técnica y verificable. Como mínimo debe documentar:

- Identificador exacto de la tarea completada.
- Fecha de la iteración.
- Objetivo técnico de la tarea.
- Requisitos y decisiones de diseño relacionados.
- Archivos creados, modificados o eliminados.
- Arquitectura aplicada y razones de las decisiones técnicas.
- Modelo de datos afectado, migraciones, índices y restricciones si aplica.
- Endpoints, contratos, servicios, componentes, jobs o módulos implementados.
- Flujos de ejecución relevantes.
- Validaciones, permisos, seguridad, privacidad e internacionalización aplicadas.
- Estrategia de errores, logs, auditoría y observabilidad si aplica.
- Tests añadidos o modificados y comandos usados para verificarlos.
- Riesgos, limitaciones, deuda técnica y tareas pendientes derivadas.
- Evidencia de verificación: comandos ejecutados, resultado resumido y cualquier comprobación manual relevante.

No se debe cerrar una tarea ni marcarla con `[x]` si esta documentación técnica no se ha actualizado.

## Documentación obligatoria del código

Es indispensable documentar todo el código implementado en el sistema.

Reglas:

- Cada módulo, servicio, componente, job, endpoint, modelo de dominio, migración relevante y helper compartido debe tener documentación suficiente para entender su responsabilidad, entradas, salidas, efectos secundarios y restricciones.
- Las APIs públicas o internas deben documentar contrato, errores esperados, permisos requeridos e invariantes de negocio.
- Las funciones con lógica de negocio, concurrencia, seguridad, pagos, penalizaciones, disponibilidad o verificación empresarial deben incluir comentarios técnicos cuando la intención no sea evidente.
- Las decisiones complejas deben quedar explicadas en `technical-implementation.md`, no solo en comentarios del código.
- Los comentarios deben aportar contexto real. No se deben añadir comentarios triviales que repitan literalmente lo que ya dice el código.
- Si se implementa una migración, debe documentarse qué cambia, por qué cambia y cómo afecta a datos existentes.
- Si se implementa UI, debe documentarse la estructura de componentes, estados, validaciones, accesibilidad, i18n y comportamiento responsive.
- Si se implementa integración externa, debe documentarse autenticación, timeouts, reintentos, idempotencia, errores y datos almacenados.

## Formato mínimo del registro de conversación

Cada entrada nueva en `conversation-tracking.md` debe incluir:

- Fecha.
- Resumen de la conversación.
- Archivos modificados.
- Requisitos impactados.
- Tareas impactadas.
- Tareas completadas, si las hay.
- Siguiente tarea pendiente recomendada.
- Decisiones o aclaraciones relevantes.

## Estado de tareas

El estado de avance se determina exclusivamente desde `tasks.md`. El documento de seguimiento sirve como histórico y explicación, pero no sustituye a los checkboxes de `tasks.md`.
