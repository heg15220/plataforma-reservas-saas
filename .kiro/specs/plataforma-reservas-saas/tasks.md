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
- [ ] 2.15. Implementar CRUD de pestañas personalizadas del local para propietario.
- [ ] 2.16. Mostrar pestañas personalizadas activas dentro de la ficha pública del local.
- [ ] 2.17. Crear tests de permisos, orden, publicación, sanitización e i18n de pestañas personalizadas.

## 5. Fase 3 - Búsqueda pública y descubrimiento

- [ ] 3.1. Implementar endpoint `GET /api/public/venues/search`.
- [ ] 3.2. Añadir búsqueda por nombre y palabras clave.
- [ ] 3.3. Añadir filtros por categoría.
- [ ] 3.4. Añadir filtros por ciudad, zona o dirección normalizada.
- [ ] 3.5. Añadir filtro por radio si hay coordenadas.
- [ ] 3.6. Añadir ordenación por relevancia, valoración, cercanía y disponibilidad.
- [ ] 3.7. Añadir estado resumido de local en resultados.
- [ ] 3.8. Crear pantalla de inicio con buscador y mensaje principal.
- [ ] 3.9. Crear pantalla de resultados con tarjetas.
- [ ] 3.10. Crear panel de filtros desktop y móvil.
- [ ] 3.11. Crear secciones iniciales de recomendados, destacados y cercanos con lógica simple.
- [ ] 3.12. Crear estado vacío para local no encontrado.
- [ ] 3.13. Crear tests de búsqueda y filtros.
- [ ] 3.14. Crear traducciones ES/EN de buscador, filtros, resultados, estados vacíos y tarjetas.

## 6. Fase 4 - Horarios, franjas y disponibilidad

- [ ] 4.1. Crear migraciones de `venue_opening_hours`, `time_slots` y `availability_blocks`.
- [ ] 4.2. Implementar configuración de horario semanal.
- [ ] 4.3. Implementar días cerrados y reservas activas/inactivas por día.
- [ ] 4.4. Implementar creación manual de franjas.
- [ ] 4.5. Implementar generación automática de franjas por duración.
- [ ] 4.6. Implementar capacidad máxima por franja.
- [ ] 4.7. Implementar bloqueo y reapertura manual de franjas.
- [ ] 4.8. Implementar cierre de día completo.
- [ ] 4.9. Implementar cálculo de estado del local.
- [ ] 4.10. Implementar endpoint de disponibilidad pública por local y fecha.
- [ ] 4.11. Crear calendario público de disponibilidad.
- [ ] 4.12. Crear panel privado de horarios y franjas.
- [ ] 4.13. Crear vista de calendario interno básica.
- [ ] 4.14. Crear tests de cálculo de disponibilidad.

## 7. Fase 5 - Equipo, recursos y servicios MVP

- [ ] 5.1. Crear migraciones de `services`, `employee_resources`, `employee_resource_hours` y `service_employee_resources`.
- [ ] 5.2. Implementar CRUD de servicios básicos.
- [ ] 5.3. Implementar CRUD de empleados o recursos.
- [ ] 5.4. Implementar estados activo, inactivo, solo interno y archivado.
- [ ] 5.5. Implementar horario semanal básico por empleado o recurso.
- [ ] 5.6. Implementar asociación entre servicios y empleados o recursos.
- [ ] 5.7. Actualizar cálculo de disponibilidad para exigir recurso disponible cuando aplique.
- [ ] 5.8. Implementar opción "cualquier profesional disponible".
- [ ] 5.9. Implementar asignación automática simple por primera disponibilidad.
- [ ] 5.10. Crear sección "Equipo y disponibilidad" en panel.
- [ ] 5.11. Mostrar selector de servicio y profesional en reserva cuando el local lo configure.
- [ ] 5.12. Crear tests de disponibilidad con empleados, recursos y servicios.

## 8. Fase 6 - Formularios personalizados

- [ ] 6.1. Crear migraciones de `reservation_form_fields` y `reservation_form_responses`.
- [ ] 6.2. Implementar campos base obligatorios del sistema.
- [ ] 6.3. Implementar CRUD de campos personalizados.
- [ ] 6.4. Implementar tipos: texto corto, texto largo, número, selector, checkbox, fecha, teléfono y email.
- [ ] 6.5. Implementar obligatoriedad y orden.
- [ ] 6.6. Implementar opciones para campos selector.
- [ ] 6.7. Implementar previsualización del formulario.
- [ ] 6.8. Implementar validación backend de respuestas.
- [ ] 6.9. Crear UI de configuración del formulario.
- [ ] 6.10. Crear tests de validación de formularios.
- [ ] 6.11. Permitir labels y opciones de campos personalizados en español e inglés.
- [ ] 6.12. Bloquear publicación de formularios con textos públicos sin traducción obligatoria o fallback aprobado.

## 9. Fase 7 - Reservas, holds y concurrencia

- [ ] 7.1. Crear migración de `reservations`.
- [ ] 7.2. Implementar endpoint `POST /api/public/reservations/holds`.
- [ ] 7.3. Implementar hold temporal de 5 minutos.
- [ ] 7.4. Implementar transacción con bloqueo de franja o control optimista.
- [ ] 7.5. Implementar cálculo de capacidad con reservas confirmadas y holds vigentes.
- [ ] 7.6. Implementar endpoint `POST /api/public/reservations/{id}/confirm`.
- [ ] 7.7. Validar hold vigente antes de confirmar.
- [ ] 7.8. Validar capacidad real antes de confirmar.
- [ ] 7.9. Validar respuestas del formulario.
- [ ] 7.10. Generar token seguro de gestión de reserva.
- [ ] 7.11. Encolar emails de confirmación.
- [ ] 7.12. Implementar job de expiración de holds.
- [ ] 7.13. Crear formulario público de reserva con contador visible.
- [ ] 7.14. Crear pantalla de confirmación.
- [ ] 7.15. Crear tests de concurrencia para última plaza.
- [ ] 7.16. Crear tests de confirmación de hold expirado.

## 10. Fase 8 - Emails y enlace seguro de gestión

- [ ] 8.1. Configurar proveedor de email transaccional.
- [ ] 8.2. Crear plantillas ES/EN de verificación de email y recuperación de contraseña.
- [ ] 8.3. Crear plantillas ES/EN de confirmación para usuario.
- [ ] 8.4. Crear plantillas ES/EN de aviso de nueva reserva para local.
- [ ] 8.5. Crear plantillas ES/EN de cancelación por usuario.
- [ ] 8.6. Crear plantillas ES/EN de cancelación por local.
- [ ] 8.7. Implementar cola de envío con reintentos.
- [ ] 8.8. Implementar almacenamiento de errores de envío.
- [ ] 8.9. Implementar endpoint `GET /api/public/reservations/manage/{token}`.
- [ ] 8.10. Implementar cancelación por token seguro.
- [ ] 8.11. Validar plazo de cancelación configurado por local.
- [ ] 8.12. Crear pantalla pública de consulta/cancelación.
- [ ] 8.13. Crear tests de token inválido, expirado y cancelación válida.
- [ ] 8.14. Crear tests de selección de idioma en emails según locale del destinatario.

## 11. Fase 9 - Panel de reservas del local

- [ ] 9.1. Implementar endpoint `GET /api/venue/me/reservations`.
- [ ] 9.2. Implementar filtros por día, semana, mes, franja, estado y usuario.
- [ ] 9.3. Implementar endpoint de detalle de reserva.
- [ ] 9.4. Mostrar respuestas del formulario en detalle.
- [ ] 9.5. Mostrar empleado o recurso asignado.
- [ ] 9.6. Mostrar historial de incidencias asociado al email.
- [ ] 9.7. Crear pantalla de reservas del día.
- [ ] 9.8. Crear detalle de reserva desktop y móvil.
- [ ] 9.9. Añadir actualización tras nueva reserva.
- [ ] 9.10. Crear tests de permisos y filtros.

## 12. Fase 10 - Asistencia, incidencias y penalizaciones

- [ ] 10.1. Crear migraciones de `no_show_incidents`, `penalties` y `venue_booking_rules`.
- [ ] 10.2. Implementar reglas básicas de cancelación por local.
- [ ] 10.3. Implementar marcado de asistida, no asistida y pendiente.
- [ ] 10.4. Implementar job para marcar asistida por defecto tras periodo configurado.
- [ ] 10.5. Implementar reporte de no asistencia con confirmación.
- [ ] 10.6. Implementar auditoría del reporte.
- [ ] 10.7. Implementar cálculo de penalización 7, 14, 21 y 60 días.
- [ ] 10.8. Implementar validación de penalización activa durante confirmación de reserva.
- [ ] 10.9. Implementar historial profesional de incidencias por email.
- [ ] 10.10. Implementar cancelación preventiva por local con motivo.
- [ ] 10.11. Crear sección "Incidencias y reglas de reserva".
- [ ] 10.12. Crear vista móvil de asistencia e incidencias.
- [ ] 10.13. Crear tests de escalado de penalizaciones.
- [ ] 10.14. Crear tests de bloqueo de email penalizado.
- [ ] 10.15. Crear tests de auditoría de cancelación y reporte.
- [ ] 10.16. Crear traducciones ES/EN para incidencias, penalizaciones, advertencias y mensajes de restricción.

## 13. Fase 11 - Reseñas y valoraciones

- [ ] 11.1. Crear migración de `reviews`.
- [ ] 11.2. Implementar creación de reseña solo con reserva confirmada/finalizada.
- [ ] 11.3. Impedir más de una reseña por reserva.
- [ ] 11.4. Calcular valoración media y número de reseñas.
- [ ] 11.5. Mostrar reseñas en ficha pública.
- [ ] 11.6. Mostrar reseñas en panel del local.
- [ ] 11.7. Crear UI de valoración de 1 a 5 estrellas.
- [ ] 11.8. Crear tests de autorización de reseñas.
- [ ] 11.9. Añadir botón "Hacer reseña" dentro de los detalles de la ficha pública del local.
- [ ] 11.10. Implementar comprobación de elegibilidad de reseña por email normalizado, local y reserva pasada confirmada/finalizada.
- [ ] 11.11. Mostrar mensaje i18n cuando el email no tenga reservas pasadas elegibles en ese local o cuando todas sus reservas elegibles ya tengan reseña.
- [ ] 11.12. Crear tests de elegibilidad por email/local, rechazo sin reserva, rechazo por reseña duplicada y no exposición de datos de reservas.

## 14. Fase 12 - Estadísticas básicas

- [ ] 12.1. Crear migración de `stats_daily_venue`.
- [ ] 12.2. Implementar agregación diaria de estadísticas.
- [ ] 12.3. Implementar métricas de reservas, ocupación, cancelaciones, no asistencias y valoración media.
- [ ] 12.4. Implementar filtros hoy, semana, mes, año y rango personalizado.
- [ ] 12.5. Crear panel de estadísticas desktop.
- [ ] 12.6. Crear panel móvil con tarjetas y gráficos simples.
- [ ] 12.7. Crear tests de agregación.

## 15. Fase 13 - Suscripción y RedSys preparado sin cobro real en MVP

- [ ] 13.1. Crear migraciones de `plans`, `subscriptions` y `payments`.
- [ ] 13.2. Crear planes gratuito, profesional y premium.
- [ ] 13.3. Implementar estados de suscripción.
- [ ] 13.4. Crear pantalla de suscripción del local.
- [ ] 13.5. Mostrar estado de monetización y aviso de pago seguro externo RedSys solo cuando el cobro real esté habilitado.
- [ ] 13.6. Implementar interfaz de proveedor de pagos y adaptador simulado para local, test y staging.
- [ ] 13.7. Preparar adaptador RedSys por redirección, configuración segura y contratos de creación de orden, retorno y notificación.
- [ ] 13.8. Implementar validación de firma e idempotencia mediante simulador y fixtures oficiales, sin activar producción.
- [ ] 13.9. Actualizar estado de suscripción tras pago simulado o confirmado por un proveedor habilitado.
- [ ] 13.10. Registrar pago simulado o real como confirmado, rechazado, cancelado, error o pendiente.
- [ ] 13.11. Crear historial básico de facturación.
- [ ] 13.12. Crear tests de callbacks, firma e idempotencia del contrato RedSys.

## 16. Fase 14 - Administración inicial

- [ ] 14.1. Crear acceso admin protegido.
- [ ] 14.2. Implementar gestión de categorías.
- [ ] 14.3. Implementar listado y edición básica de locales.
- [ ] 14.4. Implementar suspensión de local.
- [ ] 14.5. Implementar revisión de incidencias.
- [ ] 14.6. Implementar revisión de cuentas empresariales pendientes.
- [ ] 14.7. Implementar aprobación, rechazo y reintento manual de verificación empresarial.
- [ ] 14.8. Implementar revisión de documentos de respaldo con aprobación, rechazo o solicitud de corrección.
- [ ] 14.9. Implementar gestión básica de penalizaciones.
- [ ] 14.10. Implementar gestión básica de planes con textos ES/EN.
- [ ] 14.11. Implementar métricas globales iniciales.
- [ ] 14.12. Crear auditoría visible para acciones críticas.
- [ ] 14.13. Crear tests de permisos admin.
- [ ] 14.14. Crear tests de aprobación/rechazo manual de cuenta empresarial y documentos de respaldo.

## 17. Fase 15 - Responsive y experiencia móvil

- [ ] 15.1. Validar inicio móvil con buscador, ubicación y categorías.
- [ ] 15.2. Validar resultados móviles con tarjetas.
- [ ] 15.3. Validar filtros móviles como panel o modal.
- [ ] 15.4. Validar ficha móvil con pestañas y botón fijo de reserva.
- [ ] 15.5. Validar calendario compacto y franjas táctiles.
- [ ] 15.6. Validar formulario móvil por bloques con contador.
- [ ] 15.7. Validar pantalla móvil de confirmación.
- [ ] 15.8. Validar login móvil de locales.
- [ ] 15.9. Validar panel resumen móvil del local.
- [ ] 15.10. Validar reservas del día y detalle móvil.
- [ ] 15.11. Validar asistencia e incidencias móvil.
- [ ] 15.12. Validar estadísticas y suscripción móvil.
- [ ] 15.13. Corregir textos que desborden botones, tarjetas o paneles.
- [ ] 15.14. Ejecutar pruebas visuales en móvil, tablet y escritorio.
- [ ] 15.15. Ejecutar pruebas visuales con locale español e inglés.
- [ ] 15.16. Validar ficha móvil con pestañas personalizadas y flujo de reseña por email desde el botón de detalles.

## 18. Fase 16 - Seguridad, privacidad y endurecimiento

- [ ] 16.1. Revisar validación backend de todos los endpoints públicos.
- [ ] 16.2. Revisar autorización de endpoints de local y admin.
- [ ] 16.3. Implementar protección CSRF si se usan cookies.
- [ ] 16.4. Sanitizar comentarios, descripciones y campos libres.
- [ ] 16.5. Validar subida de archivos.
- [ ] 16.6. Añadir rate limiting a reserva, login, recuperación y enlaces públicos.
- [ ] 16.7. Hashear tokens públicos de gestión.
- [ ] 16.8. Crear política de privacidad y condiciones de uso.
- [ ] 16.9. Añadir consentimiento explícito en registro y reserva.
- [ ] 16.10. Definir conservación de incidencias y penalizaciones.
- [ ] 16.11. Auditar cancelaciones, reportes, penalizaciones, pagos y cambios de reglas.
- [ ] 16.12. Revisar que no se almacenan datos completos de tarjeta.
- [ ] 16.13. Revisar minimización de datos fiscales/registrales y respuestas de proveedores de verificación empresarial.
- [ ] 16.14. Revisar que todos los mensajes de error públicos usan claves i18n y no filtran detalles de proveedores externos.

## 19. Fase 17 - Observabilidad y operación

- [ ] 17.1. Implementar logs estructurados.
- [ ] 17.2. Implementar tracking de errores de API.
- [ ] 17.3. Implementar tracking de errores de jobs.
- [ ] 17.4. Implementar métricas de reservas confirmadas, fallidas y expiradas.
- [ ] 17.5. Implementar métricas de emails enviados y fallidos.
- [ ] 17.6. Implementar métricas de callbacks RedSys.
- [ ] 17.7. Crear alertas para fallos críticos.
- [ ] 17.8. Documentar runbook de incidencias básicas.

## 20. Fase 18 - Recomendaciones post-MVP

- [ ] 18.1. Definir eventos de interacción para recomendación.
- [ ] 18.2. Crear tabla de interacciones usuario-local con email pseudonimizado.
- [ ] 18.3. Crear dataset de reservas, valoraciones, categorías y ubicación aproximada.
- [ ] 18.4. Implementar recomendaciones simples por popularidad, valoración, cercanía y disponibilidad.
- [ ] 18.5. Preparar pipeline batch de factorización matricial.
- [ ] 18.6. Guardar resultados en tabla de recomendaciones.
- [ ] 18.7. Servir "Recomendados para ti", "Populares cerca de ti", "Mejor valorados", "Locales similares" y "Nuevos disponibles".
- [ ] 18.8. Combinar recomendaciones con filtros activos.
- [ ] 18.9. Crear métricas de clics y reservas generadas por recomendaciones.

## 21. Fase 19 - QA de aceptación MVP

- [ ] 19.1. Validar flujo completo de registro de local.
- [ ] 19.2. Validar registro con identificador empresarial válido.
- [ ] 19.3. Validar rechazo o revisión pendiente con identificador empresarial inválido o proveedor no disponible.
- [ ] 19.4. Validar subida y revisión de documento de respaldo cuando la verificación automática no sea concluyente.
- [ ] 19.5. Validar publicación de local tras verificación de email y verificación empresarial.
- [ ] 19.6. Validar selección automática de español con navegador `es-*`.
- [ ] 19.7. Validar selección automática de inglés con navegador no `es-*`.
- [ ] 19.8. Validar que emails, errores, estados y textos legales se muestran en el idioma resuelto.
- [ ] 19.9. Validar configuración de horarios, franjas y capacidad.
- [ ] 19.10. Validar búsqueda por nombre.
- [ ] 19.11. Validar filtros por ubicación, categoría y disponibilidad.
- [ ] 19.12. Validar ficha pública con calendario.
- [ ] 19.13. Validar reserva sin cuenta.
- [ ] 19.14. Validar email de confirmación y enlace seguro.
- [ ] 19.15. Validar cancelación por usuario dentro de plazo.
- [ ] 19.16. Validar bloqueo de última plaza con dos usuarios simultáneos.
- [ ] 19.17. Validar expiración de hold.
- [ ] 19.18. Validar panel de reservas del local.
- [ ] 19.19. Validar marcado de asistida.
- [ ] 19.20. Validar reporte de no asistencia.
- [ ] 19.21. Validar penalización activa en nueva reserva.
- [ ] 19.22. Validar cancelación por local con auditoría.
- [ ] 19.23. Validar reseña tras reserva desde el botón de la ficha con email elegible.
- [ ] 19.24. Validar estadísticas básicas.
- [ ] 19.25. Validar navegación móvil de usuario final.
- [ ] 19.26. Validar navegación móvil de local.
- [ ] 19.27. Validar rechazo de reseña cuando el email no tenga reserva pasada en ese local.
- [ ] 19.28. Validar pestañas personalizadas de la ficha pública, incluyendo carta, menú, precios, orden, i18n y responsive.
- [ ] 19.29. Validar que todo texto español visible en UI, emails, errores, estados, seeds y documentación de usuario conserva tildes, eñes, signos `¿`/`¡`, caracteres especiales y codificación UTF-8 correcta.

## 22. Criterios de salida del MVP

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

## 23. Backlog priorizado

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

- Recomendaciones por factorización matricial.
- Activación completa de RedSys en producción tras contrato con entidad adquirente, credenciales reales y validación del entorno de pruebas.
- Estadísticas avanzadas.
- Servicios con duración variable avanzada.
- Excepciones de empleados por rango de fechas.
- Reasignación de reservas.
- Multiusuario por local.
- Recordatorios automáticos.
- SMS/WhatsApp.
- Integraciones con calendarios externos.

## 24. Orden técnico recomendado

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

La razón técnica es que disponibilidad y reservas forman el núcleo del producto. Todo lo demás debe integrarse sobre ese flujo sin comprometer consistencia ni permisos.
