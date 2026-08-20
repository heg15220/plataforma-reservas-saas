package com.reserly.platform.demand.attribution;

import java.time.Duration;

/** Política inmutable del baseline observacional de atribución. */
public final class BookingAttributionPolicy {

  public static final String VERSION = "booking-attribution-v1";
  public static final Duration WINDOW = Duration.ofDays(7);
  public static final int MAX_EVIDENCE_ITEMS = 20;

  private BookingAttributionPolicy() {}
}
