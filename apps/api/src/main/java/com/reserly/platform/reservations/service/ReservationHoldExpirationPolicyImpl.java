package com.reserly.platform.reservations.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Política única de holds de cinco minutos conforme a RF-014 y RB-004. */
@Service
public class ReservationHoldExpirationPolicyImpl implements ReservationHoldExpirationPolicy {

  private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

  @Override
  public Instant expiresAt(Instant createdAt) {
    Objects.requireNonNull(createdAt, "createdAt");
    return createdAt.plus(HOLD_DURATION);
  }

  @Override
  public boolean isActive(Instant expiresAt, Instant now) {
    validate(expiresAt, now);
    return now.isBefore(expiresAt);
  }

  @Override
  public long remainingSeconds(Instant expiresAt, Instant now) {
    validate(expiresAt, now);
    return Math.max(0, Duration.between(now, expiresAt).toSeconds());
  }

  private void validate(Instant expiresAt, Instant now) {
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(now, "now");
  }
}
