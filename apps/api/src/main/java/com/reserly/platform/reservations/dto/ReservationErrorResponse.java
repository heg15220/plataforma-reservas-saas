package com.reserly.platform.reservations.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error público estable que no revela entidades ni restricciones internas. */
public record ReservationErrorResponse(String code, String messageKey) {
  public ReservationErrorResponse(String code) {
    this(code, PublicErrorMessageCatalog.messageKey(code));
  }
}
