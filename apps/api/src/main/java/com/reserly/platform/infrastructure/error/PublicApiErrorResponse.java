package com.reserly.platform.infrastructure.error;

/** Contrato de último recurso sin mensaje técnico, payload externo ni dato identificativo. */
public record PublicApiErrorResponse(String error, String messageKey) {

  public static PublicApiErrorResponse unavailable() {
    String code = "PUBLIC_SERVICE_UNAVAILABLE";
    return new PublicApiErrorResponse(code, PublicErrorMessageCatalog.messageKey(code));
  }
}
