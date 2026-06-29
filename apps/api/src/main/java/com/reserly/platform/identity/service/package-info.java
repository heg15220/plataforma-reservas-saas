/**
 * Casos de uso y contratos de servicio del contexto de identidad.
 *
 * <p>Las contraseñas solo se procesan mediante {@link
 * com.reserly.platform.identity.service.PasswordHashingService}; ningún caso de uso debe crear o
 * comparar hashes directamente. Los desafíos de email se generan mediante {@link
 * com.reserly.platform.identity.service.OneTimeTokenService} y se consumen transaccionalmente.
 */
package com.reserly.platform.identity.service;
