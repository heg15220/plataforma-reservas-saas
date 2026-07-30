package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Decisión manual cerrada sobre una identidad empresarial pendiente. */
public record AdminBusinessDecisionRequest(
    @NotBlank @Pattern(regexp = "approved|rejected") String decision,
    @NotBlank @Size(max = 1000) String reason) {}
