package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.RegistrationErrorResponse;
import com.reserly.platform.identity.service.RegistrationConflictException;
import com.reserly.platform.identity.service.RegistrationValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce errores esperados del registro a códigos públicos no sensibles.
 *
 * <p>No devuelve campos duplicados, mensajes de base de datos ni contenido del payload. Los textos
 * localizados se incorporarán mediante catálogos en 1.21.
 */
@RestControllerAdvice(assignableTypes = VenueRegistrationControllerImpl.class)
public class RegistrationExceptionHandler {

  /** Devuelve un conflicto genérico para no facilitar enumeración de identidades. */
  @ExceptionHandler(RegistrationConflictException.class)
  public ResponseEntity<RegistrationErrorResponse> handleConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new RegistrationErrorResponse("REGISTRATION_CONFLICT"));
  }

  /** Agrupa payloads mal formados e invariantes de seguridad bajo un código estable. */
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    RegistrationValidationException.class
  })
  public ResponseEntity<RegistrationErrorResponse> handleInvalidRequest() {
    return ResponseEntity.badRequest().body(new RegistrationErrorResponse("REGISTRATION_INVALID"));
  }
}
