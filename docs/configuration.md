# Configuración por entornos

Reserly reconoce tres entornos desplegables:

- `local`: desarrollo en la máquina del equipo, HTTP y cookies no seguras permitidos.
- `staging`: validación previa a producción, URLs públicas HTTPS y cookies seguras obligatorias.
- `production`: tráfico real, URLs públicas HTTPS y cookies seguras obligatorias.

Existe además el perfil interno `test`, reservado a pruebas automatizadas.

## Preparación local

Desde la raíz:

```powershell
Copy-Item .env.local.example .env.local
npm install
npm run dev
```

Los ficheros `.env.local`, `.env.staging` y cualquier otra variante con valores reales están ignorados por Git. Solo se versionan las plantillas `*.example`.

## Responsabilidad de las variables

| Variable                                           | Consumidor       | Pública | Obligatoria ahora          |
| -------------------------------------------------- | ---------------- | ------- | -------------------------- |
| `RESERLY_ENVIRONMENT`                              | API              | No      | Sí                         |
| `RESERLY_PUBLIC_BASE_URL`                          | API              | No      | Sí                         |
| `RESERLY_WEB_BASE_URL`                             | API              | No      | Sí                         |
| `RESERLY_ALLOWED_ORIGINS`                          | API              | No      | Sí                         |
| `RESERLY_SECURE_COOKIES`                           | API              | No      | Sí                         |
| `RESERLY_REAL_PAYMENTS_ENABLED`                    | API              | No      | Sí; debe ser `false`       |
| `RESERLY_DATABASE_NAME`                            | Docker Compose   | No      | Sí                         |
| `RESERLY_DATABASE_PORT`                            | Docker Compose   | No      | Sí                         |
| `RESERLY_DATABASE_URL`                             | API              | No      | Sí                         |
| `RESERLY_DATABASE_USERNAME`                        | API y Compose    | No      | Sí                         |
| `RESERLY_DATABASE_PASSWORD`                        | API y Compose    | No      | Sí                         |
| `RESERLY_REDIS_PORT`                               | Docker Compose   | No      | Sí                         |
| `RESERLY_REDIS_PASSWORD`                           | Docker Compose   | No      | Sí                         |
| `RESERLY_REDIS_URL`                                | API              | No      | Sí                         |
| `RESERLY_RABBITMQ_PORT`                            | Docker Compose   | No      | Sí                         |
| `RESERLY_RABBITMQ_MANAGEMENT_PORT`                 | Docker Compose   | No      | Sí                         |
| `RESERLY_RABBITMQ_USERNAME`                        | Docker Compose   | No      | Sí                         |
| `RESERLY_RABBITMQ_PASSWORD`                        | Docker Compose   | No      | Sí                         |
| `RESERLY_RABBITMQ_URL`                             | API              | No      | Sí                         |
| `RESERLY_BUSINESS_VERIFICATION_CONNECT_TIMEOUT`    | API              | No      | Sí; límite de conexión     |
| `RESERLY_BUSINESS_VERIFICATION_READ_TIMEOUT`       | API              | No      | Sí; límite de lectura      |
| `RESERLY_BUSINESS_VERIFICATION_MAX_ATTEMPTS`       | API              | No      | Sí; entre 1 y 5            |
| `RESERLY_BUSINESS_VERIFICATION_INITIAL_BACKOFF`    | API              | No      | Sí; espera inicial         |
| `RESERLY_BUSINESS_VERIFICATION_MAX_BACKOFF`        | API              | No      | Sí; tope de espera         |
| `RESERLY_BUSINESS_VERIFICATION_BACKOFF_MULTIPLIER` | API              | No      | Sí; entre 1 y 4            |
| `NEXT_PUBLIC_APP_ENV`                              | Web y navegador  | Sí      | Sí                         |
| `NEXT_PUBLIC_API_BASE_URL`                         | Web y navegador  | Sí      | Sí                         |
| `RESERLY_API_INTERNAL_URL`                         | Servidor Next.js | No      | No; fallback a URL pública |

Las variables de PostgreSQL, Redis y RabbitMQ ya son consumidas por Spring Boot y Docker Compose. S3 permanece como contrato reservado hasta las tareas de archivos.

## Reglas de seguridad

- Una variable `NEXT_PUBLIC_*` se considera pública y puede acabar en JavaScript del navegador. Nunca debe contener secretos.
- Staging y producción deben usar HTTPS en todas las URLs públicas.
- Los secretos reales se inyectan desde el entorno de despliegue o un gestor de secretos.
- Producción no debe cargar un fichero `.env.production` desde el repositorio o la imagen.
- Los pagos reales permanecen desactivados hasta completar y aprobar la integración RedSys.
- Los certificados y claves privadas de AEAT nunca se guardan en estos ficheros.
- Las URLs de Redis y RabbitMQ contienen credenciales y deben tratarse como secretos.
- Redis y RabbitMQ deben usar TLS en staging y producción cuando el proveedor no garantice una red privada equivalente.

## Perfiles Spring

- `application-local.yaml` ofrece valores seguros para desarrollo.
- `application-staging.yaml` obliga a inyectar URLs y orígenes.
- `application-production.yaml` obliga a inyectar URLs y orígenes.
- `application-test.yaml` contiene únicamente valores aislados para tests.

El fichero `application.yaml` define el contrato común. Si faltan variables o una política es insegura, Spring falla durante el arranque.

## Build y despliegue web

Next.js valida las variables durante `next dev` y `next build`. El build de staging o producción debe recibir las variables del entorno de despliegue:

```powershell
$env:NEXT_PUBLIC_APP_ENV = "production"
$env:NEXT_PUBLIC_API_BASE_URL = "https://api.reserly.example"
$env:RESERLY_API_INTERNAL_URL = "http://reserly-api:8080"
npm run build:web
```

`npm run verify` usa valores de test explícitos y no depende de secretos ni de ficheros locales.

`npm run env:check` comprueba que las tres plantillas mantienen las mismas variables, que staging y producción usan HTTPS y cookies seguras, que los pagos reales siguen desactivados y que ninguna clave potencialmente secreta usa el prefijo público de Next.js.

## Caché y mensajería

La API consume:

- `RESERLY_REDIS_URL` mediante Spring Data Redis y Lettuce.
- `RESERLY_RABBITMQ_URL` mediante Spring AMQP.
- `RESERLY_CACHE_DEFAULT_TTL`, opcional, con valor predeterminado de cinco minutos.
- Timeouts opcionales `RESERLY_REDIS_CONNECT_TIMEOUT`, `RESERLY_REDIS_COMMAND_TIMEOUT`, `RESERLY_RABBITMQ_CONNECTION_TIMEOUT` y `RESERLY_RABBITMQ_REQUESTED_HEARTBEAT`.

La URI AMQP local no incluye un path de vhost; el cliente usa `/` por defecto. No debe añadirse `/%2f`, porque Spring Boot 4.1 lo interpreta como el nombre literal `%2f`.

## Verificación empresarial remota

El gateway configura por entorno:

- timeout de conexión de 2 segundos;
- timeout de lectura de 5 segundos;
- máximo de 3 intentos;
- backoff inicial de 250 milisegundos;
- backoff máximo de 2 segundos;
- multiplicador 2.

Los valores son límites operativos, no credenciales. Cada adaptador concreto deberá aplicar los
timeouts en su cliente y el gateway añade un watchdog total de conexión más lectura. URLs,
certificados y secretos de VIES, AEAT u otros proveedores se incorporarán solo con el adaptador de
la tarea correspondiente y nunca usarán variables `NEXT_PUBLIC_*`.
