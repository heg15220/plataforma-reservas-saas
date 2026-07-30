package com.reserly.platform.businessverification.document;

/** Puerto de almacenamiento de objetos privados ya cifrados. */
public interface PrivateObjectStorage {

  /** Guarda bytes cifrados bajo una clave interna sin generar URL pública. */
  void put(String objectKey, byte[] encryptedContent);

  /** Recupera bytes cifrados; solo los casos de uso autorizados pueden solicitar una clave. */
  byte[] get(String objectKey, long maximumBytes);

  /** Elimina un objeto durante una compensación o flujo de supresión. */
  void delete(String objectKey);
}
