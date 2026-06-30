/**
 * Casos de uso y contratos de servicio del contexto de identidad.
 *
 * <p>Las contraseñas solo se procesan mediante {@link
 * com.reserly.platform.identity.service.PasswordHashingService}; ningún caso de uso debe crear o
 * comparar hashes directamente. Los desafíos de email se generan mediante {@link
 * com.reserly.platform.identity.service.OneTimeTokenService}; verificación y recuperación los
 * consumen transaccionalmente con propósitos separados.
 */
package com.reserly.platform.identity.service;
