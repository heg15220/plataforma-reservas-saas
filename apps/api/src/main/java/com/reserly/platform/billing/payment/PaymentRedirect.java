package com.reserly.platform.billing.payment;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Formulario que el navegador debe publicar directamente en el proveedor externo.
 *
 * <p>Los campos viven solo en memoria y nunca forman parte del payload persistible del pago.
 *
 * @param action endpoint HTTPS del proveedor
 * @param fields campos ocultos que deben enviarse por POST
 */
public record PaymentRedirect(URI action, Map<String, String> fields) {

  /** Valida transporte y crea una copia defensiva de los campos firmados. */
  public PaymentRedirect {
    Objects.requireNonNull(action, "action");
    if (!"https".equalsIgnoreCase(action.getScheme()) || fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("Invalid payment redirect");
    }
    fields = Map.copyOf(new LinkedHashMap<>(fields));
  }
}
