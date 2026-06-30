package com.reserly.platform.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Verifica que los errores públicos no expongan discriminadores ni detalles de Redis. */
class RateLimitExceptionHandlerTests {

  private final RateLimitExceptionHandler handler = new RateLimitExceptionHandler();

  @Test
  void exposesRetryAfterForExhaustedQuota() {
    ResponseEntity<RateLimitErrorResponse> response =
        handler.handleExceeded(new RateLimitExceededException(Duration.ofMillis(1_001)));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
    assertThat(response.getBody()).isEqualTo(new RateLimitErrorResponse("RATE_LIMIT_EXCEEDED"));
  }

  @Test
  void failsClosedWithoutLeakingInfrastructureDetails() {
    ResponseEntity<RateLimitErrorResponse> response = handler.handleUnavailable();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isEqualTo(new RateLimitErrorResponse("RATE_LIMIT_UNAVAILABLE"));
  }
}
