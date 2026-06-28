package com.reserly.platform.businessverification.remote;

/** Resultados técnicos que un proveedor remoto puede devolver sin cambiar el estado agregado. */
public enum RemoteVerificationStatus {
  VERIFIED("verified"),
  INVALID("invalid"),
  INCONCLUSIVE("inconclusive");

  private final String persistedValue;

  RemoteVerificationStatus(String persistedValue) {
    this.persistedValue = persistedValue;
  }

  /** Valor permitido por la tabla de historial de comprobaciones. */
  public String persistedValue() {
    return persistedValue;
  }
}
