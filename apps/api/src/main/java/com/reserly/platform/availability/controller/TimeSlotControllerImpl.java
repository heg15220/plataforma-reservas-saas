package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.dto.TimeSlotResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.service.TimeSlotService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de franjas manuales. */
@RestController
public class TimeSlotControllerImpl implements TimeSlotController {

  private final TimeSlotService timeSlotService;

  public TimeSlotControllerImpl(TimeSlotService timeSlotService) {
    this.timeSlotService = timeSlotService;
  }

  @Override
  public ResponseEntity<List<TimeSlotResponse>> list(AuthenticatedAccount account, LocalDate date) {
    return ResponseEntity.ok(
        timeSlotService.list(account.userId(), date).stream().map(this::toResponse).toList());
  }

  @Override
  public ResponseEntity<TimeSlotResponse> create(
      AuthenticatedAccount account, TimeSlotRequest request) {
    TimeSlotEntity slot = timeSlotService.create(account.userId(), request);
    return ResponseEntity.created(URI.create("/api/venue/me/time-slots/" + slot.getId()))
        .body(toResponse(slot));
  }

  private TimeSlotResponse toResponse(TimeSlotEntity slot) {
    return new TimeSlotResponse(
        slot.getId(),
        slot.getDate(),
        slot.getWeekday(),
        slot.getStartsAt(),
        slot.getEndsAt(),
        slot.getCapacity(),
        slot.getStatus(),
        slot.isCreatedByRule(),
        slot.getVersion());
  }
}
