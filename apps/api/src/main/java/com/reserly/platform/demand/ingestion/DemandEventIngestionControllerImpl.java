package com.reserly.platform.demand.ingestion;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP sin lógica ni logging de payloads para la ingesta interna. */
@RestController
public class DemandEventIngestionControllerImpl implements DemandEventIngestionController {

  private final DemandEventIngestionService ingestionService;

  public DemandEventIngestionControllerImpl(DemandEventIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @Override
  public ResponseEntity<EventBatchIngestionResponse> ingest(
      Principal principal, EventBatchIngestionRequest request) {
    return ResponseEntity.ok(ingestionService.ingest(principal.getName(), request));
  }
}
