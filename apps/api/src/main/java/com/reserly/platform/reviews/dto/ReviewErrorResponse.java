package com.reserly.platform.reviews.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error público estable sin detalles de reservas ni identidades. */
public record ReviewErrorResponse(String error, String messageKey) {
  public ReviewErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
