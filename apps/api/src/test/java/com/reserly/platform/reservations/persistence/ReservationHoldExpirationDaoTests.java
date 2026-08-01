package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Protege el contrato atómico e idempotente de expiración de holds. */
class ReservationHoldExpirationDaoTests {

  @Test
  void expiresOnlyHoldsStrictlyOlderThanTheJobBoundary() throws NoSuchMethodException {
    var method = ReservationDao.class.getMethod("expireHoldsBefore", Instant.class);
    Query query = method.getAnnotation(Query.class);
    Modifying modifying = method.getAnnotation(Modifying.class);

    assertThat(query).isNotNull();
    assertThat(normalize(query.value()))
        .contains("set reservation.status = 'expired', reservation.updatedAt = :now")
        .contains("reservation.status = 'hold'")
        .contains("reservation.holdExpiresAt < :now")
        .doesNotContain("reservation.holdExpiresAt <= :now");
    assertThat(modifying).isNotNull();
    assertThat(modifying.clearAutomatically()).isTrue();
    assertThat(modifying.flushAutomatically()).isTrue();
  }

  private String normalize(String query) {
    return query.replaceAll("\\s+", " ").trim();
  }
}
