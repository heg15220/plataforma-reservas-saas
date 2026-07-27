package com.reserly.platform.incidents.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DAO de reglas acotadas por propietario o por identificador interno acreditado. */
public interface VenueBookingRuleDao extends JpaRepository<VenueBookingRuleEntity, UUID> {

  /** Consulta la regla del único local vigente del propietario autenticado. */
  @Query(
      """
      select rule
      from VenueBookingRuleEntity rule
      join fetch rule.venue venue
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueBookingRuleEntity> findOwned(@Param("ownerUserId") UUID ownerUserId);

  /** Serializa cambios concurrentes de la configuración privada. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select rule
      from VenueBookingRuleEntity rule
      join fetch rule.venue venue
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueBookingRuleEntity> findOwnedForUpdate(@Param("ownerUserId") UUID ownerUserId);

  /** Resuelve la política desde una reserva que ya acreditó el local. */
  @Query(
      """
      select rule
      from VenueBookingRuleEntity rule
      where rule.venue.id = :venueId
      """)
  Optional<VenueBookingRuleEntity> findByVenueId(@Param("venueId") UUID venueId);
}
