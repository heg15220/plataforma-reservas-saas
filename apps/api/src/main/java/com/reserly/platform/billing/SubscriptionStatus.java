package com.reserly.platform.billing;

import java.util.Arrays;

/**
 * Estado canónico de una suscripción SaaS.
 *
 * <p>Los valores persistidos forman parte del contrato SQL. La transición efectiva se implementará
 * en el servicio transaccional de pagos; este catálogo evita cadenas libres y divergencias entre
 * persistencia, dominio y futuras respuestas.
 */
public enum SubscriptionStatus {
  /** Periodo promocional temporal, que requiere una fecha de finalización. */
  TRIAL("trial"),

  /** Suscripción con acceso vigente. */
  ACTIVE("active"),

  /** Renovación o alta a la espera de una confirmación de pago. */
  PENDING_PAYMENT("pending_payment"),

  /** Acceso detenido por una decisión operativa o un pago no resuelto. */
  SUSPENDED("suspended"),

  /** Estado terminal con fecha de cancelación obligatoria. */
  CANCELLED("cancelled");

  private final String persistedValue;

  SubscriptionStatus(String persistedValue) {
    this.persistedValue = persistedValue;
  }

  /** Valor estable almacenado en PostgreSQL y usado por los contratos externos. */
  public String persistedValue() {
    return persistedValue;
  }

  /**
   * Resuelve exclusivamente valores del esquema vigente.
   *
   * @throws IllegalArgumentException si aplicación y base de datos no comparten catálogo
   */
  public static SubscriptionStatus fromPersistedValue(String value) {
    return Arrays.stream(values())
        .filter(status -> status.persistedValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported subscription status"));
  }
}
