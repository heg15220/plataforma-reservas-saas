package com.reserly.platform.infrastructure.validation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Convierte la validación de parámetros HTTP en un contrato opaco y uniforme.
 *
 * <p>Las validaciones de cuerpos conservan los manejadores específicos de cada módulo. Este
 * manejador cubre path variables, query strings y cabeceras validadas por Spring MVC sin reflejar
 * el valor rechazado ni detalles de Bean Validation.
 */
@RestControllerAdvice
public class RequestValidationExceptionHandler {

  /** Responde 400 sin enumerar el parámetro ni la regla que falló. */
  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<RequestValidationErrorResponse> handleInvalidParameter() {
    return ResponseEntity.badRequest().body(new RequestValidationErrorResponse("REQUEST_INVALID"));
  }
}
