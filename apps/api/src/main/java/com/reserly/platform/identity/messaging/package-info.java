/**
 * Transporte asíncrono del contexto de identidad.
 *
 * <p>Los mensajes de verificación y recuperación contienen secretos efímeros destinados
 * exclusivamente a enlaces enviados al titular. Nunca deben aparecer en logs, métricas o respuestas
 * HTTP.
 */
package com.reserly.platform.identity.messaging;
