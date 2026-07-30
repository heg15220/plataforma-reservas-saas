package com.reserly.platform.billing.payment;

import com.reserly.platform.billing.PaymentStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resultado neutral de creación de orden.
 *
 * @param provider código persistible del proveedor
 * @param providerOrderId pedido idempotente reconocido por el proveedor
 * @param status resultado inicial
 * @param redirect formulario externo o {@code null} para simulador/resultado no redirigible
 * @param requestPayloadHash SHA-256 canónico sin guardar el request completo
 * @param responsePayload subconjunto sanitizado permitido para persistencia
 */
public record PaymentOrderResult(
    String provider,
    String providerOrderId,
    PaymentStatus status,
    PaymentRedirect redirect,
    String requestPayloadHash,
    Map<String, Object> responsePayload) {

  /** Copia el payload para que el adaptador no pueda mutarlo tras devolver el resultado. */
  public PaymentOrderResult {
    responsePayload =
        responsePayload == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(responsePayload));
  }
}
