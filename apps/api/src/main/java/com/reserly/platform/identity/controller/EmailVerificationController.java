package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.EmailVerificationResponse;
import com.reserly.platform.identity.dto.RequestEmailVerificationRequest;
import com.reserly.platform.identity.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato público de consumo y reenvío de verificaciones de email. */
@RequestMapping(path = "/api/auth/email", produces = MediaType.APPLICATION_JSON_VALUE)
public interface EmailVerificationController {

  /** Consume exactamente una vez el token recibido por el titular. */
  @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<EmailVerificationResponse> verify(@Valid @RequestBody VerifyEmailRequest request);

  /** Solicita una rotación sin revelar si el email existe o ya fue verificado. */
  @PostMapping(path = "/verification/request", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> request(@Valid @RequestBody RequestEmailVerificationRequest request);
}
