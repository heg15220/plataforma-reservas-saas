package com.reserly.platform.demand.ingestion;

import java.util.List;

/** Resumen idempotente del lote sin reproducir ningún dato de contexto. */
public record EventBatchIngestionResponse(
    int acceptedCount, int duplicateCount, List<EventIngestionItemResponse> results) {}
