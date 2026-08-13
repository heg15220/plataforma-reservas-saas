package com.reserly.platform.demand.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Lote acotado de eventos v1; el límite configurable puede ser inferior al límite de transporte.
 */
public record EventBatchIngestionRequest(
    @NotEmpty @Size(max = 100) List<@Valid EventIngestionRequest> events) {}
