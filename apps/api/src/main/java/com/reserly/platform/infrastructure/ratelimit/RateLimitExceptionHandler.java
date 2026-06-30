package com.reserly.platform.infrastructure.ratelimit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce fallos de cuota a contratos HTTP genéricos y accionables. */
@RestControllerAdvice
public class RateLimitExceptionHandler {

  /** Devuelve 429 y el mínimo entero de segundos que evita reintentos prematuros. */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<RateLimitErrorResponse> handleExceeded(
      RateLimitExceededException exception) {
    long retryAfterSeconds = Math.max(1L, (exception.retryAfter().toMillis() + 999L) / 1_000L);
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
        .body(new RateLimitErrorResponse("RATE_LIMIT_EXCEEDED"));
  }

  /** Falla cerrado con un error temporal cuando Redis no puede garantizar la cuota. */
  @ExceptionHandler(RateLimitUnavailableException.class)
  public ResponseEntity<RateLimitErrorResponse> handleUnavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new RateLimitErrorResponse("RATE_LIMIT_UNAVAILABLE"));
  }
}
