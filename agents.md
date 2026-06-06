# AGENTS.md

## Instrucciones obligatorias al iniciar una conversación

Antes de responder a cualquier petición nueva sobre este proyecto, el agente debe revisar el estado real de la especificación en `.kiro`.

Archivos fuente de verdad:

- `.kiro/specs/plataforma-reservas-saas/requirements.md`
- `.kiro/specs/plataforma-reservas-saas/design.md`
- `.kiro/specs/plataforma-reservas-saas/tasks.md`
- `.kiro/specs/plataforma-reservas-saas/conversation-tracking.md`

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
- Si una conversación cambia requisitos, diseño o tareas, actualizar `conversation-tracking.md` en la misma conversación.
- Si se añaden nuevas tareas, deben colocarse en la fase correspondiente de `tasks.md` y mencionarse en el registro.
- Si una decisión sustituye una decisión anterior, registrar la decisión nueva y la razón.
- Mantener el lenguaje del proyecto en español.

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
