# Política de consentimiento del motor de demanda

Versión: `demand-consent.v1` — efectiva desde 2026-08-13.

La reserva, consulta de disponibilidad, gestión de una reserva y mensajes transaccionales son
operaciones necesarias para prestar el servicio. No dependen de aceptar ninguna finalidad opcional.

Las decisiones opcionales se solicitan separadas, desactivadas inicialmente y pueden cambiarse en
cualquier momento:

- **Analítica:** medir de forma minimizada cómo funcionan búsqueda, fichas y reserva.
- **Personalización:** utilizar actividad consentida para ordenar resultados conforme a intereses.
- **Activación comercial:** permitir promociones u ofertas comerciales; nunca incluye mensajes
  transaccionales de una reserva solicitada.

Rechazar o revocar no empeora la operación de reserva. Sin personalización se usan contexto actual,
reglas y agregados permitidos. Sin analítica, el navegador no emite eventos opcionales. Sin
activación comercial no se generan contactos promocionales. Una nueva versión material exige pedir
decisión otra vez. El almacenamiento web conserva solo tres booleanos, versión y fecha; no crea
identidad, cookie ni fingerprint. Los derechos y la propagación backend se implementan en 19.17.
