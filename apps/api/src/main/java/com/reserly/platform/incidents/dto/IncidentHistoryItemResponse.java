package com.reserly.platform.incidents.dto;

import java.time.Instant;

/** Proyección profesional que omite email, local, reserva, actor y notas. */
public record IncidentHistoryItemResponse(String incidentType, Instant reportedAt, String status) {}
