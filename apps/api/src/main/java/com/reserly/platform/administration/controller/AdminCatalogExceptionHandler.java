package com.reserly.platform.administration.controller;

import com.reserly.platform.administration.dto.AdminErrorResponse;
import com.reserly.platform.administration.service.AdminResourceConflictException;
import com.reserly.platform.administration.service.AdminResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores estables del catálogo administrativo sin entidades ni restricciones internas. */
@RestControllerAdvice(assignableTypes = AdminCatalogControllerImpl.class)
public class AdminCatalogExceptionHandler {

  @ExceptionHandler(AdminResourceNotFoundException.class)
  ResponseEntity<AdminErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new AdminErrorResponse("ADMIN_RESOURCE_NOT_FOUND"));
  }

  @ExceptionHandler(AdminResourceConflictException.class)
  ResponseEntity<AdminErrorResponse> conflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new AdminErrorResponse("ADMIN_RESOURCE_CONFLICT"));
  }
}
