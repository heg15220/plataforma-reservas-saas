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
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula disponibilidad pública desde horario semanal, excepciones diarias y estado de franjas.
 */
@Service
public class PublicVenueAvailabilityServiceImpl implements PublicVenueAvailabilityService {

  private static final String STATUS_AVAILABLE = "available";
  private static final String STATUS_FULL = "full";
  private static final String STATUS_UNAVAILABLE = "unavailable";
  private static final String KIND_CLOSED_DAY = "closed_day";
  private static final String KIND_RESERVATIONS_DISABLED = "reservations_disabled";

  private final VenueDao venueDao;
  private final VenueOpeningHourDao openingHourDao;
  private final AvailabilityBlockDao blockDao;
  private final TimeSlotDao timeSlotDao;
  private final ServiceDao serviceDao;
  private final EmployeeResourceAvailabilityService employeeResourceAvailabilityService;

  public PublicVenueAvailabilityServiceImpl(
      VenueDao venueDao,
      VenueOpeningHourDao openingHourDao,
      AvailabilityBlockDao blockDao,
      TimeSlotDao timeSlotDao,
      ServiceDao serviceDao,
      EmployeeResourceAvailabilityService employeeResourceAvailabilityService) {
    this.venueDao = venueDao;
    this.openingHourDao = openingHourDao;
    this.blockDao = blockDao;
    this.timeSlotDao = timeSlotDao;
    this.serviceDao = serviceDao;
    this.employeeResourceAvailabilityService = employeeResourceAvailabilityService;
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
    Map<UUID, String> serviceNames = loadServiceNames(venue.getId(), slots, resolvedLocale);
    var resourceAvailability =
        employeeResourceAvailabilityService.resolve(venue.getId(), weekday, slots);
    List<PublicTimeSlotAvailabilityResponse> publicSlots =
        slots.stream()
            .map(
                slot ->
                    toSlotResponse(
                        slot,
                        slot.getServiceId() == null ? null : serviceNames.get(slot.getServiceId()),
                        resourceAvailability.getOrDefault(
                            slot.getId(), EmployeeResourceSlotAvailability.unrestricted())))
            .toList();
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

  private Map<UUID, String> loadServiceNames(
      UUID venueId, List<TimeSlotEntity> slots, SupportedLocale locale) {
    Set<UUID> serviceIds =
        slots.stream()
            .map(TimeSlotEntity::getServiceId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    if (serviceIds.isEmpty()) {
      return Map.of();
    }
    return serviceDao.findPublishedActiveByVenueIdAndIds(venueId, serviceIds).stream()
        .collect(
            Collectors.toUnmodifiableMap(
                ServiceEntity::getId,
                configured -> resolveServiceName(configured, locale),
                (first, ignored) -> first));
  }

  private String resolveServiceName(ServiceEntity service, SupportedLocale locale) {
    if (service.getNameI18n() == null) {
      return service.getName();
    }
    return service.getNameI18n().resolve(locale).orElse(service.getName());
  }

  private PublicTimeSlotAvailabilityResponse toSlotResponse(
      TimeSlotEntity slot,
      String serviceName,
      EmployeeResourceSlotAvailability resourceAvailability) {
    boolean slotAvailable = STATUS_AVAILABLE.equals(slot.getStatus());
    boolean bookingAvailable = slotAvailable && resourceAvailability.requirementsSatisfied();
    String effectiveStatus =
        bookingAvailable || !slotAvailable ? slot.getStatus() : STATUS_UNAVAILABLE;
    return new PublicTimeSlotAvailabilityResponse(
        slot.getId(),
        slot.getServiceId(),
        serviceName,
        slot.getStartsAt(),
        slot.getEndsAt(),
        slot.getCapacity(),
        bookingAvailable ? slot.getCapacity() : 0,
        effectiveStatus,
        bookingAvailable,
        resourceAvailability.employeeResourceRequired(),
        resourceAvailability.anyAvailableResourceAllowed(),
        resourceAvailability.availableEmployeeResources());
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
    if (!slots.isEmpty()) {
      return unavailable(spanish, "slots");
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
