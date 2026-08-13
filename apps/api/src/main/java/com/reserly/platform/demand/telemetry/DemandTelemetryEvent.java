package com.reserly.platform.demand.telemetry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Resultado operativo minimizado publicado dentro de una transacción de negocio. */
public record DemandTelemetryEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID requestId,
    UUID venueId,
    UUID serviceId,
    UUID resourceId,
    UUID timeSlotId,
    Map<String, Object> context) {

  /** Crea un evento sin PII, notas ni respuestas de formulario. */
  public static DemandTelemetryEvent create(
      String eventType,
      Instant occurredAt,
      UUID requestId,
      UUID venueId,
      UUID serviceId,
      UUID resourceId,
      UUID timeSlotId,
      Map<String, Object> context) {
    return new DemandTelemetryEvent(
        UUID.randomUUID(),
        eventType,
        occurredAt,
        requestId,
        venueId,
        serviceId,
        resourceId,
        timeSlotId,
        Map.copyOf(context));
  }
}
