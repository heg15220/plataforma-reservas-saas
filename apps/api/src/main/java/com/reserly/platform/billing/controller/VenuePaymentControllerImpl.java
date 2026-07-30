package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.VenuePaymentHistoryResponse;
import com.reserly.platform.billing.service.VenuePaymentHistoryService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Deriva el propietario de la sesión y no acepta identificadores manipulables en la URL. */
@RestController
public class VenuePaymentControllerImpl implements VenuePaymentController {

  private final VenuePaymentHistoryService service;

  public VenuePaymentControllerImpl(VenuePaymentHistoryService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenuePaymentHistoryResponse> get(AuthenticatedAccount account) {
    return ResponseEntity.ok(service.findOwned(account.userId()));
  }
}
