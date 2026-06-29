package com.reserly.platform.businessverification.document;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Clave de cifrado cliente-side.
 *
 * @param keyId identificador no secreto para rotación
 * @param keyBase64 clave AES-256 codificada en Base64
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.documents.encryption")
public record DocumentEncryptionProperties(@NotBlank String keyId, @NotBlank String keyBase64) {

  @AssertTrue(message = "La clave documental debe contener exactamente 32 bytes")
  public boolean isAes256Key() {
    try {
      return keyBase64 != null && Base64.getDecoder().decode(keyBase64).length == 32;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
