package com.reserly.platform.demand.identity.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a UUID anónimos de primera parte; no ofrece búsquedas por señales de dispositivo. */
public interface AnonymousIdentityDao extends JpaRepository<AnonymousIdentityEntity, UUID> {

  /** Exige consentimiento, ausencia de revocación, vigencia y retención antes de personalizar. */
  @Query(
      """
      select identity
      from AnonymousIdentityEntity identity
      where identity.id = :identityId
        and identity.personalizationConsentedAt is not null
        and identity.personalizationRevokedAt is null
        and identity.expiresAt > :now
        and identity.retentionExpiresAt > :now
      """)
  Optional<AnonymousIdentityEntity> findPersonalizableById(
      @Param("identityId") UUID identityId, @Param("now") Instant now);

  /**
   * Actualiza actividad únicamente dentro de la vigencia existente. No extiende expiración ni
   * retención y no reactiva una identidad revocada.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update AnonymousIdentityEntity identity
      set identity.lastSeenAt = :seenAt
      where identity.id = :identityId
        and identity.personalizationRevokedAt is null
        and identity.lastSeenAt <= :seenAt
        and identity.expiresAt > :seenAt
        and identity.retentionExpiresAt > :seenAt
      """)
  int touchActive(@Param("identityId") UUID identityId, @Param("seenAt") Instant seenAt);

  /** Lote acotado de identidades anónimas vencidas para el futuro job de retención. */
  @Query(
      """
      select identity
      from AnonymousIdentityEntity identity
      where identity.retentionExpiresAt <= :cutoff
      order by identity.retentionExpiresAt, identity.id
      """)
  List<AnonymousIdentityEntity> findRetentionExpired(
      @Param("cutoff") Instant cutoff, Pageable pageable);
}
