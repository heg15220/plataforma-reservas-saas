package com.reserly.platform.reservations.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;
import java.time.LocalDate;

/**
 * Rechazo público minimizado de una confirmación con restricción temporal.
 *
 * <p>El cliente localiza el código estable y muestra la fecha; la respuesta no revela contador,
 * incidencias, locales ni motivo interno.
 */
public record ReservationRestrictionErrorResponse(
    String error, LocalDate restrictedUntil, String messageKey) {
  public ReservationRestrictionErrorResponse(String error, LocalDate restrictedUntil) {
    this(error, restrictedUntil, PublicErrorMessageCatalog.messageKey(error));
  }
}
