package com.reserly.platform.reservations.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reservations.converter.VenueReservationConverter;
import com.reserly.platform.reservations.dto.VenueReservationDetailResponse;
import com.reserly.platform.reservations.dto.VenueReservationListResponse;
import com.reserly.platform.reservations.service.VenueReservationService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST que deriva toda frontera de propiedad del principal autenticado. */
@RestController
public class VenueReservationControllerImpl implements VenueReservationController {

  private final VenueReservationService reservationService;
  private final VenueReservationConverter converter;

  public VenueReservationControllerImpl(
      VenueReservationService reservationService, VenueReservationConverter converter) {
    this.reservationService = reservationService;
    this.converter = converter;
  }

  @Override
  public ResponseEntity<VenueReservationListResponse> list(
      AuthenticatedAccount account,
      String period,
      LocalDate date,
      UUID timeSlotId,
      String status,
      String user,
      int page,
      int size) {
    return ResponseEntity.ok(
        converter.toListResponse(
            reservationService.list(
                account.userId(), period, date, timeSlotId, status, user, page, size)));
  }

  @Override
  public ResponseEntity<VenueReservationDetailResponse> findDetail(
      AuthenticatedAccount account, UUID reservationId) {
    return ResponseEntity.ok(
        converter.toDetailResponse(
            reservationService.findDetail(account.userId(), reservationId)));
  }
}
