package com.reserly.platform.demand.embedding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Lote interno acotado para impedir payloads vectoriales sin límite. */
public record SubjectEmbeddingBatchRequest(
    @NotEmpty @Size(max = 100) List<@Valid SubjectEmbeddingWrite> embeddings) {}
