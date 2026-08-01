package com.reserly.platform.incidents.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.reservations.persistence.ReservationDao;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Protege las condiciones que evitan sobrescribir decisiones manuales. */
class DefaultAttendanceDaoTests {

  @Test
  void updateIsNativeAtomicIdempotentAndUsesConfiguredGracePeriod() throws NoSuchMethodException {
    var method =
        ReservationDao.class.getMethod(
            "markUnresolvedFinishedReservationsAttended", Instant.class, String.class);
    Query query = method.getAnnotation(Query.class);
    Modifying modifying = method.getAnnotation(Modifying.class);
    String sql = normalize(query.value());

    assertThat(query.nativeQuery()).isTrue();
    assertThat(sql)
        .contains("\"status\" = 'attended'")
        .contains("\"attendanceMarkedAt\" = :now")
        .contains("reservation.\"status\" = 'confirmed'")
        .contains("reservation.\"attendanceMarkedAt\" IS NULL")
        .contains("rule.\"autoMarkAttendedAfterMinutes\"")
        .contains("AT TIME ZONE :zoneId")
        .contains("COALESCE")
        .contains("120")
        .contains("<= :now");
    assertThat(modifying.clearAutomatically()).isTrue();
    assertThat(modifying.flushAutomatically()).isTrue();
  }

  private String normalize(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
