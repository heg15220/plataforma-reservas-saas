package com.reserly.platform.services.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Reemplazo completo de recursos compatibles con un servicio propio. */
public record ServiceResourceAssignmentRequest(@NotNull @Size(max = 100) Set<UUID> resourceIds) {}
