package com.reserly.platform.infrastructure.validation;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error público estable que no expone nombres de parámetros ni restricciones internas. */
public record RequestValidationErrorResponse(String error, String messageKey) {
  public RequestValidationErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
