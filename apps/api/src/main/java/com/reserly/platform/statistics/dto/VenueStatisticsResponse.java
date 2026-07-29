package com.reserly.platform.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Métricas privadas agregadas y serie diaria del periodo.
 *
 * <p>No contiene IDs, emails, reservas ni comentarios. La media del rango está ponderada por el
 * número diario de reseñas.
 */
public record VenueStatisticsResponse(
    String period,
    LocalDate fromDate,
    LocalDate toDate,
    long reservationsCount,
    long confirmedCount,
    long cancelledCount,
    long noShowCount,
    long attendedCount,
    long occupiedCapacity,
    long availableCapacity,
    BigDecimal occupancyRate,
    long reviewsCount,
    BigDecimal averageRating,
    List<VenueStatisticsDailyResponse> series) {

  /** Impide que el llamador pueda mutar la evolución después de construir la respuesta. */
  public VenueStatisticsResponse {
    series = List.copyOf(series);
  }
}
