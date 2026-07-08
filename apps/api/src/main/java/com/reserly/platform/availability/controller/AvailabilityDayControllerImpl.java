package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.dto.AvailabilityDayResponse;
import com.reserly.platform.availability.service.AvailabilityDayService;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de excepciones diarias privadas. */
@RestController
public class AvailabilityDayControllerImpl implements AvailabilityDayController {

  private final AvailabilityDayService availabilityDayService;

  public AvailabilityDayControllerImpl(AvailabilityDayService availabilityDayService) {
    this.availabilityDayService = availabilityDayService;
  }

  @Override
  public ResponseEntity<AvailabilityDayResponse> find(
      AuthenticatedAccount account, LocalDate date) {
    return ResponseEntity.ok(availabilityDayService.find(account.userId(), date));
  }

  @Override
  public ResponseEntity<AvailabilityDayResponse> replace(
      AuthenticatedAccount account, AvailabilityDayRequest request) {
    return ResponseEntity.ok(availabilityDayService.replace(account.userId(), request));
  }
}
