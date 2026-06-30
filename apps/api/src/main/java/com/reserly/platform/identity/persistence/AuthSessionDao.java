package com.reserly.platform.identity.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso persistente a sesiones.
 *
 * <p>Las futuras consultas de autenticación deberán filtrar explícitamente revocación y expiración
 * mediante {@code @Query}.
 */
public interface AuthSessionDao extends JpaRepository<AuthSessionEntity, UUID> {

  /**
   * Revoca idempotentemente la sesión representada por un hash.
   *
   * <p>No carga usuario ni entidad y no distingue secreto desconocido, expirado o ya revocado.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update AuthSessionEntity session
      set session.revokedAt = :revokedAt
      where session.tokenHash = :tokenHash
        and session.revokedAt is null
      """)
  int revokeByTokenHash(
      @Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);

  /**
   * Revoca todas las sesiones de una cuenta después de cambiar su credencial.
   *
   * <p>Incluye sesiones expiradas sin revocar para impedir que una lectura defectuosa las recupere.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update AuthSessionEntity session
      set session.revokedAt = :revokedAt
      where session.user.id = :userId
        and session.revokedAt is null
      """)
  int revokeActiveByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
