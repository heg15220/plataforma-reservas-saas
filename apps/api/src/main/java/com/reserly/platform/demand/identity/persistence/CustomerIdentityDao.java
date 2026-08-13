package com.reserly.platform.demand.identity.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a identidades de cliente seudónimas, siempre mediante HMAC y versión de clave. */
public interface CustomerIdentityDao extends JpaRepository<CustomerIdentityEntity, UUID> {

  /** Localiza la identidad exacta durante una rotación controlada; nunca acepta email en claro. */
  @Query(
      """
      select identity
      from CustomerIdentityEntity identity
      where identity.emailHmac = :emailHmac
        and identity.keyVersion = :keyVersion
      """)
  Optional<CustomerIdentityEntity> findByVersionedHmac(
      @Param("emailHmac") String emailHmac, @Param("keyVersion") String keyVersion);

  /**
   * Devuelve una identidad personalizable solo si el consentimiento sigue vigente y dentro de
   * retención. La comprobación debe repetirse bajo la transacción que cree cualquier derivado.
   */
  @Query(
      """
      select identity
      from CustomerIdentityEntity identity
      where identity.id = :identityId
        and identity.personalizationConsentedAt is not null
        and identity.personalizationRevokedAt is null
        and identity.retentionExpiresAt > :now
      """)
  Optional<CustomerIdentityEntity> findPersonalizableById(
      @Param("identityId") UUID identityId, @Param("now") Instant now);

  /** Lote acotado de identidades cuyo plazo de conservación ya finalizó. */
  @Query(
      """
      select identity
      from CustomerIdentityEntity identity
      where identity.retentionExpiresAt <= :cutoff
      order by identity.retentionExpiresAt, identity.id
      """)
  List<CustomerIdentityEntity> findRetentionExpired(
      @Param("cutoff") Instant cutoff, Pageable pageable);
}
