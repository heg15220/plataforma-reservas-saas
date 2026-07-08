package com.reserly.platform.venues.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Payload privado para crear o editar una pestaña personalizada del local autenticado. */
public record VenueCustomTabRequest(
    @Valid @NotNull VenueCustomTabLocalizedTextDto titleI18n,
    @Valid @NotNull VenueCustomTabLocalizedTextDto contentI18n,
    boolean active) {}
