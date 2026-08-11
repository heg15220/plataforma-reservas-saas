package com.reserly.platform.identity.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Error uniforme que no distingue secreto, cuenta ni política de contraseña. */
public record PasswordResetErrorResponse(String error, String messageKey) {
  public PasswordResetErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
