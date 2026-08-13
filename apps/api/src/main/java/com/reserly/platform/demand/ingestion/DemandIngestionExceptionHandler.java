package com.reserly.platform.demand.ingestion;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce fallos de ingesta sin reflejar payload, campos, constraints ni mensajes internos. */
@RestControllerAdvice(assignableTypes = DemandEventIngestionControllerImpl.class)
public class DemandIngestionExceptionHandler {

  /** Contrato, catálogo, identidad o contexto se exponen bajo un único código externo. */
  @ExceptionHandler({
    DemandIngestionException.class,
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    DataIntegrityViolationException.class
  })
  public ResponseEntity<DemandIngestionErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest().body(new DemandIngestionErrorResponse("EVENT_INVALID"));
  }

  /** El interruptor operativo comunica indisponibilidad sin detalles de configuración. */
  @ExceptionHandler(DemandIngestionDisabledException.class)
  public ResponseEntity<DemandIngestionErrorResponse> handleDisabled() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new DemandIngestionErrorResponse("EVENT_INGESTION_UNAVAILABLE"));
  }
}
