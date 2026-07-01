package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueGalleryImageResponse;
import com.reserly.platform.venues.dto.VenueGalleryOrderRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/** Contrato privado de galería y entrega pública de cada imagen publicada. */
public interface VenueGalleryController {

  @GetMapping(path = "/api/venue/me/gallery")
  ResponseEntity<List<VenueGalleryImageResponse>> list(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @org.springframework.web.bind.annotation.PostMapping(
      path = "/api/venue/me/gallery",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<VenueGalleryImageResponse> upload(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam String altText,
      @RequestPart("file") MultipartFile file);

  @PutMapping(path = "/api/venue/me/gallery/order")
  ResponseEntity<List<VenueGalleryImageResponse>> reorder(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody VenueGalleryOrderRequest request);

  @DeleteMapping(path = "/api/venue/me/gallery/{imageId}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID imageId);

  @GetMapping(path = "/api/public/venue-gallery-images/{imageId}")
  ResponseEntity<byte[]> findPublished(@PathVariable UUID imageId);
}
