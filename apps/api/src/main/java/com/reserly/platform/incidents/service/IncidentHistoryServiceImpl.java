package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protege el historial profesional mediante una reserva propia y una ventana móvil de doce meses.
 *
 * <p>El endpoint nunca recibe ni devuelve el email. Tampoco expone referencias a otras reservas,
 * locales, actores o notas.
 */
@Service
public class IncidentHistoryServiceImpl implements IncidentHistoryService {

  private static final int MAX_PAGE = 100_000;
  private static final int MAX_PAGE_SIZE = 50;

  private final ReservationDao reservationDao;
  private final NoShowIncidentDao incidentDao;
  private final Clock clock;

  public IncidentHistoryServiceImpl(
      ReservationDao reservationDao, NoShowIncidentDao incidentDao, Clock clock) {
    this.reservationDao = reservationDao;
    this.incidentDao = incidentDao;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<NoShowIncidentEntity> find(UUID ownerUserId, UUID reservationId, int page, int size) {
    if (ownerUserId == null || reservationId == null) {
      throw new IncidentHistoryNotFoundException();
    }
    if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_PAGE_SIZE) {
      throw new IncidentHistoryInvalidException();
    }
    ReservationEntity reservation =
        reservationDao
            .findOwnedDetail(ownerUserId, reservationId)
            .orElseThrow(IncidentHistoryNotFoundException::new);
    String email = reservation.getCustomerEmailNormalized();
    if (email == null || email.isBlank()) {
      throw new IncidentHistoryNotFoundException();
    }
    Instant cutoff = clock.instant().atZone(clock.getZone()).minusMonths(12).toInstant();
    return incidentDao.findOperationalHistory(email, cutoff, PageRequest.of(page, size));
  }
}
