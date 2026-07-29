package com.reserly.platform.billing;

import java.util.Arrays;

/**
 * Resultado persistido de un intento de pago.
 *
 * <p>El catálogo refleja el contrato RedSys preparado en diseño, aunque el proveedor real continúa
 * desactivado. Solo {@link #CONFIRMED} puede tener fecha de pago.
 */
public enum PaymentStatus {
  CONFIRMED("confirmed"),
  REJECTED("rejected"),
  CANCELLED_BY_USER("cancelled_by_user"),
  COMMUNICATION_ERROR("communication_error"),
  PENDING_CONFIRMATION("pending_confirmation");

  private final String persistedValue;

  PaymentStatus(String persistedValue) {
    this.persistedValue = persistedValue;
  }

  /** Valor canónico persistido. */
  public String persistedValue() {
    return persistedValue;
  }

  /** Resuelve de forma estricta un resultado procedente de persistencia. */
  public static PaymentStatus fromPersistedValue(String value) {
    return Arrays.stream(values())
        .filter(status -> status.persistedValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported payment status"));
  }
}
