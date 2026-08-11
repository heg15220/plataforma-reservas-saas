package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AuditLogService;
import com.reserly.platform.incidents.dto.VenueBookingRuleUpdateRequest;
import com.reserly.platform.incidents.persistence.VenueBookingRuleDao;
import com.reserly.platform.incidents.persistence.VenueBookingRuleEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cobertura focalizada de propiedad, límites y resolución consumida por cancelación pública. */
class VenueBookingRuleServiceTests {

  private final VenueBookingRuleDao ruleDao = mock(VenueBookingRuleDao.class);
  private final VenueDao venueDao = mock(VenueDao.class);
  private final AuditLogService auditLogService = mock(AuditLogService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneOffset.UTC);
  private final VenueBookingRuleService service =
      new VenueBookingRuleServiceImpl(ruleDao, venueDao, auditLogService, clock);

  @BeforeEach
  void returnSavedRule() {
    when(ruleDao.saveAndFlush(any(VenueBookingRuleEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void updatesOwnedRuleAndKeepsLegacyCancellationNoticeSynchronized() {
    UUID ownerUserId = UUID.randomUUID();
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setCancellationNoticeMinutes(1440);
    VenueBookingRuleEntity rule = new VenueBookingRuleEntity();
    rule.setVenue(venue);
    rule.setCancellationAllowed(true);
    rule.setFreeCancellationUntilMinutesBefore(1440);
    when(ruleDao.findOwnedForUpdate(ownerUserId)).thenReturn(Optional.of(rule));

    VenueBookingRuleEntity updated =
        service.update(ownerUserId, new VenueBookingRuleUpdateRequest(false, 2880));

    assertThat(updated.isCancellationAllowed()).isFalse();
    assertThat(updated.getFreeCancellationUntilMinutesBefore()).isEqualTo(2880);
    assertThat(updated.getVenue().getCancellationNoticeMinutes()).isEqualTo(2880);
    assertThat(updated.getUpdatedAt()).isEqualTo(clock.instant());
    verify(ruleDao).findOwnedForUpdate(ownerUserId);
    verify(ruleDao).saveAndFlush(rule);
    verify(auditLogService).record(any());
  }

  @Test
  void rejectsInvalidNoticeBeforeReadingPersistence() {
    assertThatThrownBy(
            () ->
                service.update(UUID.randomUUID(), new VenueBookingRuleUpdateRequest(true, 525601)))
        .isInstanceOf(VenueBookingRuleInvalidException.class);

    verify(ruleDao, never()).findOwnedForUpdate(any());
  }

  @Test
  void resolvesPersistedRuleAndUsesLegacyOnlyWhenSeedIsMissing() {
    UUID venueId = UUID.randomUUID();
    VenueBookingRuleEntity rule = new VenueBookingRuleEntity();
    rule.setCancellationAllowed(false);
    rule.setFreeCancellationUntilMinutesBefore(60);
    when(ruleDao.findByVenueId(venueId)).thenReturn(Optional.of(rule));

    var configured = service.resolveCancellation(venueId, 1440);
    var fallback = service.resolveCancellation(UUID.randomUUID(), 30);

    assertThat(configured.allowed()).isFalse();
    assertThat(configured.noticeMinutes()).isEqualTo(60);
    assertThat(fallback.allowed()).isTrue();
    assertThat(fallback.noticeMinutes()).isEqualTo(30);
  }

  @Test
  void createsDefaultRuleOnFirstUpdateForVenueCreatedAfterMigration() {
    UUID ownerUserId = UUID.randomUUID();
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setCancellationNoticeMinutes(90);
    when(ruleDao.findOwnedForUpdate(ownerUserId)).thenReturn(Optional.empty());
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)).thenReturn(Optional.of(venue));

    VenueBookingRuleEntity updated =
        service.update(ownerUserId, new VenueBookingRuleUpdateRequest(true, 120));

    assertThat(updated.getVenue()).isSameAs(venue);
    assertThat(updated.getCreatedAt()).isEqualTo(clock.instant());
    assertThat(updated.getFreeCancellationUntilMinutesBefore()).isEqualTo(120);
  }
}
