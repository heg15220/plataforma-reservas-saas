package com.reserly.platform.infrastructure.ratelimit;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error público estable que no revela operación, cuota, clave ni infraestructura interna. */
public record RateLimitErrorResponse(String error, String messageKey) {
  public RateLimitErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
