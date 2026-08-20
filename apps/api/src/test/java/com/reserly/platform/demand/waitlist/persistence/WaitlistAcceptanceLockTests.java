package com.reserly.platform.demand.waitlist.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.demand.waitlist.service.WaitlistOfferAcceptanceServiceImpl;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

/** Protege los dos locks y la transacción que serializan consumo de oferta y creación de hold. */
class WaitlistAcceptanceLockTests {

  @Test
  void locksOfferAndEntryForWrite() throws NoSuchMethodException {
    Lock offerLock =
        WaitlistOfferDao.class
            .getMethod("findByTokenHashForUpdate", String.class)
            .getAnnotation(Lock.class);
    Lock entryLock =
        WaitlistEntryDao.class
            .getMethod("findByIdForUpdate", java.util.UUID.class)
            .getAnnotation(Lock.class);
    assertThat(offerLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    assertThat(entryLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
  }

  @Test
  void acceptanceOwnsTransactionBeforeCallingHoldService() throws NoSuchMethodException {
    var method =
        WaitlistOfferAcceptanceServiceImpl.class.getMethod(
            "accept", String.class, WaitlistOfferAcceptanceRequest.class);
    assertThat(method.getAnnotation(Transactional.class)).isNotNull();
  }
}
