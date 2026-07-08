package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.dto.TimeSlotResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.service.TimeSlotService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de franjas privadas del local. */
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

  @Override
  public ResponseEntity<List<TimeSlotResponse>> generate(
      AuthenticatedAccount account, TimeSlotGenerationRequest request) {
    return ResponseEntity.ok(
        timeSlotService.generate(account.userId(), request).stream()
            .map(this::toResponse)
            .toList());
  }

  @Override
  public ResponseEntity<TimeSlotResponse> updateCapacity(
      AuthenticatedAccount account, UUID slotId, TimeSlotCapacityRequest request) {
    return ResponseEntity.ok(
        toResponse(timeSlotService.updateCapacity(account.userId(), slotId, request)));
  }

  @Override
  public ResponseEntity<TimeSlotResponse> block(AuthenticatedAccount account, UUID slotId) {
    return ResponseEntity.ok(toResponse(timeSlotService.block(account.userId(), slotId)));
  }

  @Override
  public ResponseEntity<TimeSlotResponse> reopen(AuthenticatedAccount account, UUID slotId) {
    return ResponseEntity.ok(toResponse(timeSlotService.reopen(account.userId(), slotId)));
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
