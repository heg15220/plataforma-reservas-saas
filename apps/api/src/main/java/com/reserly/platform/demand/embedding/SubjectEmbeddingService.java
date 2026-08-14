package com.reserly.platform.demand.embedding;

/** Puerto transaccional que mantiene Spring como autoridad de persistencia. */
public interface SubjectEmbeddingService {
  SubjectEmbeddingBatchResult persist(SubjectEmbeddingBatchRequest request);
}
