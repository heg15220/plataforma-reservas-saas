package com.reserly.platform.demand.waitlist.service;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;

/** Convierte una oferta vigente en el mismo hold anónimo usado por una reserva ordinaria. */
public interface WaitlistOfferAcceptanceService {

  /**
   * Consume el token una vez dentro de una transacción que conserva los locks hasta el commit.
   *
   * @throws WaitlistOfferUnavailableException ante token, ventana, consentimiento o capacidad no
   *     válidos
   */
  ReservationHoldResponse accept(String offerToken, WaitlistOfferAcceptanceRequest request);
}
