package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueEmailAssignmentRequest;
import com.reserly.platform.venues.dto.VenueEmailAssignmentResponse;
import com.reserly.platform.venues.dto.VenueEmailAssignmentsResponse;
import com.reserly.platform.venues.service.VenueEmailAssignmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP que deriva el propietario exclusivamente de la sesión. */
@RestController
public class VenueEmailAssignmentControllerImpl implements VenueEmailAssignmentController {

  private final VenueEmailAssignmentService service;

  public VenueEmailAssignmentControllerImpl(VenueEmailAssignmentService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueEmailAssignmentsResponse> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(new VenueEmailAssignmentsResponse(service.list(account.userId())));
  }

  @Override
  public ResponseEntity<VenueEmailAssignmentResponse> update(
      AuthenticatedAccount account, UUID venueId, VenueEmailAssignmentRequest request) {
    return ResponseEntity.ok(
        service.update(account.userId(), venueId, request.email(), request.password()));
  }
}
