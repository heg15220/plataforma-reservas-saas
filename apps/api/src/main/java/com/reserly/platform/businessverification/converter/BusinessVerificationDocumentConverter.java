package com.reserly.platform.businessverification.converter;

import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentRequestResponse;
import com.reserly.platform.businessverification.dto.BusinessVerificationDocumentUploadResponse;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentRequestSnapshot;
import com.reserly.platform.businessverification.service.BusinessVerificationDocumentUploadOutcome;
import org.springframework.stereotype.Component;

/** Evita que snapshots internos con IDs de cuenta o check atraviesen la frontera REST. */
@Component
public class BusinessVerificationDocumentConverter {

  /** Proyecta exclusivamente los datos que el propietario necesita para elegir una alternativa. */
  public BusinessVerificationDocumentRequestResponse toResponse(
      BusinessVerificationDocumentRequestSnapshot snapshot) {
    return new BusinessVerificationDocumentRequestResponse(
        snapshot.requestId(),
        snapshot.reasonCode(),
        snapshot.requestedDocumentTypes(),
        snapshot.status(),
        snapshot.requestedAt());
  }

  /** Convierte el resultado persistido sin revelar localizadores privados ni hashes. */
  public BusinessVerificationDocumentUploadResponse toResponse(
      BusinessVerificationDocumentUploadOutcome outcome) {
    return new BusinessVerificationDocumentUploadResponse(
        outcome.documentId(), outcome.documentRequestId(), outcome.status(), outcome.uploadedAt());
  }
}
