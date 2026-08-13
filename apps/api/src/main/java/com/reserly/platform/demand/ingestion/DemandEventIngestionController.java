package com.reserly.platform.demand.ingestion;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato HTTP interno de ingesta de eventos v1. */
@RequestMapping("/api/internal/demand/v1/events")
public interface DemandEventIngestionController {

  /**
   * Acepta un lote de 1-100 eventos bajo autenticación y cuota de servicio.
   *
   * @return 200 con estados accepted/duplicate; 400 opaco para contrato inválido
   */
  @PostMapping
  ResponseEntity<EventBatchIngestionResponse> ingest(
      Principal principal, @Valid @RequestBody EventBatchIngestionRequest request);
}
