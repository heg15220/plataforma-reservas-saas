package com.reserly.platform.demand.waitlist.controller;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato público que intercambia un token de oferta por un hold ordinario de cinco minutos. */
@RequestMapping(path = "/api/public/waitlist/offers", produces = MediaType.APPLICATION_JSON_VALUE)
public interface WaitlistOfferAcceptanceController {

  /** Consume una oferta vigente sin aceptar local, franja, servicio ni tamaño desde el cliente. */
  @PostMapping(path = "/{offerToken}/accept", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ReservationHoldResponse> accept(
      @PathVariable String offerToken, @Valid @RequestBody WaitlistOfferAcceptanceRequest request);
}
