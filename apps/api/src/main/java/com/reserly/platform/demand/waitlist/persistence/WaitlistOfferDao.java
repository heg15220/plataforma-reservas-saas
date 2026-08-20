package com.reserly.platform.demand.waitlist.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DAO de ofertas con unicidad de replay y bloqueo pesimista por token hasheado. */
public interface WaitlistOfferDao extends JpaRepository<WaitlistOfferEntity, UUID> {

  Optional<WaitlistOfferEntity> findByAllocationRequestIdAndWaitlistEntryId(
      UUID allocationRequestId, UUID waitlistEntryId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select offer from WaitlistOfferEntity offer where offer.offerTokenHash = :tokenHash")
  Optional<WaitlistOfferEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
