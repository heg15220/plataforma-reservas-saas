package com.reserly.platform.demand.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Impugnación codificada por el local afectado, sin texto libre o datos personales. */
public record DemandHumanReviewAppealRequest(
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{0,63}$") String reasonCode) {}
