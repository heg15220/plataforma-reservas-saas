package com.reserly.platform.reservations.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

/** Política basada en la hora local de inicio y la antelación configurada por el local. */
@Service
public class ReservationCancellationPolicyImpl implements ReservationCancellationPolicy {

  @Override
  public CancellationWindow evaluate(
      LocalDate date,
      LocalTime startsAt,
      boolean cancellationAllowed,
      int cancellationNoticeMinutes,
      ZoneId zoneId,
      Instant now) {
    if (date == null
        || startsAt == null
        || zoneId == null
        || now == null
        || cancellationNoticeMinutes < 0) {
      throw new IllegalArgumentException("Invalid cancellation policy input");
    }
    Instant deadline =
        date.atTime(startsAt)
            .atZone(zoneId)
            .toInstant()
            .minusSeconds(Math.multiplyExact(cancellationNoticeMinutes, 60L));
    return new CancellationWindow(deadline, cancellationAllowed && !now.isAfter(deadline));
  }
}
