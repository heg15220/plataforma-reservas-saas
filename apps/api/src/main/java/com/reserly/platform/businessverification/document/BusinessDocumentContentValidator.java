package com.reserly.platform.businessverification.document;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Aplica límite, allowlist MIME y magic bytes antes de cualquier almacenamiento.
 *
 * <p>Cierra el stream recibido. No confía en extensión ni nombre original y nunca los persiste.
 */
@Component
public class BusinessDocumentContentValidator {

  private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
  private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  private final BusinessDocumentUploadProperties properties;

  public BusinessDocumentContentValidator(BusinessDocumentUploadProperties properties) {
    this.properties = properties;
  }

  public ValidatedBusinessDocumentContent validate(String declaredMediaType, InputStream input) {
    if (input == null || declaredMediaType == null) {
      throw new BusinessDocumentUploadValidationException();
    }
    byte[] content = readBounded(input);
    String detectedMediaType = detectMediaType(content);
    if (!detectedMediaType.equals(declaredMediaType.strip().toLowerCase(java.util.Locale.ROOT))) {
      throw new BusinessDocumentUploadValidationException();
    }
    return new ValidatedBusinessDocumentContent(content, detectedMediaType, sha256(content));
  }

  private byte[] readBounded(InputStream input) {
    try (input) {
      byte[] content = input.readNBytes(properties.maxBytes() + 1);
      if (content.length == 0 || content.length > properties.maxBytes()) {
        throw new BusinessDocumentUploadValidationException();
      }
      return content;
    } catch (IOException exception) {
      throw new BusinessDocumentUploadValidationException();
    }
  }

  private String detectMediaType(byte[] content) {
    if (startsWith(content, PDF_SIGNATURE)) {
      return "application/pdf";
    }
    if (startsWith(content, PNG_SIGNATURE)) {
      return "image/png";
    }
    if (startsWith(content, JPEG_SIGNATURE)) {
      return "image/jpeg";
    }
    throw new BusinessDocumentUploadValidationException();
  }

  private boolean startsWith(byte[] content, byte[] signature) {
    if (content.length < signature.length) {
      return false;
    }
    for (int index = 0; index < signature.length; index++) {
      if (content[index] != signature[index]) {
        return false;
      }
    }
    return true;
  }

  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 algorithm is unavailable", exception);
    }
  }
}
