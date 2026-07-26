package com.reserly.platform.reservations.service;

import com.reserly.platform.forms.persistence.ReservationFormResponseDao;
import com.reserly.platform.forms.persistence.ReservationFormResponseEntity;
import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de lectura privada con filtros normalizados y límites defensivos. */
@Service
public class VenueReservationServiceImpl implements VenueReservationService {

  private static final int MAX_PAGE = 100_000;
  private static final int MAX_PAGE_SIZE = 100;
  private static final int MAX_USER_FILTER_LENGTH = 320;
  private static final int MAX_INCIDENT_HISTORY = 50;
  private static final Set<String> VISIBLE_STATUSES =
      Set.of(
          "confirmed",
          "cancelled_by_user",
          "cancelled_by_venue",
          "attended",
          "no_show",
          "reported");

  private final ReservationDao reservationDao;
  private final ReservationFormResponseDao formResponseDao;
  private final EmployeeResourceDao employeeResourceDao;
  private final NoShowIncidentDao incidentDao;

  public VenueReservationServiceImpl(
      ReservationDao reservationDao,
      ReservationFormResponseDao formResponseDao,
      EmployeeResourceDao employeeResourceDao,
      NoShowIncidentDao incidentDao) {
    this.reservationDao = reservationDao;
    this.formResponseDao = formResponseDao;
    this.employeeResourceDao = employeeResourceDao;
    this.incidentDao = incidentDao;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReservationEntity> list(
      UUID ownerUserId,
      String periodValue,
      LocalDate anchorDate,
      UUID timeSlotId,
      String statusValue,
      String user,
      int page,
      int size) {
    requireOwner(ownerUserId);
    validatePagination(page, size);
    VenueReservationPeriod period = resolvePeriod(periodValue, anchorDate);
    DateRange dateRange = resolveDateRange(period, anchorDate);
    String status = normalizeStatus(statusValue);
    String userPattern = normalizeUserPattern(user);
    return reservationDao.findOwnedReservations(
        ownerUserId,
        dateRange.from(),
        dateRange.toExclusive(),
        timeSlotId,
        status,
        userPattern,
        PageRequest.of(page, size));
  }

  @Override
  @Transactional(readOnly = true)
  public VenueReservationDetail findDetail(UUID ownerUserId, UUID reservationId) {
    requireOwner(ownerUserId);
    if (reservationId == null) {
      throw new VenueReservationNotFoundException();
    }
    ReservationEntity reservation =
        reservationDao
            .findOwnedDetail(ownerUserId, reservationId)
            .orElseThrow(VenueReservationNotFoundException::new);
    List<ReservationFormResponseEntity> formResponses =
        formResponseDao.findAllByReservationId(reservationId);
    EmployeeResourceEntity assignedResource =
        findAssignedResource(ownerUserId, reservation.getEmployeeResourceId());
    String customerEmailNormalized = reservation.getCustomerEmailNormalized();
    List<NoShowIncidentEntity> incidents =
        incidentDao.findRecentByCustomerEmailNormalized(
            customerEmailNormalized, PageRequest.of(0, MAX_INCIDENT_HISTORY));
    long incidentTotal = incidentDao.countByCustomerEmailNormalized(customerEmailNormalized);
    return new VenueReservationDetail(
        reservation, formResponses, assignedResource, incidentTotal, incidents);
  }

  private EmployeeResourceEntity findAssignedResource(UUID ownerUserId, UUID resourceId) {
    if (resourceId == null) {
      return null;
    }
    return employeeResourceDao
        .findOwnedHistoricalReference(ownerUserId, resourceId)
        .orElseThrow(VenueReservationNotFoundException::new);
  }

  private VenueReservationPeriod resolvePeriod(String value, LocalDate anchorDate) {
    VenueReservationPeriod parsed = VenueReservationPeriod.parse(value).orElse(null);
    if (parsed == null && anchorDate != null) {
      return VenueReservationPeriod.DAY;
    }
    if (parsed != null && anchorDate == null) {
      throw new VenueReservationFilterInvalidException();
    }
    return parsed;
  }

  private DateRange resolveDateRange(VenueReservationPeriod period, LocalDate anchorDate) {
    if (period == null) {
      return new DateRange(null, null);
    }
    return switch (period) {
      case DAY -> new DateRange(anchorDate, anchorDate.plusDays(1));
      case WEEK -> {
        LocalDate monday =
            anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        yield new DateRange(monday, monday.plusWeeks(1));
      }
      case MONTH -> {
        LocalDate firstDay = anchorDate.withDayOfMonth(1);
        yield new DateRange(firstDay, firstDay.plusMonths(1));
      }
    };
  }

  private String normalizeStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (!VISIBLE_STATUSES.contains(normalized)) {
      throw new VenueReservationFilterInvalidException();
    }
    return normalized;
  }

  private String normalizeUserPattern(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() > MAX_USER_FILTER_LENGTH) {
      throw new VenueReservationFilterInvalidException();
    }
    return "%" + escapeLike(normalized) + "%";
  }

  private String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_PAGE_SIZE) {
      throw new VenueReservationFilterInvalidException();
    }
  }

  private void requireOwner(UUID ownerUserId) {
    if (ownerUserId == null) {
      throw new VenueReservationNotFoundException();
    }
  }

  private record DateRange(LocalDate from, LocalDate toExclusive) {}
}
