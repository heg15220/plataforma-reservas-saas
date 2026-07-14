package com.reserly.platform.reservations.service;

import java.time.Instant;

/** Define la vigencia temporal autoritativa de los holds de reserva. */
public interface ReservationHoldExpirationPolicy {

  /** Calcula el instante exclusivo de expiración desde el momento de creación. */
  Instant expiresAt(Instant createdAt);

  /** Devuelve true únicamente mientras now sea estrictamente anterior a expiresAt. */
  boolean isActive(Instant expiresAt, Instant now);

  /** Devuelve los segundos completos restantes, limitados a cero tras expirar. */
  long remainingSeconds(Instant expiresAt, Instant now);
}
