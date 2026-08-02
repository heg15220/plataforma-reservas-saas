package com.reserly.platform.venues.service;

import com.reserly.platform.venues.dto.VenueEmailAssignmentResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional con aislamiento horizontal por propietario y local. */
@Service
public class VenueEmailAssignmentServiceImpl implements VenueEmailAssignmentService {

  private final VenueDao venueDao;

  public VenueEmailAssignmentServiceImpl(VenueDao venueDao) {
    this.venueDao = venueDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueEmailAssignmentResponse> list(UUID ownerUserId) {
    return venueDao.findAllPublishedByOwnerUserId(ownerUserId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public VenueEmailAssignmentResponse update(UUID ownerUserId, UUID venueId, String email) {
    VenueEntity venue =
        venueDao
            .findPublishedOwnedByIdForUpdate(ownerUserId, venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    venue.setNotificationEmail(normalize(email));
    venue.setUpdatedAt(Instant.now());
    return toResponse(venueDao.saveAndFlush(venue));
  }

  private String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  private VenueEmailAssignmentResponse toResponse(VenueEntity venue) {
    return new VenueEmailAssignmentResponse(
        venue.getId(),
        venue.getName(),
        venue.getSlug(),
        venue.getNotificationEmail(),
        venue.getUpdatedAt());
  }
}
