package com.reserly.platform.billing.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error opaco de facturación o callback externo, acompañado solo por una clave i18n cerrada. */
public record BillingErrorResponse(String error, String messageKey) {
  public BillingErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
