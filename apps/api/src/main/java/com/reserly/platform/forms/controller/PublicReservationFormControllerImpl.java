package com.reserly.platform.forms.controller;

import com.reserly.platform.forms.dto.PublicReservationFormResponse;
import com.reserly.platform.forms.service.PublicReservationFormService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST sin estado del esquema público de reserva. */
@RestController
public class PublicReservationFormControllerImpl implements PublicReservationFormController {
  private final PublicReservationFormService service;

  public PublicReservationFormControllerImpl(PublicReservationFormService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<PublicReservationFormResponse> find(String slug) {
    return ResponseEntity.ok(service.findPublishedByVenueSlug(slug));
  }
}
