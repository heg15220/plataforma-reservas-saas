package com.reserly.platform.incidents.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.incidents.dto.AttendanceResponse;
import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.incidents.service.AttendanceService;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que deriva el propietario del principal y proyecta un resultado minimizado. */
@RestController
public class AttendanceControllerImpl implements AttendanceController {

  private final AttendanceService attendanceService;

  public AttendanceControllerImpl(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @Override
  public ResponseEntity<AttendanceResponse> update(
      AuthenticatedAccount account, UUID reservationId, AttendanceUpdateRequest request) {
    ReservationEntity reservation =
        attendanceService.update(account.userId(), reservationId, request);
    return ResponseEntity.ok(
        new AttendanceResponse(
            reservation.getId(),
            reservation.getStatus(),
            reservation.getAttendanceMarkedAt(),
            reservation.getUpdatedAt()));
  }
}
