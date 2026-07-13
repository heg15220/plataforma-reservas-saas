package com.reserly.platform.forms.controller;

import com.reserly.platform.forms.dto.ReservationFormFieldErrorResponse;
import com.reserly.platform.forms.service.ReservationFormFieldInvalidException;
import com.reserly.platform.forms.service.ReservationFormFieldNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce fallos del CRUD a codigos estables sin filtrar constraints ni propiedad. */
@RestControllerAdvice(assignableTypes = ReservationFormFieldControllerImpl.class)
public class ReservationFormFieldExceptionHandler {

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    ReservationFormFieldInvalidException.class
  })
  public ResponseEntity<ReservationFormFieldErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest()
        .body(new ReservationFormFieldErrorResponse("RESERVATION_FORM_FIELD_INVALID"));
  }

  @ExceptionHandler(ReservationFormFieldNotFoundException.class)
  public ResponseEntity<ReservationFormFieldErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ReservationFormFieldErrorResponse("RESERVATION_FORM_FIELD_NOT_FOUND"));
  }
}
