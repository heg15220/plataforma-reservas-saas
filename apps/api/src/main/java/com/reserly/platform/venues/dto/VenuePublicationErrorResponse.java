package com.reserly.platform.venues.dto;

import java.util.List;

/** Rechazo accionable sin identidad, fiscalidad ni evidencia empresarial. */
public record VenuePublicationErrorResponse(String error, List<String> requirements) {}
