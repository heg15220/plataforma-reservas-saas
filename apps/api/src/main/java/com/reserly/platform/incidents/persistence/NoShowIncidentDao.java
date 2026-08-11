package com.reserly.platform.incidents.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lecturas explícitas del historial profesional por identidad normalizada. */
public interface NoShowIncidentDao extends JpaRepository<NoShowIncidentEntity, UUID> {

  /** Cola administrativa reciente, limitada por el pageable recibido. */
  @Query(
      "select incident from NoShowIncidentEntity incident "
          + "where incident.anonymizedAt is null "
          + "order by incident.reportedAt desc, incident.id desc")
  List<NoShowIncidentEntity> findAdminPage(Pageable pageable);

  /** Serializa una decisión administrativa sobre la incidencia. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select incident from NoShowIncidentEntity incident "
          + "where incident.id = :incidentId and incident.anonymizedAt is null")
  Optional<NoShowIncidentEntity> findByIdForAdminReview(@Param("incidentId") UUID incidentId);

  /**
   * Obtiene el tramo reciente sin cargar referencias sensibles ni aceptar un email arbitrario desde
   * HTTP. El servicio deriva la identidad de una reserva cuya propiedad ya fue acreditada.
   */
  @Query(
      """
      select incident
      from NoShowIncidentEntity incident
      where incident.customerEmailNormalized = :customerEmailNormalized
        and incident.reportedAt >= :cutoff
        and incident.anonymizedAt is null
        and incident.status in ('reported', 'confirmed')
      order by incident.reportedAt desc, incident.id desc
      """)
  List<NoShowIncidentEntity> findRecentByCustomerEmailNormalized(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff,
      Pageable pageable);

  /** Cuenta el historial operativo visible, excluyendo registros antiguos o desestimados. */
  @Query(
      """
      select count(incident)
      from NoShowIncidentEntity incident
      where incident.customerEmailNormalized = :customerEmailNormalized
        and incident.reportedAt >= :cutoff
        and incident.anonymizedAt is null
        and incident.status in ('reported', 'confirmed')
      """)
  long countByCustomerEmailNormalized(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff);

  /**
   * Resume en una sola consulta los correos ya presentes en una página privada de reservas.
   *
   * <p>Solo cuenta estados operativos dentro de la retención visible. El correo se usa como clave
   * interna y no se incorpora a la respuesta de riesgo.
   */
  @Query(
      """
      select incident.customerEmailNormalized as customerEmailNormalized,
             count(incident) as operationalCount,
             sum(case when incident.reportedAt > :recentCutoff then 1 else 0 end) as recentCount
      from NoShowIncidentEntity incident
      where incident.customerEmailNormalized in :customerEmails
        and incident.reportedAt >= :retentionCutoff
        and incident.anonymizedAt is null
        and incident.status in ('reported', 'confirmed')
      group by incident.customerEmailNormalized
      """)
  List<IncidentRiskAggregateProjection> summarizeOperationalRisk(
      @Param("customerEmails") Set<String> customerEmails,
      @Param("retentionCutoff") Instant retentionCutoff,
      @Param("recentCutoff") Instant recentCutoff);

  /** Página profesional minimizada dentro de la ventana operativa de doce meses. */
  @Query(
      value =
          """
          select incident
          from NoShowIncidentEntity incident
          where incident.customerEmailNormalized = :customerEmailNormalized
            and incident.reportedAt >= :cutoff
            and incident.anonymizedAt is null
            and incident.status in ('reported', 'confirmed')
          order by incident.reportedAt desc, incident.id desc
          """,
      countQuery =
          """
          select count(incident)
          from NoShowIncidentEntity incident
          where incident.customerEmailNormalized = :customerEmailNormalized
            and incident.reportedAt >= :cutoff
            and incident.anonymizedAt is null
            and incident.status in ('reported', 'confirmed')
          """)
  Page<NoShowIncidentEntity> findOperationalHistory(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff,
      Pageable pageable);

  /**
   * Cuenta incidencias operativas desde la frontera inclusiva de conservación o de reinicio.
   *
   * <p>La incidencia recién guardada por el reporte forma parte del resultado.
   */
  @Query(
      """
      select count(incident)
      from NoShowIncidentEntity incident
      where incident.customerEmailNormalized = :customerEmailNormalized
        and incident.incidentType = 'no_show'
        and incident.status in ('reported', 'confirmed')
        and incident.reportedAt >= :cutoff
        and incident.anonymizedAt is null
      """)
  long countOperationalNoShows(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff);

  /** Retira email y notas del uso ordinario al vencer la ventana operativa. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE "NoShowIncidents"
          SET "customerEmailNormalized" = 'retained-' || "id"::text || '@anonymous.invalid',
              "notes" = NULL,
              "anonymizedAt" = :anonymizedAt
          WHERE "anonymizedAt" IS NULL
            AND "reportedAt" < :operationalCutoff
          """,
      nativeQuery = true)
  int anonymizeOperationalHistory(
      @Param("operationalCutoff") Instant operationalCutoff,
      @Param("anonymizedAt") Instant anonymizedAt);

  /** Borra evidencia anonimizada vencida cuando ninguna penalización conserva su referencia. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          DELETE FROM "NoShowIncidents" incident
          WHERE incident."anonymizedAt" IS NOT NULL
            AND incident."reportedAt" < :evidenceCutoff
            AND NOT EXISTS (
              SELECT 1 FROM "Penalties" penalty
              WHERE penalty."createdFromIncidentId" = incident."id"
            )
          """,
      nativeQuery = true)
  int deleteExpiredEvidence(@Param("evidenceCutoff") Instant evidenceCutoff);
}
