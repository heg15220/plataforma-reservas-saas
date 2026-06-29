package com.reserly.platform.businessverification.document;

/** Rechaza contenido, tamaño, tipo o firma no permitidos sin reflejar datos del fichero. */
public class BusinessDocumentUploadValidationException extends RuntimeException {

  public BusinessDocumentUploadValidationException() {
    super("Business document upload is invalid");
  }
}
