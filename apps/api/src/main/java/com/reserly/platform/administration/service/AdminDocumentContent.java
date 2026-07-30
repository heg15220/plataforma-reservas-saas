package com.reserly.platform.administration.service;

/** Contenido descifrado efímero que nunca se persiste ni se registra en logs. */
public record AdminDocumentContent(byte[] bytes, String mediaType) {
  public AdminDocumentContent {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
