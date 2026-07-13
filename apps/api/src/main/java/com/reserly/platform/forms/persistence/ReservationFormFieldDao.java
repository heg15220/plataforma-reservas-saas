package com.reserly.platform.forms.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de campos personalizados acotada siempre al local del propietario autenticado. */
public interface ReservationFormFieldDao extends JpaRepository<ReservationFormFieldEntity, UUID> {

  @Query(
      """
      select field from ReservationFormFieldEntity field
      where field.venue.ownerUser.id = :ownerUserId
        and field.venue.status <> 'archived'
        and field.active = true
      order by field.position, field.id
      """)
  List<ReservationFormFieldEntity> findAllOwned(@Param("ownerUserId") UUID ownerUserId);

  /**
   * Bloquea el conjunto completo para que la reordenación valide y escriba una permutación atómica.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select field from ReservationFormFieldEntity field
      where field.venue.ownerUser.id = :ownerUserId
        and field.venue.status <> 'archived'
        and field.active = true
      order by field.position, field.id
      """)
  List<ReservationFormFieldEntity> findAllOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select field from ReservationFormFieldEntity field
      where field.id = :fieldId
        and field.venue.ownerUser.id = :ownerUserId
        and field.venue.status <> 'archived'
        and field.active = true
      """)
  Optional<ReservationFormFieldEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("fieldId") UUID fieldId);

  @Query(
      """
      select coalesce(max(field.position), -1) from ReservationFormFieldEntity field
      where field.venue.id = :venueId and field.active = true
      """)
  int findLastActivePosition(@Param("venueId") UUID venueId);
}
