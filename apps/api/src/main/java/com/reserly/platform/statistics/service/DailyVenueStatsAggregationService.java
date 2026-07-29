package com.reserly.platform.statistics.service;

import java.time.LocalDate;

/** Caso de uso idempotente para regenerar las métricas de una fecha de negocio. */
public interface DailyVenueStatsAggregationService {

  /**
   * Recalcula todos los locales para la fecha indicada.
   *
   * @return número de instantáneas insertadas o actualizadas
   * @throws IllegalArgumentException si la fecha es nula o futura
   */
  int aggregate(LocalDate date);
}
