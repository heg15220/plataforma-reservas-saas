package com.reserly.platform.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Plan completo para administración, siempre con ES y EN explícitos. */
public record AdminPlanResponse(
    UUID id,
    String slug,
    String nameEs,
    String nameEn,
    BigDecimal priceMonthly,
    BigDecimal priceYearly,
    AdminPlanLimits limits,
    List<AdminPlanFeature> features,
    boolean active,
    Instant updatedAt) {
  public AdminPlanResponse {
    features = List.copyOf(features);
  }
}
