package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de la suscripción única de cada local. */
public interface SubscriptionDao extends JpaRepository<SubscriptionEntity, UUID> {

  /** Cuenta suscripciones por estado normalizado sin cargar entidades. */
  @Query(
      "select count(subscription) from SubscriptionEntity subscription"
          + " where subscription.status = :status")
  long countAdminByStatus(@Param("status") SubscriptionStatus status);

  /** Obtiene la suscripción por propietario sin aceptar un local arbitrario desde HTTP. */
  @Query(
      """
      select subscription
      from SubscriptionEntity subscription
      where subscription.venueId = :venueId
      """)
  Optional<SubscriptionEntity> findByVenueId(@Param("venueId") UUID venueId);

  /**
   * Bloquea una suscripción antes de aplicar un callback o pago confirmado.
   *
   * <p>Debe invocarse dentro de una transacción corta; el lock serializa callbacks concurrentes.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select subscription from SubscriptionEntity subscription where subscription.id = :id")
  Optional<SubscriptionEntity> findByIdForUpdate(@Param("id") UUID id);
}
