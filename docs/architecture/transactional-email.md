# Email transaccional y plantillas localizadas

## Alcance

La fase 8 usa un puerto `TransactionalEmailProvider` y un adaptador
`SmtpTransactionalEmailProvider`. El dominio no conoce Brevo ni Mailpit. El adaptador recibe un
`TransactionalEmailMessage` ya localizado, genera un único mensaje MIME
`multipart/alternative` en UTF-8 y lo entrega mediante Spring Mail.

Esta iteración configura la entrega y el renderizado, pero no consume todavía las colas RabbitMQ.
Los listeners, reintentos, idempotencia persistente y dead letters corresponden a 8.7; el
almacenamiento de fallos corresponde a 8.8. Ningún flujo HTTP de identidad o reserva espera al
proveedor.

## Proveedores y entornos

- Local usa Mailpit `v1.30.0` en `127.0.0.1:1025`; la bandeja de desarrollo queda en
  `127.0.0.1:8025`. Los mensajes son efímeros y no deben contener datos reales.
- Staging y producción usan el relay `smtp-relay.brevo.com:465` con autenticación y SSL/TLS.
- El remitente visible, host y puertos son configuración; usuario y contraseña se inyectan desde el
  gestor de secretos. No se incluyen credenciales funcionales en ejemplos ni repositorio.
- Los timeouts de conexión, lectura y escritura son 3 s, 5 s y 5 s por defecto. El adaptador realiza
  un solo intento y propaga `EmailDeliveryException`; no implementa bucles ni reintentos internos.
- `spring.mail.testConnection=false` evita bloquear el arranque si el proveedor está temporalmente
  inaccesible. La salud de entrega se observará desde el consumidor y sus métricas.

Brevo debe tener verificado el dominio/remitente antes de activar un entorno desplegado. Aceptación
SMTP no equivale a entrega final; rebotes, quejas y eventos del proveedor quedan fuera de 8.1.

## Contrato de plantillas

Los catálogos versionados viven en `src/main/resources/email-templates/es.properties` y
`en.properties`. Cada plantilla ofrece asunto, texto plano y HTML:

- verificación de email: enlace de un solo uso y caducidad;
- recuperación de contraseña: enlace de un solo uso, caducidad y aviso para solicitudes ajenas;
- confirmación de reserva: local, dirección, fecha, franja, personas, respuestas, reglas de
  cancelación/no asistencia, enlace seguro de gestión y su caducidad.
- aviso de nueva reserva al local: cliente, email, agenda, personas y respuestas, sin token;
- cancelación por usuario al local: cliente y reserva inactiva, sin motivo inventado ni token;
- cancelación por local al usuario: resumen y motivo obligatorio auditado, sin atribuirle no-show.

`LocalizedEmailTemplateService` es la API tipada. Solo acepta los locales base `es` y `en`;
cualquier valor ausente o no soportado usa fallback `en`. Fechas y horas se formatean con el locale
resuelto; los instantes de caducidad se muestran explícitamente en UTC porque el evento actual no
transporta una zona del destinatario.

El motor exige que todo marcador `{{variable}}` tenga valor. Un catálogo incompleto falla antes de
entregar y nunca muestra una clave técnica. Los valores dinámicos se escapan para HTML, incluidas
respuestas personalizadas, dirección, reglas y nombre del local. El texto plano conserva el valor
legible. Los tokens solo forman parte del enlace dirigido al usuario: no se registran, no se envían
al local y no se persisten en claro desde este módulo.

## Errores, privacidad y operación

El adaptador no registra destinatario, asunto, cuerpo, respuestas ni URL. Si el email está
deshabilitado falla cerrado con `EmailDeliveryException`; la futura cola decidirá reintentos y
registro mínimo usando el identificador del evento. El puerto admite sustituir SMTP por la API de
Brevo sin cambiar servicios de identidad o reservas.

Para comprobar localmente la representación:

1. iniciar `docker compose --env-file .env.local -f infrastructure/compose.yaml up -d mailpit`;
2. ejecutar la API con las variables locales;
3. abrir `http://127.0.0.1:8025`;
4. usar exclusivamente cuentas y tokens de prueba.

Las pruebas unitarias no conectan a ninguno de los proveedores. Validan el sobre MIME, identidad del
remitente, configuración, locale/fallback, contenido obligatorio, UTF-8 y escape de HTML.


## Entrega, fallos y consulta segura

El consumidor de confirmaciones procesa cliente y local como entregas idempotentes independientes. Aplica tres intentos totales con backoff de 1 y 2 segundos; un payload inválido o un fallo agotado se rechaza sin reencolar y RabbitMQ lo dirige a la DLQ. `EmailDeliveries` conserva únicamente evento, reserva, clase de destinatario, estado, intentos, código cerrado y fechas: nunca dirección, cuerpo o token.

`GET /api/public/reservations/manage/{token}` valida el formato opaco antes de calcular SHA-256. Solo una huella vigente devuelve la proyección mínima de esa reserva. Enlaces inválidos, expirados o revocados comparten un 404 estable, y el secreto no aparece en respuestas ni logs.