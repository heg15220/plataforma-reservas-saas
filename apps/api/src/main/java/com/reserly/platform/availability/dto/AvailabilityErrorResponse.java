package com.reserly.platform.availability.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error estable de disponibilidad que no expone IDs ajenos ni constraints internas. */
public record AvailabilityErrorResponse(String error, String messageKey) {
  public AvailabilityErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
