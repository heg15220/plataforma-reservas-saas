package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.PublicTimeSlotAvailabilityResponse;
import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.AvailabilityBlockEntity;
import com.reserly.platform.availability.persistence.TimeSlotDao;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula disponibilidad pública desde horario semanal, excepciones diarias y estado de franjas.
 */
@Service
public class PublicVenueAvailabilityServiceImpl implements PublicVenueAvailabilityService {

  private static final String STATUS_AVAILABLE = "available";
  private static final String STATUS_FULL = "full";
  private static final String KIND_CLOSED_DAY = "closed_day";
  private static final String KIND_RESERVATIONS_DISABLED = "reservations_disabled";

  private final VenueDao venueDao;
  private final VenueOpeningHourDao openingHourDao;
  private final AvailabilityBlockDao blockDao;
  private final TimeSlotDao timeSlotDao;

  public PublicVenueAvailabilityServiceImpl(
      VenueDao venueDao,
      VenueOpeningHourDao openingHourDao,
      AvailabilityBlockDao blockDao,
      TimeSlotDao timeSlotDao) {
    this.venueDao = venueDao;
    this.openingHourDao = openingHourDao;
    this.blockDao = blockDao;
    this.timeSlotDao = timeSlotDao;
  }

  @Override
  @Transactional(readOnly = true)
  public PublicVenueAvailabilityResponse findBySlug(
      String slug, LocalDate date, SupportedLocale locale) {
    if (slug == null || slug.isBlank() || date == null) {
      throw new TimeSlotInvalidException();
    }
    SupportedLocale resolvedLocale = locale == null ? SupportedLocale.EN : locale;
    VenueEntity venue =
        venueDao.findPublishedBySlug(slug.strip()).orElseThrow(VenueProfileNotFoundException::new);
    int weekday = date.getDayOfWeek().getValue();
    List<TimeSlotEntity> slots = timeSlotDao.findPublishedByVenueIdAndDate(venue.getId(), date);
    List<PublicTimeSlotAvailabilityResponse> publicSlots =
        slots.stream().map(this::toSlotResponse).toList();
    long availableSlotCount =
        publicSlots.stream().filter(PublicTimeSlotAvailabilityResponse::bookingAvailable).count();

    AvailabilityBlockEntity dayOverride =
        blockDao.findPublishedDayOverride(venue.getId(), date).orElse(null);
    VenueOpeningHourEntity openingHour =
        openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), weekday).orElse(null);
    StatusSummary status =
        summarizeStatus(
            venue.getId(),
            date,
            resolvedLocale,
            dayOverride,
            openingHour,
            slots,
            availableSlotCount);
    return new PublicVenueAvailabilityResponse(
        venue.getSlug(),
        date,
        weekday,
        status.code(),
        status.label(),
        status.bookingAvailable(),
        status.closed(),
        status.reservationsEnabled(),
        status.source(),
        Math.toIntExact(availableSlotCount),
        publicSlots);
  }

  private PublicTimeSlotAvailabilityResponse toSlotResponse(TimeSlotEntity slot) {
    boolean bookingAvailable = STATUS_AVAILABLE.equals(slot.getStatus());
    return new PublicTimeSlotAvailabilityResponse(
        slot.getId(),
        slot.getStartsAt(),
        slot.getEndsAt(),
        slot.getCapacity(),
        bookingAvailable ? slot.getCapacity() : 0,
        slot.getStatus(),
        bookingAvailable);
  }

  private StatusSummary summarizeStatus(
      java.util.UUID venueId,
      LocalDate date,
      SupportedLocale locale,
      AvailabilityBlockEntity dayOverride,
      VenueOpeningHourEntity openingHour,
      List<TimeSlotEntity> slots,
      long availableSlotCount) {
    boolean spanish = locale == SupportedLocale.ES;
    if (dayOverride != null) {
      if (KIND_CLOSED_DAY.equals(dayOverride.getKind())) {
        return closed(spanish, "override");
      }
      if (KIND_RESERVATIONS_DISABLED.equals(dayOverride.getKind())) {
        return unavailable(spanish, "override");
      }
    }
    if (openingHour == null || openingHour.isClosed()) {
      return closed(spanish, "weekly");
    }
    if (!openingHour.isReservationsEnabled()) {
      return unavailable(spanish, "weekly");
    }
    if (availableSlotCount > 0) {
      return open(spanish);
    }
    if (!slots.isEmpty() && slots.stream().allMatch(slot -> STATUS_FULL.equals(slot.getStatus()))) {
      return full(spanish);
    }
    if (timeSlotDao.existsPublishedAvailableAfter(venueId, date)) {
      return upcoming(spanish);
    }
    return unavailable(spanish, "slots");
  }

  private static StatusSummary open(boolean spanish) {
    return new StatusSummary("open", spanish ? "Abierto" : "Open", true, false, true, "slots");
  }

  private static StatusSummary closed(boolean spanish, String source) {
    return new StatusSummary("closed", spanish ? "Cerrado" : "Closed", false, true, false, source);
  }

  private static StatusSummary unavailable(boolean spanish, String source) {
    return new StatusSummary(
        "unavailable", spanish ? "No disponible" : "Unavailable", false, false, false, source);
  }

  private static StatusSummary full(boolean spanish) {
    return new StatusSummary("full", spanish ? "Completo" : "Full", false, false, true, "slots");
  }

  private static StatusSummary upcoming(boolean spanish) {
    return new StatusSummary(
        "upcoming_available",
        spanish ? "Próximamente disponible" : "Available soon",
        false,
        false,
        true,
        "future_slots");
  }

  private record StatusSummary(
      String code,
      String label,
      boolean bookingAvailable,
      boolean closed,
      boolean reservationsEnabled,
      String source) {}
}
