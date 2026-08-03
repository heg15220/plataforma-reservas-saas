package com.reserly.platform.statistics.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import com.reserly.platform.statistics.service.VenueStatisticsService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador que obtiene el propietario solo de la sesión y no acepta IDs de local. */
@RestController
public class VenueStatisticsControllerImpl implements VenueStatisticsController {

  private final VenueStatisticsService service;

  public VenueStatisticsControllerImpl(VenueStatisticsService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueStatisticsResponse> get(
      AuthenticatedAccount account, UUID venueId, String period, LocalDate from, LocalDate to) {
    return ResponseEntity.ok(service.findOwned(account.userId(), venueId, period, from, to));
  }
}
