package com.reserly.platform.businessverification.persistence;

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

/**
 * Acceso persistente a identidades empresariales.
 *
 * <p>Las búsquedas futuras por propietario o identificador normalizado deberán usar {@code @Query}
 * y aplicar el alcance de autorización correspondiente.
 */
public interface BusinessAccountDao extends JpaRepository<BusinessAccountEntity, UUID> {

  /** Cola administrativa pendiente con propietario precargado y límite explícito. */
  @Query(
      """
      select account from BusinessAccountEntity account
      join fetch account.ownerUser
      where account.businessVerificationStatus = 'pending_review'
        and account.manualReviewStatus = 'pending_review'
      order by account.updatedAt asc, account.id asc
      """)
  List<BusinessAccountEntity> findPendingAdminReview(Pageable pageable);

  /** Detalle pendiente; no permite consultar identidades fuera de la cola. */
  @Query(
      """
      select account from BusinessAccountEntity account
      join fetch account.ownerUser
      where account.id = :accountId
        and account.businessVerificationStatus = 'pending_review'
        and account.manualReviewStatus = 'pending_review'
      """)
  Optional<BusinessAccountEntity> findPendingAdminReviewById(@Param("accountId") UUID accountId);

  /**
   * Resuelve la identidad empresarial propiedad del actor autenticado sin aceptar IDs del cliente.
   */
  @Query(
      """
      select account
      from BusinessAccountEntity account
      where account.ownerUser.id = :ownerUserId
      """)
  Optional<BusinessAccountEntity> findByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

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

  /**
   * Mantiene estable la elegibilidad durante la transacción que publique un local.
   *
   * <p>La carga anticipada del propietario evita acceder a identidad fuera de la frontera
   * transaccional. El futuro caso de uso de publicación debe invocar la política dentro de su misma
   * transacción para conservar el lock hasta el cambio de visibilidad.
   */
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query(
      """
      select account
      from BusinessAccountEntity account
      join fetch account.ownerUser
      where account.id = :accountId
      """)
  Optional<BusinessAccountEntity> findByIdForPublicationEligibility(
      @Param("accountId") UUID accountId);

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
