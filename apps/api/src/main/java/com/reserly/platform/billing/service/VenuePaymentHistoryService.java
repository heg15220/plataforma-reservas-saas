package com.reserly.platform.billing.service;

import com.reserly.platform.billing.dto.VenuePaymentHistoryResponse;
import java.util.UUID;

/** Consulta el historial de pagos perteneciente al local del propietario autenticado. */
public interface VenuePaymentHistoryService {

  /**
   * Resuelve el local desde la sesión y devuelve sus movimientos recientes.
   *
   * @throws VenueSubscriptionNotFoundException si el propietario no tiene local vigente
   */
  VenuePaymentHistoryResponse findOwned(UUID ownerUserId);
}
