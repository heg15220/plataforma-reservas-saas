package com.reserly.platform.reservations.messaging;

/** Nombres versionados del trabajo de email por cancelación iniciada por el local. */
public final class VenueReservationCancellationMessagingTopology {

  public static final String QUEUE = "reserly.reservations.venue-cancellation-email.v1";
  public static final String ROUTING_KEY = "reservations.venue-cancellation-email.requested.v1";

  private VenueReservationCancellationMessagingTopology() {}
}
