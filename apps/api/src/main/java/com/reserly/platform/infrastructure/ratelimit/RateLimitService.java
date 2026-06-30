package com.reserly.platform.infrastructure.ratelimit;

/**
 * Puerta distribuida para operaciones sensibles.
 *
 * <p>El discriminador puede ser una dirección de red o un identificador interno; la implementación
 * debe transformarlo antes de persistirlo y nunca registrarlo.
 */
public interface RateLimitService {

  /**
   * Consume una unidad de la cuota o interrumpe la operación.
   *
   * @throws RateLimitExceededException cuando la ventana ya agotó su máximo
   * @throws RateLimitUnavailableException cuando no puede aplicarse la protección
   */
  void check(RateLimitScope scope, String discriminator);
}
