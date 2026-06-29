package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.converter.EmailVerificationConverter;
import com.reserly.platform.identity.dto.EmailVerificationResponse;
import com.reserly.platform.identity.dto.RequestEmailVerificationRequest;
import com.reserly.platform.identity.dto.VerifyEmailRequest;
import com.reserly.platform.identity.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP del ciclo de verificación de email. */
@RestController
public class EmailVerificationControllerImpl implements EmailVerificationController {

  private final EmailVerificationService verificationService;
  private final EmailVerificationConverter converter;

  public EmailVerificationControllerImpl(
      EmailVerificationService verificationService, EmailVerificationConverter converter) {
    this.verificationService = verificationService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<EmailVerificationResponse> verify(VerifyEmailRequest request) {
    return ResponseEntity.ok(converter.toResponse(verificationService.verify(request.token())));
  }

  @Override
  public ResponseEntity<Void> request(RequestEmailVerificationRequest request) {
    verificationService.requestChallenge(request.email());
    return ResponseEntity.accepted().build();
  }
}
