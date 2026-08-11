package com.reserly.platform.identity.dto;

import com.reserly.platform.infrastructure.error.PublicErrorMessageCatalog;

/** Código público genérico de autenticación, sin distinguir identidad, estado o contraseña. */
public record AuthenticationErrorResponse(String error, String messageKey) {
  public AuthenticationErrorResponse(String error) {
    this(error, PublicErrorMessageCatalog.messageKey(error));
  }
}
