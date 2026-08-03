package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.ReservationOperationalWindow;
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

  private static final Set<String> ATTENDANCE_VALUES = Set.of("attended", "no_show");

  private final ReservationDao reservationDao;
  private final Clock clock;
  private final ReservationOperationalWindow operationalWindow;

  public AttendanceServiceImpl(
      ReservationDao reservationDao, Clock clock, ReservationOperationalWindow operationalWindow) {
    this.reservationDao = reservationDao;
    this.clock = clock;
    this.operationalWindow = operationalWindow;
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
            .findAccessibleForAttendanceUpdate(ownerUserId, reservationId)
            .orElseThrow(AttendanceNotFoundException::new);
    if (!operationalWindow.allowsManualAction(reservation)) {
      throw new AttendanceInvalidException();
    }

    Instant now = clock.instant();
    reservation.setStatus(requestedStatus);
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
