package com.reserly.platform.demand.correlation;

import java.time.Instant;
import java.util.UUID;

/** Referencia minimizada para reconciliación; excluye contexto, sujetos e identidades. */
public record DemandEventReference(
    UUID eventId, String eventType, String producer, Instant occurredAt) {}
