package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.converter.VenueRegistrationConverter;
import com.reserly.platform.identity.dto.VenueRegistrationRequest;
import com.reserly.platform.identity.dto.VenueRegistrationResponse;
import com.reserly.platform.identity.service.VenueRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP del caso de uso de registro empresarial. */
@RestController
public class VenueRegistrationControllerImpl implements VenueRegistrationController {

  private final VenueRegistrationConverter converter;
  private final VenueRegistrationService registrationService;

  public VenueRegistrationControllerImpl(
      VenueRegistrationConverter converter, VenueRegistrationService registrationService) {
    this.converter = converter;
    this.registrationService = registrationService;
  }

  @Override
  public ResponseEntity<VenueRegistrationResponse> register(VenueRegistrationRequest request) {
    VenueRegistrationResponse response = registrationService.register(converter.toCommand(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
