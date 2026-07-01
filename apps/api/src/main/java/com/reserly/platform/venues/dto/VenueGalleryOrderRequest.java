package com.reserly.platform.venues.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Orden completo y sin posiciones elegidas directamente por el cliente. */
public record VenueGalleryOrderRequest(@NotEmpty @Size(max = 8) List<UUID> imageIds) {}
