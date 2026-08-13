package com.reserly.platform.infrastructure.ratelimit;

/**
 * Operaciones sensibles con cuota independiente.
 *
 * <p>El segmento estable forma parte de la clave Redis y evita que una ráfaga en un flujo agote la
 * cuota de otro.
 */
public enum RateLimitScope {
  LOGIN("login"),
  REGISTRATION("registration"),
  PASSWORD_RESET_REQUEST("password-reset-request"),
  PASSWORD_RESET_CONSUME("password-reset-consume"),
  RESERVATION("reservation"),
  PUBLIC_LINK("public-link"),
  REVIEW("review"),
  BUSINESS_VERIFICATION("business-verification"),
  DEMAND_EVENT_INGESTION("demand-event-ingestion");

  private final String keySegment;

  RateLimitScope(String keySegment) {
    this.keySegment = keySegment;
  }

  public String keySegment() {
    return keySegment;
  }
}
