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

/** Acceso a vínculos seudónimos limitado por finalidad, consentimiento, revocación y retención. */
public interface IdentityLinkDao extends JpaRepository<IdentityLinkEntity, UUID> {

  /** Resuelve el replay de una sesión/finalidad sin volver a derivar ni crear otro vínculo. */
  @Query(
      """
      select link
      from IdentityLinkEntity link
      join fetch link.customerIdentity
      join fetch link.anonymousIdentity
      where link.sessionId = :sessionId
        and link.purpose = :purpose
        and link.revokedAt is null
        and link.retentionExpiresAt > :now
      """)
  Optional<IdentityLinkEntity> findActiveBySessionAndPurpose(
      @Param("sessionId") UUID sessionId,
      @Param("purpose") String purpose,
      @Param("now") Instant now);

  /**
   * Resuelve un vínculo utilizable y carga la identidad de cliente en la misma consulta. El
   * llamante todavía debe validar que ambas identidades conservan su consentimiento propio.
   */
  @Query(
      """
      select link
      from IdentityLinkEntity link
      join fetch link.customerIdentity
      join fetch link.anonymousIdentity
      where link.anonymousIdentity.id = :anonymousIdentityId
        and link.purpose = :purpose
        and link.revokedAt is null
        and link.retentionExpiresAt > :now
      """)
  Optional<IdentityLinkEntity> findActiveByAnonymousAndPurpose(
      @Param("anonymousIdentityId") UUID anonymousIdentityId,
      @Param("purpose") String purpose,
      @Param("now") Instant now);

  /** Revoca atómicamente todos los vínculos activos de una identidad anónima. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update IdentityLinkEntity link
      set link.revokedAt = :revokedAt
      where link.anonymousIdentity.id = :anonymousIdentityId
        and link.revokedAt is null
        and link.linkedAt <= :revokedAt
      """)
  int revokeActiveByAnonymous(
      @Param("anonymousIdentityId") UUID anonymousIdentityId,
      @Param("revokedAt") Instant revokedAt);

  /**
   * Revoca una finalidad concreta para la identidad de cliente sin afectar la reserva operativa.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update IdentityLinkEntity link
      set link.revokedAt = :revokedAt
      where link.customerIdentity.id = :customerIdentityId
        and link.purpose = :purpose
        and link.revokedAt is null
        and link.linkedAt <= :revokedAt
      """)
  int revokeActiveByCustomerAndPurpose(
      @Param("customerIdentityId") UUID customerIdentityId,
      @Param("purpose") String purpose,
      @Param("revokedAt") Instant revokedAt);

  /** Lote acotado de vínculos vencidos para borrado propagado posterior. */
  @Query(
      """
      select link
      from IdentityLinkEntity link
      where link.retentionExpiresAt <= :cutoff
      order by link.retentionExpiresAt, link.id
      """)
  List<IdentityLinkEntity> findRetentionExpired(@Param("cutoff") Instant cutoff, Pageable pageable);
}
