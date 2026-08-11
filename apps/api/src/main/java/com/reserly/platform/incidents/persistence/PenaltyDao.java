package com.reserly.platform.incidents.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso explícito a restricciones activas por identidad normalizada. */
public interface PenaltyDao extends JpaRepository<PenaltyEntity, UUID> {

  /** Cuenta restricciones vigentes sin cargar emails normalizados. */
  @Query(
      """
      select count(penalty) from PenaltyEntity penalty
      where penalty.status = 'active'
        and penalty.anonymizedAt is null
        and penalty.endsAt > :now
      """)
  long countAdminActive(@Param("now") Instant now);

  /** Listado administrativo reciente y acotado de restricciones. */
  @Query(
      "select penalty from PenaltyEntity penalty "
          + "where penalty.anonymizedAt is null "
          + "order by penalty.updatedAt desc, penalty.id desc")
  List<PenaltyEntity> findAdminPage(Pageable pageable);

  /** Serializa una modificación administrativa de una penalización. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select penalty from PenaltyEntity penalty "
          + "where penalty.id = :penaltyId and penalty.anonymizedAt is null")
  Optional<PenaltyEntity> findByIdForAdminUpdate(@Param("penaltyId") UUID penaltyId);

  /**
   * Serializa decisiones para una identidad incluso cuando aún no existe fila de penalización.
   *
   * <p>El lock asesor es transaccional y usa el hash de PostgreSQL únicamente como clave de
   * coordinación; el email continúa comparándose completo en las consultas de dominio.
   */
  @Query(
      value =
          """
          select pg_advisory_xact_lock(
            hashtextextended(cast(:customerEmailNormalized as text), 0)
          )
          """,
      nativeQuery = true)
  void lockGlobalIdentity(@Param("customerEmailNormalized") String customerEmailNormalized);

  /** Obtiene y bloquea la única fila global activa, aunque su plazo ya haya finalizado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select penalty
      from PenaltyEntity penalty
      where penalty.customerEmailNormalized = :customerEmailNormalized
        and penalty.scope = 'global'
        and penalty.anonymizedAt is null
        and penalty.status = 'active'
      """)
  Optional<PenaltyEntity> findActiveGlobalForUpdate(
      @Param("customerEmailNormalized") String customerEmailNormalized);

  /**
   * Busca la penalización global todavía vigente sin incluir restricciones cerradas o revocadas.
   */
  @Query(
      """
      select penalty
      from PenaltyEntity penalty
      where penalty.customerEmailNormalized = :customerEmailNormalized
        and penalty.scope = 'global'
        and penalty.anonymizedAt is null
        and penalty.status = 'active'
        and penalty.endsAt > :now
      """)
  Optional<PenaltyEntity> findActiveGlobal(
      @Param("customerEmailNormalized") String customerEmailNormalized, @Param("now") Instant now);

  /**
   * Devuelve el final del último bloqueo de 60 días completado. Esa frontera reinicia el contador
   * operativo sin borrar ni alterar el histórico auditable.
   */
  @Query(
      """
      select max(penalty.endsAt)
      from PenaltyEntity penalty
      where penalty.customerEmailNormalized = :customerEmailNormalized
        and penalty.scope = 'global'
        and penalty.anonymizedAt is null
        and penalty.incidentCountOperational >= 4
        and penalty.endsAt <= :now
        and penalty.status in ('active', 'expired')
      """)
  Optional<Instant> findLatestCompletedResetBoundary(
      @Param("customerEmailNormalized") String customerEmailNormalized, @Param("now") Instant now);

  /** Desidentifica restricciones finalizadas y las excluye de cualquier decisión futura. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE "Penalties"
          SET "customerEmailNormalized" = 'retained-' || "id"::text || '@anonymous.invalid',
              "incidentCountOperational" = 0,
              "status" = CASE WHEN "status" = 'active' THEN 'expired' ELSE "status" END,
              "reason" = 'retention_anonymized',
              "anonymizedAt" = :anonymizedAt,
              "updatedAt" = :anonymizedAt
          WHERE "anonymizedAt" IS NULL
            AND "endsAt" < :operationalCutoff
          """,
      nativeQuery = true)
  int anonymizeOperationalHistory(
      @Param("operationalCutoff") Instant operationalCutoff,
      @Param("anonymizedAt") Instant anonymizedAt);

  /** Elimina primero penalizaciones vencidas para liberar la FK de su incidencia origen. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          DELETE FROM "Penalties"
          WHERE "anonymizedAt" IS NOT NULL
            AND "endsAt" < :evidenceCutoff
          """,
      nativeQuery = true)
  int deleteExpiredEvidence(@Param("evidenceCutoff") Instant evidenceCutoff);
}
