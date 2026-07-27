package com.reserly.platform.incidents.controller;

import com.reserly.platform.incidents.dto.IncidentErrorResponse;
import com.reserly.platform.incidents.service.AttendanceInvalidException;
import com.reserly.platform.incidents.service.AttendanceNotFoundException;
import com.reserly.platform.incidents.service.AttendanceTooEarlyException;
import com.reserly.platform.incidents.service.VenueBookingRuleInvalidException;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de dominio sin filtrar datos internos ni payloads inválidos. */
@RestControllerAdvice(
    assignableTypes = {VenueBookingRuleControllerImpl.class, AttendanceControllerImpl.class})
public class IncidentExceptionHandler {

  @ExceptionHandler(VenueBookingRuleInvalidException.class)
  ResponseEntity<IncidentErrorResponse> invalidRule() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new IncidentErrorResponse("VENUE_BOOKING_RULE_INVALID"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<IncidentErrorResponse> invalidRequest() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new IncidentErrorResponse("INCIDENT_REQUEST_INVALID"));
  }

  @ExceptionHandler(VenueProfileNotFoundException.class)
  ResponseEntity<IncidentErrorResponse> venueNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new IncidentErrorResponse("VENUE_PROFILE_NOT_FOUND"));
  }

  @ExceptionHandler(AttendanceNotFoundException.class)
  ResponseEntity<IncidentErrorResponse> attendanceNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new IncidentErrorResponse("VENUE_RESERVATION_NOT_FOUND"));
  }

  @ExceptionHandler(AttendanceTooEarlyException.class)
  ResponseEntity<IncidentErrorResponse> attendanceTooEarly() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new IncidentErrorResponse("ATTENDANCE_RESERVATION_NOT_FINISHED"));
  }

  @ExceptionHandler(AttendanceInvalidException.class)
  ResponseEntity<IncidentErrorResponse> attendanceInvalid() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new IncidentErrorResponse("ATTENDANCE_TRANSITION_INVALID"));
  }
}
