package com.reserly.platform.businessverification.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso persistente a identidades empresariales.
 *
 * <p>Las búsquedas futuras por propietario o identificador normalizado deberán usar {@code @Query}
 * y aplicar el alcance de autorización correspondiente.
 */
public interface BusinessAccountDao extends JpaRepository<BusinessAccountEntity, UUID> {

  /** Comprueba conflictos por país e identificador fiscal canónico. */
  @Query(
      """
      select count(account) > 0
      from BusinessAccountEntity account
      where account.taxCountry = :taxCountry
        and account.businessTaxIdentifierNormalized = :normalizedIdentifier
      """)
  boolean existsByTaxIdentity(
      @Param("taxCountry") String taxCountry,
      @Param("normalizedIdentifier") String normalizedIdentifier);
}
