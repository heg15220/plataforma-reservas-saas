package com.reserly.platform.businessverification.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmDocumentEncryptionServiceTests {

  private static final byte[] PLAINTEXT = "documento privado".getBytes(StandardCharsets.UTF_8);

  private final AesGcmDocumentEncryptionServiceImpl service =
      new AesGcmDocumentEncryptionServiceImpl(
          new DocumentEncryptionProperties(
              "test-key-v1",
              Base64.getEncoder()
                  .encodeToString(
                      "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII))));

  @Test
  void encryptsWithVersionedEnvelopeAndRandomNonce() {
    byte[] first = service.encrypt(PLAINTEXT);
    byte[] second = service.encrypt(PLAINTEXT);

    assertThat(first).startsWith((byte) 'R', (byte) 'S', (byte) 'Y', (byte) '1');
    assertThat(first).isNotEqualTo(PLAINTEXT).hasSizeGreaterThan(PLAINTEXT.length);
    assertThat(second).isNotEqualTo(first);
    assertThat(service.keyId()).isEqualTo("test-key-v1");
  }
}
