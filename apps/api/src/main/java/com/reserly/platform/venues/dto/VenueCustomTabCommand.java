package com.reserly.platform.venues.dto;

import com.reserly.platform.localization.LocalizedText;

/** Comando interno del caso de uso; la propiedad se toma siempre del principal autenticado. */
public record VenueCustomTabCommand(
    LocalizedText titleI18n, LocalizedText contentI18n, boolean active) {}
