package com.reserly.platform.identity.converter;

import com.reserly.platform.identity.dto.EmailVerificationResponse;
import com.reserly.platform.identity.service.EmailVerificationResult;
import org.springframework.stereotype.Component;

/** Separa el resultado interno de verificación de su representación REST. */
@Component
public class EmailVerificationConverter {

  /** Convierte únicamente metadatos no sensibles del resultado correcto. */
  public EmailVerificationResponse toResponse(EmailVerificationResult result) {
    return new EmailVerificationResponse(true, result.verifiedAt(), result.accountStatus());
  }
}
