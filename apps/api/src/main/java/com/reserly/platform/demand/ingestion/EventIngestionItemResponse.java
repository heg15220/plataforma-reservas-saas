package com.reserly.platform.demand.ingestion;

import java.util.UUID;

/** Resultado opaco por identificador: persistido o ya existente. */
public record EventIngestionItemResponse(UUID eventId, String status) {}
