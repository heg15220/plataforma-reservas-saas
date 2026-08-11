package com.reserly.platform.incidents.service;

import com.reserly.platform.administration.service.AuditLogEntry;
import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.dto.VenueBookingRuleUpdateRequest;
import com.reserly.platform.incidents.persistence.VenueBookingRuleDao;
import com.reserly.platform.incidents.persistence.VenueBookingRuleEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional de reglas únicas por local. */
@Service
public class VenueBookingRuleServiceImpl implements VenueBookingRuleService {

  private static final int MAX_CANCELLATION_NOTICE_MINUTES = 525600;

  private final VenueBookingRuleDao ruleDao;
  private final VenueDao venueDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public VenueBookingRuleServiceImpl(
      VenueBookingRuleDao ruleDao,
      VenueDao venueDao,
      AuditLogService auditLogService,
      Clock clock) {
    this.ruleDao = ruleDao;
    this.venueDao = venueDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public VenueBookingRuleEntity get(UUID ownerUserId) {
    requireOwner(ownerUserId);
    return ruleDao
        .findOwned(ownerUserId)
        .orElseGet(
            () ->
                defaultRule(
                    venueDao
                        .findCurrentByOwnerUserId(ownerUserId)
                        .orElseThrow(VenueProfileNotFoundException::new)));
  }

  @Override
  @Transactional
  public VenueBookingRuleEntity update(UUID ownerUserId, VenueBookingRuleUpdateRequest request) {
    requireOwner(ownerUserId);
    validate(request);
    VenueBookingRuleEntity rule =
        ruleDao
            .findOwnedForUpdate(ownerUserId)
            .orElseGet(
                () ->
                    defaultRule(
                        venueDao
                            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
                            .orElseThrow(VenueProfileNotFoundException::new)));
    Map<String, Object> before = snapshot(rule);
    Instant now = clock.instant();
    rule.setCancellationAllowed(request.cancellationAllowed());
    rule.setFreeCancellationUntilMinutesBefore(request.freeCancellationUntilMinutesBefore());
    rule.setUpdatedAt(now);

    // V25 sigue siendo leído por plantillas existentes; mantenerlo sincronizado evita dos
    // políticas.
    rule.getVenue().setCancellationNoticeMinutes(request.freeCancellationUntilMinutesBefore());
    VenueBookingRuleEntity saved = ruleDao.saveAndFlush(rule);
    auditLogService.record(
        new AuditLogEntry(
            ownerUserId,
            "venue_owner",
            "venue_booking_rules",
            saved.getVenue().getId(),
            "booking_rules.updated",
            before,
            snapshot(saved),
            null,
            null));
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public CancellationRule resolveCancellation(UUID venueId, int legacyNoticeMinutes) {
    if (venueId == null) {
      throw new VenueProfileNotFoundException();
    }
    return ruleDao
        .findByVenueId(venueId)
        .map(
            rule ->
                new CancellationRule(
                    rule.isCancellationAllowed(), rule.getFreeCancellationUntilMinutesBefore()))
        .orElseGet(() -> new CancellationRule(true, legacyNoticeMinutes));
  }

  private void requireOwner(UUID ownerUserId) {
    if (ownerUserId == null) {
      throw new VenueProfileNotFoundException();
    }
  }

  private void validate(VenueBookingRuleUpdateRequest request) {
    if (request == null
        || request.freeCancellationUntilMinutesBefore() < 0
        || request.freeCancellationUntilMinutesBefore() > MAX_CANCELLATION_NOTICE_MINUTES) {
      throw new VenueBookingRuleInvalidException();
    }
  }

  private Map<String, Object> snapshot(VenueBookingRuleEntity rule) {
    return Map.of(
        "cancellationAllowed",
        rule.isCancellationAllowed(),
        "freeCancellationUntilMinutesBefore",
        rule.getFreeCancellationUntilMinutesBefore());
  }

  /**
   * Compatibilidad para locales creados después de V27 y antes de configurar reglas por primera
   * vez. La lectura devuelve el snapshot por defecto sin escribir; el primer PUT lo persiste.
   */
  private VenueBookingRuleEntity defaultRule(VenueEntity venue) {
    Instant now = clock.instant();
    VenueBookingRuleEntity rule = new VenueBookingRuleEntity();
    rule.setVenue(venue);
    rule.setCancellationAllowed(true);
    rule.setFreeCancellationUntilMinutesBefore(venue.getCancellationNoticeMinutes());
    rule.setAutoMarkAttendedAfterMinutes(120);
    rule.setRequiresConfirmation(false);
    rule.setCreatedAt(now);
    rule.setUpdatedAt(now);
    return rule;
  }
}
