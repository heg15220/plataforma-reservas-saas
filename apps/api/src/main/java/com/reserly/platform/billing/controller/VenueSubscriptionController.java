package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.VenueSubscriptionResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato privado del panel de suscripción. Requiere {@code ROLE_VENUE_OWNER}. */
@RequestMapping(path = "/api/venue/me/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueSubscriptionController {

  /** Devuelve el plan efectivo y la configuración visible de monetización. */
  @GetMapping
  ResponseEntity<VenueSubscriptionResponse> get(
      @AuthenticationPrincipal AuthenticatedAccount account);
}
