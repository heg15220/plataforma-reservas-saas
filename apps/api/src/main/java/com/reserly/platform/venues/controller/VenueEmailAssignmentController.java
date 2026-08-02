package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueEmailAssignmentRequest;
import com.reserly.platform.venues.dto.VenueEmailAssignmentResponse;
import com.reserly.platform.venues.dto.VenueEmailAssignmentsResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato privado para asignar destinatarios a locales publicados propios. */
@RequestMapping(
    path = "/api/venue/me/email-assignments",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueEmailAssignmentController {

  @GetMapping
  ResponseEntity<VenueEmailAssignmentsResponse> list(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @PutMapping(path = "/{venueId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueEmailAssignmentResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID venueId,
      @Valid @RequestBody VenueEmailAssignmentRequest request);
}
