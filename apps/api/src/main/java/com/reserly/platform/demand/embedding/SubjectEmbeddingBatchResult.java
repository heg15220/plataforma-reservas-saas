package com.reserly.platform.demand.embedding;

/** Contadores opacos del UPSERT; nunca devuelve vectores ni texto. */
public record SubjectEmbeddingBatchResult(int inserted, int updated, int unchanged) {}
