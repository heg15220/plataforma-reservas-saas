# Rate limiting de operaciones sensibles

## Alcance

La tarea `1.16` protege las operaciones que hoy pueden recibir tráfico anónimo o provocar trabajo
costoso:

| Operación                        | Discriminador              | Cuota inicial      |
| -------------------------------- | -------------------------- | ------------------ |
| `POST /api/auth/login`           | Dirección remota           | 10 cada 5 minutos  |
| `POST /api/auth/venues/register` | Dirección remota           | 5 cada hora        |
| `POST /api/auth/password/forgot` | Dirección remota           | 5 cada 15 minutos  |
| `POST /api/auth/password/reset`  | Dirección remota           | 10 cada 15 minutos |
| Verificación empresarial remota  | UUID de cuenta empresarial | 5 cada hora        |

Cada operación tiene contador y ventana independientes. Un resultado empresarial ya persistido que
se consulta de nuevo con el mismo `requestId` conserva su semántica idempotente y no consume otra
unidad.

## Algoritmo y atomicidad

`RateLimitServiceImpl` usa una ventana fija distribuida. Un script Lua ejecuta `INCR`, asigna
`PEXPIRE` solo al primer incremento y devuelve contador y TTL en una única operación atómica. Esto
evita carreras entre instancias y claves sin caducidad si dos peticiones inauguran una ventana a la
vez.

Las claves siguen el contrato:

```text
reserly:rate-limit:v1:<operación>:<sha256-discriminador>
```

No se persisten direcciones, emails ni identificadores de cuenta en claro. La versión permite
cambiar algoritmo o semántica sin interpretar contadores antiguos.

## Contrato HTTP

Una cuota agotada devuelve:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: <segundos>
Content-Type: application/json

{"error":"RATE_LIMIT_EXCEEDED"}
```

La respuesta no revela la cuota, la clave ni si una identidad existe. Si Redis no puede evaluar la
protección, el flujo falla cerrado con `503` y `RATE_LIMIT_UNAVAILABLE`; no se continúa
silenciosamente sin barrera.

## Dirección de cliente y proxies

El interceptor usa `HttpServletRequest.getRemoteAddr()` y no confía directamente en
`X-Forwarded-For`, porque un cliente podría falsificarlo. El proxy o ingress de producción debe
eliminar cabeceras aportadas por Internet, establecer la dirección verificada y normalizarla antes
de llegar a la aplicación. Esta configuración de borde debe probarse en el despliegue.

## Configuración y operación

Las cuotas y ventanas se externalizan con variables `RESERLY_RATE_LIMIT_*`. Staging y producción
mantienen `RESERLY_RATE_LIMIT_ENABLED=true`; la desactivación existe exclusivamente para suites que
prueban otros casos de uso sin Redis. Las ventanas admitidas van de un segundo a 24 horas y los
límites de 1 a 10.000.

La métrica de rechazos y alertas agregadas se incorporará con la observabilidad de la Fase 17. No
deben registrarse discriminadores, hashes de clave completos ni payloads de autenticación.
