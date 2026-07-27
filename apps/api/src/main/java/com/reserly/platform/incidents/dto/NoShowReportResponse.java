package com.reserly.platform.incidents.dto;

import java.time.Instant;
import java.util.UUID;

/** Resultado minimizado del reporte auditado. */
public record NoShowReportResponse(
    UUID incidentId, UUID reservationId, String status, Instant reportedAt) {}
