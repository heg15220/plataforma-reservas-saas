/**
 * Contratos y adaptadores HTTP del contexto de identidad.
 *
 * <p>Los secretos de sesión se transportan solo mediante cookies construidas por {@link
 * com.reserly.platform.identity.controller.SessionCookieFactory}. Los secretos de verificación se
 * aceptan exclusivamente en el cuerpo validado de su endpoint y nunca forman parte de la respuesta.
 */
package com.reserly.platform.identity.controller;
