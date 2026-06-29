package com.reserly.platform.businessverification.service;

/** Oculta si falló propiedad, rol o existencia del recurso sensible. */
public class BusinessVerificationDocumentUploadForbiddenException extends RuntimeException {

  public BusinessVerificationDocumentUploadForbiddenException() {
    super("Business document upload is not allowed");
  }
}
