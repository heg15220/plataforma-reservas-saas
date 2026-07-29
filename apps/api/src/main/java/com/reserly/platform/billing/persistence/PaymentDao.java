package com.reserly.platform.billing.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de pagos con consultas orientadas a idempotencia e historial básico. */
public interface PaymentDao extends JpaRepository<PaymentEntity, UUID> {

  /** Recupera el intento ya registrado para no duplicar una orden reenviada por el proveedor. */
  @Query(
      """
      select payment
      from PaymentEntity payment
      where payment.provider = :provider
        and payment.providerOrderId = :providerOrderId
      """)
  Optional<PaymentEntity> findByProviderOrderId(
      @Param("provider") String provider, @Param("providerOrderId") String providerOrderId);

  /** Historial del local en orden estable y sin cruzar datos de otros propietarios. */
  @Query(
      """
      select payment
      from PaymentEntity payment
      where payment.venueId = :venueId
      order by payment.createdAt desc, payment.id desc
      """)
  List<PaymentEntity> findHistoryByVenueId(@Param("venueId") UUID venueId);
}
