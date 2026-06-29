package com.reserly.platform.businessverification.document;

import java.util.Objects;

/**
 * Contenido acotado y validado antes del antivirus.
 *
 * @param bytes original en claro
 * @param mediaType tipo detectado
 * @param sha256 hash hexadecimal del original
 */
public record ValidatedBusinessDocumentContent(byte[] bytes, String mediaType, String sha256) {

  public ValidatedBusinessDocumentContent {
    bytes = bytes.clone();
    Objects.requireNonNull(mediaType);
    Objects.requireNonNull(sha256);
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
