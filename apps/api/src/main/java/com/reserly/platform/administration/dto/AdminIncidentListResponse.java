package com.reserly.platform.administration.dto;

import java.util.List;

/** Cola administrativa acotada de incidencias recientes. */
public record AdminIncidentListResponse(List<AdminIncidentResponse> incidents) {}
