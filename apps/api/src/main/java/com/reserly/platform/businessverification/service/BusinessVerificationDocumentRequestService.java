package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Caso de uso interno para generar y consultar solicitudes de respaldo. */
public interface BusinessVerificationDocumentRequestService {

  /**
   * Crea de forma idempotente el requerimiento asociado a un check inconcluso.
   *
   * <p>Debe invocarse dentro de la misma transacción que establece `pending_review`.
   */
  BusinessVerificationDocumentRequestSnapshot ensureRequested(
      UUID businessAccountId, UUID verificationCheckId);

  /** Cancela el requerimiento abierto antes de iniciar una nueva verificación. */
  void cancelOpenForRevalidation(UUID businessAccountId, Instant cancelledAt);

  /** Recupera el requerimiento abierto sin exponer datos fiscales ni documentos. */
  Optional<BusinessVerificationDocumentRequestSnapshot> findOpen(UUID businessAccountId);
}
