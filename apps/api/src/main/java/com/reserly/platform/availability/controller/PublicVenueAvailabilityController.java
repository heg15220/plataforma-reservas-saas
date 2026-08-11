package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
      @PathVariable @Size(max = 160) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @Size(max = 35) String locale,
      @RequestHeader(name = "Accept-Language", required = false) @Size(max = 256)
          String acceptLanguage);
}
