package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación serializada que valida propiedad, finalización y máquina de estados. */
@Service
public class AttendanceServiceImpl implements AttendanceService {

  private static final Set<String> MARKABLE_STATUSES = Set.of("confirmed", "attended", "no_show");
  private static final Set<String> ATTENDANCE_VALUES = Set.of("attended", "no_show", "pending");

  private final ReservationDao reservationDao;
  private final Clock clock;

  public AttendanceServiceImpl(ReservationDao reservationDao, Clock clock) {
    this.reservationDao = reservationDao;
    this.clock = clock;
  }

  @Override
  @Transactional
  public ReservationEntity update(
      UUID ownerUserId, UUID reservationId, AttendanceUpdateRequest request) {
    if (ownerUserId == null || reservationId == null) {
      throw new AttendanceNotFoundException();
    }
    String requestedStatus = normalizeStatus(request);
    ReservationEntity reservation =
        reservationDao
            .findOwnedForAttendanceUpdate(ownerUserId, reservationId)
            .orElseThrow(AttendanceNotFoundException::new);
    if (!MARKABLE_STATUSES.contains(reservation.getStatus())) {
      throw new AttendanceInvalidException();
    }

    Instant now = clock.instant();
    Instant reservationEnd =
        reservation.getDate().atTime(reservation.getEndsAt()).atZone(clock.getZone()).toInstant();
    if (now.isBefore(reservationEnd)) {
      throw new AttendanceTooEarlyException();
    }

    boolean pending = "pending".equals(requestedStatus);
    reservation.setStatus(pending ? "confirmed" : requestedStatus);
    reservation.setAttendanceMarkedAt(now);
    reservation.setUpdatedAt(now);
    return reservationDao.saveAndFlush(reservation);
  }

  private String normalizeStatus(AttendanceUpdateRequest request) {
    if (request == null || request.status() == null) {
      throw new AttendanceInvalidException();
    }
    String normalized = request.status().trim().toLowerCase(Locale.ROOT);
    if (!ATTENDANCE_VALUES.contains(normalized)) {
      throw new AttendanceInvalidException();
    }
    return normalized;
  }
}
