package com.reserly.platform.incidents.service;

import java.time.Duration;

/** Política pura que traduce el contador operativo a una duración temporal de restricción. */
public interface PenaltyCalculationPolicy {

  /**
   * Calcula 7, 14, 21 o 60 días para el primer, segundo, tercer o sucesivos reportes.
   *
   * @throws IllegalArgumentException si el contador no es positivo
   */
  Duration durationFor(int incidentCountOperational);
}
