package com.reserly.platform.venues.dto;

/**
 * Error seguro y accionable para una descripción que excede el límite.
 *
 * @param error código estable del error
 * @param locale idioma que debe acortarse
 * @param maxWords máximo permitido
 * @param actualWords palabras detectadas
 */
public record VenueDescriptionLimitErrorResponse(
    String error, String locale, int maxWords, int actualWords) {}
