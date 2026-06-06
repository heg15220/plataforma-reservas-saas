# Plataforma SaaS de gestión y búsqueda de reservas online - Requisitos

## 1. Objetivo

Construir una plataforma SaaS que conecte usuarios finales con locales, negocios, instalaciones o servicios que aceptan reservas por franjas horarias. El producto debe permitir buscar, consultar disponibilidad en tiempo real, reservar sin fricción y ofrecer a los locales un panel profesional para gestionar disponibilidad, reservas, asistencia, incidencias, estadísticas y, en fases posteriores, suscripciones de pago.

La primera versión debe priorizar un MVP sólido: buscador público, ficha de local, calendario de disponibilidad, reserva online sin cuenta mediante email, panel privado del local, gestión de franjas, control de concurrencia, asistencia/no asistencia, penalizaciones básicas, reseñas y estadísticas iniciales.

## 2. Alcance

### 2.1 Incluido en MVP

- Registro, verificación de email e inicio de sesión de locales.
- Perfil público editable del local.
- Categorías configurables.
- Búsqueda pública por nombre, palabras clave, ubicación y categoría.
- Resultados con tarjetas de locales.
- Ficha pública de local con estado, imágenes, descripción, mapa, reseñas y disponibilidad.
- Calendario de disponibilidad con franjas actuales y futuras.
- Gestión de horarios, franjas y capacidad por franja.
- Reserva online para usuarios finales sin cuenta, identificados por email.
- Bloqueo temporal de plazas durante el proceso de reserva.
- Confirmación de reserva por email al usuario y aviso al local.
- Enlace seguro por email para consultar o cancelar una reserva.
- Panel de reservas del local.
- Marcado de asistencia, no asistencia y pendiente.
- Reporte de no asistencia.
- Penalización básica por email con escalado 7, 14, 21 y 60 días.
- Historial profesional de incidencias visible para el local.
- Reseñas y valoraciones asociadas a reservas confirmadas.
- Estadísticas básicas del local.
- Equipo y disponibilidad en versión básica: empleados o recursos, estado activo/inactivo, horarios semanales y asignación de reservas.
- Pantallas responsive principales para usuario final y local.
- Internacionalización completa de textos de sistema en español e inglés.
- Registro de locales condicionado a verificación empresarial mediante identificador fiscal/registral único comprobable contra API remota oficial, pública o proveedor autorizado.
- Integración preparada para pagos externos RedSys, con pantalla de suscripción y estados, aunque el cobro completo puede activarse por fase.

### 2.2 Fuera del MVP inicial

- Sistema avanzado de recomendación por factorización matricial.
- Suscripciones de pago completamente operativas si no se decide activar monetización en MVP.
- Estadísticas avanzadas y comparativas anuales profundas.
- App móvil nativa.
- Multiusuario por local.
- Lista de espera.
- Reasignación automática avanzada de empleados.
- Integración con calendarios externos.
- Disputas formales entre usuarios y locales.
- Formularios personalizados por servicio, empleado o recurso.

### 2.3 Preparado para fases posteriores

El diseño debe dejar puntos de extensión para recomendaciones, pagos RedSys, multiusuario, motor de reglas avanzado, formularios por servicio/recurso, estadísticas avanzadas, auditoría ampliada e integraciones externas.

## 3. Perfiles de usuario

### 3.1 Usuario final

Persona que busca locales o servicios, consulta disponibilidad y realiza reservas. En MVP no necesita cuenta registrada.

Capacidades:

- Buscar locales por nombre, palabra clave, categoría y ubicación.
- Usar filtros de ubicación, radio, categoría, disponibilidad y valoración.
- Consultar ficha pública de un local.
- Ver estado del local: abierto, cerrado, no disponible, completo o próximamente disponible.
- Consultar calendario y franjas disponibles.
- Seleccionar servicio, empleado o recurso cuando el local lo permita.
- Realizar reserva mediante formulario.
- Recibir confirmación por email.
- Consultar o cancelar reserva mediante enlace seguro.
- Valorar y reseñar locales tras una reserva confirmada.
- Ver mensajes claros cuando su email tenga una restricción temporal activa.

### 3.2 Local, negocio o entidad registrada

Responsable de un negocio o instalación que ofrece reservas en la plataforma.

Capacidades:

- Registrar y verificar una cuenta.
- Configurar datos públicos del local.
- Gestionar foto principal y galería.
- Configurar horarios, franjas y capacidad.
- Activar o desactivar reservas.
- Configurar formulario de reserva.
- Gestionar reservas recibidas.
- Marcar asistencia, no asistencia o pendiente.
- Reportar incidencias de no asistencia.
- Consultar historial de incidencias asociado al email de una reserva.
- Cancelar reservas de forma auditada cuando exista causa operativa.
- Gestionar equipo, empleados, recursos o unidades reservables.
- Consultar estadísticas.
- Consultar y gestionar plan de suscripción cuando se active monetización.

### 3.3 Administrador de plataforma

Perfil interno responsable de la operación global del SaaS.

Capacidades:

- Gestionar locales registrados.
- Gestionar categorías.
- Revisar incidencias y reportes.
- Gestionar planes, suscripciones y estados de cuenta.
- Supervisar métricas globales.
- Administrar emails bloqueados o penalizados.
- Auditar acciones críticas.
- Resolver incidencias operativas o disputas si se habilita el flujo.

## 4. Requisitos funcionales

### RF-001 Buscador principal

**Prioridad:** MVP

El sistema debe mostrar una pantalla pública de inicio con una barra de búsqueda principal y el mensaje "¿Dónde quieres pedir cita hoy?".

#### Criterios de aceptación

- WHEN un usuario accede a la página pública, THEN debe ver el buscador como acción principal.
- WHEN el usuario escribe el nombre o palabras clave de un local, THEN el sistema debe mostrar resultados coincidentes.
- WHEN existan coincidencias parciales, THEN el sistema debe poder mostrar sugerencias o autocompletado.
- WHEN no existan resultados, THEN el sistema debe mostrar un mensaje claro y sugerir cambiar filtros, buscar otro local o invitar al local a registrarse.

### RF-002 Filtros avanzados

**Prioridad:** MVP

El sistema debe permitir refinar búsquedas por ubicación, categoría, disponibilidad, valoración, estado y radio.

#### Criterios de aceptación

- WHEN el usuario abre filtros, THEN puede seleccionar ubicación actual previa autorización del navegador.
- WHEN el usuario introduce una ciudad, zona o dirección, THEN el sistema debe filtrar locales por esa localización.
- WHEN el usuario selecciona un radio, THEN los resultados deben limitarse a ese rango aproximado si existen coordenadas.
- WHEN el usuario selecciona una o varias categorías, THEN se deben mostrar solo locales compatibles.
- WHEN el usuario filtra por disponibilidad, THEN puede elegir ahora, hoy, mañana, esta semana, fecha concreta o rango horario.
- WHEN el usuario filtra por valoración, THEN puede ordenar o limitar por puntuación media.
- WHEN el usuario limpia filtros, THEN los resultados vuelven al estado base de búsqueda.

### RF-003 Resultados de búsqueda

**Prioridad:** MVP

El sistema debe mostrar resultados como tarjetas de local.

#### Criterios de aceptación

- WHEN se listan locales, THEN cada tarjeta muestra foto principal, nombre, categoría, ubicación aproximada, estado, valoración, número de reseñas, descripción breve y disponibilidad resumida.
- WHEN un local tiene huecos disponibles, THEN la tarjeta muestra accesos para ver ficha y reservar.
- WHEN un local no tiene huecos, THEN la tarjeta muestra estado completo o próxima disponibilidad.
- WHEN el usuario está en móvil, THEN las tarjetas se muestran en una lista vertical táctil.

### RF-004 Ficha pública del local

**Prioridad:** MVP

Cada local debe disponer de una ficha pública con su información y disponibilidad.

#### Criterios de aceptación

- WHEN el usuario abre un local, THEN ve nombre, imagen principal, categoría, dirección, ubicación en mapa, descripción, horario, estado, valoración y reseñas.
- WHEN el local tenga galería, THEN la ficha puede mostrar imágenes adicionales.
- WHEN la descripción supere 350 palabras, THEN el sistema debe impedir guardarla o solicitar recorte.
- WHEN existan franjas disponibles, THEN el usuario puede iniciar una reserva desde la ficha.
- WHEN no existan franjas disponibles, THEN el sistema debe mostrar disponibilidad futura o estado no disponible.

### RF-005 Estado público del local

**Prioridad:** MVP

El sistema debe calcular y mostrar el estado operativo del local.

#### Criterios de aceptación

- WHEN el local está dentro de horario y acepta reservas, THEN el estado debe ser abierto.
- WHEN el local está fuera de horario habitual, THEN el estado debe ser cerrado.
- WHEN el local bloquea reservas manualmente, THEN el estado debe ser no disponible.
- WHEN una franja seleccionada no tenga plazas, THEN el estado de franja debe ser completa.
- WHEN no haya huecos actuales pero sí futuros, THEN se puede mostrar próximamente disponible.

### RF-006 Calendario de disponibilidad

**Prioridad:** MVP

La ficha del local debe incluir un módulo de calendario para consultar fechas y franjas.

#### Criterios de aceptación

- WHEN el usuario abre el calendario, THEN ve días disponibles, cerrados, completos, sin disponibilidad y día seleccionado.
- WHEN selecciona un día, THEN se listan las franjas de ese día.
- WHEN una franja tiene plazas, THEN muestra hora de inicio, hora de fin, capacidad total, plazas disponibles y botón de reserva.
- WHEN una franja está completa o bloqueada, THEN aparece deshabilitada con estado claro.
- WHEN el usuario consulta una fecha futura, THEN el sistema muestra disponibilidad futura si existe.

### RF-007 Registro de local

**Prioridad:** MVP

Los locales deben poder registrarse desde la interfaz pública.

#### Criterios de aceptación

- WHEN un local pulsa "Registra tu local", THEN accede al formulario de alta.
- WHEN completa nombre del local, responsable, email, contraseña, teléfono, categoría, dirección, ubicación, imagen, descripción, horarios y condiciones legales, THEN el sistema puede crear la cuenta.
- WHEN completa el registro, THEN debe informar país fiscal, razón social e identificador fiscal/registral de empresa o profesional.
- WHEN el identificador fiscal/registral ya esté asociado a una cuenta empresarial verificada, THEN el sistema debe impedir crear otra cuenta empresarial duplicada salvo flujo administrativo de multi-sede o autorización explícita.
- WHEN el identificador no pueda validarse contra una API remota configurada, THEN la cuenta debe quedar pendiente de revisión o rechazada y no podrá publicar locales ni recibir reservas públicas.
- WHEN se crea la cuenta, THEN se envía email de verificación.
- WHEN el email no está verificado, THEN el local no debe poder publicar el perfil o recibir reservas públicas.
- WHEN la verificación empresarial no esté aprobada, THEN el local no debe poder publicar el perfil o recibir reservas públicas.
- WHEN el local sube imagen o logo, THEN el sistema valida tipo, tamaño y seguridad del archivo.

### RF-008 Acceso y panel privado del local

**Prioridad:** MVP

El local debe iniciar sesión y acceder a un panel privado.

#### Criterios de aceptación

- WHEN el local introduce credenciales válidas, THEN accede a su panel.
- WHEN las credenciales son inválidas, THEN se muestra error genérico sin revelar si el email existe.
- WHEN el local está autenticado, THEN puede acceder solo a sus propios datos.
- WHEN el local usa móvil, THEN el panel muestra una versión simplificada con resumen, reservas, calendario y más.

### RF-009 Gestión de perfil público

**Prioridad:** MVP

El local debe poder editar los datos visibles de su ficha.

#### Criterios de aceptación

- WHEN el local edita nombre, descripción, categoría, dirección, ubicación, imagen o datos de contacto, THEN los cambios se guardan en su perfil.
- WHEN el local desactiva visibilidad de un dato de contacto, THEN ese dato no aparece en la ficha pública.
- WHEN el local cambia su dirección o coordenadas, THEN las búsquedas por ubicación deben usar los nuevos datos.

### RF-010 Gestión de horarios

**Prioridad:** MVP

El local debe configurar horario semanal y días cerrados.

#### Criterios de aceptación

- WHEN el local edita un día, THEN puede marcarlo como abierto, cerrado o con reservas inactivas.
- WHEN el local define horario, THEN debe indicar hora de apertura y cierre válidas.
- WHEN el local cambia horarios, THEN la disponibilidad pública se recalcula.
- WHEN hay reservas confirmadas afectadas por un cambio, THEN el sistema debe avisar al local y no cancelarlas automáticamente sin acción explícita.

### RF-011 Gestión de franjas

**Prioridad:** MVP

El local debe poder crear, editar, bloquear y reabrir franjas de reserva.

#### Criterios de aceptación

- WHEN el local crea franjas manuales, THEN cada franja tiene inicio, fin, capacidad máxima y estado.
- WHEN el local usa reglas automáticas, THEN puede generar franjas de 30 minutos, 1 hora o duración personalizada.
- WHEN una franja se marca no disponible, THEN el sistema impide nuevas reservas en esa franja.
- WHEN se modifica capacidad, THEN el sistema valida que no sea menor que las plazas ya confirmadas salvo que se gestione el conflicto.

### RF-012 Gestión de disponibilidad en tiempo real

**Prioridad:** MVP

El local debe poder bloquear o reabrir disponibilidad con efecto inmediato.

#### Criterios de aceptación

- WHEN el local bloquea una franja, THEN desaparece como reservable para usuarios nuevos.
- WHEN el local cierra un día completo, THEN todas sus franjas futuras se muestran no disponibles.
- WHEN la disponibilidad cambia, THEN la ficha pública y el panel deben reflejar el cambio sin depender de validación frontend.

### RF-013 Formulario de reserva configurable

**Prioridad:** MVP

Cada local debe poder definir campos adicionales para su formulario de reserva.

#### Criterios de aceptación

- WHEN el local edita el formulario, THEN puede crear, ordenar, editar, eliminar y previsualizar campos.
- WHEN crea un campo, THEN puede elegir tipo: texto corto, texto largo, número, selector, checkbox, fecha, teléfono o email.
- WHEN marca un campo obligatorio, THEN el usuario debe completarlo para confirmar.
- WHEN se reserva, THEN nombre, email, número de personas, fecha y franja siempre son obligatorios.
- WHEN el usuario envía el formulario, THEN las respuestas quedan asociadas a la reserva.

### RF-014 Bloqueo temporal de reserva

**Prioridad:** MVP crítico

El sistema debe bloquear temporalmente plazas durante el proceso de reserva para evitar sobreventas.

#### Criterios de aceptación

- WHEN el usuario pulsa reservar sobre una franja disponible, THEN el sistema crea un bloqueo temporal transaccional.
- WHEN el bloqueo se crea correctamente, THEN la plaza queda reservada durante 5 minutos por defecto.
- WHEN el usuario confirma dentro del tiempo, THEN la reserva pasa a confirmada.
- WHEN el tiempo expira, THEN el bloqueo pasa a expirado y la plaza vuelve a estar disponible.
- WHEN dos usuarios intentan reservar la última plaza, THEN solo uno puede crear bloqueo o confirmación válida.

### RF-015 Confirmación de reserva

**Prioridad:** MVP crítico

El sistema debe confirmar reservas solo si hay disponibilidad real, bloqueo vigente y ausencia de penalización activa.

#### Criterios de aceptación

- WHEN el usuario confirma, THEN el backend valida disponibilidad real en base de datos.
- WHEN el email tiene penalización activa, THEN no se confirma la reserva y se muestra fecha de fin de restricción.
- WHEN la capacidad restante es suficiente para el número de personas, THEN se registra reserva confirmada.
- WHEN se confirma la reserva, THEN se reduce la disponibilidad de la franja.
- WHEN se confirma la reserva, THEN se envía email al usuario y aviso al local.
- WHEN falla el envío de email, THEN la reserva no debe duplicarse y el fallo debe quedar registrado para reintento.

### RF-016 Emails de reserva

**Prioridad:** MVP

El sistema debe enviar notificaciones por email en eventos principales.

#### Criterios de aceptación

- WHEN se confirma una reserva, THEN el usuario recibe email con local, dirección, fecha, franja, número de personas, respuestas del formulario, política de cancelación, enlace seguro y aviso de no asistencia.
- WHEN se confirma una reserva, THEN el local recibe email con datos de reserva, usuario, email, número de personas, fecha, franja y respuestas.
- WHEN una reserva se cancela, THEN el usuario y el local reciben notificación según corresponda.
- WHEN se reporta una no asistencia, THEN el usuario puede recibir aviso si la política activa lo contempla.

### RF-017 Consulta y cancelación por enlace seguro

**Prioridad:** MVP

El usuario sin cuenta debe poder consultar y cancelar su reserva mediante enlace seguro enviado por email.

#### Criterios de aceptación

- WHEN el usuario abre el enlace, THEN puede ver solo los datos de esa reserva.
- WHEN el enlace es inválido, expirado o revocado, THEN se muestra error seguro.
- WHEN el usuario cancela dentro del plazo permitido por el local, THEN la reserva pasa a cancelada por usuario y se libera capacidad.
- WHEN el usuario intenta cancelar fuera de plazo, THEN el sistema muestra política aplicable.

### RF-018 Panel de reservas del local

**Prioridad:** MVP

El local debe consultar y gestionar reservas recibidas.

#### Criterios de aceptación

- WHEN el local entra en reservas, THEN puede ver reservas por día, semana, mes, franja, estado y usuario.
- WHEN abre una reserva, THEN ve datos del usuario, email, personas, fecha, franja, respuestas del formulario e historial de incidencias.
- WHEN una reserva se confirma, THEN aparece en el panel del local.
- WHEN el local filtra por estado, THEN el listado se actualiza.

### RF-019 Marcado de asistencia

**Prioridad:** MVP

El local debe marcar asistencia de reservas finalizadas.

#### Criterios de aceptación

- WHEN una reserva ya finalizó, THEN el local puede marcar asistida, no asistida o pendiente.
- WHEN el local no marca nada tras el periodo configurado, THEN el sistema puede marcar automáticamente como asistida.
- WHEN se marca asistida, THEN no se genera penalización.
- WHEN se marca no asistida, THEN el local puede reportarla para activar protocolo de incidencia.

### RF-020 Reporte de no asistencia

**Prioridad:** MVP

El local debe poder reportar una no asistencia de forma auditada.

#### Criterios de aceptación

- WHEN el local pulsa reportar no asistencia, THEN el sistema muestra confirmación y advertencia de auditoría.
- WHEN el local confirma, THEN se registra la incidencia con reserva, email, local, fecha, motivo y actor.
- WHEN se registra la incidencia, THEN se actualiza el historial de incidencias del email.
- WHEN procede penalización, THEN se calcula y activa restricción temporal.
- WHEN se muestra lenguaje de interfaz, THEN debe usar términos profesionales como "historial de incidencias" o "riesgo de no asistencia".

### RF-021 Penalizaciones por email

**Prioridad:** MVP

El sistema debe bloquear temporalmente reservas para emails con no asistencias reportadas.

#### Criterios de aceptación

- WHEN se registra la primera no asistencia operativa, THEN se aplica bloqueo de 7 días.
- WHEN se registra la segunda, THEN se aplica bloqueo de 14 días.
- WHEN se registra la tercera, THEN se aplica bloqueo de 21 días.
- WHEN se registra la cuarta o superior, THEN se aplica bloqueo de 60 días.
- WHEN finaliza un bloqueo de 60 días, THEN el contador operativo puede reiniciarse según regla definida.
- WHEN un usuario intenta reservar con penalización activa, THEN no puede completar reserva y se informa fecha de fin.
- WHEN el sistema consulta penalizaciones, THEN debe normalizar el email para evitar duplicados por mayúsculas o espacios.

### RF-022 Incidencias y reglas de reserva

**Prioridad:** MVP parcial

El local debe disponer de una sección "Incidencias y reglas de reserva" para definir normas operativas.

#### Criterios de aceptación

- WHEN el local configura reglas, THEN puede definir plazo mínimo de cancelación, política de no asistencia y texto visible para usuarios.
- WHEN se confirma una reserva, THEN el usuario debe ver las condiciones de cancelación y no asistencia.
- WHEN el local reporta una incidencia, THEN se aplica la política global de MVP salvo configuración más restrictiva permitida.
- WHEN la fase avanzada esté habilitada, THEN se podrán configurar avisos, señales, bloqueos por local y restricciones por franja o servicio.

### RF-023 Cancelación preventiva por el local

**Prioridad:** MVP

El local debe poder cancelar una reserva por causa operativa o riesgo elevado, de forma auditada.

#### Criterios de aceptación

- WHEN el local cancela una reserva, THEN debe introducir motivo.
- WHEN se cancela, THEN la reserva pasa a cancelada por local y se libera capacidad.
- WHEN se cancela, THEN el usuario recibe email.
- WHEN se cancela, THEN queda registro de actor, fecha, motivo y datos anteriores.

### RF-024 Reseñas y valoraciones

**Prioridad:** MVP

Usuarios con reserva confirmada deben poder valorar locales.

#### Criterios de aceptación

- WHEN una reserva esté confirmada y finalizada, THEN el usuario puede dejar puntuación de 1 a 5 y comentario opcional.
- WHEN se guarda una reseña, THEN se asocia al local y a la reserva.
- WHEN existen reseñas, THEN el sistema calcula valoración media y número total.
- WHEN el local consulta su panel, THEN puede ver reseñas recibidas.
- WHEN un usuario sin reserva intenta reseñar, THEN el sistema debe impedirlo.

### RF-025 Estadísticas básicas para locales

**Prioridad:** MVP

El local debe consultar métricas básicas.

#### Criterios de aceptación

- WHEN el local abre estadísticas, THEN ve reservas, ocupación, no asistencias, valoración media y evolución simple.
- WHEN filtra por hoy, semana, mes, año o rango, THEN las métricas se recalculan.
- WHEN está en móvil, THEN las métricas se muestran como tarjetas y gráficos simples.

### RF-026 Equipo y disponibilidad

**Prioridad:** MVP parcial

El local debe poder crear empleados, profesionales, recursos o unidades reservables y usarlos para calcular disponibilidad.

#### Criterios de aceptación

- WHEN el local crea un empleado o recurso, THEN puede indicar nombre, alias público, especialidad, estado, horarios semanales y observaciones internas.
- WHEN un empleado o recurso está inactivo, THEN no debe recibir nuevas reservas.
- WHEN un servicio o reserva requiere personal, THEN la disponibilidad debe comprobar que existe al menos un empleado o recurso disponible.
- WHEN el usuario reserva, THEN puede elegir "cualquier profesional disponible" si el local lo permite.
- WHEN la reserva se confirma, THEN queda asignada al empleado o recurso seleccionado o asignado automáticamente.
- WHEN un empleado se archiva, THEN se conserva histórico de reservas.

### RF-027 Servicios del local

**Prioridad:** MVP recomendado

El local debe poder definir servicios reservables básicos para calcular duración y asignación.

#### Criterios de aceptación

- WHEN el local crea un servicio, THEN puede indicar nombre, duración, descripción, capacidad y estado.
- WHEN el servicio se asocia a empleados o recursos, THEN solo esos empleados o recursos pueden ser asignados.
- WHEN el usuario selecciona servicio, THEN el sistema calcula disponibilidad con su duración.
- WHEN no se define servicio, THEN la reserva usa la duración de la franja seleccionada.

### RF-028 Suscripción y RedSys

**Prioridad:** Preparado MVP, cobro completo opcional

La plataforma debe contemplar planes SaaS y pagos externos mediante RedSys.

#### Criterios de aceptación

- WHEN el local consulta suscripción, THEN ve plan actual, estado, fecha de renovación, funcionalidades, historial básico y acciones.
- WHEN el local inicia pago, THEN se muestra resumen del plan y aviso de pago seguro externo RedSys.
- WHEN se redirige a RedSys, THEN la plataforma no solicita ni almacena datos completos de tarjeta.
- WHEN RedSys devuelve respuesta, THEN el sistema registra pago confirmado, rechazado, cancelado, error o pendiente.
- WHEN se registra pago confirmado, THEN la suscripción se actualiza.

### RF-029 Recomendaciones

**Prioridad:** Post-MVP

La plataforma debe poder incorporar recomendaciones personalizadas y listas destacadas.

#### Criterios de aceptación

- WHEN el usuario accede a inicio, THEN el sistema puede mostrar recomendados, destacados y cercanos.
- WHEN no hay historial del usuario, THEN las recomendaciones usan popularidad, valoración, disponibilidad y cercanía.
- WHEN exista suficiente historial, THEN se podrá usar factorización matricial con reservas, valoraciones, categorías y comportamiento.
- WHEN se combinen filtros y recomendaciones, THEN los resultados deben respetar filtros activos.

### RF-030 Administración de plataforma

**Prioridad:** Post-MVP temprano

El administrador debe gestionar el SaaS de forma global.

#### Criterios de aceptación

- WHEN el administrador accede, THEN puede gestionar locales, categorías, incidencias, penalizaciones, planes y métricas globales.
- WHEN audita una incidencia, THEN puede ver reserva, local, email normalizado, estado y actor.
- WHEN modifica una penalización, THEN queda registro auditado.

### RF-031 Internacionalización de textos

**Prioridad:** MVP

Todo texto visible para usuarios finales, locales y administradores debe estar internacionalizado en español e inglés.

#### Criterios de aceptación

- WHEN el idioma del navegador, dispositivo o app empieza por `es`, THEN el sistema debe mostrar la interfaz en español.
- WHEN el idioma no empieza por `es`, THEN el sistema debe mostrar la interfaz en inglés.
- WHEN el usuario haya elegido un idioma manualmente, THEN esa preferencia debe prevalecer sobre el idioma del navegador.
- WHEN se renderice cualquier pantalla, THEN botones, menús, títulos, etiquetas, validaciones, errores, estados, mensajes vacíos, tooltips y textos legales deben salir de catálogos de traducción.
- WHEN se envíe un email, THEN debe enviarse en el idioma resuelto para el destinatario.
- WHEN se genere una notificación o mensaje de sistema, THEN debe existir versión en español e inglés.
- WHEN se muestre una fecha, hora, número o moneda, THEN debe formatearse según el locale activo.
- WHEN falte una clave de traducción, THEN el sistema debe registrar el error y usar fallback controlado en inglés, sin mostrar claves técnicas al usuario.
- WHEN un local configure textos visibles al usuario como descripción, servicios, reglas, campos del formulario o políticas, THEN el sistema debe permitir traducción en español e inglés o aplicar una política explícita de fallback antes de publicar.
- WHEN se añada una nueva pantalla o flujo, THEN no debe aceptarse texto hardcodeado sin clave de traducción.

### RF-032 Verificación empresarial de cuentas de local

**Prioridad:** MVP crítico

El sistema debe diferenciar cuentas normales de cuentas de local mediante un tipo de cuenta empresarial y un identificador fiscal/registral verificable remotamente.

#### Criterios de aceptación

- WHEN se crea una cuenta de local, THEN `account_type` debe quedar como `venue_business` y no como cuenta normal de usuario final.
- WHEN el local se registra, THEN debe aportar `tax_country`, `business_legal_name` y `business_tax_identifier`.
- WHEN el país fiscal sea España, THEN el identificador esperado debe ser NIF/CIF/NIF-IVA según corresponda al tipo de empresa o profesional.
- WHEN el identificador pertenezca a un país con reglas conocidas, THEN el sistema debe validar formato y dígito de control localmente antes de llamar a servicios remotos.
- WHEN el país fiscal pertenezca a la UE y aplique IVA intracomunitario, THEN el sistema debe poder validar el VAT ID mediante VIES u otro proveedor oficial/autorizado equivalente.
- WHEN el país fiscal sea España y no aplique VIES, THEN el sistema debe intentar validación mediante AEAT, servicio autorizado equivalente o proveedor privado aprobado por la plataforma.
- WHEN el país tenga otro registro público o servicio fiscal verificable, THEN debe usarse un adaptador de verificación específico de país.
- WHEN la API remota confirme que el identificador es válido, THEN `business_verification_status` puede pasar a `verified` si el resto de datos obligatorios es coherente.
- WHEN la API confirme nombre o dirección asociados, THEN el sistema debe compararlos con la razón social y dirección aportadas aplicando tolerancia configurable.
- WHEN la API devuelva inválido, THEN `business_verification_status` debe pasar a `rejected` o quedar pendiente de revisión según la política configurada.
- WHEN la API esté caída, no disponible o no devuelva datos suficientes, THEN `business_verification_status` debe quedar en `pending_review` sin publicar locales hasta revisión automática posterior o revisión administrativa.
- WHEN la verificación automática no sea concluyente, THEN el sistema debe solicitar documento de respaldo antes de aprobar manualmente la cuenta.
- WHEN se solicite documento de respaldo en España, THEN se deben admitir documentos como alta censal 036/037, certificado censal, licencia de actividad/apertura o documento administrativo equivalente.
- WHEN se suba un documento de respaldo, THEN debe quedar asociado a la cuenta empresarial y protegido como documentación sensible.
- WHEN un administrador revise documentación de respaldo, THEN debe poder aprobar, rechazar o solicitar corrección con motivo auditado.
- WHEN se complete una verificación, THEN el sistema debe guardar proveedor, fecha, resultado, referencia de consulta si existe y evidencia mínima necesaria para auditoría.
- WHEN se almacene el identificador, THEN debe normalizarse y aplicarse unicidad por país e identificador.
- WHEN se trate de datos fiscales o registrales, THEN el sistema debe minimizar datos guardados y no almacenar respuestas completas de terceros salvo necesidad legal o auditoría definida.

## 5. Requisitos no funcionales

### RNF-001 Seguridad

- Las contraseñas deben almacenarse con hashing robusto y sal.
- Producción debe usar HTTPS obligatorio.
- La API debe validar entradas en servidor.
- El sistema debe proteger contra SQL injection, XSS y CSRF cuando aplique.
- Los endpoints sensibles deben aplicar rate limiting.
- El acceso debe estar protegido por roles: usuario anonimo, local, admin.
- Las acciones críticas deben auditarse.
- Los enlaces seguros de reserva deben usar tokens de alta entropía, expiración o revocación.

### RNF-002 Privacidad y protección de datos

- El sistema debe minimizar datos personales.
- El sistema debe informar finalidad del tratamiento y política de privacidad.
- Debe existir consentimiento para condiciones legales y tratamiento de datos.
- Los datos de incidencias deben tener conservación limitada y reglas claras.
- El sistema debe permitir acceso, rectificación y supresión cuando sea legalmente aplicable.
- La ubicación del usuario solo debe usarse con autorización.
- La información del personal del local solo debe mostrarse si el local la configura como pública.

### RNF-003 Concurrencia y consistencia

- La confirmación de reservas debe ser transaccional.
- El sistema no debe depender de validación frontend para disponibilidad.
- La capacidad disponible debe calcularse con reservas confirmadas y bloqueos temporales vigentes.
- Deben existir jobs de limpieza para reservas expiradas.
- La base de datos debe impedir sobreventa mediante bloqueo, versión o restricción atómica.

### RNF-004 Rendimiento

- Las búsquedas deben responder de forma fluida para catálogos crecientes.
- La ficha del local debe cargar disponibilidad inicial sin bloquear la interfaz.
- Las estadísticas pueden precalcularse o agregarse en background.
- Los resultados frecuentes pueden usar cache con invalidación por cambios de disponibilidad.

### RNF-005 Escalabilidad

- La arquitectura debe soportar crecimiento en locales, reservas simultáneas, búsquedas y emails.
- El backend inicial puede ser monolito modular con límites claros para extraer servicios en el futuro.
- El envío de emails, estadísticas, expiración de bloqueos y entrenamiento de recomendaciones deben ejecutarse en jobs asíncronos.

### RNF-006 Disponibilidad operativa

- Los flujos críticos son búsqueda, disponibilidad, reserva, pagos y penalizaciones.
- Los fallos de email deben reintentarse sin duplicar reservas.
- Los pagos RedSys deben registrar trazas de request, retorno y estado.
- Las operaciones críticas deben ser idempotentes cuando sea posible.

### RNF-007 Usabilidad

- La interfaz debe ser responsive desde el inicio.
- En móvil se deben evitar tablas complejas y usar tarjetas.
- La acción principal debe estar visible en cada pantalla crítica.
- Los errores deben aparecer cerca del campo correspondiente.
- El contador de bloqueo temporal debe ser visible durante el formulario de reserva.
- El lenguaje de incidencias debe ser profesional y no acusatorio.

### RNF-008 Observabilidad

- El sistema debe registrar errores de API, jobs, emails y pagos.
- Deben existir logs estructurados para acciones críticas.
- Deben monitorizarse reservas fallidas, expiraciones, errores de email y callbacks de RedSys.

### RNF-009 Internacionalización y localización

- Los locales soportados inicialmente son `es` y `en`.
- La resolución de idioma debe seguir este orden: preferencia guardada del usuario o local, parámetro explícito seguro si existe, cabecera `Accept-Language` o idioma de la app, y fallback `en`.
- Cualquier variante que empiece por `es`, como `es`, `es-ES`, `es-MX` o `es-AR`, debe resolverse a español.
- Cualquier otro idioma debe resolverse a inglés.
- Los catálogos de traducción deben estar versionados y cubiertos por tests de claves completas.
- Las plantillas de email deben tener versión en ambos idiomas.
- Las categorías, planes y textos de plataforma deben soportar valores localizados.
- Los textos configurados por locales que se muestren públicamente deben almacenar idioma origen y traducciones `es`/`en` cuando sean obligatorias para publicación.

### RNF-010 Verificación empresarial remota

- La verificación empresarial debe ejecutarse en backend y nunca depender únicamente del frontend.
- El módulo debe estar basado en adaptadores por país/proveedor para evitar acoplar el dominio a una API concreta.
- Las llamadas a proveedores externos deben tener timeouts, reintentos controlados, trazabilidad e idempotencia.
- Si el proveedor está caído, el sistema debe degradar a revisión pendiente y no a aprobación automática.
- Los resultados deben auditarse sin guardar más datos fiscales de los necesarios.
- El sistema debe permitir revalidación periódica o manual de una cuenta empresarial.

## 6. Reglas de negocio clave

### RB-001 Identidad del usuario final

En MVP, el usuario final no necesita cuenta. El email normalizado será el identificador principal para reservas, confirmaciones, enlaces seguros, reseñas e incidencias.

### RB-002 Descripción del local

La descripción pública de un local no puede superar 350 palabras.

### RB-003 Capacidad de franja

Una reserva solo puede confirmarse si la capacidad restante de la franja es mayor o igual al número de personas solicitado.

### RB-004 Bloqueo temporal

El bloqueo temporal por defecto dura 5 minutos. Al expirar, la plaza vuelve a estar disponible.

### RB-005 Prioridad de reserva

La prioridad corresponde al usuario que primero consiga crear el bloqueo o confirmación a nivel transaccional.

### RB-006 Asistencia por defecto

Si la reserva ya pasó y el local no marca manualmente no asistencia o pendiente dentro del periodo configurado, el sistema puede marcarla como asistida por defecto.

### RB-007 Penalización global MVP

Las penalizaciones se aplican al email normalizado:

- Primera no asistencia: 7 días sin reservar.
- Segunda no asistencia: 14 días sin reservar.
- Tercera no asistencia: 21 días sin reservar.
- Cuarta o superior: 60 días sin reservar.
- Tras completar un bloqueo de 60 días, el contador operativo puede reiniciarse.

### RB-008 Cancelación de usuario

El usuario puede cancelar mediante enlace seguro si está dentro del plazo permitido por el local.

### RB-009 Cancelación por local

Toda cancelación hecha por el local requiere motivo y registro de auditoría.

### RB-010 Disponibilidad con equipo o recursos

Cuando un local active equipo, servicios o recursos, una franja solo es reservable si se cumple:

- Local abierto.
- Franja activa.
- Capacidad suficiente.
- Empleado o recurso disponible si aplica.
- Servicio compatible si aplica.
- Sin bloqueo manual.
- Sin reservas confirmadas o temporales que consuman la capacidad.

### RB-011 Resolución de idioma

El idioma efectivo se calcula así:

- Si existe preferencia explícita del usuario o local, se usa esa preferencia.
- Si no existe preferencia y el idioma del navegador o app empieza por `es`, se usa español.
- En cualquier otro caso, se usa inglés.

### RB-012 Publicación de cuentas de local

Una cuenta de local solo puede publicar locales y recibir reservas públicas si cumple todas estas condiciones:

- Email verificado.
- `account_type = venue_business`.
- Identificador fiscal/registral normalizado.
- Verificación empresarial aprobada o revisión administrativa aprobada.
- Datos mínimos del local completos.

## 7. Pantallas mínimas del MVP

### Usuario final

- Inicio con buscador.
- Resultados de búsqueda.
- Panel de filtros.
- Ficha del local.
- Disponibilidad y calendario.
- Formulario de reserva.
- Confirmación de reserva.
- Consulta/cancelación mediante enlace seguro.
- Formulario de reseña.

### Local registrado

- Registro de local.
- Verificación de email.
- Verificación empresarial.
- Acceso para locales.
- Panel resumen.
- Perfil del local.
- Horarios y franjas.
- Calendario interno.
- Reservas del día y detalle.
- Formulario de reserva configurable.
- Equipo y disponibilidad.
- Asistencia e incidencias.
- Estadísticas básicas.
- Suscripción y pago externo RedSys.

### Administrador

- Login admin.
- Gestión de locales.
- Gestión de categorías.
- Revisión básica de incidencias.
- Gestión básica de penalizaciones.

## 8. Definición de terminado del MVP

El MVP se considera listo cuando:

- Un local puede registrarse, verificar email, configurar perfil, horarios, franjas y capacidad.
- Una cuenta de local solo puede publicarse tras verificación empresarial aprobada o revisión administrativa aprobada.
- Un usuario puede buscar un local, ver su ficha, consultar calendario y reservar sin cuenta.
- Dos usuarios no pueden confirmar simultáneamente la última plaza disponible.
- El usuario recibe email de confirmación con enlace seguro.
- El local recibe la reserva en su panel.
- El local puede marcar asistencia, reportar no asistencia y activar penalización.
- Un email penalizado no puede completar nuevas reservas hasta la fecha indicada.
- El usuario puede cancelar una reserva mediante enlace seguro dentro de plazo.
- Existen reseñas asociadas a reservas válidas.
- El local puede ver estadísticas básicas.
- Las pantallas críticas funcionan correctamente en móvil.
- Las pantallas, emails, errores y textos legales principales están disponibles en español e inglés con selección automática por idioma.
- Existen tests automatizados para disponibilidad, concurrencia, penalizaciones y permisos.
