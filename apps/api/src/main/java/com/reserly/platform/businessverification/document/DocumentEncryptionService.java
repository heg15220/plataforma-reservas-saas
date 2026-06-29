package com.reserly.platform.businessverification.document;

/** Cifra contenido limpio antes de abandonar la API. */
public interface DocumentEncryptionService {

  byte[] encrypt(byte[] plaintext);

  String keyId();
}
