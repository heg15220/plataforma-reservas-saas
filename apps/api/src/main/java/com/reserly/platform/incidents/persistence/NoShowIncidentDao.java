package com.reserly.platform.incidents.persistence;

import java.util.List;
import java.util.UUID;
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
      order by incident.reportedAt desc, incident.id desc
      """)
  List<NoShowIncidentEntity> findRecentByCustomerEmailNormalized(
      @Param("customerEmailNormalized") String customerEmailNormalized, Pageable pageable);

  /** Cuenta el historial completo para indicar si el tramo devuelto fue truncado. */
  @Query(
      """
      select count(incident)
      from NoShowIncidentEntity incident
      where incident.customerEmailNormalized = :customerEmailNormalized
      """)
  long countByCustomerEmailNormalized(
      @Param("customerEmailNormalized") String customerEmailNormalized);
}
