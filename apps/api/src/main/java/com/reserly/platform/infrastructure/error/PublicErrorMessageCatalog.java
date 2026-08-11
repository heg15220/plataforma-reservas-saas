package com.reserly.platform.infrastructure.error;

import java.util.Map;

/**
 * Catálogo cerrado que vincula códigos HTTP estables con claves i18n públicas.
 *
 * <p>La API nunca traduce ni incorpora el mensaje de una excepción o proveedor. Los clientes
 * resuelven estas claves contra sus catálogos locales; un código nuevo sin registro falla durante
 * desarrollo en lugar de degradarse a texto técnico.
 */
public final class PublicErrorMessageCatalog {

  private static final Map<String, String> MESSAGE_KEYS =
      Map.ofEntries(
          Map.entry("REQUEST_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("VALIDATION_ERROR", "PublicErrors.invalidRequest"),
          Map.entry("REGISTRATION_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("REGISTRATION_CONFLICT", "PublicErrors.registrationConflict"),
          Map.entry("AUTHENTICATION_INVALID", "PublicErrors.authenticationInvalid"),
          Map.entry("EMAIL_VERIFICATION_INVALID", "PublicErrors.emailVerificationInvalid"),
          Map.entry("PASSWORD_RESET_INVALID", "PublicErrors.passwordResetInvalid"),
          Map.entry("RATE_LIMIT_EXCEEDED", "PublicErrors.rateLimited"),
          Map.entry("RATE_LIMIT_UNAVAILABLE", "PublicErrors.unavailable"),
          Map.entry("REDSYS_CALLBACK_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("VENUE_SUBSCRIPTION_NOT_FOUND", "PublicErrors.notFound"),
          Map.entry("VENUE_SUBSCRIPTION_UNAVAILABLE", "PublicErrors.unavailable"),
          Map.entry("OPENING_HOURS_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("AVAILABILITY_DAY_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("TIME_SLOT_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("TIME_SLOT_DELETE_CONFLICT", "PublicErrors.conflict"),
          Map.entry("VENUE_PROFILE_NOT_FOUND", "PublicErrors.notFound"),
          Map.entry("REVIEW_NOT_ELIGIBLE", "PublicErrors.reviewNotEligible"),
          Map.entry("REVIEW_ALREADY_SUBMITTED", "PublicErrors.reviewAlreadySubmitted"),
          Map.entry("RESERVATION_HOLD_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("RESERVATION_CONFIRMATION_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("RESERVATION_FORM_INVALID", "PublicErrors.invalidRequest"),
          Map.entry("RESERVATION_HOLD_EXPIRED", "PublicErrors.holdExpired"),
          Map.entry("RESERVATION_CAPACITY_UNAVAILABLE", "PublicErrors.capacityUnavailable"),
          Map.entry("ACTIVE_BOOKING_RESTRICTION", "PublicErrors.restrictionActive"),
          Map.entry("RESERVATION_MANAGEMENT_LINK_INVALID", "PublicErrors.notFound"),
          Map.entry(
              "RESERVATION_CANCELLATION_DEADLINE_PASSED",
              "PublicErrors.cancellationDeadlinePassed"),
          Map.entry("PUBLIC_SERVICE_UNAVAILABLE", "PublicErrors.unavailable"));

  private PublicErrorMessageCatalog() {}

  /** Obtiene la clave visible autorizada para un código público conocido. */
  public static String messageKey(String errorCode) {
    String messageKey = MESSAGE_KEYS.get(errorCode);
    if (messageKey == null) {
      throw new IllegalArgumentException("Unsupported public error code");
    }
    return messageKey;
  }
}
