package com.reserly.platform.incidents.service;

import java.time.LocalDate;

/** Indica al flujo público la fecha local hasta la que una identidad no puede reservar. */
public class ActiveBookingRestrictionException extends RuntimeException {

  private final LocalDate restrictedUntil;

  public ActiveBookingRestrictionException(LocalDate restrictedUntil) {
    super("Active booking restriction");
    this.restrictedUntil = restrictedUntil;
  }

  public LocalDate getRestrictedUntil() {
    return restrictedUntil;
  }
}
