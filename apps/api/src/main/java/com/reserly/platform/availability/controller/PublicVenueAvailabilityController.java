package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato anónimo de disponibilidad pública de un local publicado. */
@RequestMapping(path = "/api/public/venues", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PublicVenueAvailabilityController {

  /** Devuelve estado operativo y franjas de una fecha para un slug público. */
  @GetMapping("/{slug}/availability")
  ResponseEntity<PublicVenueAvailabilityResponse> find(
      @PathVariable String slug,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String locale,
      @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage);
}
