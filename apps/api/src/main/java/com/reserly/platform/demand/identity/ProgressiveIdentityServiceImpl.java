package com.reserly.platform.demand.identity;

import com.reserly.platform.demand.identity.persistence.AnonymousIdentityDao;
import com.reserly.platform.demand.identity.persistence.AnonymousIdentityEntity;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityDao;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityEntity;
import com.reserly.platform.demand.identity.persistence.IdentityLinkDao;
import com.reserly.platform.demand.identity.persistence.IdentityLinkEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene una identidad canónica al rotar HMAC y crea evidencia session-device-customer.
 *
 * <p>El email y los secretos solo viven durante la llamada. La salida, excepciones y comentarios de
 * auditoría no contienen digest. La ausencia de consentimiento falla antes de cualquier escritura.
 */
@Service
public class ProgressiveIdentityServiceImpl implements ProgressiveIdentityService {

  private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
  private static final Set<String> PURPOSES =
      Set.of("analytics", "personalization", "experimentation", "commercial_activation");
  private static final Set<String> REASONS =
      Set.of(
          "booking_email_confirmed",
          "authenticated_session",
          "consent_reconfirmed",
          "controlled_key_rotation");

  private final AnonymousIdentityDao anonymousIdentityDao;
  private final CustomerIdentityDao customerIdentityDao;
  private final IdentityLinkDao identityLinkDao;
  private final VersionedEmailHmacDeriver hmacDeriver;
  private final DemandIdentityHmacProperties properties;
  private final Clock clock;

  public ProgressiveIdentityServiceImpl(
      AnonymousIdentityDao anonymousIdentityDao,
      CustomerIdentityDao customerIdentityDao,
      IdentityLinkDao identityLinkDao,
      VersionedEmailHmacDeriver hmacDeriver,
      DemandIdentityHmacProperties properties,
      Clock clock) {
    this.anonymousIdentityDao = anonymousIdentityDao;
    this.customerIdentityDao = customerIdentityDao;
    this.identityLinkDao = identityLinkDao;
    this.hmacDeriver = hmacDeriver;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public ProgressiveIdentityResult link(ProgressiveIdentityCommand command) {
    validate(command);
    Instant now = clock.instant();
    VersionedEmailHmac active = hmacDeriver.deriveActive(command.email());
    VersionedEmailHmac previous = hmacDeriver.derivePrevious(command.email());
    var replay =
        identityLinkDao.findActiveBySessionAndPurpose(command.sessionId(), command.purpose(), now);
    if (replay.isPresent()) {
      IdentityLinkEntity link = replay.orElseThrow();
      CustomerIdentityEntity linkedCustomer = link.getCustomerIdentity();
      boolean consentActive =
          anonymousIdentityDao
                  .findPersonalizableById(command.anonymousIdentityId(), now)
                  .map(identity -> identity.getId().equals(link.getAnonymousIdentity().getId()))
                  .orElse(false)
              && customerIdentityDao
                  .findPersonalizableById(linkedCustomer.getId(), now)
                  .isPresent();
      boolean activeMatch = matches(linkedCustomer, active);
      boolean previousMatch = previous != null && matches(linkedCustomer, previous);
      if (!consentActive || (!activeMatch && !previousMatch)) {
        throw new ProgressiveIdentityException("DEMAND_IDENTITY_SESSION_CONFLICT");
      }
      if (previousMatch) {
        linkedCustomer.setEmailHmac(active.digest());
        linkedCustomer.setKeyVersion(active.keyVersion());
        refreshConsent(linkedCustomer, command, now);
        customerIdentityDao.saveAndFlush(linkedCustomer);
      }
      return toResult(link, previousMatch);
    }
    AnonymousIdentityEntity anonymous =
        anonymousIdentityDao
            .findPersonalizableById(command.anonymousIdentityId(), now)
            .orElseThrow(
                () -> new ProgressiveIdentityException("DEMAND_IDENTITY_CONSENT_REQUIRED"));
    CustomerResolution customer = resolveCustomer(command, active, now);
    IdentityLinkEntity link = new IdentityLinkEntity();
    link.setSessionId(command.sessionId());
    link.setAnonymousIdentity(anonymous);
    link.setCustomerIdentity(customer.identity());
    link.setLinkReason(customer.rotated() ? "controlled_key_rotation" : command.linkReason());
    link.setPurpose(command.purpose());
    link.setConsentVersion(command.consentVersion());
    link.setConsentedAt(command.consentedAt());
    link.setLinkedAt(now);
    link.setRetentionExpiresAt(now.plus(properties.linkRetention()));
    link.setCreatedAt(now);
    try {
      return toResult(identityLinkDao.saveAndFlush(link), customer.rotated());
    } catch (DataIntegrityViolationException conflict) {
      throw new ProgressiveIdentityException("DEMAND_IDENTITY_LINK_CONFLICT");
    }
  }

  private CustomerResolution resolveCustomer(
      ProgressiveIdentityCommand command, VersionedEmailHmac active, Instant now) {
    var current = customerIdentityDao.findByVersionedHmac(active.digest(), active.keyVersion());
    if (current.isPresent()) {
      refreshConsent(current.orElseThrow(), command, now);
      return new CustomerResolution(current.orElseThrow(), false);
    }
    VersionedEmailHmac previous = hmacDeriver.derivePrevious(command.email());
    if (previous != null) {
      var legacy =
          customerIdentityDao.findByVersionedHmac(previous.digest(), previous.keyVersion());
      if (legacy.isPresent()) {
        CustomerIdentityEntity identity = legacy.orElseThrow();
        identity.setEmailHmac(active.digest());
        identity.setKeyVersion(active.keyVersion());
        refreshConsent(identity, command, now);
        return new CustomerResolution(customerIdentityDao.saveAndFlush(identity), true);
      }
    }
    CustomerIdentityEntity created = new CustomerIdentityEntity();
    created.setEmailHmac(active.digest());
    created.setKeyVersion(active.keyVersion());
    created.setCreatedAt(now);
    created.setRetentionExpiresAt(now.plus(properties.customerRetention()));
    refreshConsent(created, command, now);
    return new CustomerResolution(customerIdentityDao.saveAndFlush(created), false);
  }

  private void refreshConsent(
      CustomerIdentityEntity identity, ProgressiveIdentityCommand command, Instant now) {
    identity.setPersonalizationConsentVersion(command.consentVersion());
    identity.setPersonalizationConsentedAt(command.consentedAt());
    identity.setPersonalizationRevokedAt(null);
    if (identity.getRetentionExpiresAt() == null
        || identity.getRetentionExpiresAt().isBefore(now.plus(properties.customerRetention()))) {
      identity.setRetentionExpiresAt(now.plus(properties.customerRetention()));
    }
    identity.setUpdatedAt(now);
  }

  private void validate(ProgressiveIdentityCommand command) {
    Instant now = clock.instant();
    if (command == null
        || command.sessionId() == null
        || command.anonymousIdentityId() == null
        || !PURPOSES.contains(command.purpose())
        || !REASONS.contains(command.linkReason())
        || command.consentVersion() == null
        || !CODE.matcher(command.consentVersion()).matches()
        || command.consentedAt() == null
        || command.consentedAt().isAfter(now)
        || command.consentedAt().isBefore(now.minus(properties.linkRetention()))) {
      throw new ProgressiveIdentityException("DEMAND_IDENTITY_LINK_INVALID");
    }
  }

  private ProgressiveIdentityResult toResult(IdentityLinkEntity link, boolean rotated) {
    return new ProgressiveIdentityResult(
        link.getId(),
        link.getSessionId(),
        link.getAnonymousIdentity().getId(),
        link.getCustomerIdentity().getId(),
        link.getCustomerIdentity().getKeyVersion(),
        link.getPurpose(),
        rotated,
        link.getLinkedAt());
  }

  private boolean matches(CustomerIdentityEntity identity, VersionedEmailHmac candidate) {
    return identity.getKeyVersion().equals(candidate.keyVersion())
        && identity.getEmailHmac().equals(candidate.digest());
  }

  private record CustomerResolution(CustomerIdentityEntity identity, boolean rotated) {}
}
