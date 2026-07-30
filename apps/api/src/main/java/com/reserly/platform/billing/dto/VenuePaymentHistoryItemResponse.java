package com.reserly.platform.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Movimiento de facturación minimizado visible para el local propietario.
 *
 * @param orderReference referencia externa apta para soporte
 * @param amount importe de la orden
 * @param currency moneda ISO
 * @param status estado canónico del pago
 * @param createdAt fecha de creación del intento
 * @param paidAt fecha de confirmación o {@code null}
 */
public record VenuePaymentHistoryItemResponse(
    String orderReference,
    BigDecimal amount,
    String currency,
    String status,
    Instant createdAt,
    Instant paidAt) {}
