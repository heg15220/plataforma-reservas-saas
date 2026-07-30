package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Resultado documental y motivo obligatorio de la decisión administrativa. */
public record AdminDocumentReviewRequest(
    @NotBlank @Pattern(regexp = "accepted|rejected|needs_correction") String decision,
    @NotBlank @Size(max = 2000) String reason) {}
