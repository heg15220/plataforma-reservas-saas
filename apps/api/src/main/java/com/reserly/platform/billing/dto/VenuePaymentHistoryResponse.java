package com.reserly.platform.billing.dto;

import java.util.List;

/**
 * Historial básico acotado y ordenado del local.
 *
 * @param payments hasta cincuenta movimientos recientes
 */
public record VenuePaymentHistoryResponse(List<VenuePaymentHistoryItemResponse> payments) {

  /** Conserva una colección inmutable en la frontera HTTP. */
  public VenuePaymentHistoryResponse {
    payments = List.copyOf(payments);
  }
}
