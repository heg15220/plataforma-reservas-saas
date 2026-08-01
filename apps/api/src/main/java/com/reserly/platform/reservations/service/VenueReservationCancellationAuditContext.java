package com.reserly.platform.reservations.service;

/** Metadatos técnicos capturados por el servidor para la auditoría de cancelación. */
public record VenueReservationCancellationAuditContext(String ipAddress, String userAgent) {}
