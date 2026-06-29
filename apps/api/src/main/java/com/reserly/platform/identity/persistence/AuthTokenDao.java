package com.reserly.platform.identity.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso persistente a tokens de un solo uso.
 *
 * <p>Las futuras consultas de consumo deberán declarar mediante {@code @Query} todas las
 * condiciones de propósito, expiración, revocación y consumo.
 */
public interface AuthTokenDao extends JpaRepository<AuthTokenEntity, UUID> {

  /**
   * Bloquea el desafío y su usuario para que dos consumos concurrentes no puedan validarlo.
   *
   * <p>La vigencia y los estados finales se comprueban en servicio bajo este bloqueo.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select token
      from AuthTokenEntity token
      join fetch token.user
      where token.tokenHash = :tokenHash
        and token.purpose = :purpose
      """)
  Optional<AuthTokenEntity> findForConsumption(
      @Param("tokenHash") String tokenHash, @Param("purpose") String purpose);

  /** Revoca desafíos activos anteriores antes de emitir uno nuevo. */
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update AuthTokenEntity token
      set token.revokedAt = :revokedAt
      where token.user.id = :userId
        and token.purpose = :purpose
        and token.consumedAt is null
        and token.revokedAt is null
      """)
  int revokeActiveByUserAndPurpose(
      @Param("userId") UUID userId,
      @Param("purpose") String purpose,
      @Param("revokedAt") Instant revokedAt);

  /** Revoca cualquier desafío hermano al completar correctamente una verificación. */
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update AuthTokenEntity token
      set token.revokedAt = :revokedAt
      where token.user.id = :userId
        and token.purpose = :purpose
        and token.id <> :consumedTokenId
        and token.consumedAt is null
        and token.revokedAt is null
      """)
  int revokeOtherActiveTokens(
      @Param("userId") UUID userId,
      @Param("purpose") String purpose,
      @Param("consumedTokenId") UUID consumedTokenId,
      @Param("revokedAt") Instant revokedAt);
}
