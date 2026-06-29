package com.reserly.platform.businessverification.service;

/** Caso de uso interno de carga privada y segura. */
public interface BusinessVerificationDocumentUploadService {

  BusinessVerificationDocumentUploadOutcome upload(
      BusinessVerificationDocumentUploadCommand command);
}
