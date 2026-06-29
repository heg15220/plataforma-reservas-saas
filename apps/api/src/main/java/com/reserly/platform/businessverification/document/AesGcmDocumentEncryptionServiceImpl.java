package com.reserly.platform.businessverification.document;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Cifrado autenticado AES-256-GCM con IV aleatorio por objeto.
 *
 * <p>El formato binario es `RSY1 || IV(12) || ciphertext+tag`. La clave nunca se persiste ni forma
 * parte del object key.
 */
@Component
public class AesGcmDocumentEncryptionServiceImpl implements DocumentEncryptionService {

  private static final byte[] FORMAT_HEADER = "RSY1".getBytes(StandardCharsets.US_ASCII);
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final DocumentEncryptionProperties properties;
  private final SecretKeySpec key;
  private final SecureRandom secureRandom = new SecureRandom();

  public AesGcmDocumentEncryptionServiceImpl(DocumentEncryptionProperties properties) {
    this.properties = properties;
    this.key = new SecretKeySpec(Base64.getDecoder().decode(properties.keyBase64()), "AES");
  }

  @Override
  public byte[] encrypt(byte[] plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext);
      return ByteBuffer.allocate(FORMAT_HEADER.length + iv.length + ciphertext.length)
          .put(FORMAT_HEADER)
          .put(iv)
          .put(ciphertext)
          .array();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Document encryption failed", exception);
    }
  }

  @Override
  public String keyId() {
    return properties.keyId();
  }
}
