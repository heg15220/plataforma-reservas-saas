package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.OpeningHourResponse;
import com.reserly.platform.availability.dto.OpeningHoursResponse;
import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.availability.service.OpeningHoursService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST del horario semanal privado. */
@RestController
public class OpeningHoursControllerImpl implements OpeningHoursController {

  private final OpeningHoursService openingHoursService;

  public OpeningHoursControllerImpl(OpeningHoursService openingHoursService) {
    this.openingHoursService = openingHoursService;
  }

  @Override
  public ResponseEntity<OpeningHoursResponse> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponse(openingHoursService.list(account.userId())));
  }

  @Override
  public ResponseEntity<OpeningHoursResponse> replace(
      AuthenticatedAccount account, OpeningHoursUpdateRequest request) {
    return ResponseEntity.ok(toResponse(openingHoursService.replace(account.userId(), request)));
  }

  private OpeningHoursResponse toResponse(List<VenueOpeningHourEntity> days) {
    return new OpeningHoursResponse(
        days.stream()
            .map(
                day ->
                    new OpeningHourResponse(
                        day.getId(),
                        day.getWeekday(),
                        day.isClosed(),
                        day.isReservationsEnabled(),
                        day.getOpensAt(),
                        day.getClosesAt()))
            .toList());
  }
}
