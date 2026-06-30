package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.PasswordResetErrorResponse;
import com.reserly.platform.identity.service.InvalidPasswordResetException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mantiene un único error público para toda recuperación no admisible. */
@RestControllerAdvice(assignableTypes = PasswordResetControllerImpl.class)
public class PasswordResetExceptionHandler {

  /** Token, cuenta y contraseña inválidos comparten el mismo contrato. */
  @ExceptionHandler(InvalidPasswordResetException.class)
  public ResponseEntity<PasswordResetErrorResponse> handleInvalidReset() {
    return ResponseEntity.badRequest()
        .body(new PasswordResetErrorResponse("PASSWORD_RESET_INVALID"));
  }

  /** Los payloads malformados tampoco exponen qué validación falló. */
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<PasswordResetErrorResponse> handleInvalidRequest() {
    return ResponseEntity.badRequest()
        .body(new PasswordResetErrorResponse("PASSWORD_RESET_INVALID"));
  }
}
