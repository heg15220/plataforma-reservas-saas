package com.reserly.platform.venues.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia explícita de credenciales delegadas con bloqueo para rotaciones atómicas. */
public interface VenuePanelCredentialDao extends JpaRepository<VenuePanelCredentialEntity, UUID> {

  /** Obtiene la credencial visible en el listado administrativo de locales del propietario. */
  @Query(
      """
      select credential
      from VenuePanelCredentialEntity credential
      join fetch credential.user
      where credential.venue.id = :venueId
      """)
  Optional<VenuePanelCredentialEntity> findByVenueId(@Param("venueId") UUID venueId);

  /** Serializa creación o rotación de las credenciales de un local concreto. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select credential
      from VenuePanelCredentialEntity credential
      join fetch credential.user
      where credential.venue.id = :venueId
      """)
  Optional<VenuePanelCredentialEntity> findByVenueIdForUpdate(@Param("venueId") UUID venueId);
}
