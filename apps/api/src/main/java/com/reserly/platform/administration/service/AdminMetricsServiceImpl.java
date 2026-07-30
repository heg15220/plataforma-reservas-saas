package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminMetricsResponse;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.venues.persistence.VenueDao;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ejecuta únicamente conteos SQL y nunca carga filas o identidades en memoria. */
@Service
public class AdminMetricsServiceImpl implements AdminMetricsService {
  private final VenueDao venueDao;
  private final ReservationDao reservationDao;
  private final BusinessAccountDao businessAccountDao;
  private final SubscriptionDao subscriptionDao;
  private final PenaltyDao penaltyDao;
  private final Clock clock;

  public AdminMetricsServiceImpl(
      VenueDao venueDao,
      ReservationDao reservationDao,
      BusinessAccountDao businessAccountDao,
      SubscriptionDao subscriptionDao,
      PenaltyDao penaltyDao,
      Clock clock) {
    this.venueDao = venueDao;
    this.reservationDao = reservationDao;
    this.businessAccountDao = businessAccountDao;
    this.subscriptionDao = subscriptionDao;
    this.penaltyDao = penaltyDao;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminMetricsResponse snapshot() {
    var now = clock.instant();
    return new AdminMetricsResponse(
        venueDao.count(),
        venueDao.countAdminByStatus("published"),
        venueDao.countAdminByStatus("suspended"),
        reservationDao.count(),
        reservationDao.countAdminByStatus("confirmed"),
        businessAccountDao.count(),
        businessAccountDao.countPendingAdminReview(),
        subscriptionDao.countAdminByStatus(SubscriptionStatus.ACTIVE)
            + subscriptionDao.countAdminByStatus(SubscriptionStatus.TRIAL),
        penaltyDao.countAdminActive(now),
        now);
  }
}
