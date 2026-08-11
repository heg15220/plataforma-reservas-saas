package com.reserly.platform.incidents.service;

/** Aplica ventanas de uso operativo y eliminación de evidencia a incidencias y penalizaciones. */
public interface IncidentRetentionService {

  /** Ejecuta un ciclo idempotente con una única frontera temporal. */
  IncidentRetentionResult enforceRetention();
}
