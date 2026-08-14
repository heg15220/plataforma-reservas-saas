package com.reserly.platform.demand.embedding;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato interno v1 para persistir exclusivamente artefactos vectoriales calculados. */
@RequestMapping("/api/internal/demand/v1/embeddings")
public interface SubjectEmbeddingController {
  @PutMapping
  SubjectEmbeddingBatchResult persist(@Valid @RequestBody SubjectEmbeddingBatchRequest request);
}
