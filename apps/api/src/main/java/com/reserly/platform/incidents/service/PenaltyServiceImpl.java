package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.incidents.persistence.PenaltyDao;
import com.reserly.platform.incidents.persistence.PenaltyEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene una única penalización global activa por email y reinicia el contador tras completar un
 * tramo de 60 días.
 *
 * <p>El lock asesor transaccional cubre también el primer alta, donde todavía no existe una fila
 * que pueda bloquearse con JPA. Así el reporte y la confirmación observan un orden único por
 * identidad.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PenaltyServiceImpl implements PenaltyService {

  private static final String GLOBAL_SCOPE = "global";
  private static final String ACTIVE_STATUS = "active";
  private static final String EXPIRED_STATUS = "expired";
  private static final String REASON = "operational_no_show_incidents";

  private final PenaltyDao penaltyDao;
  private final NoShowIncidentDao incidentDao;
  private final PenaltyCalculationPolicy calculationPolicy;
  private final Clock clock;

  public PenaltyServiceImpl(
      PenaltyDao penaltyDao,
      NoShowIncidentDao incidentDao,
      PenaltyCalculationPolicy calculationPolicy,
      Clock clock) {
    this.penaltyDao = penaltyDao;
    this.incidentDao = incidentDao;
    this.calculationPolicy = calculationPolicy;
    this.clock = clock;
  }

  @Override
  public PenaltyEntity applyFor(NoShowIncidentEntity incident) {
    requireApplicableIncident(incident);
    String email = normalizeEmail(incident.getCustomerEmailNormalized());
    Instant now = clock.instant();
    penaltyDao.lockGlobalIdentity(email);

    Optional<PenaltyEntity> current = penaltyDao.findActiveGlobalForUpdate(email);
    if (current.isPresent() && !current.orElseThrow().getEndsAt().isAfter(now)) {
      PenaltyEntity expired = current.orElseThrow();
      expired.setStatus(EXPIRED_STATUS);
      expired.setUpdatedAt(now);
      penaltyDao.saveAndFlush(expired);
      current = Optional.empty();
    }

    Instant retentionCutoff = now.atZone(clock.getZone()).minusMonths(12).toInstant();
    Instant resetBoundary =
        penaltyDao
            .findLatestCompletedResetBoundary(email, now)
            .filter(boundary -> boundary.isAfter(retentionCutoff))
            .orElse(retentionCutoff);
    long count = incidentDao.countOperationalNoShows(email, resetBoundary);
    if (count < 1 || count > Integer.MAX_VALUE) {
      throw new IllegalStateException("Operational incident count is invalid");
    }

    PenaltyEntity penalty = current.orElseGet(PenaltyEntity::new);
    penalty.setCustomerEmailNormalized(email);
    penalty.setScope(GLOBAL_SCOPE);
    penalty.setVenueId(null);
    penalty.setIncidentCountOperational((int) count);
    penalty.setStartsAt(now);
    penalty.setEndsAt(now.plus(calculationPolicy.durationFor((int) count)));
    penalty.setStatus(ACTIVE_STATUS);
    penalty.setReason(REASON);
    penalty.setCreatedFromIncidentId(incident.getId());
    if (penalty.getCreatedAt() == null) {
      penalty.setCreatedAt(now);
    }
    penalty.setUpdatedAt(now);
    return penaltyDao.saveAndFlush(penalty);
  }

  @Override
  public void requireBookingAllowed(String customerEmailNormalized) {
    String email = normalizeEmail(customerEmailNormalized);
    Instant now = clock.instant();
    penaltyDao.lockGlobalIdentity(email);
    penaltyDao
        .findActiveGlobal(email, now)
        .ifPresent(
            penalty -> {
              throw new ActiveBookingRestrictionException(
                  penalty.getEndsAt().atZone(clock.getZone()).toLocalDate());
            });
  }

  private void requireApplicableIncident(NoShowIncidentEntity incident) {
    if (incident == null
        || incident.getId() == null
        || !"no_show".equals(incident.getIncidentType())
        || !("reported".equals(incident.getStatus()) || "confirmed".equals(incident.getStatus()))) {
      throw new IllegalArgumentException("Applicable persisted no-show incident is required");
    }
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Normalized email is required");
    }
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
