package com.reserly.platform.demand.embedding;

import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP sin logging de vectores ni metadatos de sujetos. */
@RestController
public class SubjectEmbeddingControllerImpl implements SubjectEmbeddingController {
  private final SubjectEmbeddingService service;

  public SubjectEmbeddingControllerImpl(SubjectEmbeddingService service) {
    this.service = service;
  }

  @Override
  public SubjectEmbeddingBatchResult persist(SubjectEmbeddingBatchRequest request) {
    return service.persist(request);
  }
}
