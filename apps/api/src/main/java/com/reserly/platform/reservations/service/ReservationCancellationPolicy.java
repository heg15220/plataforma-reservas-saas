package com.reserly.platform.reservations.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Calcula la frontera absoluta de cancelación desde la política persistida del local. */
public interface ReservationCancellationPolicy {

  /** La cancelación se admite hasta la frontera inclusive. */
  CancellationWindow evaluate(
      LocalDate date,
      LocalTime startsAt,
      int cancellationNoticeMinutes,
      ZoneId zoneId,
      Instant now);

  /** Resultado inmutable que la consulta y la mutación comparten para evitar divergencias. */
  record CancellationWindow(Instant deadline, boolean allowed) {}
}
