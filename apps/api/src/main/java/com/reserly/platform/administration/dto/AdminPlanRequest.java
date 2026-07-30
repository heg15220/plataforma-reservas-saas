package com.reserly.platform.administration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Contrato completo de creación/edición de un plan SaaS localizado. */
public record AdminPlanRequest(
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$") String slug,
    @NotBlank @Size(max = 120) String nameEs,
    @NotBlank @Size(max = 120) String nameEn,
    @NotNull @DecimalMin("0.00") BigDecimal priceMonthly,
    @NotNull @DecimalMin("0.00") BigDecimal priceYearly,
    @NotNull @Valid AdminPlanLimits limits,
    @NotNull @Size(max = 50) List<@Valid AdminPlanFeature> features,
    boolean active) {
  public AdminPlanRequest {
    features = features == null ? null : List.copyOf(features);
  }
}
