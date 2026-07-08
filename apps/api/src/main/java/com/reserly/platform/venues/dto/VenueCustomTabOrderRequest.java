package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Orden completo esperado para reordenar pestañas de forma atómica y sin posiciones parciales. */
public record VenueCustomTabOrderRequest(@NotNull @Size(max = 16) List<@NotNull UUID> tabIds) {}
