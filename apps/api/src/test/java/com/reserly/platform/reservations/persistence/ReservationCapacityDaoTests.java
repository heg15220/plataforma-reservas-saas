package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** Protege la semántica JPQL que define qué reservas consumen capacidad. */
class ReservationCapacityDaoTests {

  @Test
  void countsConfirmedLifecycleAndOnlyStrictlyActiveHolds()
      throws NoSuchMethodException {
    var method =
        ReservationDao.class.getMethod(
            "sumOccupiedCapacity", UUID.class, Instant.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query).isNotNull();
    assertThat(normalize(query.value()))
        .contains("reservation.status in ('confirmed', 'attended', 'no_show', 'reported')")
        .contains("reservation.status = 'hold'")
        .contains("reservation.holdExpiresAt > :now")
        .doesNotContain("reservation.holdExpiresAt >= :now");
  }

  @Test
  void confirmationCapacityExcludesOnlyTheOwnedHold()
      throws NoSuchMethodException {
    var method =
        ReservationDao.class.getMethod(
            "sumOccupiedCapacityExcluding",
            UUID.class,
            UUID.class,
            Instant.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query).isNotNull();
    assertThat(normalize(query.value()))
        .contains("reservation.id <> :excludedReservationId")
        .contains("reservation.status in ('confirmed', 'attended', 'no_show', 'reported')")
        .contains("reservation.status = 'hold'")
        .contains("reservation.holdExpiresAt > :now");
  }

  private String normalize(String query) {
    return query.replaceAll("\\s+", " ").trim();
  }
}
