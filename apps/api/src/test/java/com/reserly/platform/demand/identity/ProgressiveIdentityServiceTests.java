package com.reserly.platform.demand.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.identity.persistence.AnonymousIdentityDao;
import com.reserly.platform.demand.identity.persistence.AnonymousIdentityEntity;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityDao;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityEntity;
import com.reserly.platform.demand.identity.persistence.IdentityLinkDao;
import com.reserly.platform.demand.identity.persistence.IdentityLinkEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba consentimiento, idempotencia y rotación sin cambiar la identidad canónica. */
class ProgressiveIdentityServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-20T14:00:00Z");
  private static final UUID SESSION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID ANONYMOUS_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

  private AnonymousIdentityDao anonymousDao;
  private CustomerIdentityDao customerDao;
  private IdentityLinkDao linkDao;
  private VersionedEmailHmacDeriver deriver;
  private ProgressiveIdentityService service;

  @BeforeEach
  void setUp() {
    anonymousDao = mock(AnonymousIdentityDao.class);
    customerDao = mock(CustomerIdentityDao.class);
    linkDao = mock(IdentityLinkDao.class);
    DemandIdentityHmacProperties properties = properties();
    deriver = new VersionedEmailHmacDeriver(properties);
    service =
        new ProgressiveIdentityServiceImpl(
            anonymousDao,
            customerDao,
            linkDao,
            deriver,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(customerDao.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(linkDao.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              IdentityLinkEntity link = invocation.getArgument(0);
              link.setId(UUID.fromString("30000000-0000-0000-0000-000000000001"));
              return link;
            });
  }

  @Test
  void createsCustomerAndSessionLinkWithoutReturningEmailOrHmac() {
    AnonymousIdentityEntity anonymous = anonymousIdentity();
    when(linkDao.findActiveBySessionAndPurpose(SESSION_ID, "personalization", NOW))
        .thenReturn(Optional.empty());
    when(anonymousDao.findPersonalizableById(ANONYMOUS_ID, NOW)).thenReturn(Optional.of(anonymous));
    when(customerDao.findByVersionedHmac(any(), any())).thenReturn(Optional.empty());

    ProgressiveIdentityResult result = service.link(command());

    assertThat(result.sessionId()).isEqualTo(SESSION_ID);
    assertThat(result.anonymousIdentityId()).isEqualTo(ANONYMOUS_ID);
    assertThat(result.keyVersion()).isEqualTo("hmac-v2");
    assertThat(result.keyRotated()).isFalse();
    assertThat(result.toString()).doesNotContain("person@example.test", "emailHmac");
    verify(customerDao).saveAndFlush(any(CustomerIdentityEntity.class));
    verify(linkDao).saveAndFlush(any(IdentityLinkEntity.class));
  }

  @Test
  void rotatesPreviousDigestOnSameCustomerIdAndMarksReason() {
    AnonymousIdentityEntity anonymous = anonymousIdentity();
    VersionedEmailHmac previous = deriver.derivePrevious("person@example.test");
    CustomerIdentityEntity legacy = customer(previous);
    UUID canonicalId = legacy.getId();
    when(linkDao.findActiveBySessionAndPurpose(SESSION_ID, "personalization", NOW))
        .thenReturn(Optional.empty());
    when(anonymousDao.findPersonalizableById(ANONYMOUS_ID, NOW)).thenReturn(Optional.of(anonymous));
    when(customerDao.findByVersionedHmac(any(), any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(1).equals("hmac-v1")
                    ? Optional.of(legacy)
                    : Optional.empty());

    ProgressiveIdentityResult result = service.link(command());

    assertThat(result.customerIdentityId()).isEqualTo(canonicalId);
    assertThat(result.keyRotated()).isTrue();
    assertThat(legacy.getKeyVersion()).isEqualTo("hmac-v2");
    assertThat(legacy.getEmailHmac())
        .isEqualTo(deriver.deriveActive("person@example.test").digest());
    verify(linkDao)
        .saveAndFlush(
            org.mockito.ArgumentMatchers.argThat(
                link -> link.getLinkReason().equals("controlled_key_rotation")));
  }

  @Test
  void exactSessionReplayReturnsSameLinkAndRejectsDifferentEmail() {
    IdentityLinkEntity existing =
        existingLink(customer(deriver.deriveActive("person@example.test")));
    when(linkDao.findActiveBySessionAndPurpose(SESSION_ID, "personalization", NOW))
        .thenReturn(Optional.of(existing));
    when(anonymousDao.findPersonalizableById(ANONYMOUS_ID, NOW))
        .thenReturn(Optional.of(existing.getAnonymousIdentity()));
    when(customerDao.findPersonalizableById(existing.getCustomerIdentity().getId(), NOW))
        .thenReturn(Optional.of(existing.getCustomerIdentity()));

    ProgressiveIdentityResult replay = service.link(command());
    assertThat(replay.linkId()).isEqualTo(existing.getId());
    verify(linkDao, never()).saveAndFlush(any());

    ProgressiveIdentityCommand conflict =
        new ProgressiveIdentityCommand(
            SESSION_ID,
            ANONYMOUS_ID,
            "other@example.test",
            "personalization",
            "personalization-v2",
            NOW.minusSeconds(10),
            "booking_email_confirmed");
    assertThatThrownBy(() -> service.link(conflict))
        .isInstanceOf(ProgressiveIdentityException.class)
        .hasMessage("DEMAND_IDENTITY_SESSION_CONFLICT");
  }

  @Test
  void refusesToDeriveOrPersistWithoutActiveAnonymousConsent() {
    when(linkDao.findActiveBySessionAndPurpose(SESSION_ID, "personalization", NOW))
        .thenReturn(Optional.empty());
    when(anonymousDao.findPersonalizableById(ANONYMOUS_ID, NOW)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.link(command()))
        .isInstanceOf(ProgressiveIdentityException.class)
        .hasMessage("DEMAND_IDENTITY_CONSENT_REQUIRED");
    verify(customerDao, never()).saveAndFlush(any());
    verify(linkDao, never()).saveAndFlush(any());
  }

  private ProgressiveIdentityCommand command() {
    return new ProgressiveIdentityCommand(
        SESSION_ID,
        ANONYMOUS_ID,
        "person@example.test",
        "personalization",
        "personalization-v2",
        NOW.minusSeconds(10),
        "booking_email_confirmed");
  }

  private AnonymousIdentityEntity anonymousIdentity() {
    AnonymousIdentityEntity identity = new AnonymousIdentityEntity();
    identity.setId(ANONYMOUS_ID);
    return identity;
  }

  private CustomerIdentityEntity customer(VersionedEmailHmac hmac) {
    CustomerIdentityEntity identity = new CustomerIdentityEntity();
    identity.setId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
    identity.setEmailHmac(hmac.digest());
    identity.setKeyVersion(hmac.keyVersion());
    identity.setCreatedAt(NOW.minus(Duration.ofDays(30)));
    identity.setUpdatedAt(NOW.minus(Duration.ofDays(30)));
    identity.setRetentionExpiresAt(NOW.plus(Duration.ofDays(30)));
    return identity;
  }

  private IdentityLinkEntity existingLink(CustomerIdentityEntity customer) {
    IdentityLinkEntity link = new IdentityLinkEntity();
    link.setId(UUID.fromString("30000000-0000-0000-0000-000000000001"));
    link.setSessionId(SESSION_ID);
    link.setAnonymousIdentity(anonymousIdentity());
    link.setCustomerIdentity(customer);
    link.setPurpose("personalization");
    link.setLinkedAt(NOW.minusSeconds(5));
    return link;
  }

  private DemandIdentityHmacProperties properties() {
    return new DemandIdentityHmacProperties(
        "hmac-v2",
        "0123456789abcdef0123456789abcdef",
        "hmac-v1",
        "abcdef0123456789abcdef0123456789",
        Duration.ofDays(365),
        Duration.ofDays(90));
  }
}
