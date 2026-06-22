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

| Variable                        | Consumidor       | Pública | Obligatoria ahora          |
| ------------------------------- | ---------------- | ------- | -------------------------- |
| `RESERLY_ENVIRONMENT`           | API              | No      | Sí                         |
| `RESERLY_PUBLIC_BASE_URL`       | API              | No      | Sí                         |
| `RESERLY_WEB_BASE_URL`          | API              | No      | Sí                         |
| `RESERLY_ALLOWED_ORIGINS`       | API              | No      | Sí                         |
| `RESERLY_SECURE_COOKIES`        | API              | No      | Sí                         |
| `RESERLY_REAL_PAYMENTS_ENABLED` | API              | No      | Sí; debe ser `false`       |
| `NEXT_PUBLIC_APP_ENV`           | Web y navegador  | Sí      | Sí                         |
| `NEXT_PUBLIC_API_BASE_URL`      | Web y navegador  | Sí      | Sí                         |
| `RESERLY_API_INTERNAL_URL`      | Servidor Next.js | No      | No; fallback a URL pública |

Las variables de PostgreSQL, Redis, RabbitMQ y S3 aparecen en las plantillas como contrato reservado, pero no se consumen hasta las tareas `0.5` y `0.6`.

## Reglas de seguridad

- Una variable `NEXT_PUBLIC_*` se considera pública y puede acabar en JavaScript del navegador. Nunca debe contener secretos.
- Staging y producción deben usar HTTPS en todas las URLs públicas.
- Los secretos reales se inyectan desde el entorno de despliegue o un gestor de secretos.
- Producción no debe cargar un fichero `.env.production` desde el repositorio o la imagen.
- Los pagos reales permanecen desactivados hasta completar y aprobar la integración RedSys.
- Los certificados y claves privadas de AEAT nunca se guardan en estos ficheros.

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
