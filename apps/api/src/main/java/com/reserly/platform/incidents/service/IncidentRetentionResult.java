package com.reserly.platform.incidents.service;

/** Resultado agregado y sin datos personales de una ejecución de conservación. */
public record IncidentRetentionResult(
    int incidentsAnonymized, int penaltiesAnonymized, int penaltiesDeleted, int incidentsDeleted) {

  /** Indica si el ciclo produjo alguna mutación auditable. */
  public boolean changed() {
    return incidentsAnonymized + penaltiesAnonymized + penaltiesDeleted + incidentsDeleted > 0;
  }
}
