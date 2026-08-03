package com.reserly.platform.venues.dto;

import java.util.List;

/** Colección privada de perfiles y capacidad efectiva de alta del titular autenticado. */
public record VenueProfilesResponse(
    List<VenueProfileResponse> profiles, boolean canCreateAdditionalVenue) {}
