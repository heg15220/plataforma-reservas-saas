package com.reserly.platform.businessverification.service;

import java.util.Locale;

/** Estados cerrados de la identidad empresarial y sus valores persistidos. */
public enum BusinessVerificationStatus {
  UNVERIFIED,
  PENDING_REMOTE_CHECK,
  VERIFIED,
  PENDING_REVIEW,
  REJECTED,
  EXPIRED;

  /** Valor estable usado por PostgreSQL y contratos internos. */
  public String persistedValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Convierte un valor persistido validado por el esquema. */
  public static BusinessVerificationStatus fromPersistedValue(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
