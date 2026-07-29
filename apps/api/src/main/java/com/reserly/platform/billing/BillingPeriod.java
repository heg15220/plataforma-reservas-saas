package com.reserly.platform.billing;

import java.util.Arrays;

/** Periodicidad comercial seleccionada para una suscripción. */
public enum BillingPeriod {
  MONTHLY("monthly"),
  YEARLY("yearly");

  private final String persistedValue;

  BillingPeriod(String persistedValue) {
    this.persistedValue = persistedValue;
  }

  /** Valor canónico persistido. */
  public String persistedValue() {
    return persistedValue;
  }

  /** Resuelve de forma estricta la periodicidad almacenada. */
  public static BillingPeriod fromPersistedValue(String value) {
    return Arrays.stream(values())
        .filter(period -> period.persistedValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported billing period"));
  }
}
