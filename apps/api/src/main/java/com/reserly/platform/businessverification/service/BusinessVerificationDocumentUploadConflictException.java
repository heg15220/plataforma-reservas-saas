package com.reserly.platform.businessverification.service;

/** Oculta la restricción persistente concreta que impidió registrar el documento. */
public class BusinessVerificationDocumentUploadConflictException extends RuntimeException {

  public BusinessVerificationDocumentUploadConflictException(Throwable cause) {
    super("Business document upload conflicts with existing data", cause);
  }
}
