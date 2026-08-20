package com.reserly.platform.demand.waitlist.service;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.demand.waitlist.persistence.WaitlistEntryDao;
import com.reserly.platform.demand.waitlist.persistence.WaitlistEntryEntity;
import com.reserly.platform.demand.waitlist.persistence.WaitlistOfferDao;
import com.reserly.platform.demand.waitlist.persistence.WaitlistOfferEntity;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.service.ReservationHoldInvalidException;
import com.reserly.platform.reservations.service.ReservationHoldService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aceptación serializada que delega toda decisión de capacidad al caso de uso ordinario de hold.
 */
@Service
public class WaitlistOfferAcceptanceServiceImpl implements WaitlistOfferAcceptanceService {

  private final WaitlistOfferDao offerDao;
  private final WaitlistEntryDao entryDao;
  private final ReservationHoldService holdService;
  private final OneTimeTokenService tokenService;
  private final Clock clock;

  @Autowired
  public WaitlistOfferAcceptanceServiceImpl(
      WaitlistOfferDao offerDao,
      WaitlistEntryDao entryDao,
      ReservationHoldService holdService,
      OneTimeTokenService tokenService) {
    this(offerDao, entryDao, holdService, tokenService, Clock.systemUTC());
  }

  WaitlistOfferAcceptanceServiceImpl(
      WaitlistOfferDao offerDao,
      WaitlistEntryDao entryDao,
      ReservationHoldService holdService,
      OneTimeTokenService tokenService,
      Clock clock) {
    this.offerDao = offerDao;
    this.entryDao = entryDao;
    this.holdService = holdService;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  /**
   * Mantiene el lock de oferta mientras {@link ReservationHoldService} bloquea la franja y crea el
   * hold. Cualquier excepción runtime revierte también los cambios de oferta/entrada.
   */
  @Override
  @Transactional
  public ReservationHoldResponse accept(String offerToken, WaitlistOfferAcceptanceRequest request) {
    if (request == null || !tokenService.isValid(offerToken)) {
      throw new WaitlistOfferUnavailableException();
    }
    String tokenHash = tokenService.hash(offerToken);
    WaitlistOfferEntity offer =
        offerDao
            .findByTokenHashForUpdate(tokenHash)
            .orElseThrow(WaitlistOfferUnavailableException::new);
    Instant now = clock.instant();
    if (!("scheduled".equals(offer.getStatus()) || "active".equals(offer.getStatus()))
        || now.isBefore(offer.getAvailableAt())
        || !now.isBefore(offer.getExpiresAt())) {
      throw new WaitlistOfferUnavailableException();
    }

    WaitlistEntryEntity entry =
        entryDao
            .findByIdForUpdate(offer.getWaitlistEntryId())
            .orElseThrow(WaitlistOfferUnavailableException::new);
    if (!("queued".equals(entry.getStatus()) || "offered".equals(entry.getStatus()))
        || entry.getContactRevokedAt() != null) {
      throw new WaitlistOfferUnavailableException();
    }

    ReservationHoldResponse hold;
    try {
      hold =
          holdService.create(
              new ReservationHoldRequest(
                  entry.getVenueId(),
                  entry.getTimeSlotId(),
                  entry.getServiceId(),
                  request.employeeResourceId(),
                  request.assignmentPreference(),
                  entry.getPartySize()));
    } catch (ReservationHoldInvalidException exception) {
      throw new WaitlistOfferUnavailableException();
    }

    offer.setStatus("accepted");
    offer.setAcceptedReservationId(hold.reservationId());
    offer.setUpdatedAt(now);
    entry.setStatus("accepted");
    entry.setUpdatedAt(now);
    offerDao.save(offer);
    entryDao.save(entry);
    return hold;
  }
}
