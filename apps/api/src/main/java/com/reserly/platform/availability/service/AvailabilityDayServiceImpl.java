package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.dto.AvailabilityDayResponse;
import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.AvailabilityBlockEntity;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de excepciones de día completo basada en AvailabilityBlocks. */
@Service
public class AvailabilityDayServiceImpl implements AvailabilityDayService {

  private static final String SCOPE_VENUE = "venue";
  private static final String KIND_CLOSED_DAY = "closed_day";
  private static final String KIND_RESERVATIONS_DISABLED = "reservations_disabled";

  private final VenueDao venueDao;
  private final AvailabilityBlockDao availabilityBlockDao;

  public AvailabilityDayServiceImpl(VenueDao venueDao, AvailabilityBlockDao availabilityBlockDao) {
    this.venueDao = venueDao;
    this.availabilityBlockDao = availabilityBlockDao;
  }

  @Override
  @Transactional(readOnly = true)
  public AvailabilityDayResponse find(UUID ownerUserId, LocalDate date) {
    requireDate(date);
    requireCurrentVenue(ownerUserId);
    return availabilityBlockDao
        .findOwnedDayOverride(ownerUserId, date)
        .map(this::toResponse)
        .orElseGet(() -> new AvailabilityDayResponse(date, false, true, "weekly", null, null));
  }

  @Override
  @Transactional
  public AvailabilityDayResponse replace(UUID ownerUserId, AvailabilityDayRequest request) {
    validate(request);
    VenueEntity venue = requireCurrentVenueForUpdate(ownerUserId);
    List<AvailabilityBlockEntity> existing =
        availabilityBlockDao.findOwnedDayOverridesForUpdate(ownerUserId, request.date());
    if (request.closed() || !request.reservationsEnabled()) {
      AvailabilityBlockEntity block =
          existing.isEmpty() ? new AvailabilityBlockEntity() : existing.get(0);
      if (block.getId() == null) {
        block.setVenue(venue);
        UserEntity createdBy = new UserEntity();
        createdBy.setId(ownerUserId);
        block.setCreatedByUser(createdBy);
        block.setCreatedAt(Instant.now());
      }
      block.setScope(SCOPE_VENUE);
      block.setKind(request.closed() ? KIND_CLOSED_DAY : KIND_RESERVATIONS_DISABLED);
      block.setDate(request.date());
      block.setStartsAt(null);
      block.setEndsAt(null);
      block.setReason(normalizeReason(request.reason()));
      if (existing.size() > 1) {
        availabilityBlockDao.deleteAll(existing.subList(1, existing.size()));
      }
      return toResponse(availabilityBlockDao.saveAndFlush(block));
    }
    availabilityBlockDao.deleteAll(existing);
    return new AvailabilityDayResponse(request.date(), false, true, "weekly", null, null);
  }

  private VenueEntity requireCurrentVenue(UUID ownerUserId) {
    return venueDao
        .findCurrentByOwnerUserId(ownerUserId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private VenueEntity requireCurrentVenueForUpdate(UUID ownerUserId) {
    return venueDao
        .findCurrentByOwnerUserIdForUpdate(ownerUserId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private void validate(AvailabilityDayRequest request) {
    if (request == null) {
      throw new AvailabilityDayInvalidException();
    }
    requireDate(request.date());
    if (request.closed() && request.reservationsEnabled()) {
      throw new AvailabilityDayInvalidException();
    }
  }

  private void requireDate(LocalDate date) {
    if (date == null) {
      throw new AvailabilityDayInvalidException();
    }
  }

  private AvailabilityDayResponse toResponse(AvailabilityBlockEntity block) {
    boolean closed = KIND_CLOSED_DAY.equals(block.getKind());
    return new AvailabilityDayResponse(
        block.getDate(), closed, false, "override", block.getId(), block.getReason());
  }

  private String normalizeReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    return reason.strip();
  }
}
