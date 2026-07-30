package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.VenuePaymentHistoryResponse;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato privado del historial de facturación. Requiere {@code ROLE_VENUE_OWNER}. */
@RequestMapping(path = "/api/venue/me/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenuePaymentController {

  /** Devuelve como máximo cincuenta movimientos recientes del local autenticado. */
  @GetMapping
  ResponseEntity<VenuePaymentHistoryResponse> get(
      @AuthenticationPrincipal AuthenticatedAccount account);
}
