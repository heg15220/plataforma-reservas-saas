package com.reserly.platform.infrastructure.ratelimit;

/**
 * Indica que la cuota distribuida no se pudo evaluar.
 *
 * <p>Los flujos protegidos fallan cerrados: continuar sin Redis convertiría una incidencia
 * operativa en una omisión silenciosa de seguridad.
 */
public class RateLimitUnavailableException extends RuntimeException {

  public RateLimitUnavailableException(Throwable cause) {
    super("Rate limiting is unavailable", cause);
  }
}
