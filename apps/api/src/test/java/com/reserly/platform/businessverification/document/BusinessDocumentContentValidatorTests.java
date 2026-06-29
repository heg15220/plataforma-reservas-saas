package com.reserly.platform.businessverification.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BusinessDocumentContentValidatorTests {

  private final BusinessDocumentContentValidator validator =
      new BusinessDocumentContentValidator(new BusinessDocumentUploadProperties(16));

  @Test
  void acceptsPdfFromSignatureAndCalculatesPlaintextDigest() {
    byte[] content = "%PDF-1.7\nok".getBytes(StandardCharsets.US_ASCII);

    ValidatedBusinessDocumentContent validated =
        validator.validate("application/pdf", new ByteArrayInputStream(content));

    assertThat(validated.mediaType()).isEqualTo("application/pdf");
    assertThat(validated.bytes()).containsExactly(content);
    assertThat(validated.sha256())
        .isEqualTo("edb71096119316e96e23314e59d7edc620b2bd8ef440397bc8417e71358ad6d8");
  }

  @Test
  void rejectsDeclaredMimeThatDoesNotMatchMagicBytes() {
    byte[] content = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

    assertThatThrownBy(() -> validator.validate("image/png", new ByteArrayInputStream(content)))
        .isInstanceOf(BusinessDocumentUploadValidationException.class);
  }

  @Test
  void rejectsContentAboveConfiguredLimit() {
    byte[] content = "%PDF-123456789012".getBytes(StandardCharsets.US_ASCII);

    assertThatThrownBy(
            () -> validator.validate("application/pdf", new ByteArrayInputStream(content)))
        .isInstanceOf(BusinessDocumentUploadValidationException.class);
  }
}
