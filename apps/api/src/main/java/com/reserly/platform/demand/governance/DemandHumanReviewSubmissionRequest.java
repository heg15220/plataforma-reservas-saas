package com.reserly.platform.demand.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** Solicitud MLOps idempotente, minimizada y sin autoridad de ejecución. */
public record DemandHumanReviewSubmissionRequest(
    @NotNull UUID reviewId,
    @NotBlank @Pattern(regexp = "attribute|commercial_decision") String reviewType,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{0,47}$") String subjectType,
    @NotBlank @Pattern(regexp = "^[a-z][A-Za-z0-9._:-]{0,127}$") String subjectKey,
    @NotBlank @Pattern(regexp = "^[a-z0-9][A-Za-z0-9._-]{0,63}$") String subjectVersion,
    UUID venueId,
    @NotBlank @Pattern(regexp = "^[a-z][A-Za-z0-9._-]{0,63}$") String policyVersion,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{0,63}$") String explanationCode,
    @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String evidenceSha256) {}
