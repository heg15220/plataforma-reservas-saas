package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.AuthenticationErrorResponse;
import com.reserly.platform.identity.service.InvalidAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mantiene uniforme el error público de autenticación sin filtrar el campo que falló. */
@RestControllerAdvice(assignableTypes = AuthenticationControllerImpl.class)
public class AuthenticationExceptionHandler {

  /** Credencial, estado o tipo inválidos comparten 401 y código estable. */
  @ExceptionHandler(InvalidAuthenticationException.class)
  public ResponseEntity<AuthenticationErrorResponse> handleInvalidCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new AuthenticationErrorResponse("AUTHENTICATION_INVALID"));
  }

  /** Un payload inválido tampoco expone detalles internos. */
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<AuthenticationErrorResponse> handleInvalidRequest() {
    return ResponseEntity.badRequest()
        .body(new AuthenticationErrorResponse("AUTHENTICATION_INVALID"));
  }
}
