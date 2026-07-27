package com.reserly.platform.incidents.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lecturas explícitas del historial profesional por identidad normalizada. */
public interface NoShowIncidentDao extends JpaRepository<NoShowIncidentEntity, UUID> {

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
        and incident.status in ('reported', 'confirmed')
      """)
  long countByCustomerEmailNormalized(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff);

  /** Página profesional minimizada dentro de la ventana operativa de doce meses. */
  @Query(
      value =
          """
          select incident
          from NoShowIncidentEntity incident
          where incident.customerEmailNormalized = :customerEmailNormalized
            and incident.reportedAt >= :cutoff
            and incident.status in ('reported', 'confirmed')
          order by incident.reportedAt desc, incident.id desc
          """,
      countQuery =
          """
          select count(incident)
          from NoShowIncidentEntity incident
          where incident.customerEmailNormalized = :customerEmailNormalized
            and incident.reportedAt >= :cutoff
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
      """)
  long countOperationalNoShows(
      @Param("customerEmailNormalized") String customerEmailNormalized,
      @Param("cutoff") Instant cutoff);
}
