package com.reserly.platform.services.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload privado para crear o editar un servicio del local autenticado. */
public record ServiceRequest(
    @NotBlank @Size(max = 160) String name,
    @Valid ServiceLocalizedTextDto nameI18n,
    @Size(max = 2000) String description,
    @Valid ServiceLocalizedTextDto descriptionI18n,
    @Min(1) @Max(1440) int durationMinutes,
    @Min(1) int capacityRequired,
    boolean active,
    Boolean allowsAnyAvailableResource) {}
