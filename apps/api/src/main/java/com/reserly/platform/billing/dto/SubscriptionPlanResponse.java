package com.reserly.platform.billing.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plan apto para presentación privada.
 *
 * @param slug identidad semántica estable
 * @param name nombre localizado
 * @param priceMonthly precio mensual en EUR
 * @param priceYearly precio anual en EUR
 * @param limits límites conocidos
 * @param features funciones localizadas
 */
public record SubscriptionPlanResponse(
    String slug,
    String name,
    BigDecimal priceMonthly,
    BigDecimal priceYearly,
    PlanLimitsResponse limits,
    List<PlanFeatureResponse> features) {

  /** Conserva una lista inmutable después de convertir el catálogo persistido. */
  public SubscriptionPlanResponse {
    features = List.copyOf(features);
  }
}
