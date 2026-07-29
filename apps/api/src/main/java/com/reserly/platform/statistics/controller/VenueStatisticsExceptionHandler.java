package com.reserly.platform.statistics.controller;

import com.reserly.platform.statistics.dto.StatisticsErrorResponse;
import com.reserly.platform.statistics.service.VenueStatisticsFilterInvalidException;
import com.reserly.platform.statistics.service.VenueStatisticsNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores del panel sin filtrar consultas, IDs ni estado editorial. */
@RestControllerAdvice(assignableTypes = VenueStatisticsControllerImpl.class)
public class VenueStatisticsExceptionHandler {

  @ExceptionHandler(VenueStatisticsFilterInvalidException.class)
  ResponseEntity<StatisticsErrorResponse> invalidFilter() {
    return ResponseEntity.badRequest()
        .body(new StatisticsErrorResponse("STATISTICS_FILTER_INVALID"));
  }

  @ExceptionHandler(VenueStatisticsNotFoundException.class)
  ResponseEntity<StatisticsErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new StatisticsErrorResponse("VENUE_STATISTICS_NOT_FOUND"));
  }
}
