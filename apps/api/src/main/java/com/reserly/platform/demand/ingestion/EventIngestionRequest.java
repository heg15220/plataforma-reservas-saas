package com.reserly.platform.demand.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evento v1 recibido por la frontera interna.
 *
 * <p>El servicio aplica el catálogo tipo/familia/productor y valida el contexto cerrado antes de
 * construir la entidad. No se admite un campo payload ni texto libre.
 */
public record EventIngestionRequest(
    @NotNull UUID eventId,
    @NotNull Short schemaVersion,
    @NotBlank String eventType,
    @NotNull Instant occurredAt,
    @NotNull UUID requestId,
    @NotBlank String purpose,
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$") String consentVersion,
    UUID sessionId,
    UUID anonymousId,
    UUID customerId,
    UUID venueId,
    UUID serviceId,
    UUID resourceId,
    UUID timeSlotId,
    @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
    @NotNull Map<String, Object> context) {}
