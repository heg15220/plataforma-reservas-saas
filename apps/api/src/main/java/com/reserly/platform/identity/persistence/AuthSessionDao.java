package com.reserly.platform.identity.persistence;

import java.time.Instant;
import java.util.Optional;
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
   * Carga una sesión y su cuenta únicamente cuando el secreto sigue vigente y no fue revocado.
   *
   * <p>El join fetch permite construir el principal dentro de una sola frontera transaccional.
   */
  @Query(
      """
      select session
      from AuthSessionEntity session
      join fetch session.user
      where session.tokenHash = :tokenHash
        and session.revokedAt is null
        and session.expiresAt > :now
      """)
  Optional<AuthSessionEntity> findActiveForAuthentication(
      @Param("tokenHash") String tokenHash, @Param("now") Instant now);

  /**
   * Actualiza actividad solo cuando vence el intervalo de escritura y la sesión continúa activa.
   *
   * <p>La condición evita una escritura por petición y no prolonga la caducidad absoluta.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update AuthSessionEntity session
      set session.lastSeenAt = :now
      where session.id = :sessionId
        and session.lastSeenAt <= :updateThreshold
        and session.revokedAt is null
        and session.expiresAt > :now
      """)
  int touchActiveSession(
      @Param("sessionId") UUID sessionId,
      @Param("now") Instant now,
      @Param("updateThreshold") Instant updateThreshold);

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
