package com.reserly.platform.identity.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/**
 * Error público estable del registro.
 *
 * <p>Solo expone un código no sensible y su clave localizada cerrada.
 */
public record RegistrationErrorResponse(String error, String messageKey) {
  public RegistrationErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
