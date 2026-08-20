package com.reserly.platform.demand.experiment;

/** Puerto transaccional para asignar y registrar la exposición de políticas A/B. */
public interface ExperimentAssignmentService {

  /**
   * Resuelve una variante determinista. Reintentar con la misma definición y unidad devuelve la
   * asignación persistida y nunca vuelve a sortear.
   */
  ExperimentAssignmentResult assign(ExperimentAssignmentCommand command);

  /**
   * Vincula idempotentemente la asignación y la recomendación antes de mostrarla. Rechaza cualquier
   * divergencia entre experimento, variante o política.
   */
  ExperimentAssignmentResult registerExposure(ExperimentExposureCommand command);
}
