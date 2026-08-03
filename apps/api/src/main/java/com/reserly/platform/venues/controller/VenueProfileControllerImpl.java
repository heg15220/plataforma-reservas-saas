package com.reserly.platform.venues.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.venues.converter.VenueProfileConverter;
import com.reserly.platform.venues.dto.VenueProfileRequest;
import com.reserly.platform.venues.dto.VenueProfileResponse;
import com.reserly.platform.venues.dto.VenueProfilesResponse;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileService;
import com.reserly.platform.venues.service.VenuePublicationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que deriva siempre el alcance del principal autenticado. */
@RestController
public class VenueProfileControllerImpl implements VenueProfileController {

  private final VenueProfileService venueProfileService;
  private final VenuePublicationService publicationService;
  private final VenueProfileConverter converter;

  public VenueProfileControllerImpl(
      VenueProfileService venueProfileService,
      VenuePublicationService publicationService,
      VenueProfileConverter converter) {
    this.venueProfileService = venueProfileService;
    this.publicationService = publicationService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<VenueProfileResponse> find(AuthenticatedAccount account) {
    return ResponseEntity.ok(converter.toResponse(venueProfileService.find(account.userId())));
  }

  @Override
  public ResponseEntity<VenueProfilesResponse> list(AuthenticatedAccount account) {
    return ResponseEntity.ok(
        new VenueProfilesResponse(
            venueProfileService.list(account.userId()).stream().map(converter::toResponse).toList(),
            venueProfileService.canCreateAdditional(account.userId())));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> findById(AuthenticatedAccount account, UUID venueId) {
    return ResponseEntity.ok(
        converter.toResponse(venueProfileService.find(account.userId(), venueId)));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> create(
      AuthenticatedAccount account, VenueProfileRequest request) {
    VenueEntity venue = venueProfileService.create(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create("/api/venue/me")).body(converter.toResponse(venue));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> createAdditional(
      AuthenticatedAccount account, VenueProfileRequest request) {
    VenueEntity venue =
        venueProfileService.createAdditional(account.userId(), converter.toCommand(request));
    return ResponseEntity.created(URI.create("/api/venue/me/profiles/" + venue.getId()))
        .body(converter.toResponse(venue));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> update(
      AuthenticatedAccount account, VenueProfileRequest request) {
    VenueEntity venue = venueProfileService.update(account.userId(), converter.toCommand(request));
    return ResponseEntity.ok(converter.toResponse(venue));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> updateById(
      AuthenticatedAccount account, UUID venueId, VenueProfileRequest request) {
    return ResponseEntity.ok(
        converter.toResponse(
            venueProfileService.update(account.userId(), venueId, converter.toCommand(request))));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> publish(AuthenticatedAccount account) {
    return ResponseEntity.ok(converter.toResponse(publicationService.publish(account.userId())));
  }

  @Override
  public ResponseEntity<VenueProfileResponse> publishById(
      AuthenticatedAccount account, UUID venueId) {
    return ResponseEntity.ok(
        converter.toResponse(publicationService.publish(account.userId(), venueId)));
  }

  @Override
  public ResponseEntity<Void> archive(AuthenticatedAccount account) {
    venueProfileService.archive(account.userId());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> archiveById(AuthenticatedAccount account, UUID venueId) {
    venueProfileService.archive(account.userId(), venueId);
    return ResponseEntity.noContent().build();
  }
}
