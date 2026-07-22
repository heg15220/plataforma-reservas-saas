package com.reserly.platform.notifications;

/** Identificadores estables de las plantillas transaccionales disponibles. */
public enum EmailTemplateType {
  EMAIL_VERIFICATION("emailVerification"),
  PASSWORD_RESET("passwordReset"),
  RESERVATION_CONFIRMATION("reservationConfirmation"),
  VENUE_RESERVATION_NOTIFICATION("venueReservationNotification"),
  USER_CANCELLATION_NOTICE("userCancellationNotice"),
  VENUE_CANCELLATION_NOTICE("venueCancellationNotice");

  private final String catalogKey;

  EmailTemplateType(String catalogKey) {
    this.catalogKey = catalogKey;
  }

  public String catalogKey() {
    return catalogKey;
  }
}
