package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.converter.VenueBookingRuleConverter;
import com.reserly.platform.incidents.dto.VenueBookingRuleResponse;
import com.reserly.platform.incidents.dto.VenueBookingRuleUpdateRequest;
import com.reserly.platform.incidents.service.VenueBookingRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST sin lógica de propiedad ni exposición de entidades. */
@RestController
public class VenueBookingRuleControllerImpl implements VenueBookingRuleController {

  private final VenueBookingRuleService ruleService;
  private final VenueBookingRuleConverter converter;

  public VenueBookingRuleControllerImpl(
      VenueBookingRuleService ruleService, VenueBookingRuleConverter converter) {
    this.ruleService = ruleService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<VenueBookingRuleResponse> get(AuthenticatedAccount account) {
    return ResponseEntity.ok(converter.toResponse(ruleService.get(account.userId())));
  }

  @Override
  public ResponseEntity<VenueBookingRuleResponse> update(
      AuthenticatedAccount account, VenueBookingRuleUpdateRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(ruleService.update(account.userId(), request)));
  }
}
