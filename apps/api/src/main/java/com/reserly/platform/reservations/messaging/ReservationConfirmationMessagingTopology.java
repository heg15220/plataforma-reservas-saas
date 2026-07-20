package com.reserly.platform.reservations.messaging;

/** Nombres versionados del trabajo que entregará los emails de reserva confirmada. */
public final class ReservationConfirmationMessagingTopology {

  public static final String QUEUE = "reserly.reservations.confirmation-email.v1";
  public static final String ROUTING_KEY = "reservations.confirmation-email.requested.v1";

  private ReservationConfirmationMessagingTopology() {}
}
