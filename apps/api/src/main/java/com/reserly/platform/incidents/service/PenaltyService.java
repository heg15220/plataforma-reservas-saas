package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.incidents.persistence.PenaltyEntity;

/** Casos de uso para aplicar restricciones y proteger una confirmación pública. */
public interface PenaltyService {

  /**
   * Crea o recalcula la restricción global causada por una incidencia ya persistida.
   *
   * <p>Debe invocarse dentro de la transacción del reporte para conservar atomicidad.
   */
  PenaltyEntity applyFor(NoShowIncidentEntity incident);

  /**
   * Serializa la identidad y rechaza la confirmación si existe una restricción global vigente.
   *
   * @param customerEmailNormalized email canónico validado por el flujo de reserva
   */
  void requireBookingAllowed(String customerEmailNormalized);
}
