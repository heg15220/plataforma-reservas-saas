package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Decisión administrativa acotada a confirmar o desestimar una incidencia. */
public record AdminIncidentReviewRequest(
    @NotBlank @Pattern(regexp = "confirmed|dismissed") String status,
    @NotBlank @Size(max = 500) String reason) {}
