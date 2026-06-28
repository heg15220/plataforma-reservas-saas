package com.reserly.platform.businessverification.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

  /** Bloquea una identidad durante una transición corta para serializar cambios de estado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select account
      from BusinessAccountEntity account
      where account.id = :accountId
      """)
  Optional<BusinessAccountEntity> findByIdForStateUpdate(@Param("accountId") UUID accountId);

  /** Caduca en bloque aprobaciones vencidas sin cargar datos fiscales en memoria. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update BusinessAccountEntity account
      set account.businessVerificationStatus = 'expired',
          account.updatedAt = :now
      where account.businessVerificationStatus = 'verified'
        and account.businessVerificationExpiresAt <= :now
      """)
  int expireVerifiedAccounts(@Param("now") Instant now);
}
