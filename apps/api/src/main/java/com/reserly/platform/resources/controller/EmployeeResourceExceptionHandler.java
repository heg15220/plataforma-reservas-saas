package com.reserly.platform.resources.controller;

import com.reserly.platform.resources.dto.EmployeeResourceErrorResponse;
import com.reserly.platform.resources.service.EmployeeResourceInvalidException;
import com.reserly.platform.resources.service.EmployeeResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de equipo a códigos estables sin filtrar constraints internas. */
@RestControllerAdvice(assignableTypes = EmployeeResourceControllerImpl.class)
public class EmployeeResourceExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class, EmployeeResourceInvalidException.class})
  public ResponseEntity<EmployeeResourceErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest()
        .body(new EmployeeResourceErrorResponse("TEAM_RESOURCE_INVALID"));
  }

  @ExceptionHandler(EmployeeResourceNotFoundException.class)
  public ResponseEntity<EmployeeResourceErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new EmployeeResourceErrorResponse("TEAM_RESOURCE_NOT_FOUND"));
  }
}
