package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueCustomTabOrderRequest;
import com.reserly.platform.venues.dto.VenueCustomTabRequest;
import com.reserly.platform.venues.dto.VenueCustomTabResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato privado para crear, editar, ordenar, activar y desactivar pestañas propias. */
public interface VenueCustomTabController {

  @GetMapping(path = "/api/venue/me/custom-tabs")
  ResponseEntity<List<VenueCustomTabResponse>> list(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @PostMapping(path = "/api/venue/me/custom-tabs")
  ResponseEntity<VenueCustomTabResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueCustomTabRequest request);

  @PutMapping(path = "/api/venue/me/custom-tabs/{tabId}")
  ResponseEntity<VenueCustomTabResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID tabId,
      @Valid @RequestBody VenueCustomTabRequest request);

  @PutMapping(path = "/api/venue/me/custom-tabs/order")
  ResponseEntity<List<VenueCustomTabResponse>> reorder(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueCustomTabOrderRequest request);

  @DeleteMapping(path = "/api/venue/me/custom-tabs/{tabId}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID tabId);
}
