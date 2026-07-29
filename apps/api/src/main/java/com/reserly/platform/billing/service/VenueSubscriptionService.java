package com.reserly.platform.billing.service;

import com.reserly.platform.billing.dto.VenueSubscriptionResponse;
import java.util.UUID;

/** Consulta privada de suscripción para el propietario autenticado. */
public interface VenueSubscriptionService {

  /**
   * Devuelve plan efectivo, estado, fechas y catálogo localizado.
   *
   * @param ownerUserId usuario derivado de la sesión
   * @param localeValue preferencia persistida de la cuenta
   * @return resumen minimizado sin IDs ni datos de pagos
   * @throws VenueSubscriptionNotFoundException si no existe local vigente
   * @throws VenueSubscriptionUnavailableException si el catálogo persistido es incoherente
   */
  VenueSubscriptionResponse findOwned(UUID ownerUserId, String localeValue);
}
