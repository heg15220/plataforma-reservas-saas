/**
 * Transporte asíncrono del contexto de identidad.
 *
 * <p>Los mensajes de verificación contienen un secreto efímero destinado exclusivamente al enlace
 * enviado al titular. Nunca deben aparecer en logs, métricas o respuestas HTTP.
 */
package com.reserly.platform.identity.messaging;
