package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Función de plan con código estable y traducciones completas. */
public record AdminPlanFeature(
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$") String code,
    @NotBlank @Size(max = 160) String labelEs,
    @NotBlank @Size(max = 160) String labelEn) {}
