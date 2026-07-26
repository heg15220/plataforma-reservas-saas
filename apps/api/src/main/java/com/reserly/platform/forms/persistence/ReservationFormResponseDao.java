package com.reserly.platform.forms.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de snapshots validados asociados atómicamente a una reserva. */
public interface ReservationFormResponseDao
    extends JpaRepository<ReservationFormResponseEntity, UUID> {

  /**
   * Recupera los snapshots en el orden estable en que fueron persistidos durante la confirmación.
   */
  @Query(
      """
      select response
      from ReservationFormResponseEntity response
      where response.reservationId = :reservationId
      order by response.createdAt asc, response.id asc
      """)
  List<ReservationFormResponseEntity> findAllByReservationId(
      @Param("reservationId") UUID reservationId);
}
