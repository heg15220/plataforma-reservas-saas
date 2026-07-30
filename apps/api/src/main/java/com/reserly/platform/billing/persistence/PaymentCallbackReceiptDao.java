package com.reserly.platform.billing.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Inserta recibos de callback de forma atomica para resolver carreras entre reintentos. */
public interface PaymentCallbackReceiptDao
    extends JpaRepository<PaymentCallbackReceiptEntity, UUID> {

  /**
   * Reserva un payload verificado una sola vez.
   *
   * @return uno para el primer procesamiento o cero para un duplicado exacto
   */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO "PaymentCallbackReceipts" (
            "id", "paymentId", "provider", "providerOrderId", "channel",
            "payloadHash", "outcome", "receivedAt"
          )
          VALUES (
            :id, :paymentId, :provider, :providerOrderId, :channel,
            :payloadHash, :outcome, :receivedAt
          )
          ON CONFLICT ("provider", "providerOrderId", "payloadHash") DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("id") UUID id,
      @Param("paymentId") UUID paymentId,
      @Param("provider") String provider,
      @Param("providerOrderId") String providerOrderId,
      @Param("channel") String channel,
      @Param("payloadHash") String payloadHash,
      @Param("outcome") String outcome,
      @Param("receivedAt") Instant receivedAt);
}
