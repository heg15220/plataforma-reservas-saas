# Latencia o error budget del Demand Engine

Owner: `demand-platform`. Congelar rollout al 50 % de consumo y cambios al 100 %.

1. Separar error/latencia por ruta normalizada y etapa, nunca por UUID o cliente.
2. Reducir canary; si p95 >250 ms o errores >1 % persisten, activar fallback.
3. Comprobar pool, timeouts, cache y dependencia; no relajar privacidad, capacidad o elegibilidad.
4. Revertir versión reciente y validar p95/p99/error durante dos ventanas de 10 min.
5. Escalar si el budget sigue quemándose y auditar versión, causa y acción.
