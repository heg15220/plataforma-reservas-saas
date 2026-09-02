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
- Ficha pública de local con estado, imágenes, descripción, mapa, reseñas, disponibilidad, botón para hacer reseña y pestañas personalizadas por el local.
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
- Reseñas y valoraciones asociadas a reservas confirmadas, accesibles desde la ficha del local mediante verificación de email con reserva pasada en ese local.
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

El diseño debe dejar puntos de extensión para el motor de generación de demanda, pagos RedSys,
multiusuario, motor de reglas avanzado, formularios por servicio/recurso, estadísticas avanzadas,
auditoría ampliada e integraciones externas. El motor de demanda comprende identidad seudónima,
eventos y alternativas, ontología, búsqueda semántica, ranking explicable, predicción de capacidad,
experimentación, incrementalidad, recuperación de huecos y MLOps, pero ninguna de estas capacidades
debe bloquear la entrega ni el camino transaccional del MVP.

## 3. Perfiles de usuario

### 3.1 Usuario final

Persona que busca locales o servicios, consulta disponibilidad y realiza reservas. En MVP no necesita cuenta registrada.

Capacidades:

- Buscar locales por nombre, palabra clave, categoría y ubicación.
- Usar filtros de ubicación, radio, categoría, disponibilidad y valoración.
- Consultar ficha pública de un local.
- Consultar pestañas personalizadas configuradas por el local, como carta, menú, precios, normas, servicios o información específica del negocio.
- Ver estado del local: abierto, cerrado, no disponible, completo o próximamente disponible.
- Consultar calendario y franjas disponibles.
- Seleccionar servicio, empleado o recurso cuando el local lo permita.
- Realizar reserva mediante formulario.
- Recibir confirmación por email.
- Consultar o cancelar reserva mediante enlace seguro.
- Valorar y reseñar locales desde la ficha pública cuando el email introducido tenga al menos una reserva pasada elegible en ese local.
- Ver mensajes claros cuando su email tenga una restricción temporal activa.

### 3.2 Local, negocio o entidad registrada

Responsable de un negocio o instalación que ofrece reservas en la plataforma.

Capacidades:

- Registrar y verificar una cuenta.
- Configurar datos públicos del local.
- Configurar pestañas personalizadas visibles en la ficha pública del local, con contenido localizado y orden definido por el propio local.
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
- WHEN el usuario pulsa cualquier zona libre de una tarjeta de catálogo, THEN navega a la ficha
  pública del local sin interferir con acciones secundarias como reservar.
- WHEN una tarjeta se muestra en los bloques de catálogo del inicio, THEN presenta la categoría
  como etiqueta visible y sustituye el botón redundante de disponibilidad por un estado binario
  `Abierto` o `Cerrado`.
- WHEN una tarjeta de catálogo presenta su categoría, THEN reutiliza la iconografía y el tratamiento
  outlined de los filtros rápidos de categoría.
- WHEN el local dispone de dirección pública, THEN la tarjeta muestra calle, código postal, ciudad,
  provincia y país, omitiendo únicamente los fragmentos ausentes.
- WHEN una imagen se muestra en una tarjeta de resultados de `Explorar`, THEN se encaja completa en
  un marco interior con separación lateral, sin recortar su contenido, ocupar todo el ancho exterior
  de la tarjeta ni desbordar el viewport.
- WHEN la tarjeta se muestra desde el breakpoint de ordenador, THEN el marco queda centrado y limita
  su ancho máximo para mantener una proporción equilibrada respecto al contenido del local.
- WHEN los resultados de `Explorar` se muestran desde el breakpoint de ordenador, THEN el catálogo
  usa tres columnas compactas para reducir el tamaño individual de las tarjetas sin afectar a la
  lista vertical de móvil y tablet.

### RF-004 Ficha pública del local

**Prioridad:** MVP

Cada local debe disponer de una ficha pública con su información y disponibilidad.

#### Criterios de aceptación

- WHEN el usuario abre un local, THEN ve nombre, imagen principal, categoría, dirección, ubicación en mapa, descripción, horario, estado, valoración y reseñas.
- WHEN el local tenga galería, THEN la ficha puede mostrar imágenes adicionales.
- WHEN el local configure pestañas personalizadas, THEN la ficha debe mostrarlas dentro de los detalles del local respetando orden, título, contenido localizado y estado activo.
- WHEN el local sea un restaurante u otro negocio con información específica, THEN debe poder publicar una pestaña como "Carta" con carta completa, menú, precios y detalles equivalentes.
- WHEN la descripción supere 350 palabras, THEN el sistema debe impedir guardarla o solicitar recorte.
- WHEN existan franjas disponibles, THEN el usuario puede iniciar una reserva desde la ficha.
- WHEN el usuario quiera escribir una reseña, THEN la ficha debe ofrecer un botón visible para iniciar el flujo de reseña desde los detalles del local.
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
- WHEN el usuario consulta la disponibilidad, THEN el selector presenta el mes natural completo en una cuadrícula de siete columnas y 28 a 31 días, no una única semana.
- WHEN el usuario cambia de mes, THEN puede avanzar o retroceder por meses completos sin navegar a un mes enteramente pasado.
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
- WHEN el perfil local contiene Azahar & Brasa creado manualmente, THEN su propietario dispone de
  una identidad de desarrollo estable y reproducible que conserva acceso tras reiniciar la API.
- WHEN la pantalla de acceso se sirve desde Next en desarrollo sobre `localhost` o `127.0.0.1`,
  THEN muestra una acción asistida para cargar de forma exacta la credencial local de Azahar sin
  depender del autocompletado almacenado por el navegador.
- WHEN las credenciales son inválidas, THEN se muestra error genérico sin revelar si el email existe.
- WHEN un local solicita recuperar su contraseña, THEN la respuesta no revela si el email existe,
  está suspendido o no admite recuperación.
- WHEN consume un enlace de recuperación válido con una nueva contraseña segura, THEN el sistema
  reemplaza el hash y revoca todas sus sesiones anteriores.
- WHEN el enlace es inválido, expiró, fue revocado o ya se usó, THEN el sistema devuelve un error
  genérico sin cambiar la credencial.
- WHEN el local está autenticado, THEN puede acceder solo a sus propios datos.
- WHEN una cuenta de local autenticada todavía no dispone de un perfil vigente, THEN el panel
  muestra un estado de bienvenida con una acción directa para crear su primer local.
- WHEN el local usa móvil, THEN el panel muestra una versión simplificada con resumen, reservas, calendario y más.
- WHEN el local navega por cualquier sección privada, THEN dispone de una acción visible para
  volver al inicio público tanto en escritorio como en móvil.
- WHEN el local solicita cerrar sesión, THEN el panel revoca la sesión mediante backend antes de
  dirigirlo al inicio y muestra un error reintentable si no puede confirmar la revocación.
- WHEN una cuenta empresarial gestiona varios locales publicados, THEN el panel muestra una sección
  de emails con todos sus locales y permite asignar a cada uno un destinatario operativo distinto.
- WHEN el propietario cambia el email de un local, THEN el backend valida formato, pertenencia y
  estado publicado usando el ID explícito del local sin permitir consultar o modificar locales ajenos.
- WHEN el propietario configura el acceso de un local, THEN debe asignar conjuntamente un email y
  una contraseña de 12 a 72 caracteres que cumpla el límite criptográfico de BCrypt.
- WHEN se guardan las credenciales de un local, THEN el email sirve para iniciar sesión y recibir
  avisos operativos, la contraseña solo se persiste como hash y la identidad accede exclusivamente
  al panel de ese local sin perder la administración multi-local de la cuenta propietaria.
- WHEN el propietario cambia la contraseña de un local, THEN se revocan todas las sesiones previas
  de esa identidad antes de admitir nuevos accesos.
- WHEN la cuenta propietaria dispone de varios locales vigentes, THEN la sección Perfil público
  muestra un desplegable con todos ellos y permite elegir inequívocamente cuál se está editando.
- WHEN una cuenta dispone de la capacidad multi-local explícita y crea un local adicional, THEN se
  genera un borrador asociado a la misma identidad empresarial sin sustituir ni mezclar los datos
  de las fichas existentes.
- WHEN una cuenta de local único ya dispone de una ficha vigente, THEN Perfil público permite
  editar esa ficha pero no muestra selector, alta ni eliminación de locales adicionales, y la API
  rechaza cualquier intento directo de crear otro local.
- WHEN una cuenta de local único todavía no tiene ficha vigente, THEN puede crear exclusivamente
  su primer local; la capacidad multi-local no se deduce del rol ni de accesos delegados.
- WHEN la cuenta archiva un local desde Perfil público, THEN el panel exige confirmación, retira la
  ficha de la selección y conserva su historial en lugar de efectuar un borrado físico.

### RF-009 Gestión de perfil público

**Prioridad:** MVP

El local debe poder editar los datos visibles de su ficha.

#### Criterios de aceptación

- WHEN el local edita nombre, descripción, categoría, dirección, ubicación, imagen o datos de contacto, THEN los cambios se guardan en su perfil.
- WHEN el local selecciona una imagen principal desde su dispositivo, THEN el panel muestra de inmediato una vista previa local y el nombre del archivo, distingue que aún está pendiente de subida y solo la persiste tras una confirmación explícita.
- WHEN el local consulta o modifica su galería, THEN el panel muestra el número actual de imágenes cargadas y lo actualiza tras cada alta o eliminación correcta.
- WHEN el local selecciona una o varias imágenes para la galería desde su dispositivo, THEN el panel muestra la vista previa y nombre de cada una, solicita un texto alternativo individual y solo habilita el envío cuando existe un perfil y todas las selecciones están descritas.
- WHEN el local desactiva visibilidad de un dato de contacto, THEN ese dato no aparece en la ficha pública.
- WHEN el perfil se carga, crea o actualiza, THEN los controles de visibilidad de correo y teléfono conservan un estado React estable y reflejan el valor persistido sin advertencias de controles no controlados.
- WHEN el backend confirma la publicación del local, THEN el panel muestra un mensaje inequívoco de éxito y una acción para volver a la página de inicio y observar el local; un rechazo no debe mostrar ese estado.
- WHEN el local cambia su dirección o coordenadas, THEN las búsquedas por ubicación deben usar los nuevos datos.
- WHEN el local crea, edita, ordena, activa o desactiva pestañas personalizadas de la ficha, THEN los cambios deben guardarse solo para su local y mostrarse públicamente según estado activo y locale resuelto.
- WHEN el usuario cambia el local seleccionado, THEN formulario, estado editorial, imagen principal
  y galería se recargan para ese identificador explícito y no reutilizan datos de otra ficha.

### RF-010 Gestión de horarios

**Prioridad:** MVP

El local debe configurar horario semanal y días cerrados.

#### Criterios de aceptación

- WHEN el local edita un día, THEN puede marcarlo como abierto, cerrado o con reservas inactivas.
- WHEN el local define horario, THEN debe indicar hora de apertura y cierre válidas.
- WHEN el local cambia horarios, THEN la disponibilidad pública se recalcula.
- WHEN hay reservas confirmadas afectadas por un cambio, THEN el sistema debe avisar al local y no cancelarlas automáticamente sin acción explícita.
- WHEN el local configura un festivo, vacaciones, mantenimiento o día libre, THEN puede aplicarlo a una fecha o a un intervalo inclusivo, indicar un motivo interno y elegir entre cierre completo o pausa de nuevas reservas.
- WHEN el local elimina excepciones de varias fechas, THEN puede restaurar en bloque el horario semanal sin recrear manualmente cada día.
- WHEN el intervalo es inverso, inválido o supera 366 días, THEN el panel bloquea la operación y muestra una explicación clara.
- WHEN el local todavía no tiene un horario semanal persistido, THEN Calendario muestra un asistente de primera configuración en lugar del editor operativo.
- WHEN el local completa la primera configuración, THEN debe escoger mediante desplegables los días abiertos, el cierre semanal opcional, la política de festivos, las jornadas de mañana/tarde/noche, la duración opcional y la capacidad por rango.
- WHEN la primera configuración define duración y capacidad, THEN el sistema guarda el snapshot semanal y genera franjas para las cuatro semanas siguientes omitiendo cierres y festivos concretos.
- WHEN la primera configuración elige gestionar solo por día y sin rangos horarios, THEN el
  sistema guarda el snapshot semanal con cero franjas y no invoca ninguna generación automática.
- WHEN ya existe un horario semanal persistido, THEN el asistente inicial no vuelve a mostrarse y el local puede editar libremente horario, fechas, rangos, festivos, capacidad y franjas.

### RF-011 Gestión de franjas

**Prioridad:** MVP

El local debe poder crear, editar, bloquear y reabrir franjas de reserva.

#### Criterios de aceptación

- WHEN el local crea franjas manuales, THEN cada franja tiene inicio, fin, capacidad máxima y estado.
- WHEN el local usa reglas automáticas, THEN puede generar franjas de 30 minutos, 1 hora o duración personalizada.
- WHEN una franja se marca no disponible, THEN el sistema impide nuevas reservas en esa franja.
- WHEN se modifica capacidad, THEN el sistema valida que no sea menor que las plazas ya confirmadas salvo que se gestione el conflicto.
- WHEN la circunstancia afecta a horas concretas, THEN el local puede crear franjas con inicio y fin o generarlas por duración; WHEN afecta al día completo, THEN puede operar sin rango horario mediante una excepción de fecha.
- WHEN el local genera franjas automáticas, THEN puede escoger al menos 15, 30, 45, 60, 90, 120, 180 o 240 minutos.
- WHEN el local ya no quiere usar las franjas de una fecha, THEN puede retirar todas mediante una acción explícita con confirmación.
- WHEN alguna franja conserva una reserva asociada, THEN la retirada completa se rechaza sin borrar ninguna franja y el panel explica que puede bloquearlas para preservar el historial.

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
- WHEN se crea un local nuevo, THEN su formulario base queda habilitado para que la primera
  publicación del perfil no muestre franjas que desemboquen en un proceso de reserva incompleto.
- WHEN un formulario no está disponible o el slug no identifica un local publicado, THEN la API
  pública devuelve un 404 estable y nunca un error interno 500.
- WHEN el usuario envía el formulario, THEN las respuestas quedan asociadas a la reserva.
- WHEN el propietario usa el editor privado en español, THEN todas las etiquetas, ayudas,
  confirmaciones y estados deben mostrarse en UTF-8 correcto, con singular y plural gramaticales.
- WHEN el editor se representa en cualquier breakpoint, THEN sus propiedades de maquetación deben
  resolverse mediante la API de estilos de MUI sin propagarse como atributos DOM no válidos.

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
- WHEN el propietario haya asignado un email operativo al local reservado, THEN el aviso de la
  reserva se envía a ese email; si no existe asignación se conserva el fallback seguro ya definido.
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
- WHEN la cuenta propietaria o la identidad delegada de un local consulta Reservas, THEN ve las
  reservas confirmadas de todos y solo los locales que puede administrar, sin errores internos al
  omitir filtros opcionales.
- WHEN el local filtra por estado, THEN el listado se actualiza.
- WHEN el local entra en Reservas, THEN dispone en el mismo espacio de agenda, calendario interno y gestión de horarios/disponibilidad, sin perder cambios locales al alternar entre herramientas ya visitadas.
- WHEN una reserva confirmada todavía no ha alcanzado su fecha y hora de inicio, THEN la agenda la muestra con estado temporal `pending` sin modificar el estado confirmado persistido ni liberar capacidad.
- WHEN llega la fecha y hora de inicio, THEN la agenda la muestra como `confirmed`.
- WHEN transcurre una hora desde el inicio sin una decisión manual, THEN la reserva continúa en estado `confirmed` y el sistema no infiere asistencia ni ejecuta una transición automática.
- WHEN una reserva de la agenda tiene nivel de incidencias `watch` o `high`, THEN junto a su estado se muestra un aviso profesional amarillo o rojo que enlaza al detalle para revisar el historial.
- WHEN el nivel de incidencias es `low`, THEN la agenda no muestra un aviso adicional; el listado recibe únicamente el nivel resumido y no expone fechas, tipos ni contenido del historial.

### RF-019 Marcado de asistencia

**Prioridad:** MVP

El local debe decidir manualmente la asistencia durante la hora operativa posterior al inicio.

#### Criterios de aceptación

- WHEN llega la hora de inicio y no ha transcurrido todavía una hora, THEN una cuenta con acceso al local puede marcar la reserva como asistida o no asistida.
- WHEN la reserva todavía no ha comenzado o ya ha transcurrido una hora desde el inicio, THEN los controles de asistencia no se muestran y el backend rechaza cualquier intento directo.
- WHEN el local no toma una decisión durante la hora operativa, THEN la reserva permanece confirmada indefinidamente y nunca se marca asistida por defecto.
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
- WHEN el local consulta el historial, THEN se muestra un indicador informativo verde, amarillo o rojo calculado con el estado operativo, el tiempo desde la última incidencia y la reincidencia dentro de la ventana visible de 12 meses.
- WHEN se muestra el indicador, THEN el nivel también se comunica mediante etiqueta, icono y explicación profesional, sin depender exclusivamente del color.
- WHEN una incidencia está desestimada, THEN no participa en el indicador; el resultado visual tampoco crea penalizaciones, cancela reservas ni sustituye las reglas autoritativas del backend.

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

Usuarios con una reserva pasada válida en un local deben poder valorar ese local desde la ficha pública tras introducir su email.

#### Criterios de aceptación

- WHEN el usuario pulsa el botón de hacer reseña dentro de la ficha del local, THEN el sistema debe solicitar un correo electrónico.
- WHEN el email normalizado introducido tenga al menos una reserva confirmada y finalizada en el pasado para ese local, THEN el sistema puede permitir introducir puntuación de 1 a 5 y comentario opcional.
- WHEN el email normalizado no tenga ninguna reserva pasada elegible en ese local, THEN el sistema debe impedir la reseña y mostrar un mensaje claro e internacionalizado.
- WHEN todas las reservas pasadas elegibles del email para ese local ya tengan reseña asociada, THEN el sistema debe impedir crear otra reseña y mostrar un mensaje claro.
- WHEN se guarda una reseña, THEN se asocia al local, al email normalizado y a una reserva elegible sin reseña previa.
- WHEN existen reseñas, THEN el sistema calcula valoración media y número total.
- WHEN el local consulta su panel, THEN puede ver reseñas recibidas.
- WHEN un usuario sin reserva pasada elegible en ese local intenta reseñar, THEN el sistema debe impedirlo.

### RF-025 Estadísticas básicas para locales

**Prioridad:** MVP

El local debe consultar métricas básicas.

#### Criterios de aceptación

- WHEN el local abre estadísticas, THEN ve reservas, ocupación, no asistencias, valoración media y evolución simple.
- WHEN filtra por hoy, semana, mes, año o rango, THEN las métricas se recalculan.
- WHEN está en móvil, THEN las métricas se muestran como tarjetas y gráficos simples.
- WHEN el backend agrupe reseñas por fecha local, THEN debe reutilizar una única expresión de zona horaria compatible con parámetros preparados de PostgreSQL y devolver la serie aunque no existan reseñas en el periodo.
- WHEN una cuenta tenga acceso a varios locales, THEN el panel de estadísticas debe permitir elegir el local y mostrar exclusivamente las métricas del local seleccionado.
- WHEN se confirme o modifique una reserva mientras el panel de estadísticas permanece abierto, THEN las métricas deben actualizarse periódicamente y al recuperar el foco o la visibilidad, sin exigir una recarga completa de la página.
- WHEN el local consulta estadísticas, THEN debe ver una gráfica temporal del número de incidencias operativas activadas en cada fecha del periodo seleccionado.
- WHEN una incidencia se encuentre en estado `reported` o `confirmed`, THEN debe contabilizarse para su local según la fecha local de `reported_at`; una incidencia `dismissed` no debe formar parte del balance operativo.
- WHEN no existan incidencias activadas durante el periodo, THEN la gráfica debe mostrar un estado vacío profesional, accesible e internacionalizado sin exponer identidades, reservas, emails, motivos ni actores.

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

- WHEN el local sea una clínica con varias secciones, THEN cada especialidad puede asociar uno o varios profesionales visibles por nombre al paciente.
- WHEN el paciente elija un profesional concreto, THEN el sistema debe impedir dos citas solapadas para ese profesional, aunque pertenezcan a servicios o franjas diferentes.

### RF-027 Servicios del local

**Prioridad:** MVP recomendado

El local debe poder definir servicios reservables básicos para calcular duración y asignación.

#### Criterios de aceptación

- WHEN el local crea un servicio, THEN puede indicar nombre, duración, descripción, capacidad y estado.
- WHEN el servicio se asocia a empleados o recursos, THEN solo esos empleados o recursos pueden ser asignados.
- WHEN el usuario selecciona servicio, THEN el sistema calcula disponibilidad con su duración.
- WHEN no se define servicio, THEN la reserva usa la duración de la franja seleccionada.

- WHEN un servicio se configure como cita a hora exacta, THEN el paciente selecciona especialidad, profesional, fecha y hora de inicio sin mostrar un rango, mientras backend conserva la duración para validar solapes.
- WHEN el propietario gestione una especialidad clínica, THEN puede editar nombre, descripción, duración, estado, modo de presentación y profesionales compatibles desde el panel privado.
- WHEN los fixtures de demostración estén habilitados en desarrollo local, THEN el catálogo debe incluir una clínica privada ficticia publicada, con imagen propia, varias especialidades, profesionales identificados y citas futuras a hora exacta para comprobar el recorrido completo sin introducir datos médicos reales.

### RF-028 Suscripción y RedSys

**Prioridad:** Preparado MVP, cobro real post-MVP salvo disponibilidad previa de contrato y credenciales

La plataforma debe contemplar planes SaaS y pagos externos mediante RedSys.

#### Criterios de aceptación

- WHEN el local consulta suscripción, THEN ve plan actual, estado, fecha de renovación, funcionalidades, historial básico y acciones.
- WHEN el cobro real no esté activado, THEN la pantalla debe mostrar el plan y estado sin ofrecer una acción de pago que simule una transacción real.
- WHEN el entorno sea local, test o staging, THEN el adaptador de pagos debe poder usar un simulador determinista para validar estados, idempotencia, firma y callbacks sin dinero real.
- WHEN exista contrato con entidad adquirente, credenciales RedSys y validación de pruebas, THEN podrá activarse el flujo real mediante configuración, sin cambiar el dominio de suscripciones.
- WHEN el local inicia un pago real ya habilitado, THEN se muestra resumen del plan y aviso de pago seguro externo RedSys.
- WHEN se redirige a RedSys, THEN la plataforma no solicita ni almacena datos completos de tarjeta.
- WHEN se persiste diagnóstico de un pago o callback, THEN solo se admiten canal, resultado y código
  de respuesta normalizado; el payload firmado, PAN, CVV, titular, caducidad y firma quedan fuera de
  entidades, auditoría y logs.
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
- WHEN "Recomendados para ti" contenga más tarjetas que su capacidad visible, THEN el carril
  rota automáticamente una tarjeta completa por ciclo para mostrar progresivamente todos los
  locales, con cuatro visibles en escritorio, dos en tablet y una en móvil.
- WHEN el usuario interactúe con el carril o prefiera reducción de movimiento, THEN la rotación
  automática se pausa o desactiva respectivamente.
- WHEN las recomendaciones cambien durante una rotación, THEN ninguna tarjeta debe quedar
  parcialmente recortada por los límites izquierdo o derecho del carril.
- WHEN el motor inteligente todavía no disponga de datos suficientes, THEN debe degradar de forma
  explícita y determinista a señales de contenido, contexto, popularidad, valoración, cercanía,
  disponibilidad y exploración controlada.
- WHEN una opción se muestre como recomendada, THEN debe incluir una explicación comprensible
  basada en señales permitidas y no en inferencias sensibles u opacas.
- WHEN una recomendación se genere, THEN deben quedar versionados el conjunto candidato, el orden,
  los componentes normalizados del score, la política o modelo, el experimento y la configuración
  utilizada, sin incluir datos personales directos.

Los requisitos `RF-033` a `RF-041` se documentan inmediatamente después de `RF-029` porque lo
especializan como bloque cohesivo. Los identificadores `RF-030` a `RF-032` se conservan sin
renumerar para no romper su trazabilidad histórica.

### RF-033 Instrumentación de demanda y conjuntos de alternativas

**Prioridad:** Post-MVP fundacional

La plataforma debe registrar el recorrido de descubrimiento, evaluación, conversión y resultado
posterior para aprender de elecciones reales y medir demanda generada.

#### Criterios de aceptación

- WHEN ocurra una búsqueda, vista de categoría, impresión, clic, aplicación de filtro, consulta de
  fotos/reseñas/disponibilidad, inicio/abandono/finalización de reserva, cancelación, asistencia,
  no-show, reseña, recomendación, promoción, oferta de lista de espera o asignación experimental,
  THEN se registra un evento tipado, versionado e idempotente.
- WHEN se registre una impresión o ranking, THEN se conserva el conjunto de alternativas elegibles,
  su posición, atributos visibles, restricciones y disponibilidad observada, no solo la opción elegida.
- WHEN se registre contexto, THEN se limita a identificadores técnicos, momento, zona aproximada,
  distancia, capacidad, ocupación prevista, precio, versión de ranking y resultado permitido.
- WHEN un evento llegue tarde o duplicado, THEN su identificador idempotente impide duplicidad y su
  hora de ocurrencia se conserva separada de la hora de recepción.
- WHEN el esquema de un evento cambie, THEN el consumidor puede distinguir su versión y los
  productores antiguos permanecen compatibles durante una ventana documentada.
- WHEN un evento no cumpla contrato, consentimiento o finalidad, THEN se rechaza o minimiza antes
  de persistir y se registra una métrica técnica sin copiar el payload a logs.

### RF-034 Identidad seudónima y perfil implícito

**Prioridad:** Post-MVP fundacional

La personalización debe funcionar para usuarios invitados mediante una identidad progresiva,
seudónima, limitada, transparente y revocable.

#### Criterios de aceptación

- WHEN comienza una navegación, THEN el sistema puede crear `sessionId` y, solo con base jurídica y
  consentimiento aplicables, un `anonymousId` propio y aleatorio para navegador o instalación.
- WHEN una reserva aporta email, THEN el identificador analítico estable se deriva con
  HMAC-SHA-256 y clave secreta versionada; nunca se usa un hash simple ni el email en claro como
  feature de modelos.
- WHEN se vinculan una sesión, dispositivo e identidad de cliente, THEN se registra motivo, fecha,
  finalidad y versión de consentimiento, y la vinculación puede revocarse.
- WHEN no existe consentimiento de personalización, THEN el flujo de reserva sigue operativo y las
  recomendaciones se limitan a contexto no personal y agregados permitidos.
- WHEN se actualiza un perfil implícito, THEN cada preferencia conserva valor, confianza, fuentes,
  número de evidencias y recencia, con decaimiento temporal configurable.
- WHEN la persona ejerce acceso, corrección, oposición o supresión, THEN las identidades y perfiles
  vinculados pueden localizarse y atenderse sin depender de datos incluidos en artefactos de modelo.

### RF-035 Ontología y perfil dinámico de establecimientos

**Prioridad:** Post-MVP fundacional

La plataforma debe describir locales, servicios y necesidades con una ontología gobernada de
atributos absolutos, dinámicos, relativos y subjetivos agregados.

#### Criterios de aceptación

- WHEN se crea un atributo, THEN dispone de código estable, nombres ES/EN, definición, familia,
  jerarquía, tipo, fuentes permitidas, caducidad y estado de gobernanza.
- WHEN una fuente aporta evidencia, THEN se conserva puntuación, confianza, procedencia, versión,
  fecha, expiración y referencia verificable sin almacenar texto personal innecesario.
- WHEN se calcula el perfil local-atributo, THEN se ponderan fiabilidad, confianza, diversidad,
  volumen, acuerdo y recencia, y se conservan recuentos y fecha de cálculo.
- WHEN se contradigan evidencias, THEN el score no se sobrescribe sin trazabilidad y la confianza
  refleja el desacuerdo.
- WHEN el sistema descubra un tema nuevo mediante clustering, THEN permanece como candidato hasta
  que una revisión humana lo nombre, fusione, rechace o publique.
- WHEN se analicen imágenes, THEN solo actúan como fuente auxiliar de atributos visuales; no pueden
  afirmar por sí solas limpieza, seguridad, carácter familiar, tranquilidad ni atributos sensibles.
- WHEN se importe una taxonomía fiscal o estadística de locales, THEN sus familias y tipos permanecen
  candidatos hasta revisión humana, disponen de códigos estables y no activan categorías públicas,
  entrenamiento ni promoción por el mero hecho de existir en el catálogo.
- WHEN se migren las ocho categorías sintéticas históricas, THEN `instalacion-municipal` se interpreta
  como atributo de operador y `otros` exige reclasificación por tipo físico; ninguna de ambas se usa
  como nueva verdad visual plana.
- WHEN se reutilicen imágenes de un test ya consumido para reetiquetar la taxonomía, THEN su uso queda
  limitado a desarrollo, requiere revisión humana y conserva `testEligible=false` y promoción false.

### RF-036 Matching semántico, ranking y explicabilidad

**Prioridad:** Post-MVP diferencial

El sistema debe generar candidatos y ordenar oportunidades compatibles con la necesidad, el
contexto, el local, el servicio, el recurso y la franja disponible.

#### Criterios de aceptación

- WHEN una consulta abierta se procese, THEN se combinan filtros duros transaccionales con búsqueda
  textual/vectorial multilingüe y ninguna recomendación puede eludir publicación, permisos,
  disponibilidad o capacidad.
- WHEN todavía no exista volumen, THEN el ranking usa una función ponderada y versionada de
  afinidad, conversión estimada, proximidad, disponibilidad, necesidad de capacidad, calidad y
  exploración.
- WHEN exista evidencia suficiente y se apruebe el cambio, THEN el componente de conversión puede
  evolucionar de regresión logística a boosting y el ranking a Learning to Rank sin cambiar el
  contrato público.
- WHEN se muestre una recomendación, THEN se explican como máximo las señales de mayor contribución,
  con traducciones ES/EN y sin presentar correlaciones como certezas psicológicas o causales.
- WHEN un modelo, vector store o pipeline no esté disponible, THEN se aplica un fallback
  determinista, observable y seguro basado en reglas; la reserva nunca depende del motor de ML.
- WHEN se evalúe el ranking, THEN se miden relevancia, conversión, asistencia, cobertura, diversidad,
  exposición de locales nuevos, latencia y valor comercial.
- WHEN se entrene el recomendador contextual sintético, THEN se selecciona con cinco folds temporales
  rolling-origin, se reserva un test posterior independiente y se publican accuracy, error, precision,
  recall, F1, precision@K y recall@K junto con la definición exacta de positivo.
- WHEN la accuracy por candidato pueda inflarse por siete negativos y un positivo por consulta, THEN
  ninguna puerta puede aprobar solo con accuracy: precision, recall y F1 deben superar 0,80 y el test
  debe alcanzar accuracy >=0,90 y error <0,15 sin usar outcomes, posición o IDs como features.
- WHEN se prueben flujos de negocio, THEN existen casos de afinidad con baja exposición y pocas plazas,
  ambiente visual permitido, horario habitual, cercanía, especialidad, historial, cold-start, calidad,
  disponibilidad y balance precio-distancia; estos contratos no sustituyen evidencia conductual.
- WHEN exista ubicación autorizada en el instante de la recomendación, THEN la distancia se calcula
  desde esa posición point-in-time, se respeta el radio aceptado y las coordenadas crudas no se usan
  como identificador ni como feature persistente; sin permiso se degrada a zona explícita o fallback.
- WHEN queden pocos huecos en una franja, THEN la urgencia solo puede elevar el local si conserva
  capacidad positiva, está abierto, ofrece el servicio, coincide con la intención reciente derivada
  de acciones y satisface proximidad/radio; queda prohibido un boost global por escasez.
- WHEN el usuario cambie su patrón reciente respecto a preferencias históricas, THEN búsquedas,
  filtros, vistas, mapas, consultas de disponibilidad, guardados, comparaciones e inicios de reserva
  anteriores al ranking se ponderan por recencia, y la preferencia persistente solo se usa con
  consentimiento.
- WHEN se amplíen las etiquetas del recomendador con tipos candidatos, THEN solo se asignan subtipos
  funcionalmente compatibles con los locales existentes, se publican cobertura y estado de revisión y
  ninguna etiqueta candidata se presenta como categoría pública o como verdad derivada de la imagen.
- WHEN desarrollo use decisiones observadas débiles y el test use compatibilidad adjudicada, THEN se
  declaran por split las tasas de ambigüedad, se conserva el mismo contrato de features y se impide
  seleccionar hiperparámetros después de abrir el test temporal.
- WHEN el recomendador use patrones visuales, THEN cada embedding procede de una imagen aprobada con
  hash verificado, el perfil visual solo incorpora elecciones y outcomes maduros anteriores y una
  ablación sobre el mismo test cuantifica la mejora respecto al modelo sin píxeles.
- WHEN no exista historial visual o la imagen no esté autorizada, THEN el ranking degrada a contexto
  no visual y después a reglas deterministas; la visión no puede eludir intención incompatible,
  elegibilidad, disponibilidad, capacidad ni la prohibición de inferir atributos sensibles.
- WHEN un holdout visual ya se haya consumido, THEN puede incorporarse únicamente a desarrollo
  histórico, toda mejora posterior se valida dejando fuera establecimientos/vistas completas y no se
  declara calidad confirmada hasta abrir una sola vez otro holdout de imágenes y locales nuevos.
- WHEN la vista global de CLIP pierda detalle espacial, THEN una cabeza candidata puede combinar
  embeddings de regiones deterministas derivadas en memoria, siempre sin modificar los originales,
  sin usar etiquetas/prompt como input y publicando el peor fold además de la métrica media.

### RF-037 Predicción de demanda y capacidad comercial

**Prioridad:** Post-MVP diferencial

La plataforma debe detectar capacidad que probablemente quedará libre y demanda insatisfecha por
zona, categoría y periodo.

#### Criterios de aceptación

- WHEN no haya historial suficiente, THEN la previsión usa baselines auditables por día-hora, medias
  móviles o suavizado exponencial y publica su incertidumbre.
- WHEN se calcule necesidad de capacidad, THEN se usan capacidad y ocupación esperada compatibles
  con la fuente transaccional y su zona horaria.
- WHEN se estime demanda insatisfecha, THEN se comparan búsquedas elegibles y reservas agregadas sin
  revelar consultas o personas individuales.
- WHEN se importen históricos de un local, THEN el origen, finalidad, calidad, zona temporal,
  permisos, deduplicación y retención se validan antes de entrenar o calcular features.
- WHEN la calidad o el volumen no alcancen el umbral definido, THEN no se presenta una predicción
  como fiable ni se activa una acción automática irreversible.

### RF-038 Experimentación, atribución e incrementalidad

**Prioridad:** Post-MVP fundacional para medición

Reserly debe distinguir reservas directas, asistidas, generadas y recuperadas, y separar correlación
de impacto causal.

#### Criterios de aceptación

- WHEN se confirma una reserva, THEN su clasificación comercial se deriva mediante reglas
  versionadas a partir de fuente de entrada, búsquedas, impresiones, recomendaciones y ventana de
  atribución; la evidencia queda auditable.
- WHEN una cancelación o franja liberada se cubra por una oferta o recomendación, THEN puede
  clasificarse como reserva recuperada sin perder su historial transaccional.
- WHEN se ejecute un experimento, THEN la asignación de tratamiento/control es estable,
  mutuamente excluyente, versionada y registrada antes de mostrar la intervención.
- WHEN no existe control válido, THEN el panel usa términos como `atribuido` o `estimado` y no afirma
  causalidad ni ventas incrementales demostradas.
- WHEN exista volumen y diseño experimental suficiente, THEN se pueden estimar uplift y efectos
  heterogéneos con intervalos, diagnósticos y supuestos documentados.
- WHEN se calcule ingreso incremental, THEN se incluyen asistencia, cancelación, importe neto,
  nuevo cliente y ventana de atribución, evitando doble conteo entre canales.

### RF-039 Recuperación de huecos y optimización de oportunidades

**Prioridad:** Post-MVP avanzado

La plataforma debe poder priorizar listas de espera, promociones y asignaciones sin exceder
capacidad, presupuesto, frecuencia de contacto ni restricciones de equidad.

#### Criterios de aceptación

- WHEN se libere una franja, THEN se identifican candidatos compatibles y con consentimiento de
  contacto antes de crear ofertas escalonadas y expirables.
- WHEN una oferta sea aceptada, THEN la reserva usa el mismo hold y control transaccional de
  capacidad que el flujo ordinario.
- WHEN se priorice una lista de espera, THEN se combinan probabilidad de aceptación, asistencia y
  valor permitido, con límites de frecuencia y desempate auditable.
- WHEN se optimicen promociones, THEN el objetivo usa margen neto e incrementalidad estimada y evita
  aplicar descuentos a quien previsiblemente reservaría sin incentivo.
- WHEN no haya estimaciones fiables, THEN se usa una política de cola o prioridad determinista y no
  una optimización opaca.

### RF-040 Analítica comercial del motor de demanda

**Prioridad:** Post-MVP diferencial

El local debe conocer el valor generado por Reserly con métricas trazables y niveles de confianza.

#### Criterios de aceptación

- WHEN el local consulte su panel, THEN puede separar reservas totales, directas, asistidas,
  generadas y recuperadas, además de clientes nuevos/recurrentes y horas valle cubiertas.
- WHEN se muestren ingresos atribuidos o incrementales, THEN se indica moneda, periodo, definición,
  versión de atribución, cobertura y calidad de la estimación.
- WHEN no haya muestra suficiente, THEN el panel presenta estado insuficiente y no extrapola cifras.
- WHEN se muestren demanda insatisfecha, conversión, coste por cliente, asistencia, cancelación,
  no-show o atributos que convierten, THEN se aplican umbrales de agregación y aislamiento por local.
- WHEN el usuario cambie local, rango o zona temporal, THEN las métricas se recalculan con permisos,
  filtros y definiciones coherentes.

### RF-041 Gobernanza del motor inteligente

**Prioridad:** Obligatoria antes de automatización avanzada

La administración debe gobernar ontología, datasets, modelos, experimentos, políticas de ranking y
acciones automáticas.

#### Criterios de aceptación

- WHEN se registre un modelo, THEN dispone de propietario, finalidad, dataset, features, métricas,
  versión, estado, fecha, limitaciones, umbrales y procedimiento de rollback.
- WHEN se promueva una versión, THEN supera validaciones offline, privacidad, sesgo, estabilidad,
  latencia y shadow/canary definidas para su riesgo.
- WHEN cambie un peso, política, atributo o modelo, THEN queda auditoría de actor, motivo, versión
  anterior/nueva y periodo de vigencia.
- WHEN se detecte drift, degradación, fuga de datos o comportamiento inseguro, THEN el sistema puede
  detener la automatización y volver a una política determinista aprobada.
- WHEN una decisión tenga impacto comercial material, THEN existe explicación, supervisión humana y
  mecanismo de impugnación o corrección cuando corresponda.
- WHEN se use un dataset sintético para desarrollar el recomendador, THEN debe ser reproducible,
  versionado, libre de datos personales, separar train/validación/test por tiempo e incluir cohortes
  cold-start sin presentarse como evidencia productiva ni habilitar promoción.
- WHEN dicho dataset represente el marketplace general, THEN locales e imágenes deben cubrir todas
  las categorías activas del catálogo en warm, validación cold-start y test cold-start, y cualquier
  desequilibrio de cardinalidad debe permanecer visible en el manifiesto y las métricas macro.
- WHEN se preparen imágenes sintéticas de locales, THEN cada activo conserva prompt, procedencia,
  hash y revisión humana; una especificación sin imagen materializada o sin revisar permanece
  excluida del entrenamiento y de cualquier puerta visual de producción.
- WHEN la generación de un corpus visual se detenga antes de completar la taxonomía prevista, THEN
  manifiesto e informe distinguen cantidad esperada/materializada, tipos y familias ausentes, y no
  presentan el corpus parcial como cobertura completa ni como evidencia autorizada de producción.
- WHEN se atribuya calidad a patrones de píxeles, THEN la evaluación usa embeddings calculados desde
  los bytes sellados, prohíbe introducir prompt o etiqueta como feature y compara contra un control
  permutado o ablación equivalente; pasar Recall@K no convierte en aprobada una puerta top-1 fallida.
- WHEN se cree el holdout de confirmación taxonómico, THEN cada tipo dispone de una vista development
  y otra holdout con imageId, venueId y hash disjuntos; ambos splits cubren 23 familias y 254 tipos,
  y QA estructural o revisión visual no ejecutan CLIP ni consumen el presupuesto de predicción.
- WHEN un holdout taxonómico consumido se reutilice en una iteración futura, THEN queda marcado para
  siempre como development, `testEligible=false`, y el nuevo test usa imágenes y locales distintos;
  la validación interna rota por vistas independientes del mismo tipo sin compartir la misma imagen
  entre train y validación.
- WHEN se usen arquetipos visuales como señal auxiliar, THEN describen patrones espaciales observables,
  se predicen desde píxeles en inferencia y queda prohibido introducir como feature el arquetipo real,
  el tipo, la familia o el prompt. Las personas solo pueden aportar contexto ambiental secundario:
  no se admiten rostros identificables, menores, pacientes ni inferencias biométricas o sensibles.
- WHEN una métrica visual se use para corregir activos, THEN esa cohorte queda consumida como
  desarrollo y la aceptación automática se mide una sola vez sobre un holdout nuevo, equilibrado
  por categoría, congelado antes de inferencia y conservado íntegramente aunque el resultado falle.
- WHEN un holdout sintético obtenga métricas perfectas, THEN el resultado no demuestra
  generalización ni habilita entrenamiento: se publican tamaño, dificultad, cohortes de contraste y
  la revisión humana continúa siendo obligatoria.
- WHEN se evalúe la generalización visual, THEN el test representativo exige accuracy >=0,90,
  error <=0,10, precision/recall/F1 macro >=0,80, recall por categoría >=0,70 y una brecha absoluta entre train y test
  <=0,10, con tamaño mínimo y hard negatives definidos antes de inferencia.
- WHEN se entrene la cabeza visual supervisada, THEN CLIP permanece congelado, los locales no se
  comparten entre splits, L2/early stopping se seleccionan solo con validación y test se abre una vez
  después de congelar el candidato.
- WHEN una imagen no tenga revisión humana `approved` o `developmentTrainingAllowed=true`, THEN se
  rechaza antes de extraerla como fila de entrenamiento, incluso si su QA automática pasó.
- WHEN la accuracy de desarrollo o entrenamiento sea alta, THEN no se reduce ni manipula para
  aproximarla artificialmente al 80%; el sobreajuste se determina por brecha de generalización,
  estabilidad por categoría, calibración y rendimiento sobre datos no observados.
- WHEN un test sintético pequeño alcance accuracy >=0,98, THEN se marca como posiblemente demasiado
  fácil y requiere auditoría de dificultad y un stress test nuevo; el modelo no se penaliza por
  acertar, pero el benchmark tampoco puede autorizar promoción.

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
- WHEN un local configure textos visibles al usuario como descripción, servicios, reglas, pestañas personalizadas, campos del formulario o políticas, THEN el sistema debe permitir traducción en español e inglés o aplicar una política explícita de fallback antes de publicar.
- WHEN se añada una nueva pantalla o flujo, THEN no debe aceptarse texto hardcodeado sin clave de traducción.
- WHEN una API pública rechace una operación, THEN debe devolver un código estable y una clave i18n disponible en español e inglés, sin exponer excepciones, trazas ni detalles de proveedores externos.
- WHEN ocurra un error inesperado en un endpoint público, THEN la respuesta debe ser genérica y no debe incluir payloads, URLs, estados remotos ni detalles de infraestructura.
- WHEN se cree, modifique o publique cualquier texto en español del producto, documentación de usuario, catálogos i18n, emails, errores, estados, seed de datos visibles o contenido administrativo, THEN debe conservar ortografía española correcta, tildes, apertura de signos de interrogación y exclamación, eñes, diéresis, comillas y caracteres especiales propios del idioma.
- WHEN un texto español se almacene en repositorio, base de datos, plantilla, fixture, migración, email o API, THEN debe codificarse en UTF-8 y no debe aparecer con mojibake ni caracteres sustitutos como `Ã`, `Â`, `�` o equivalentes.

### RF-032 Verificación empresarial de cuentas de local

**Prioridad:** MVP crítico

El sistema debe diferenciar cuentas normales de cuentas de local mediante un tipo de cuenta empresarial y un identificador fiscal/registral verificable remotamente.

#### Criterios de aceptación

- WHEN se crea una cuenta de local, THEN `account_type` debe quedar como `venue_business` y no como cuenta normal de usuario final.
- WHEN el local se registra, THEN debe aportar `tax_country`, `business_legal_name` y `business_tax_identifier`.
- WHEN el país fiscal sea España, THEN el identificador esperado debe ser NIF/CIF/NIF-IVA según corresponda al tipo de empresa o profesional.
- WHEN el identificador pertenezca a un país con reglas conocidas, THEN el sistema debe validar formato y dígito de control localmente antes de llamar a servicios remotos.
- WHEN el país fiscal pertenezca a la UE y aplique IVA intracomunitario, THEN el sistema debe poder validar el VAT ID mediante VIES u otro proveedor oficial/autorizado equivalente.
- WHEN el país fiscal sea España y no aplique VIES, THEN el sistema debe validar formato y dígito de control localmente e intentar comprobación censal mediante la AEAT con certificado electrónico cuando exista un canal técnicamente integrable y autorizado para la plataforma.
- WHEN la AEAT no ofrezca a la plataforma un canal máquina-a-máquina utilizable, THEN la cuenta debe pasar a `pending_review` y la comprobación oficial debe realizarse administrativamente mediante la consulta censal de la AEAT y documentación de respaldo.
- WHEN una solución oficial y gratuita cubra el caso, THEN no debe contratarse ni consultarse un proveedor comercial para esa misma verificación.
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
- WHEN se registre un intento de verificación, THEN debe referenciar la cuenta empresarial sin copiar de nuevo su identificador fiscal y cualquier referencia externa debe existir únicamente en la comprobación que la originó.
- WHEN el proveedor devuelva nombre, dirección o cuerpo de respuesta, THEN esos datos deben procesarse de forma transitoria para obtener coincidencias booleanas y no persistirse; solo se permite conservar la evidencia mínima definida, como una referencia opaca acotada o un hash SHA-256.

## 5. Requisitos no funcionales

### RNF-001 Seguridad

- Las contraseñas deben almacenarse con hashing robusto y sal.
- Producción debe usar HTTPS obligatorio.
- La API debe validar entradas en servidor.
- El sistema debe proteger contra SQL injection, XSS y CSRF cuando aplique.
- Los endpoints sensibles deben aplicar rate limiting.
- El acceso debe estar protegido por roles: usuario anonimo, local, admin.
- CORS con credenciales debe limitarse a orígenes exactos por entorno; en desarrollo local debe
  admitir los puertos web 3000 y 3001 usados por el arranque automático, sin comodines.
- Las acciones críticas deben auditarse dentro de su transacción con actor humano o sistema,
  agregado, acción y snapshots mínimos sin secretos ni datos de tarjeta.
- Los enlaces seguros de reserva deben usar tokens de alta entropía, expiración o revocación.

### RNF-002 Privacidad y protección de datos

- El sistema debe minimizar datos personales.
- El sistema debe informar finalidad del tratamiento y política de privacidad.
- Debe existir consentimiento explícito, no premarcado, para condiciones legales y tratamiento de
  datos. La evidencia mínima debe conservar fecha UTC y versión del documento aceptado; en la
  reserva también debe conservarse el texto localizado de las normas mostrado al cliente.
- Los datos de incidencias deben tener conservación limitada y reglas claras.
- Las incidencias identificables deben permanecer visibles para operación durante un máximo inicial de 12 meses desde su cierre. Después deben anonimizarse o eliminarse del historial operativo.
- Las penalizaciones identificables deben conservarse mientras estén activas y hasta 12 meses después de su finalización para gestionar reclamaciones y detectar errores operativos.
- Cuando sea necesario conservar evidencia para posibles responsabilidades, los datos suprimidos del uso operativo deben quedar bloqueados, sin acceso ordinario, durante un máximo inicial de 3 años y eliminarse al terminar el plazo aplicable, salvo obligación legal o litigio abierto.
- Los plazos de conservación deben revisarse jurídicamente antes de producción y documentarse en la política de privacidad y en el registro de actividades de tratamiento.
- Un job idempotente y auditable debe ejecutar los plazos configurados, excluir inmediatamente los
  registros anonimizados de decisiones y paneles y borrar primero las dependencias que impidan la
  eliminación referencial de la evidencia vencida.
- El sistema debe permitir acceso, rectificación y supresión cuando sea legalmente aplicable.
- La ubicación del usuario solo debe usarse con autorización.
- La información del personal del local solo debe mostrarse si el local la configura como pública.
- Las finalidades operativa, analítica, de personalización, experimentación y activación comercial
  deben estar separadas y disponer de base jurídica, consentimiento y retención propios cuando
  corresponda.
- Los perfiles de personalización y datasets de ML no deben contener email en claro; la unión estable
  por correo debe usar HMAC-SHA-256 con clave secreta versionada y procedimiento de rotación.
- No se permite fingerprinting, enriquecimiento con data brokers ni inferencia automática de género,
  edad, domicilio, personalidad, situación económica, estado emocional, salud, religión, orientación
  o ideología.
- La revocación de personalización debe impedir nuevas inferencias y activar desvinculación,
  anonimización o eliminación conforme a retención, incluidos features y recomendaciones derivadas.

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

### RNF-011 Convenciones de implementación backend y persistencia

- Las tablas físicas de base de datos deben nombrarse en `UpperCamelCase`, empezando por mayúscula y juntando palabras compuestas con mayúscula inicial en cada palabra.
- Las clases Java de Spring Boot deben nombrarse en `UpperCamelCase`.
- Los atributos de entidades, DTOs y clases Java deben nombrarse en `lowerCamelCase`, empezando por minúscula y juntando palabras compuestas con mayúscula inicial desde la segunda palabra.
- Las columnas físicas asociadas a atributos deben seguir `lowerCamelCase` cuando se definan explícitamente.
- En PostgreSQL deben usarse identificadores entrecomillados en migraciones y mapeos cuando sea necesario preservar mayúsculas.
- Las relaciones JPA deben declararse con anotaciones de Spring/JPA en los métodos `get` correspondientes, manteniendo los métodos `set` correspondientes y la consistencia de relaciones bidireccionales.
- Debe existir un DAO por cada entidad persistente y el acceso a datos debe realizarse mediante DAOs con consultas declaradas con `@Query` cuando sean consultas propias del dominio.
- Todos los servicios deben separar interfaz e implementación; otros módulos deben depender de la interfaz.
- Todos los controladores deben separar interfaz e implementación; la interfaz define el contrato de métodos expuestos.
- La capa REST debe usar DTOs y conversores explícitos entre entidades, DTOs y estructuras internas, evitando exponer entidades JPA directamente.
- Estas convenciones deben verificarse en revisión de código y, cuando sea viable, mediante tests, lint o reglas de análisis estático.

### RNF-012 Calidad lingüística, acentos y codificación de textos en español

- Todo texto en español del proyecto debe escribirse con ortografía correcta, incluyendo tildes, eñes, diéresis, signos de apertura `¿` y `¡`, símbolos de moneda, ordinales y cualquier carácter especial necesario.
- Todos los archivos de código, documentación, migraciones, catálogos i18n, plantillas de email, fixtures y seeds que contengan texto visible en español deben guardarse en UTF-8.
- No se aceptan textos en español degradados por problemas de codificación, mojibake o sustitución de caracteres.
- Los catálogos de traducción `es` deben revisarse con una estrategia automatizada y revisión humana antes de cerrar tareas de UI, emails, errores públicos o documentación de usuario.
- Las pruebas de i18n deben incluir validación de caracteres especiales frecuentes en español: `á`, `é`, `í`, `ó`, `ú`, `ü`, `ñ`, `¿`, `¡`, `€`.
- Las respuestas públicas de API, emails y pantallas no deben eliminar tildes para simplificar comparaciones, búsquedas o normalizaciones; cualquier normalización técnica debe aplicarse solo a campos internos no visibles.

### RNF-013 Flujo GitFlow y promoción entre ramas

- El desarrollo debe seguir GitFlow con dos ramas permanentes: `develop` para integración y `main` para producción.
- Cada fase numerada de `tasks.md` debe desarrollarse en una única rama de fase creada desde `develop`; no se deben crear ramas independientes para cada tarea de esa fase.
- Las tareas de una misma fase deben incorporarse mediante commits trazables dentro de su rama de fase.
- Al terminar cada tarea, el commit o commits de cierre deben subirse al repositorio remoto de GitHub en la rama de fase correspondiente, verificando que la rama local queda alineada con `origin`.
- Una fase solo puede integrarse en `develop` mediante pull request cuando sus tareas previstas estén implementadas, verificadas y documentadas, o cuando se apruebe explícitamente una integración parcial.
- `develop` debe representar el estado integrado de la siguiente versión y no debe utilizarse como rama de producción.
- `main` debe contener únicamente versiones candidatas a producción o desplegadas en producción, promovidas desde `develop` mediante pull request de release.
- Las correcciones urgentes de producción deben partir de `main` en una rama `hotfix/*` y reintegrarse tanto en `main` como en `develop`.
- Las ramas `main`, `develop` y de fase deben protegerse contra pushes directos cuando la plataforma del repositorio lo permita; la integración debe pasar por revisión y verificaciones automáticas.

### RNF-014 Rendimiento, resiliencia y MLOps del motor de demanda

- La búsqueda y reserva transaccionales no deben depender de la disponibilidad de la API de
  inteligencia, del vector store, del registro de modelos ni del pipeline de entrenamiento.
- La generación de candidatos y ranking debe tener presupuesto de latencia, timeout, circuit breaker,
  caché acotada y fallback determinista medidos por entorno.
- Entrenamiento batch e inferencia online deben permanecer separados; ningún entrenamiento puede
  escribir directamente sobre la fuente transaccional ni promoverse sin control de versión.
- Datasets, features, ontología, parámetros, modelos, embeddings, rankings y experimentos deben ser
  reproducibles y versionados.
- Deben monitorizarse calidad de datos, drift, calibración, cobertura, diversidad, exposición,
  latencia, errores y métricas de negocio, con alertas y rollback.
- Los jobs de recomputación deben ser idempotentes, reanudables, observables y coordinados entre
  instancias.

### RNF-015 Equidad, explicabilidad y seguridad de decisiones automatizadas

- Los filtros de elegibilidad y capacidad son restricciones duras y prevalecen sobre cualquier score.
- La exploración debe tener cuota máxima, guardrails de calidad y métricas de exposición para evitar
  bucles de popularidad y dar oportunidades controladas a locales nuevos.
- Ningún atributo sensible, proxy no justificado o inferencia prohibida puede participar en features,
  segmentación, ranking, promociones o experimentos.
- Las explicaciones deben derivarse de contribuciones reales del modelo o reglas ejecutadas y no de
  texto generado sin trazabilidad.
- Las acciones de alto impacto deben admitir supervisión humana, auditoría y rollback; la confianza
  insuficiente debe degradar a reglas seguras.
- Las métricas deben segmentarse de forma que permita detectar perjuicios sistemáticos sin exponer
  individuos ni crear categorías sensibles nuevas.

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

### RB-006 Estado temporal y asistencia manual

Una reserva confirmada se presenta como pendiente antes de su inicio. Desde el inicio y durante una hora se presenta como confirmada y permite una decisión manual de asistencia, no asistencia o cancelación por el local. Al finalizar esa hora, si no hubo decisión, permanece confirmada sin transición automática.

### RB-007 Penalización global MVP

Las penalizaciones se aplican al email normalizado:

- Primera no asistencia: 7 días sin reservar.
- Segunda no asistencia: 14 días sin reservar.
- Tercera no asistencia: 21 días sin reservar.
- Cuarta o superior: 60 días sin reservar.
- Tras completar un bloqueo de 60 días, el contador operativo puede reiniciarse.
- Las incidencias con más de 12 meses no participan en el contador operativo y dejan de mostrarse como historial identificable al local.

### RB-008 Cancelación de usuario

El usuario puede cancelar mediante enlace seguro si está dentro del plazo permitido por el local.

### RB-009 Cancelación por local

Toda cancelación hecha por el local requiere motivo y registro de auditoría.

La cancelación operativa desde la agenda solo puede ejecutarse desde la hora de inicio, incluida, hasta una hora después, excluida, por una cuenta con acceso al local.

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

### RB-013 Elegibilidad de reseñas por email y local

Un usuario final solo puede crear una reseña desde la ficha de un local si el email normalizado introducido tiene al menos una reserva confirmada y finalizada en el pasado para ese mismo local.

Reglas:

- La comprobación debe ejecutarse siempre en backend usando `venue_id`, email normalizado y reservas del sistema.
- Las reservas canceladas, expiradas o en hold no habilitan reseñas.
- Cada reserva puede tener como máximo una reseña.
- Si existen varias reservas elegibles sin reseña, el sistema puede asociar la reseña a la reserva elegible más reciente.
- Si no existe reserva elegible o todas las reservas elegibles ya fueron reseñadas, el sistema debe rechazar la creación con un mensaje público claro e internacionalizado.
- La respuesta pública de elegibilidad no debe exponer datos de reservas, fechas ni historial del email.

### RB-014 Clasificación comercial de reservas

- `direct`: el usuario buscó o abrió específicamente el local sin intervención decisiva registrada.
- `assisted`: el usuario comparó categoría, resultados o alternativas y Reserly influyó en la elección.
- `generated`: una recomendación, promoción o descubrimiento atribuible presentó un local nuevo al
  perfil dentro de la ventana configurada.
- `recovered`: una franja liberada se cubrió mediante lista de espera u oferta automática registrada.
- La clasificación debe ser única para el informe principal, versionada, recalculable y acompañada
  de evidencia; no equivale por sí sola a causalidad.

### RB-015 Prevalencia de restricciones sobre ranking

- Un local, servicio, recurso o franja no publicado, no elegible, sin capacidad o fuera de filtros
  nunca puede reintroducirse por score, exploración, promoción o modelo.
- Los componentes del score deben normalizarse, versionarse y configurarse sin despliegue de código.
- Confianza o datos insuficientes obligan a fallback determinista y explicación correspondiente.

### RB-016 Vinculación y revocación de identidad analítica

- Una sesión solo se vincula a una identidad seudónima por un motivo permitido y registrado.
- El email original permanece separado de datos analíticos y cifrado donde deba conservarse por el
  flujo operativo.
- La revocación impide continuar personalizando con esa vinculación y debe propagarse a perfiles y
  datasets derivados conforme a la política de retención.

## 7. Pantallas mínimas del MVP

### Usuario final

- Inicio con buscador.
- Resultados de búsqueda.
- Panel de filtros.
- Ficha del local con pestañas personalizadas y botón para hacer reseña.
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
- Gestión de pestañas personalizadas de la ficha pública.
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
- La ficha del local puede mostrar pestañas personalizadas configuradas por el local, como carta, menú, precios o información específica.
- Dos usuarios no pueden confirmar simultáneamente la última plaza disponible.
- El usuario recibe email de confirmación con enlace seguro.
- El local recibe la reserva en su panel.
- El local puede marcar asistencia, reportar no asistencia y activar penalización.
- Un email penalizado no puede completar nuevas reservas hasta la fecha indicada.
- El usuario puede cancelar una reserva mediante enlace seguro dentro de plazo.
- Existen reseñas asociadas a reservas válidas y el botón de reseña de la ficha solo permite reseñar cuando el email introducido tiene una reserva pasada elegible en ese local.
- El local puede ver estadísticas básicas.
- Las pantallas críticas funcionan correctamente en móvil.
- Las pantallas, emails, errores y textos legales principales están disponibles en español e inglés con selección automática por idioma.
- Existen tests automatizados para disponibilidad, concurrencia, penalizaciones y permisos.
