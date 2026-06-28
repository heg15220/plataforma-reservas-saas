package com.reserly.platform.identity.service;

import com.reserly.platform.identity.dto.VenueRegistrationCommand;
import com.reserly.platform.identity.dto.VenueRegistrationResponse;

/**
 * Caso de uso transaccional para registrar una cuenta empresarial de local.
 *
 * <p>El servicio fija tipo, rol y estados; el cliente no puede elegir privilegios. Crea usuario,
 * identidad empresarial y asignación {@code venue_owner} de forma atómica. No crea aún perfil de
 * local, token de email ni comprobación remota.
 */
public interface VenueRegistrationService {

  /**
   * Registra una identidad empresarial pendiente.
   *
   * @throws RegistrationConflictException si email o identidad fiscal ya existen
   * @throws RegistrationValidationException si el secreto supera el límite seguro de BCrypt
   */
  VenueRegistrationResponse register(VenueRegistrationCommand command);
}
