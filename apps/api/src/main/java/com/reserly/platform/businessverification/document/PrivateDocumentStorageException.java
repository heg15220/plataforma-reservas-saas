package com.reserly.platform.businessverification.document;

/** Abstrae fallos S3 sin filtrar endpoint, bucket, credenciales ni mensajes del proveedor. */
public class PrivateDocumentStorageException extends RuntimeException {

  public PrivateDocumentStorageException() {
    super("Private document storage operation failed");
  }
}
