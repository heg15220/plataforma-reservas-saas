package com.reserly.platform.businessverification.service;

import java.util.Locale;

/** Tipos de respaldo admitidos por la verificación empresarial. */
public enum BusinessVerificationDocumentType {
  CENSUS_REGISTRATION_036_037,
  CENSUS_CERTIFICATE,
  ACTIVITY_OR_OPENING_LICENSE,
  EQUIVALENT_ADMINISTRATIVE_DOCUMENT,
  OTHER;

  /** Valor estable persistido y compartido con la futura carga privada. */
  public String persistedValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
