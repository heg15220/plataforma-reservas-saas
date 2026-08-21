# Presupuesto de coste o saturación de capacidad

Owner: `demand-platform-finops`. Los controles de calidad y seguridad nunca se desactivan por coste.

1. Confirmar ratio mensual y saturación por clase allowlisted, sin labels de tenant o sujeto.
2. Congelar training no esencial y rollout; conservar inference determinista y obligaciones de borrado.
3. Reducir concurrencia batch, revisar loops/reintentos y aplicar cache solo con invalidación segura.
4. Escalar capacidad únicamente con aprobación humana y load test que cumpla p95/error/privacidad.
5. Verificar coste unitario, saturación <80 % y backlog/freshness antes de cerrar.
