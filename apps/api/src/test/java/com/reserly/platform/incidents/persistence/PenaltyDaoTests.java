package com.reserly.platform.incidents.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Protege las consultas de serialización, vigencia y reinicio sin arrancar PostgreSQL. */
class PenaltyDaoTests {

  @Test
  void identityLockUsesTransactionScopedPostgresqlAdvisoryLock() throws Exception {
    Method method = PenaltyDao.class.getMethod("lockGlobalIdentity", String.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value())
        .contains("pg_advisory_xact_lock")
        .contains("hashtextextended")
        .contains(":customerEmailNormalized");
  }

  @Test
  void activePenaltyUpdateIsPessimisticAndResetRequiresCompletedSixtyDayTier() throws Exception {
    Method active = PenaltyDao.class.getMethod("findActiveGlobalForUpdate", String.class);
    Method reset =
        PenaltyDao.class.getMethod("findLatestCompletedResetBoundary", String.class, Instant.class);

    assertThat(active.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    assertThat(active.getAnnotation(Query.class).value())
        .contains("penalty.scope = 'global'")
        .contains("penalty.status = 'active'");
    assertThat(reset.getAnnotation(Query.class).value())
        .contains("penalty.incidentCountOperational >= 4")
        .contains("penalty.endsAt <= :now");
  }
}
