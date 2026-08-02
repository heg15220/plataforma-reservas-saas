package com.reserly.platform.venues.dto;

import java.util.List;

/** Colección ordenada de asociaciones de email pertenecientes al propietario autenticado. */
public record VenueEmailAssignmentsResponse(List<VenueEmailAssignmentResponse> assignments) {}
