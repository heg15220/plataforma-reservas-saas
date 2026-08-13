package com.reserly.platform.demand.ingestion;

/** Contrato interno que valida, minimiza y persiste lotes idempotentes de eventos. */
public interface DemandEventIngestionService {

  /**
   * Ingiere un lote bajo la cuota de un productor autenticado.
   *
   * @throws DemandIngestionException si contrato, catálogo, identidad o lote son inválidos
   */
  EventBatchIngestionResponse ingest(String producerId, EventBatchIngestionRequest request);

  /** Persiste un evento originado dentro de Spring sin cuota Redis ni credencial HTTP. */
  EventBatchIngestionResponse ingestTrusted(EventIngestionRequest request);
}
