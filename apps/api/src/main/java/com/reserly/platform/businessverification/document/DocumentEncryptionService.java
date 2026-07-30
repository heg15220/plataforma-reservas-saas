package com.reserly.platform.businessverification.document;

/** Cifra contenido limpio antes de abandonar la API. */
public interface DocumentEncryptionService {

  byte[] encrypt(byte[] plaintext);

  /** Descifra y autentica un objeto solo cuando corresponde a la clave configurada. */
  byte[] decrypt(byte[] encryptedContent, String keyId);

  String keyId();
}
