package com.reserly.platform.services.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyeccion privada de un servicio sin exponer IDs de propietario ni local. */
public record ServiceResponse(
    UUID id,
    String name,
    ServiceLocalizedTextDto nameI18n,
    String description,
    ServiceLocalizedTextDto descriptionI18n,
    int durationMinutes,
    int capacityRequired,
    boolean active,
    boolean allowsAnyAvailableResource,
    String bookingMode,
    List<UUID> employeeResourceIds,
    Instant createdAt,
    Instant updatedAt) {}
