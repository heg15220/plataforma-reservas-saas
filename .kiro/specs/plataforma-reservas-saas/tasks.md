# Plataforma SaaS de gestión y búsqueda de reservas online - Plan de construcción

## 1. Enfoque

Este plan divide el desarrollo en incrementos verificables. El objetivo es entregar primero un MVP operativo de reservas con control de concurrencia, panel de local, penalizaciones básicas y experiencia responsive. Las capacidades avanzadas quedan preparadas pero no deben bloquear la entrega inicial.

Cada tarea debe cerrarse con:

- Código integrado.
- Migraciones o cambios de esquema aplicados.
- Tests relevantes.
- Revisión de permisos.
- Validación responsive cuando afecte UI.
- Commit trazable y push al repositorio remoto de GitHub en la rama de fase correspondiente.

Convención transversal: cualquier nombre de tabla escrito en este plan en `snake_case` se conserva como referencia conceptual histórica. La implementación física en migraciones Flyway, entidades JPA y consultas debe traducirlo a `UpperCamelCase` para tablas y `lowerCamelCase` para atributos/columnas, según `design.md` y `RNF-011`.

Convención GitFlow transversal: el desarrollo se organiza en una única rama por fase, creada desde `develop` y nombrada `phase/<numero>-<descripcion>`. Todas las tareas de la fase se implementan mediante commits en esa misma rama; no se crean ramas por tarea. Al cerrar cada tarea, el commit de cierre debe subirse al repositorio remoto de GitHub mediante `git push` sobre la rama de fase, dejando la rama local alineada con `origin`. Al cerrar la fase, su rama se integra mediante pull request en `develop`. La rama `main` queda reservada para promociones a producción desde `develop`; los hotfix urgentes parten de `main` y deben reintegrarse también en `develop`. Esta política sustituye la estrategia histórica de ramas cortas por tarea documentada durante la tarea `0.2`.

## 2. Fase 0 - Preparación del proyecto

- [x] 0.1. Seleccionar stack definitivo de frontend, backend, base de datos, ORM, cola y cache.
- [x] 0.2. Crear repositorio, estructura base y convenciones de ramas.
- [x] 0.3. Configurar linters, formatter, test runner y scripts de desarrollo.
- [x] 0.4. Configurar variables de entorno por entorno: local, staging y producción.
- [x] 0.5. Configurar PostgreSQL local y migraciones.
- [x] 0.6. Configurar cola de trabajos y cache.
- [x] 0.7. Crear layout base responsive y sistema de componentes.
- [x] 0.8. Definir paleta, tipografía, estados visuales e iconografía.
- [x] 0.9. Crear pipeline CI con tests y validación de estilo.
- [x] 0.10. Crear infraestructura i18n con catálogos `es` y `en`.
- [x] 0.11. Implementar resolución de idioma: preferencia guardada, parámetro seguro, navegador/app y fallback `en`.
- [x] 0.12. Añadir test o lint que detecte claves de traducción faltantes y textos hardcodeados en UI.
- [x] 0.13. Definir patrón para textos localizados en base de datos mediante campos `*_i18n` o JSON `{ es, en }`.
- [x] 0.14. Definir y automatizar convenciones backend: tablas `UpperCamelCase`, clases Java `UpperCamelCase`, atributos `lowerCamelCase`, JPA por getters/setters, DAOs con `@Query`, interfaces separadas de servicios/controladores, DTOs REST y conversores.
- [x] 0.15. Añadir validación de codificación UTF-8 y calidad de textos españoles para detectar tildes ausentes, signos de apertura omitidos, caracteres especiales rotos y mojibake en catálogos, plantillas, seeds, migraciones con texto visible y documentación.
- [x] 0.16. Añadir tres publicaciones idempotentes exclusivas del perfil local, con imágenes, disponibilidad móvil, reserva anónima, correo capturable y actualización real de plazas.
- [x] 0.17. Crear una cuenta local autenticable con varios locales publicados para pruebas multi-local.
- [x] 0.18. Añadir una clínica privada ficticia al catálogo local con imagen propia, especialidades, médicos y citas futuras a hora exacta.

## 3. Fase 1 - Identidad, roles y base SaaS

- [x] 1.1. Crear tablas de identidad, sesiones/tokens y roles aplicando nombres físicos `UpperCamelCase` y atributos/columnas `lowerCamelCase`.
- [x] 1.2. Implementar `account_type` con valores `customer`, `venue_business` y `admin`.
- [x] 1.3. Crear tablas `business_accounts`, `business_verification_checks` y `business_verification_documents`.
- [x] 1.4. Implementar registro de local con email, contraseña, país fiscal, razón social e identificador fiscal/registral.
- [x] 1.5. Implementar normalización, unicidad, formato y dígito de control de identificador empresarial por país cuando existan reglas conocidas.
- [x] 1.6. Implementar adaptador de verificación empresarial remoto por país/proveedor.
- [x] 1.7. Implementar validación inicial para España/UE usando NIF/CIF/NIF-IVA/VAT ID según corresponda.
- [x] 1.8. Implementar estados `pending_remote_check`, `verified`, `pending_review`, `rejected` y `expired`.
- [x] 1.9. Implementar solicitud de documento de respaldo cuando la verificación remota no sea concluyente.
- [x] 1.10. Implementar subida privada de alta censal 036/037, certificado censal, licencia de actividad/apertura o documento equivalente.
- [x] 1.11. Bloquear publicación de locales si email o verificación empresarial no están aprobados.
- [x] 1.12. Implementar hashing seguro de contraseñas.
- [x] 1.13. Implementar login y logout de locales.
- [x] 1.14. Implementar verificación de email.
- [x] 1.15. Implementar recuperación de contraseña.
- [x] 1.16. Añadir rate limiting a login, registro, recuperación y verificación empresarial.
- [x] 1.17. Implementar middleware de autorización por rol.
- [x] 1.18. Crear pantalla de registro de local con campos empresariales.
- [x] 1.19. Crear pantalla de carga de documentación de respaldo para verificaciones pendientes.
- [x] 1.20. Crear pantalla de acceso para locales.
- [x] 1.21. Crear textos ES/EN para registro, login, errores y estados de verificación.
- [x] 1.22. Crear tests de registro, login, verificación de email, verificación empresarial, documentación de respaldo y permisos.
- [x] 1.23. Añadir al menú privado acceso al inicio público y cierre de sesión responsive.
- [x] 1.24. Añadir acceso asistido local para la cuenta de Azahar sin depender del autocompletado.

## 4. Fase 2 - Locales, categorías y perfil público

- [x] 2.1. Crear migraciones de `venues`, `categories` y `venue_images`.
- [x] 2.2. Crear seed de categorías iniciales: restaurante, peluquería, campo de fútbol, pista de pádel, instalación municipal, centro deportivo, centro de estética y otros.
- [x] 2.3. Crear traducciones ES/EN para categorías iniciales.
- [x] 2.4. Implementar CRUD de perfil del local para propietario.
- [x] 2.5. Implementar campos localizados para descripción, servicios, reglas y textos públicos configurables.
- [x] 2.6. Implementar validación de descripción máxima de 350 palabras por idioma publicado.
- [x] 2.7. Implementar carga segura de imagen principal.
- [x] 2.8. Implementar galería opcional.
- [x] 2.9. Implementar publicación de local solo con email verificado, verificación empresarial aprobada y datos mínimos.
- [x] 2.10. Crear ficha pública inicial del local con textos vía i18n.
- [x] 2.11. Crear panel de edición de perfil.
- [x] 2.12. Crear tests de permisos para que un local no edite datos de otro.
- [x] 2.13. Crear tests de bloqueo de publicación por verificación empresarial pendiente o rechazada.
- [x] 2.14. Crear migración de `venue_custom_tabs` con orden, estado activo, contenido seguro y campos localizados.
- [x] 2.15. Implementar CRUD de pestañas personalizadas del local para propietario.
- [x] 2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local.
- [x] 2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas.
- [x] 2.18. Gestionar desde el panel el email operativo asociado a cada local publicado propio.
- [x] 2.19. Asignar credenciales privadas independientes a cada local de una cuenta multi-local.
- [x] 2.20. Gestionar creación, selección, edición y archivo de múltiples perfiles de local desde el panel.
- [x] 2.21. Habilitar el formulario base al crear un local y controlar su ausencia en la API pública.
- [x] 2.22. Restringir el alta de locales adicionales a cuentas con capacidad multi-local explícita.
- [x] 2.23. Estabilizar la credencial local del propietario de Azahar & Brasa.

## 5. Fase 3 - Búsqueda pública y descubrimiento

- [x] 3.1. Implementar endpoint `GET /api/public/venues/search`.
- [x] 3.2. Añadir búsqueda por nombre y palabras clave.
- [x] 3.3. Añadir filtros por categoría.
- [x] 3.4. Añadir filtros por ciudad, zona o dirección normalizada.
- [x] 3.5. Añadir filtro por radio si hay coordenadas.
- [x] 3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad.
- [x] 3.7. Añadir estado resumido de local en resultados.
- [x] 3.8. Crear pantalla de inicio con buscador y mensaje principal.
- [x] 3.9. Crear pantalla de resultados con tarjetas.
- [x] 3.10. Crear panel de filtros desktop y móvil.
- [x] 3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple.
- [x] 3.12. Crear estado vacío para local no encontrado.
- [x] 3.13. Crear tests de búsqueda y filtros.
- [x] 3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas.

## 6. Fase 4 - Horarios, franjas y disponibilidad

- [x] 4.1. Crear migraciones de `venue_opening_hours`, `time_slots` y `availability_blocks`.
- [x] 4.2. Implementar configuración de horario semanal.
- [x] 4.3. Implementar días cerrados y reservas activas/inactivas por día.
- [x] 4.4. Implementar creación manual de franjas.
- [x] 4.5. Implementar generación automática de franjas por duración.
- [x] 4.6. Implementar capacidad máxima por franja.
- [x] 4.7. Implementar bloqueo y reapertura manual de franjas.
- [x] 4.8. Implementar cierre de día completo.
- [x] 4.9. Implementar cálculo de estado del local.
- [x] 4.10. Implementar endpoint de disponibilidad pública por local y fecha.
- [x] 4.11. Crear calendario público de disponibilidad.
- [x] 4.12. Crear panel privado de horarios y franjas.
- [x] 4.13. Crear vista de calendario interno básica.
- [x] 4.14. Crear tests de cálculo de disponibilidad.
- [x] 4.15. Implementar gestión profesional de festivos, días libres y excepciones por rango de fechas desde el panel privado.
- [x] 4.16. Crear asistente de primera configuración de reservas y generar el calendario inicial del local.
- [x] 4.17. Ampliar duraciones y permitir retirar de forma segura todas las franjas de una fecha.
- [x] 4.18. Evitar la generación inicial de franjas cuando el local elige gestionar solo por día.

## 7. Fase 5 - Equipo, recursos y servicios MVP

- [x] 5.1. Crear migraciones de `services`, `employee_resources`, `employee_resource_hours` y `service_employee_resources`.
- [x] 5.2. Implementar CRUD de servicios básicos.
- [x] 5.3. Implementar CRUD de empleados o recursos.
- [x] 5.4. Implementar estados activo, inactivo, solo interno y archivado.
- [x] 5.5. Implementar horario semanal básico por empleado o recurso.
- [x] 5.6. Implementar asociación entre servicios y empleados o recursos.
- [x] 5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique.
- [x] 5.8. Implementar opción "cualquier profesional disponible".
- [x] 5.9. Implementar asignación automática simple por primera disponibilidad.
- [x] 5.10. Crear sección "Equipo y disponibilidad" en panel.
- [x] 5.11. Mostrar selector de servicio y profesional en reserva cuando el local lo configure.
- [x] 5.12. Crear tests de disponibilidad con empleados, recursos y servicios.
- [x] 5.13. Implementar especialidades clínicas con médicos, citas a hora exacta y gestión integral desde el panel privado.

## 8. Fase 6 - Formularios personalizados

- [x] 6.1. Crear migraciones de `reservation_form_fields` y `reservation_form_responses`.
- [x] 6.2. Implementar campos base obligatorios del sistema.
- [x] 6.3. Implementar CRUD de campos personalizados.
- [x] 6.4. Implementar tipos: texto corto, texto largo, número, selector, checkbox, fecha, teléfono y email.
- [x] 6.5. Implementar obligatoriedad y orden.
- [x] 6.6. Implementar opciones para campos selector.
- [x] 6.7. Implementar previsualización del formulario.
- [x] 6.8. Implementar validación backend de respuestas.
- [x] 6.9. Crear UI de configuración del formulario.
- [x] 6.10. Crear tests de validación de formularios.
- [x] 6.11. Permitir labels y opciones de campos personalizados en español e inglés.
- [x] 6.12. Bloquear publicación de formularios con textos públicos sin traducción obligatoria o fallback aprobado.
- [x] 6.13. Corregir la localización española y las propiedades responsive del editor privado de formularios.

## 9. Fase 7 - Reservas, holds y concurrencia

- [x] 7.1. Crear migración de `reservations`.
- [x] 7.2. Implementar endpoint `POST /api/public/reservations/holds`.
- [x] 7.3. Implementar hold temporal de 5 minutos.
- [x] 7.4. Implementar transacción con bloqueo de franja o control optimista.
- [x] 7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes.
- [x] 7.6. Implementar endpoint `POST /api/public/reservations/{id}/confirm`.
- [x] 7.7. Validar hold vigente antes de confirmar.
- [x] 7.8. Validar capacidad real antes de confirmar.
- [x] 7.9. Validar respuestas del formulario.
- [x] 7.10. Generar token seguro de gestión de reserva.
- [x] 7.11. Encolar emails de confirmación.
- [x] 7.12. Implementar job de expiración de holds.
- [x] 7.13. Crear formulario público de reserva con contador visible.
- [x] 7.14. Crear pantalla de confirmación.
- [x] 7.15. Crear tests de concurrencia para última plaza.
- [x] 7.16. Crear tests de confirmación de hold expirado.

## 10. Fase 8 - Emails y enlace seguro de gestión

- [x] 8.1. Configurar proveedor de email transaccional.
- [x] 8.2. Crear plantillas ES/EN de verificación de email y recuperación de contraseña.
- [x] 8.3. Crear plantillas ES/EN de confirmación para usuario.
- [x] 8.4. Crear plantillas ES/EN de aviso de nueva reserva para local.
- [x] 8.5. Crear plantillas ES/EN de cancelación por usuario.
- [x] 8.6. Crear plantillas ES/EN de cancelación por local.
- [x] 8.7. Implementar cola de envío con reintentos.
- [x] 8.8. Implementar almacenamiento de errores de envío.
- [x] 8.9. Implementar endpoint `GET /api/public/reservations/manage/{token}`.
- [x] 8.10. Implementar cancelación por token seguro.
- [x] 8.11. Validar plazo de cancelación configurado por local.
- [x] 8.12. Crear pantalla pública de consulta/cancelación.
- [x] 8.13. Crear tests de token inválido, expirado y cancelación válida.
- [x] 8.14. Crear tests de selección de idioma en emails según locale del destinatario.

## 11. Fase 9 - Panel de reservas del local

- [x] 9.1. Implementar endpoint `GET /api/venue/me/reservations`.
- [x] 9.2. Implementar filtros por día, semana, mes, franja, estado y usuario.
- [x] 9.3. Implementar endpoint de detalle de reserva.
- [x] 9.4. Mostrar respuestas del formulario en detalle.
- [x] 9.5. Mostrar empleado o recurso asignado.
- [x] 9.6. Mostrar historial de incidencias asociado al email.
- [x] 9.7. Crear pantalla de reservas del día.
- [x] 9.8. Crear detalle de reserva desktop y móvil.
- [x] 9.9. Añadir actualización tras nueva reserva.
- [x] 9.10. Crear tests de permisos y filtros.
- [x] 9.11. Unificar agenda, calendario, horarios, excepciones y franjas en el espacio profesional de Reservas.
- [x] 9.12. Corregir la consulta PostgreSQL de la agenda y habilitar reservas para identidades delegadas multi-local.

## 12. Fase 10 - Asistencia, incidencias y penalizaciones

- [x] 10.1. Crear migraciones de `no_show_incidents`, `penalties` y `venue_booking_rules`.
- [x] 10.2. Implementar reglas básicas de cancelación por local.
- [x] 10.3. Implementar marcado manual de asistida y no asistida durante la hora operativa.
- [x] 10.4. Mantener confirmada por defecto una reserva sin decisión y retirar la asistencia automática.
- [x] 10.5. Implementar reporte de no asistencia con confirmación.
- [x] 10.6. Implementar auditoría del reporte.
- [x] 10.7. Implementar cálculo de penalización 7, 14, 21 y 60 días.
- [x] 10.8. Implementar validación de penalización activa durante confirmación de reserva.
- [x] 10.9. Implementar historial profesional de incidencias por email.
- [x] 10.10. Implementar cancelación preventiva por local con motivo.
- [x] 10.11. Crear sección "Incidencias y reglas de reserva".
- [x] 10.12. Crear vista móvil de asistencia e incidencias.
- [x] 10.13. Crear tests de escalado de penalizaciones.
- [x] 10.14. Crear tests de bloqueo de email penalizado.
- [x] 10.15. Crear tests de auditoría de cancelación y reporte.
- [x] 10.16. Crear traducciones ES/EN para incidencias, penalizaciones, advertencias y mensajes de restricción.
- [x] 10.17. Proyectar pendiente antes del inicio y limitar asistencia y cancelación a la hora posterior.
- [x] 10.18. Añadir semáforo accesible verde, amarillo y rojo al historial profesional según antigüedad y reincidencia.
- [x] 10.19. Mostrar junto al estado de la agenda un aviso de incidencias previas enlazado al detalle.

## 13. Fase 11 - Reseñas y valoraciones

- [x] 11.1. Crear migración de `reviews`.
- [x] 11.2. Implementar creación de reseña solo con reserva confirmada/finalizada.
- [x] 11.3. Impedir más de una reseña por reserva.
- [x] 11.4. Calcular valoración media y número de reseñas.
- [x] 11.5. Mostrar reseñas en ficha pública.
- [x] 11.6. Mostrar reseñas en panel del local.
- [x] 11.7. Crear UI de valoración de 1 a 5 estrellas.
- [x] 11.8. Crear tests de autorización de reseñas.
- [x] 11.9. Añadir botón "Hacer reseña" dentro de los detalles de la ficha pública del local.
- [x] 11.10. Implementar comprobación de elegibilidad de reseña por email normalizado, local y reserva pasada confirmada/finalizada.
- [x] 11.11. Mostrar mensaje i18n cuando el email no tenga reservas pasadas elegibles en ese local o cuando todas sus reservas elegibles ya tengan reseña.
- [x] 11.12. Crear tests de elegibilidad por email/local, rechazo sin reserva, rechazo por reseña duplicada y no exposición de datos de reservas.

## 14. Fase 12 - Estadísticas básicas

- [x] 12.1. Crear migración de `stats_daily_venue`.
- [x] 12.2. Implementar agregación diaria de estadísticas.
- [x] 12.3. Implementar métricas de reservas, ocupación, cancelaciones, no asistencias y valoración media.
- [x] 12.4. Implementar filtros hoy, semana, mes, año y rango personalizado.
- [x] 12.5. Crear panel de estadísticas desktop.
- [x] 12.6. Crear panel móvil con tarjetas y gráficos simples.
- [x] 12.7. Crear tests de agregación.
- [x] 12.8. Corregir la agrupación PostgreSQL de reseñas por fecha local y verificar el endpoint de estadísticas sobre base real.
- [x] 12.9. Añadir selección segura de local y actualización automática de métricas en cuentas multi-local.
- [x] 12.10. Añadir una gráfica temporal del balance de incidencias operativas activadas por local.

## 15. Fase 13 - Suscripción y RedSys preparado sin cobro real en MVP

- [x] 13.1. Crear migraciones de `plans`, `subscriptions` y `payments`.
- [x] 13.2. Crear planes gratuito, profesional y premium.
- [x] 13.3. Implementar estados de suscripción.
- [x] 13.4. Crear pantalla de suscripción del local.
- [x] 13.5. Mostrar estado de monetización y aviso de pago seguro externo RedSys solo cuando el cobro real esté habilitado.
- [x] 13.6. Implementar interfaz de proveedor de pagos y adaptador simulado para local, test y staging.
- [x] 13.7. Preparar adaptador RedSys por redirección, configuración segura y contratos de creación de orden, retorno y notificación.
- [x] 13.8. Implementar validación de firma e idempotencia mediante simulador y fixtures oficiales, sin activar producción.
- [x] 13.9. Actualizar estado de suscripción tras pago simulado o confirmado por un proveedor habilitado.
- [x] 13.10. Registrar pago simulado o real como confirmado, rechazado, cancelado, error o pendiente.
- [x] 13.11. Crear historial básico de facturación.
- [x] 13.12. Crear tests de callbacks, firma e idempotencia del contrato RedSys.

## 16. Fase 14 - Administración inicial

- [x] 14.1. Crear acceso admin protegido.
- [x] 14.2. Implementar gestión de categorías.
- [x] 14.3. Implementar listado y edición básica de locales.
- [x] 14.4. Implementar suspensión de local.
- [x] 14.5. Implementar revisión de incidencias.
- [x] 14.6. Implementar revisión de cuentas empresariales pendientes.
- [x] 14.7. Implementar aprobación, rechazo y reintento manual de verificación empresarial.
- [x] 14.8. Implementar revisión de documentos de respaldo con aprobación, rechazo o solicitud de corrección.
- [x] 14.9. Implementar gestión básica de penalizaciones.
- [x] 14.10. Implementar gestión básica de planes con textos ES/EN.
- [x] 14.11. Implementar métricas globales iniciales.
- [x] 14.12. Crear auditoría visible para acciones críticas.
- [x] 14.13. Crear tests de permisos admin.
- [x] 14.14. Crear tests de aprobación/rechazo manual de cuenta empresarial y documentos de respaldo.

## 17. Fase 15 - Responsive y experiencia móvil

- [x] 15.1. Validar inicio móvil con buscador, ubicación y categorías.
- [x] 15.2. Validar resultados móviles con tarjetas.
- [x] 15.3. Validar filtros móviles como panel o modal.
- [x] 15.4. Validar ficha móvil con pestañas y botón fijo de reserva.
- [x] 15.5. Validar calendario compacto y franjas táctiles.
- [x] 15.6. Validar formulario móvil por bloques con contador.
- [x] 15.7. Validar pantalla móvil de confirmación.
- [x] 15.8. Validar login móvil de locales.
- [x] 15.9. Validar panel resumen móvil del local.
- [x] 15.10. Validar reservas del día y detalle móvil.
- [x] 15.11. Validar asistencia e incidencias móvil.
- [x] 15.12. Validar estadísticas y suscripción móvil.
- [x] 15.13. Corregir textos que desborden botones, tarjetas o paneles.
- [x] 15.14. Ejecutar pruebas visuales en móvil, tablet y escritorio.
- [x] 15.15. Ejecutar pruebas visuales con locale español e inglés.
- [x] 15.16. Validar ficha móvil con pestañas personalizadas y flujo de reseña por email desde el botón de detalles.
- [x] 15.17. Animar lateralmente los recomendados y hacer navegable la superficie completa de las tarjetas públicas de catálogo.

## 18. Fase 16 - Seguridad, privacidad y endurecimiento

- [x] 16.1. Revisar validación backend de todos los endpoints públicos.
- [x] 16.2. Revisar autorización de endpoints de local y admin.
- [x] 16.3. Implementar protección CSRF si se usan cookies.
- [x] 16.4. Sanitizar comentarios, descripciones y campos libres.
- [x] 16.5. Validar subida de archivos.
- [x] 16.6. Añadir rate limiting a reserva, login, recuperación y enlaces públicos.
- [x] 16.7. Hashear tokens públicos de gestión.
- [x] 16.8. Crear política de privacidad y condiciones de uso.
- [x] 16.9. Añadir consentimiento explícito en registro y reserva.
- [x] 16.10. Definir conservación de incidencias y penalizaciones.
- [x] 16.11. Auditar cancelaciones, reportes, penalizaciones, pagos y cambios de reglas.
- [x] 16.12. Revisar que no se almacenan datos completos de tarjeta.
- [x] 16.13. Revisar minimización de datos fiscales/registrales y respuestas de proveedores de verificación empresarial.
- [x] 16.14. Revisar que todos los mensajes de error públicos usan claves i18n y no filtran detalles de proveedores externos.

## 19. Fase 17 - Observabilidad y operación

- [ ] 17.1. Implementar logs estructurados.
- [ ] 17.2. Implementar tracking de errores de API.
- [ ] 17.3. Implementar tracking de errores de jobs.
- [ ] 17.4. Implementar métricas de reservas confirmadas, fallidas y expiradas.
- [ ] 17.5. Implementar métricas de emails enviados y fallidos.
- [ ] 17.6. Implementar métricas de callbacks RedSys.
- [ ] 17.7. Crear alertas para fallos críticos.
- [ ] 17.8. Documentar runbook de incidencias básicas.

## 20. Fase 18 - QA de aceptación MVP

- [ ] 18.1. Validar flujo completo de registro de local.
- [ ] 18.2. Validar registro con identificador empresarial válido.
- [ ] 18.3. Validar rechazo o revisión pendiente con identificador empresarial inválido o proveedor no disponible.
- [ ] 18.4. Validar subida y revisión de documento de respaldo cuando la verificación automática no sea concluyente.
- [ ] 18.5. Validar publicación de local tras verificación de email y verificación empresarial.
- [ ] 18.6. Validar selección automática de español con navegador `es-*`.
- [ ] 18.7. Validar selección automática de inglés con navegador no `es-*`.
- [ ] 18.8. Validar que emails, errores, estados y textos legales se muestran en el idioma resuelto.
- [ ] 18.9. Validar configuración de horarios, franjas y capacidad.
- [ ] 18.10. Validar búsqueda por nombre.
- [ ] 18.11. Validar filtros por ubicación, categoría y disponibilidad.
- [ ] 18.12. Validar ficha pública con calendario.
- [ ] 18.13. Validar reserva sin cuenta.
- [ ] 18.14. Validar email de confirmación y enlace seguro.
- [ ] 18.15. Validar cancelación por usuario dentro de plazo.
- [ ] 18.16. Validar bloqueo de última plaza con dos usuarios simultáneos.
- [ ] 18.17. Validar expiración de hold.
- [ ] 18.18. Validar panel de reservas del local.
- [ ] 18.19. Validar marcado de asistida.
- [ ] 18.20. Validar reporte de no asistencia.
- [ ] 18.21. Validar penalización activa en nueva reserva.
- [ ] 18.22. Validar cancelación por local con auditoría.
- [ ] 18.23. Validar reseña tras reserva desde el botón de la ficha con email elegible.
- [ ] 18.24. Validar estadísticas básicas.
- [ ] 18.25. Validar navegación móvil de usuario final.
- [ ] 18.26. Validar navegación móvil de local.
- [ ] 18.27. Validar rechazo de reseña cuando el email no tenga reserva pasada en ese local.
- [ ] 18.28. Validar pestañas personalizadas de la ficha pública, incluyendo carta, menú, precios, orden, i18n y responsive.
- [ ] 18.29. Validar que todo texto español visible en UI, emails, errores, estados, seeds y documentación de usuario conserva tildes, eñes, signos `¿`/`¡`, caracteres especiales y codificación UTF-8 correcta.

## 21. Fase 19 - Fundamentos de datos del motor de demanda

- [x] 19.1. Seleccionar y documentar el primer vertical o conjunto limitado de servicios, sus métricas de éxito y los criterios de abandono o ampliación.
- [x] 19.2. Crear ADR de límites entre el monolito transaccional y `Demand Engine`, incluyendo contratos, ownership de datos, fallbacks y prohibición de dependencia crítica desde reserva.
- [x] 19.3. Habilitar la extensión `pgvector` mediante Flyway y verificar compatibilidad, rollback lógico, índices y entornos.
- [x] 19.4. Crear migraciones, entidades y DAOs de `CustomerIdentities`, `AnonymousIdentities` e `IdentityLinks` con HMAC versionado, consentimiento, revocación y retención.
- [x] 19.5. Crear catálogo versionado de eventos y contratos JSON/Pydantic para descubrimiento, evaluación, conversión, post-reserva, activación y experimentación.
- [x] 19.6. Crear migraciones, entidades y DAOs de `BehaviorEvents` con idempotencia, fecha de ocurrencia/recepción, finalidad y contexto minimizado.
- [x] 19.7. Crear `RecommendationRequests`, `RecommendationCandidates` y `RecommendationRankings` para conservar alternativas, posiciones, componentes de score, versión y experimento.
- [x] 19.8. Implementar API interna idempotente de ingestión de eventos con validación, cuotas, lotes, contrato opaco de error y ausencia de payloads en logs.
- [x] 19.9. Instrumentar búsqueda, resultados, ficha, filtros, fotos, reseñas, disponibilidad, reserva, cancelación, asistencia y no-show en web y backend.
- [x] 19.10. Garantizar que cada impresión registra únicamente candidatos realmente elegibles y la información que el usuario pudo observar.
- [x] 19.11. Implementar reconciliación entre eventos de frontend y resultados transaccionales de backend mediante identificadores de correlación.
- [x] 19.12. Definir la ontología inicial gobernada de 30-50 atributos para el vertical elegido con familias, jerarquía, ES/EN, fuentes, vigencia y atributos prohibidos.
- [x] 19.13. Crear migraciones, entidades, DAOs y panel admin de `DemandAttributes` y `DemandAttributeCandidates` con workflow de borrador, revisión, publicación, fusión y retirada.
- [x] 19.14. Crear `VenueAttributeEvidence` y `VenueAttributeProfiles` con score, confianza, procedencia, diversidad, expiración y trazabilidad de cálculo.
- [x] 19.15. Implementar agregador configurable de evidencias con fiabilidad, confianza, volumen, acuerdo, diversidad y decaimiento temporal.
- [x] 19.16. Crear política y UI ES/EN de consentimiento separado para analítica, personalización y activación comercial, sin bloquear la reserva operativa.
- [x] 19.17. Implementar acceso, corrección, oposición, revocación, desvinculación y supresión para identidades, eventos, perfiles y resultados derivados.
- [x] 19.18. Definir retención, particionado temporal, índices, umbrales de agregación y estrategia de borrado de eventos y rankings.
- [x] 19.19. Implementar validaciones de calidad, completitud, duplicidad, orden temporal, consentimiento y fuga de PII sobre el dataset fundacional.
- [x] 19.20. Crear dashboards internos de volumen, rechazo, duplicados, latencia y cobertura de instrumentación por evento y versión.
- [x] 19.21. Crear tests unitarios, integración PostgreSQL, contrato, privacidad e idempotencia del sistema de eventos, identidad y ontología.

## 22. Fase 20 - MVP diferencial de matching y demanda

- [x] 20.1. Crear el servicio Python `Demand Engine` con FastAPI, Pydantic, health checks, autenticación servicio-a-servicio, timeouts y configuración por entorno.
- [x] 20.2. Definir contratos de `POST /events`, `POST /recommendations`, `POST /ranking`, `GET /venues/{id}/attributes`, `POST /conversion/predict` y `GET /demand/{venueId}` sin exponerlos directamente a Internet.
- [x] 20.3. Implementar perfil inicial de local desde formulario, servicios, texto localizado y datos operativos mediante reglas y clasificación interpretable.
- [x] 20.4. Seleccionar, versionar y evaluar un modelo multilingüe de Sentence Transformers con licencia, dimensiones, idiomas, latencia y calidad documentados.
- [x] 20.5. Generar embeddings de consultas, locales y servicios en jobs batch idempotentes y persistir versión, checksum, locale y vigencia en pgvector.
- [x] 20.6. Crear generación híbrida de candidatos con full-text, trigram, vector, categoría, radio, estado publicado, servicio y disponibilidad.
- [x] 20.7. Construir perfil contextual de sesión desde filtros, clics, comparaciones y consultas de disponibilidad con consentimiento aplicable.
- [x] 20.8. Implementar afinidad content-based por coseno y contribución por atributos con confianza.
- [x] 20.9. Implementar `ScoreMvp` configurable y versionado con afinidad, conversión baseline, proximidad, disponibilidad, necesidad de capacidad, calidad y exploración.
- [x] 20.10. Aplicar restricciones duras de elegibilidad, capacidad, permisos, filtros y frecuencia después de generar candidatos y antes de ordenar.
- [x] 20.11. Implementar fallbacks deterministas por popularidad contextual, valoración, cercanía, disponibilidad y novedad ante datos o dependencias insuficientes.
- [x] 20.12. Implementar explicaciones ES/EN derivadas de las contribuciones reales del score y limitar su contenido a señales permitidas.
- [x] 20.13. Implementar baseline de ocupación por día-hora con media móvil o suavizado exponencial, incertidumbre y zona horaria.
- [x] 20.14. Calcular necesidad de capacidad y demanda insatisfecha agregada por zona, categoría y periodo con umbrales de privacidad.
- [x] 20.15. Implementar Thompson Sampling básico con prior, cuota máxima de exploración, guardrails de calidad y actualización idempotente.
- [x] 20.16. Integrar recomendaciones en inicio y resultados respetando filtros, disponibilidad, accesibilidad, responsive, i18n y reducción de movimiento.
- [x] 20.17. Clasificar reservas directas, asistidas, generadas y recuperadas mediante una política de atribución versionada y auditable.
- [ ] 20.18. Crear panel inicial de nuevos clientes, reservas originadas, horas valle cubiertas e ingresos atribuidos con definiciones y cobertura visibles.
- [ ] 20.19. Implementar asignación A/B estable, registro previo a exposición y exclusiones mutuas para políticas de ranking.
- [ ] 20.20. Definir métricas offline/online, dataset de evaluación, baseline, umbrales de promoción y guardrails de negocio.
- [ ] 20.21. Crear tests de relevancia, determinismo, filtros duros, fallback, explicación, aislamiento, carga, accesibilidad y experimento.

## 23. Fase 21 - Aprendizaje con primeros datos reales

- [ ] 21.1. Implementar vinculación progresiva de sesión e identidad de cliente mediante HMAC-SHA-256 versionado y rotación de clave.
- [ ] 21.2. Construir perfiles implícitos por atributo con jerarquía de señales, decaimiento temporal, confianza y posibilidad de corrección.
- [ ] 21.3. Implementar pipeline NLP ES/EN de normalización, entidades, negación, clasificación multilabel y mapeo de sinónimos.
- [ ] 21.4. Implementar ABSA sobre reseñas verificadas con scores separados por aspecto, confianza, vigencia y evaluación humana.
- [ ] 21.5. Entrenar y calibrar regresión logística de conversión con separación temporal, prevención de leakage y model card.
- [ ] 21.6. Implementar modelo de elección discreta sobre conjuntos de alternativas para interpretar distancia, precio, atributos y contexto.
- [ ] 21.7. Evaluar LightGBM o CatBoost para conversión y ranking solo si supera baseline, calibración, latencia, estabilidad y equidad.
- [ ] 21.8. Implementar predicción calibrada de no-show como señal de riesgo, sin automatizar penalizaciones ni denegaciones.
- [ ] 21.9. Ejecutar pruebas A/B del ranking con potencia, muestra, periodo, métricas primarias, guardrails y criterio de parada documentados.
- [ ] 21.10. Implementar descubrimiento de atributos candidatos con embeddings, UMAP, HDBSCAN, BERTopic y c-TF-IDF bajo revisión humana.
- [ ] 21.11. Añadir analítica de conversión por servicio, franja, zona, segmento permitido y atributo con intervalos y muestra mínima.
- [ ] 21.12. Crear tests de reproducibilidad, leakage, calibración, sesgo, robustez lingüística, revocación y promoción de modelos.

## 24. Fase 22 - Marketplace con volumen y optimización

- [ ] 22.1. Evaluar Factorization Machines para interacciones dispersas y desplegarlas solo si mejoran el baseline content-based.
- [ ] 22.2. Implementar Learning to Rank con LambdaMART/LightGBM Ranker y evaluación NDCG, conversión, diversidad y exposición.
- [ ] 22.3. Evolucionar la exploración a LinUCB o Thompson Sampling contextual con política offline y límites de riesgo.
- [ ] 22.4. Implementar previsión avanzada de demanda con variables temporales y modelos jerárquicos o boosting, comparada siempre con baseline.
- [ ] 22.5. Diseñar y validar estimación causal mediante A/B antes de usar S/T/X-learner, Causal Forest o Doubly Robust.
- [ ] 22.6. Implementar estimación de uplift con intervalos, overlap, sensibilidad y separación explícita de atribución observacional.
- [ ] 22.7. Crear optimizador OR-Tools de oportunidades sujeto a capacidad, presupuesto, distancia, margen, frecuencia, consentimiento y equidad.
- [ ] 22.8. Implementar listas de espera y `POST /waitlist/allocate` con ofertas escalonadas, expiración e idempotencia.
- [ ] 22.9. Integrar aceptación de ofertas con holds y confirmación transaccional para impedir sobreventa.
- [ ] 22.10. Implementar promociones inteligentes solo con uplift y margen fiables, aprobación del local y límites de contacto.
- [ ] 22.11. Evaluar señales visuales con CLIP como evidencia auxiliar y prohibir inferencias no verificables o sensibles.
- [ ] 22.12. Implementar recomendaciones cruzadas entre categorías con compatibilidad de intención y controles de diversidad.
- [ ] 22.13. Implementar aprendizaje incremental y detección de drift con River, ADWIN, Page-Hinkley o CUSUM donde proceda.
- [ ] 22.14. Medir incrementalidad robusta, reservas recuperadas, coste por cliente y retorno con controles y ventanas documentadas.
- [ ] 22.15. Crear pruebas de optimización, capacidad, frecuencia, equidad, causalidad, drift, rollback y degradación segura.

## 25. Fase 23 - Industrialización, MLOps y gobernanza

- [ ] 23.1. Desplegar MLflow para tracking y registro de datasets, parámetros, métricas, artefactos y modelos con almacenamiento y acceso protegidos.
- [ ] 23.2. Seleccionar Prefect como orquestador inicial y documentar el umbral que justificaría Airflow u otra alternativa.
- [ ] 23.3. Versionar datasets, features, ontología, embeddings, configuraciones, modelos, experimentos y decisiones de promoción de extremo a extremo.
- [ ] 23.4. Implementar validación automática de esquema, calidad, distribución, PII, leakage y sesgo antes de entrenar o promover.
- [ ] 23.5. Separar entornos y permisos de entrenamiento, registro e inferencia con secretos rotables y mínimo privilegio.
- [ ] 23.6. Implementar despliegue shadow/canary, comparación con campeón, rollback automático y fallback a reglas.
- [ ] 23.7. Exponer métricas Prometheus y paneles Grafana para latencia, errores, drift, calibración, cobertura, diversidad, exposición y valor.
- [ ] 23.8. Integrar Evidently para informes de calidad y drift sin convertirlo en fuente única de decisión.
- [ ] 23.9. Crear auditoría administrativa de ontología, pesos, modelos, experimentos, promociones, listas de espera y acciones automáticas.
- [ ] 23.10. Crear model cards, data sheets, evaluación de impacto de privacidad y matriz de atributos prohibidos por versión.
- [ ] 23.11. Implementar revisión humana, corrección e impugnación para atributos y decisiones comerciales materiales.
- [ ] 23.12. Definir SLO, presupuesto de latencia/coste, capacidad, alertas y runbooks del `Demand Engine` y pipelines.
- [ ] 23.13. Ejecutar pruebas de recuperación, pérdida de dependencia, corrupción de artefacto, rotación de secretos y eliminación de datos.
- [ ] 23.14. Realizar revisión jurídica, privacidad, seguridad y equidad antes de activar personalización persistente, promociones u optimización.

## 26. Criterios de salida del MVP

- [ ] Hay documentación de configuración y ejecución local.
- [ ] Todas las migraciones se aplican desde cero.
- [ ] Las pruebas unitarias e integración críticas pasan.
- [ ] Existen pruebas de concurrencia para reservas.
- [ ] Existen pruebas de i18n para español, inglés y fallback.
- [ ] Existen pruebas o validaciones que detectan textos españoles con tildes omitidas, signos de apertura omitidos, caracteres especiales rotos o mojibake.
- [ ] Existen pruebas de verificación empresarial válida, inválida, pendiente y aprobada por documentación de respaldo.
- [ ] El flujo principal funciona en móvil y escritorio.
- [ ] No hay endpoints de local sin autorización.
- [ ] Los emails críticos se encolan y reintentan.
- [ ] La cancelación y el reporte quedan auditados.
- [ ] RedSys no almacena datos completos de tarjeta.
- [ ] La política de privacidad y condiciones están enlazadas antes de reservar.
- [ ] El lenguaje de incidencias es profesional y no acusatorio.
- [ ] No hay textos de sistema hardcodeados fuera del sistema de traducciones.
- [ ] Ningún local puede publicarse sin verificación empresarial aprobada o revisión administrativa aprobada.

## 27. Backlog priorizado

### P0 - Imprescindible MVP

- Registro/login/verificación de locales.
- Internacionalización ES/EN con resolución automática de idioma.
- Verificación empresarial remota del local mediante identificador fiscal/registral.
- Perfil público de local.
- Pestañas personalizadas en ficha pública de local.
- Búsqueda y filtros básicos.
- Horarios, franjas y capacidad.
- Calendario de disponibilidad.
- Reserva con hold temporal.
- Confirmación con control de concurrencia.
- Emails de confirmación.
- Panel de reservas.
- Asistencia/no asistencia.
- Penalizaciones por email.
- Cancelación por enlace seguro.
- Responsive de pantallas críticas.

### P1 - Alto valor MVP

- Formularios personalizados.
- Equipo y disponibilidad básico.
- Reseñas.
- Estadísticas básicas.
- Incidencias y reglas de reserva básicas.
- Cancelación preventiva por local.
- Suscripción preparada y pantalla RedSys.
- Admin básico.

### P2 - Post-MVP

- Motor de demanda fundacional con identidad seudónima, eventos, alternativas y ontología gobernada.
- Recomendación content-based explicable, búsqueda semántica, exploración controlada y predicción
  baseline de ocupación antes de introducir modelos aprendidos complejos.
- Clasificación trazable de reservas directas, asistidas, generadas y recuperadas.
- Experimentación A/B e incrementalidad causal solo cuando exista muestra y grupo de control válidos.
- Factorization Machines, Learning to Rank, bandits contextuales, optimización y señales
  multimodales únicamente tras superar baselines y guardrails.
- Listas de espera, recuperación automática de huecos y promociones inteligentes.
- MLOps, drift, explicabilidad, equidad y gobernanza obligatorios para cualquier automatización.
- Activación completa de RedSys en producción tras contrato con entidad adquirente, credenciales reales y validación del entorno de pruebas.
- Estadísticas avanzadas.
- Servicios con duración variable avanzada.
- Excepciones de empleados por rango de fechas.
- Reasignación de reservas.
- Multiusuario por local.
- Recordatorios automáticos.
- SMS/WhatsApp.
- Integraciones con calendarios externos.

## 28. Orden técnico recomendado

1. Base del proyecto, identidad y roles.
2. Internacionalización base y verificación empresarial.
3. Locales, categorías y perfil.
4. Horarios, franjas y disponibilidad.
5. Reservas con hold y concurrencia.
6. Emails y enlace seguro.
7. Panel del local.
8. Asistencia, incidencias y penalizaciones.
9. Formularios personalizados.
10. Equipo y recursos.
11. Reseñas y estadísticas.
12. Responsive completo.
13. Suscripciones, RedSys y admin.
14. Observabilidad y QA de aceptación del MVP.
15. Fundamentos de datos del motor de demanda: consentimiento, identidad, eventos, alternativas y ontología.
16. MVP diferencial content-based, semántico, explicable y medible.
17. Modelos aprendidos con primeros datos reales y experimentos controlados.
18. Optimización de marketplace solo con volumen, causalidad y guardrails suficientes.
19. Industrialización MLOps, gobernanza y revisión jurídica antes de automatización material.

La razón técnica es que disponibilidad y reservas forman el núcleo del producto. El motor de demanda
se construye después sobre eventos y contratos verificables, pero nunca entra en el camino crítico de
confirmación. Los modelos complejos quedan subordinados a volumen real, experimentación, privacidad,
explicabilidad, equidad y capacidad de rollback.
