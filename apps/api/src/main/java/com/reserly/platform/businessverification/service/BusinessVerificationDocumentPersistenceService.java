package com.reserly.platform.businessverification.service;

import java.util.UUID;

/** Frontera transaccional de autorización y metadatos documentales. */
public interface BusinessVerificationDocumentPersistenceService {

  /** Preflight sin escribir ni acceder al objeto. */
  void validateUploadAuthorization(
      UUID businessAccountId, UUID documentRequestId, UUID uploaderUserId, String documentType);

  /** Revalida bajo lock, crea el documento y satisface el requerimiento. */
  BusinessVerificationDocumentUploadOutcome persist(
      BusinessVerificationDocumentUploadPersistenceCommand command);
}
