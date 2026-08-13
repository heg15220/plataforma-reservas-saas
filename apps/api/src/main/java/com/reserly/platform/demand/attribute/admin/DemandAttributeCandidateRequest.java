package com.reserly.platform.demand.attribute.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Entrada administrativa minimizada para registrar un descubrimiento sin texto personal bruto. */
public record DemandAttributeCandidateRequest(
    @NotBlank @Pattern(regexp = "^[a-z][A-Za-z0-9]{1,95}$") String proposedCode,
    @NotBlank @Size(max = 128) String clusterKey,
    @NotBlank @Pattern(regexp = "ambience|space|experience|offer|operation|accessibility")
        String family,
    @NotBlank @Pattern(regexp = "stable|dynamic|relative|subjectiveAggregate") String attributeType,
    @NotBlank @Size(max = 160) String nameEs,
    @NotBlank @Size(max = 160) String nameEn,
    @NotBlank @Size(max = 1000) String definitionEs,
    @NotBlank @Size(max = 1000) String definitionEn,
    @NotEmpty @Size(max = 6) List<@NotBlank String> allowedSources,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 160) String> exampleSummaries) {}
