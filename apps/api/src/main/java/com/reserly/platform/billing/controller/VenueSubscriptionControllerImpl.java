package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.VenueSubscriptionResponse;
import com.reserly.platform.billing.service.VenueSubscriptionService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adapta la sesión al caso de uso sin aceptar identificadores de local desde la petición. */
@RestController
public class VenueSubscriptionControllerImpl implements VenueSubscriptionController {

  private final VenueSubscriptionService service;

  public VenueSubscriptionControllerImpl(VenueSubscriptionService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueSubscriptionResponse> get(AuthenticatedAccount account) {
    return ResponseEntity.ok(service.findOwned(account.userId(), account.preferredLocale()));
  }
}
