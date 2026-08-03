package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueMainImageResponse;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.service.VenueMainImageContent;
import com.reserly.platform.venues.service.VenueMainImageOutcome;
import com.reserly.platform.venues.service.VenueMainImageService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Adaptador multipart que no propaga nombre de fichero ni extensión aportados por el cliente. */
@RestController
public class VenueMainImageControllerImpl implements VenueMainImageController {

  private final VenueMainImageService service;

  public VenueMainImageControllerImpl(VenueMainImageService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueMainImageResponse> upload(
      AuthenticatedAccount account, MultipartFile file) {
    return upload(account, null, file);
  }

  @Override
  public ResponseEntity<VenueMainImageResponse> uploadById(
      AuthenticatedAccount account, UUID venueId, MultipartFile file) {
    return upload(account, venueId, file);
  }

  private ResponseEntity<VenueMainImageResponse> upload(
      AuthenticatedAccount account, UUID venueId, MultipartFile file) {
    if (file.isEmpty() || file.getContentType() == null) {
      throw new VenueImageValidationException();
    }
    try {
      VenueMainImageOutcome outcome =
          venueId == null
              ? service.upload(account.userId(), file.getContentType(), file.getInputStream())
              : service.upload(
                  account.userId(), venueId, file.getContentType(), file.getInputStream());
      return ResponseEntity.ok(
          new VenueMainImageResponse(
              outcome.url(),
              outcome.mediaType(),
              outcome.sizeBytes(),
              outcome.width(),
              outcome.height()));
    } catch (IOException exception) {
      throw new VenueImageValidationException();
    }
  }

  @Override
  public ResponseEntity<byte[]> findPublished(UUID venueId) {
    VenueMainImageContent content = service.findPublished(venueId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.mediaType()))
        .cacheControl(CacheControl.noCache())
        .body(content.bytes());
  }
}
