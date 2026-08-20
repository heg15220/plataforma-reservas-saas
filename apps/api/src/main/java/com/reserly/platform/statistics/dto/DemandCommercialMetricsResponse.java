package com.reserly.platform.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agregado privado de atribución con cobertura, calidad y definiciones explícitas.
 *
 * <p>Con muestra insuficiente las métricas comerciales son nulas; solo permanecen denominadores
 * propios necesarios para explicar cobertura sin extrapolar.
 */
public record DemandCommercialMetricsResponse(
    String status,
    String policyVersion,
    String definitionsVersion,
    String timeZone,
    int minimumSampleSize,
    long eligibleReservations,
    long classifiedReservations,
    BigDecimal coveragePercent,
    Long newCustomers,
    Long originatedReservations,
    Long offPeakCovered,
    BigDecimal attributedIncome,
    String attributedCurrency,
    String incomeStatus,
    Long directReservations,
    Long assistedReservations,
    Long generatedReservations,
    Long recoveredReservations,
    List<DemandMetricDefinitionResponse> definitions) {

  public DemandCommercialMetricsResponse {
    definitions = List.copyOf(definitions);
  }
}
