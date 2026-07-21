package com.reserly.platform.forms.controller;

import com.reserly.platform.forms.dto.PublicReservationFormResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Contrato anónimo que nunca expone formularios en borrador. */
public interface PublicReservationFormController {
  @GetMapping("/api/public/venues/{slug}/reservation-form")
  ResponseEntity<PublicReservationFormResponse> find(@PathVariable String slug);
}
