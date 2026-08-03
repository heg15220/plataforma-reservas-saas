package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueMainImageResponse;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/** Contrato de carga propia y entrega pública controlada de la imagen principal. */
public interface VenueMainImageController {

  @PostMapping(
      path = "/api/venue/me/main-image",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueMainImageResponse> upload(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestPart("file") MultipartFile file);

  /** Carga la imagen principal de la ficha elegida en el panel multi-local. */
  @PostMapping(
      path = "/api/venue/me/profiles/{venueId}/main-image",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<VenueMainImageResponse> uploadById(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID venueId,
      @RequestPart("file") MultipartFile file);

  @GetMapping(path = "/api/public/venue-images/{venueId}/main")
  ResponseEntity<byte[]> findPublished(@PathVariable UUID venueId);
}
