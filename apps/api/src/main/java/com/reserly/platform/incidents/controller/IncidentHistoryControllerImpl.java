package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.converter.IncidentHistoryConverter;
import com.reserly.platform.incidents.dto.IncidentHistoryResponse;
import com.reserly.platform.incidents.service.IncidentHistoryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador que deriva el propietario del principal y delega autorización y minimización. */
@RestController
public class IncidentHistoryControllerImpl implements IncidentHistoryController {

  private final IncidentHistoryService incidentHistoryService;
  private final IncidentHistoryConverter converter;

  public IncidentHistoryControllerImpl(
      IncidentHistoryService incidentHistoryService, IncidentHistoryConverter converter) {
    this.incidentHistoryService = incidentHistoryService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<IncidentHistoryResponse> find(
      AuthenticatedAccount account, UUID reservationId, int page, int size) {
    return ResponseEntity.ok(
        converter.toResponse(
            incidentHistoryService.find(account.userId(), reservationId, page, size)));
  }
}
