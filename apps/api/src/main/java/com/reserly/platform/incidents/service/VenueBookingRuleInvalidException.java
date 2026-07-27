package com.reserly.platform.incidents.service;

/** Indica que una configuración no satisface los límites operativos del dominio. */
public class VenueBookingRuleInvalidException extends RuntimeException {

  public VenueBookingRuleInvalidException() {
    super("Invalid venue booking rule");
  }
}
