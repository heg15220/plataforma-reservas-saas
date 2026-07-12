package com.reserly.platform.services.controller;

import com.reserly.platform.services.dto.ServiceErrorResponse;
import com.reserly.platform.services.service.ServiceInvalidException;
import com.reserly.platform.services.service.ServiceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de servicios a codigos estables sin filtrar constraints internas. */
@RestControllerAdvice(assignableTypes = ServiceControllerImpl.class)
public class ServiceExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class, ServiceInvalidException.class})
  public ResponseEntity<ServiceErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest().body(new ServiceErrorResponse("SERVICE_INVALID"));
  }

  @ExceptionHandler(ServiceNotFoundException.class)
  public ResponseEntity<ServiceErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ServiceErrorResponse("SERVICE_NOT_FOUND"));
  }
}
