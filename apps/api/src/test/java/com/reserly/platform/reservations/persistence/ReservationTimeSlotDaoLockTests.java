package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.service.ReservationHoldServiceImpl;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

/** Protege el contrato de bloqueo pesimista usado por la transacción de creación de holds. */
class ReservationTimeSlotDaoLockTests {

  @Test
  void locksPublishedTimeSlotForWrite() throws NoSuchMethodException {
    var method =
        ReservationTimeSlotDao.class.getMethod("findPublishedForUpdate", UUID.class, UUID.class);

    Lock lock = method.getAnnotation(Lock.class);

    assertThat(lock).isNotNull();
    assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
  }

  @Test
  void createsHoldInsideTransactionThatOwnsTheLock() throws NoSuchMethodException {
    var method = ReservationHoldServiceImpl.class.getMethod("create", ReservationHoldRequest.class);

    assertThat(method.getAnnotation(Transactional.class)).isNotNull();
  }
}
