package com.reserly.platform.billing.payment.redsys;

import com.reserly.platform.billing.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Subconjunto verificado y minimizado de una respuesta RedSys.
 *
 * @param paymentId correlacion opaca enviada como datos de comercio
 * @param order numero de pedido
 * @param amount importe en EUR
 * @param responseCode codigo RedSys sin interpretacion visible
 * @param status resultado normalizado
 * @param payloadHash SHA-256 de los parametros Base64URL recibidos
 */
public record VerifiedRedsysCallback(
    UUID paymentId,
    String order,
    BigDecimal amount,
    String responseCode,
    PaymentStatus status,
    String payloadHash) {}
