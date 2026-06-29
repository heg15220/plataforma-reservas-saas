package com.reserly.platform.businessverification.service;

import java.util.Locale;

/** Motivos cerrados por los que la automatización exige respaldo documental. */
public enum BusinessVerificationDocumentRequestReason {
  NO_AUTOMATED_CHANNEL,
  PROVIDER_UNAVAILABLE,
  INSUFFICIENT_PROVIDER_DATA,
  LEGAL_NAME_UNCONFIRMED,
  ADDRESS_UNCONFIRMED;

  /** Valor estable para persistencia y auditoría. */
  public String persistedValue() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Clave controlada para los catálogos visibles que se implementarán en `1.21`. */
  public String messageKey() {
    return "businessVerification.documents.reason." + persistedValue();
  }
}
