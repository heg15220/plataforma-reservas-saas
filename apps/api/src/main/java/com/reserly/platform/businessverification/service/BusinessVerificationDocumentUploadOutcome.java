package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado mínimo de una carga privada.
 *
 * @param documentId identificador del metadato
 * @param documentRequestId requerimiento satisfecho
 * @param status estado documental
 * @param uploadedAt instante de persistencia
 */
public record BusinessVerificationDocumentUploadOutcome(
    UUID documentId, UUID documentRequestId, String status, Instant uploadedAt) {

  public BusinessVerificationDocumentUploadOutcome {
    Objects.requireNonNull(documentId);
    Objects.requireNonNull(documentRequestId);
    Objects.requireNonNull(status);
    Objects.requireNonNull(uploadedAt);
  }
}
