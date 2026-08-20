package com.reserly.platform.demand.identity.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a preferencias agregadas y vigentes de una identidad consentida. */
public interface CustomerAttributeProfileDao
    extends JpaRepository<CustomerAttributeProfileEntity, UUID> {

  /** Resuelve una preferencia vigente por código gobernado sin cargar evidencia inexistente. */
  @Query(
      """
      select profile from CustomerAttributeProfileEntity profile
      join fetch profile.demandAttribute attribute
      where profile.customerIdentity.id = :customerIdentityId
        and attribute.code = :attributeCode
        and profile.expiresAt > :now
      """)
  Optional<CustomerAttributeProfileEntity> findCurrent(
      @Param("customerIdentityId") UUID customerIdentityId,
      @Param("attributeCode") String attributeCode,
      @Param("now") Instant now);

  /** Lista acotada y ordenada para personalización después de revalidar consentimiento. */
  @Query(
      """
      select profile from CustomerAttributeProfileEntity profile
      join fetch profile.demandAttribute attribute
      where profile.customerIdentity.id = :customerIdentityId
        and profile.expiresAt > :now
      order by profile.confidence desc, attribute.code
      """)
  List<CustomerAttributeProfileEntity> findCurrentByCustomer(
      @Param("customerIdentityId") UUID customerIdentityId, @Param("now") Instant now);
}
