package com.reserly.platform.demand.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Decisión administrativa cerrada; una corrección identifica siempre la nueva versión. */
public record DemandHumanReviewDecisionRequest(
    @NotBlank @Pattern(regexp = "approved|rejected|correction_requested|corrected") String status,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{0,63}$") String reasonCode,
    @Pattern(regexp = "^[a-z0-9][A-Za-z0-9._-]{0,63}$") String correctionVersion) {}
