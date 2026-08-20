package com.reserly.platform.demand.waitlist.controller;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.demand.waitlist.service.WaitlistOfferAcceptanceService;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP fino; el servicio conserva el token, locks y transición en una transacción. */
@RestController
public class WaitlistOfferAcceptanceControllerImpl implements WaitlistOfferAcceptanceController {

  private final WaitlistOfferAcceptanceService service;

  public WaitlistOfferAcceptanceControllerImpl(WaitlistOfferAcceptanceService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<ReservationHoldResponse> accept(
      String offerToken, WaitlistOfferAcceptanceRequest request) {
    return ResponseEntity.ok(service.accept(offerToken, request));
  }
}
