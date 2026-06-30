package com.reserly.platform.infrastructure.ratelimit;

import java.time.Duration;

/** Señala una cuota agotada sin conservar el discriminador sensible. */
public class RateLimitExceededException extends RuntimeException {

  private final Duration retryAfter;

  public RateLimitExceededException(Duration retryAfter) {
    super("Rate limit exceeded");
    this.retryAfter = retryAfter;
  }

  /** Tiempo restante de la ventana para construir {@code Retry-After}. */
  public Duration retryAfter() {
    return retryAfter;
  }
}
