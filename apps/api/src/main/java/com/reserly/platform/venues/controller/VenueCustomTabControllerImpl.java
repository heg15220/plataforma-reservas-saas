package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.converter.VenueCustomTabConverter;
import com.reserly.platform.venues.dto.VenueCustomTabOrderRequest;
import com.reserly.platform.venues.dto.VenueCustomTabRequest;
import com.reserly.platform.venues.dto.VenueCustomTabResponse;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.service.VenueCustomTabService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de pestañas que usa siempre el propietario autenticado como frontera. */
@RestController
public class VenueCustomTabControllerImpl implements VenueCustomTabController {

  private final VenueCustomTabService service;
  private final VenueCustomTabConverter converter;

  public VenueCustomTabControllerImpl(
      VenueCustomTabService service, VenueCustomTabConverter converter) {
    this.service = service;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<List<VenueCustomTabResponse>> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponses(service.list(account.userId())));
  }

  @Override
  public ResponseEntity<VenueCustomTabResponse> create(
      AuthenticatedAccount account, VenueCustomTabRequest request) {
    VenueCustomTabEntity tab = service.create(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create("/api/venue/me/custom-tabs/" + tab.getId()))
        .body(converter.toResponse(tab));
  }

  @Override
  public ResponseEntity<VenueCustomTabResponse> update(
      AuthenticatedAccount account, UUID tabId, VenueCustomTabRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            service.update(account.userId(), tabId, converter.toCommand(request))));
  }

  @Override
  public ResponseEntity<List<VenueCustomTabResponse>> reorder(
      AuthenticatedAccount account, VenueCustomTabOrderRequest request) {
    return ResponseEntity.ok(toResponses(service.reorder(account.userId(), request.tabIds())));
  }

  @Override
  public ResponseEntity<Void> delete(AuthenticatedAccount account, UUID tabId) {
    service.delete(account.userId(), tabId);
    return ResponseEntity.noContent().build();
  }

  private List<VenueCustomTabResponse> toResponses(List<VenueCustomTabEntity> tabs) {
    return tabs.stream().map(converter::toResponse).toList();
  }
}
