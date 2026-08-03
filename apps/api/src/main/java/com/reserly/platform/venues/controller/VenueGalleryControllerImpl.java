package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.dto.VenueGalleryImageResponse;
import com.reserly.platform.venues.dto.VenueGalleryOrderRequest;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.persistence.VenueImageEntity;
import com.reserly.platform.venues.service.VenueGalleryService;
import com.reserly.platform.venues.service.VenueMainImageContent;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Adaptador REST que elimina del contrato nombres originales y claves privadas. */
@RestController
public class VenueGalleryControllerImpl implements VenueGalleryController {

  private final VenueGalleryService service;

  public VenueGalleryControllerImpl(VenueGalleryService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<List<VenueGalleryImageResponse>> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(toResponses(service.list(account.userId())));
  }

  @Override
  public ResponseEntity<List<VenueGalleryImageResponse>> listById(
      AuthenticatedAccount account, UUID venueId) {
    return ResponseEntity.ok(toResponses(service.list(account.userId(), venueId)));
  }

  @Override
  public ResponseEntity<VenueGalleryImageResponse> upload(
      AuthenticatedAccount account, String altText, MultipartFile file) {
    return upload(account, null, altText, file);
  }

  @Override
  public ResponseEntity<VenueGalleryImageResponse> uploadById(
      AuthenticatedAccount account, UUID venueId, String altText, MultipartFile file) {
    return upload(account, venueId, altText, file);
  }

  private ResponseEntity<VenueGalleryImageResponse> upload(
      AuthenticatedAccount account, UUID venueId, String altText, MultipartFile file) {
    if (file.isEmpty() || file.getContentType() == null) {
      throw new VenueImageValidationException();
    }
    try {
      VenueImageEntity image =
          venueId == null
              ? service.upload(
                  account.userId(), altText, file.getContentType(), file.getInputStream())
              : service.upload(
                  account.userId(), venueId, altText, file.getContentType(), file.getInputStream());
      return ResponseEntity.created(URI.create(image.getUrl())).body(toResponse(image));
    } catch (IOException exception) {
      throw new VenueImageValidationException();
    }
  }

  @Override
  public ResponseEntity<List<VenueGalleryImageResponse>> reorder(
      AuthenticatedAccount account, VenueGalleryOrderRequest request) {
    return ResponseEntity.ok(toResponses(service.reorder(account.userId(), request.imageIds())));
  }

  @Override
  public ResponseEntity<List<VenueGalleryImageResponse>> reorderById(
      AuthenticatedAccount account, UUID venueId, VenueGalleryOrderRequest request) {
    return ResponseEntity.ok(
        toResponses(service.reorder(account.userId(), venueId, request.imageIds())));
  }

  @Override
  public ResponseEntity<Void> delete(AuthenticatedAccount account, UUID imageId) {
    service.delete(account.userId(), imageId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteById(AuthenticatedAccount account, UUID venueId, UUID imageId) {
    service.delete(account.userId(), venueId, imageId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<byte[]> findPublished(UUID imageId) {
    VenueMainImageContent content = service.findPublished(imageId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.mediaType()))
        .cacheControl(CacheControl.noCache())
        .body(content.bytes());
  }

  private List<VenueGalleryImageResponse> toResponses(List<VenueImageEntity> images) {
    return images.stream().map(this::toResponse).toList();
  }

  private VenueGalleryImageResponse toResponse(VenueImageEntity image) {
    return new VenueGalleryImageResponse(
        image.getId(),
        image.getUrl(),
        image.getAltText(),
        image.getPosition(),
        image.getMediaType(),
        image.getSizeBytes(),
        image.getWidth(),
        image.getHeight());
  }
}
