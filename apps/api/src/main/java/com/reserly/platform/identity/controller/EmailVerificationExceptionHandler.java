package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.EmailVerificationErrorResponse;
import com.reserly.platform.identity.service.InvalidEmailVerificationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Uniforma los errores de verificación para no convertir el endpoint en un oráculo. */
@RestControllerAdvice(assignableTypes = EmailVerificationControllerImpl.class)
public class EmailVerificationExceptionHandler {

  /** Un token inválido, usado, revocado o expirado comparte el mismo contrato público. */
  @ExceptionHandler(InvalidEmailVerificationException.class)
  public ResponseEntity<EmailVerificationErrorResponse> handleInvalidChallenge() {
    return ResponseEntity.badRequest()
        .body(new EmailVerificationErrorResponse("EMAIL_VERIFICATION_INVALID"));
  }

  /** Los payloads malformados conservan el mismo error estable. */
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<EmailVerificationErrorResponse> handleInvalidRequest() {
    return ResponseEntity.badRequest()
        .body(new EmailVerificationErrorResponse("EMAIL_VERIFICATION_INVALID"));
  }
}
