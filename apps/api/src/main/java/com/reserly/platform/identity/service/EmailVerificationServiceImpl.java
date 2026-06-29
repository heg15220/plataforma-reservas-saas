package com.reserly.platform.identity.service;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.persistence.AuthTokenDao;
import com.reserly.platform.identity.persistence.AuthTokenEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el ciclo de vida transaccional de los desafíos de verificación. */
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

  static final String PURPOSE = "email_verification";
  private static final String PENDING_STATUS = "pending_email_verification";
  private static final String ACTIVE_STATUS = "active";
  private final AuthTokenDao authTokenDao;
  private final UserDao userDao;
  private final OneTimeTokenService tokenService;
  private final EmailVerificationProperties properties;
  private final ApplicationEventPublisher eventPublisher;

  public EmailVerificationServiceImpl(
      AuthTokenDao authTokenDao,
      UserDao userDao,
      OneTimeTokenService tokenService,
      EmailVerificationProperties properties,
      ApplicationEventPublisher eventPublisher) {
    this.authTokenDao = authTokenDao;
    this.userDao = userDao;
    this.tokenService = tokenService;
    this.properties = properties;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void issueInitialChallenge(UserEntity user, Instant issuedAt) {
    issue(user, issuedAt);
  }

  @Override
  @Transactional
  public void requestChallenge(String email) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
    UserEntity user = userDao.findForEmailVerification(normalizedEmail).orElse(null);
    if (!isPendingVenueAccount(user)) {
      return;
    }

    Instant now = Instant.now();
    authTokenDao.revokeActiveByUserAndPurpose(user.getId(), PURPOSE, now);
    issue(user, now);
  }

  @Override
  @Transactional
  public EmailVerificationResult verify(String token) {
    if (!tokenService.isValid(token)) {
      throw new InvalidEmailVerificationException();
    }

    AuthTokenEntity challenge =
        authTokenDao
            .findForConsumption(tokenService.hash(token), PURPOSE)
            .orElseThrow(InvalidEmailVerificationException::new);
    Instant now = Instant.now();
    if (challenge.getConsumedAt() != null
        || challenge.getRevokedAt() != null
        || !challenge.getExpiresAt().isAfter(now)) {
      throw new InvalidEmailVerificationException();
    }

    UserEntity user = challenge.getUser();
    if (user.getAccountType() != AccountType.VENUE_BUSINESS
        || "disabled".equals(user.getStatus())) {
      throw new InvalidEmailVerificationException();
    }

    Instant verifiedAt = user.getEmailVerifiedAt();
    if (verifiedAt == null) {
      verifiedAt = now;
      user.setEmailVerifiedAt(verifiedAt);
      if (PENDING_STATUS.equals(user.getStatus())) {
        user.setStatus(ACTIVE_STATUS);
      }
      user.setUpdatedAt(now);
      userDao.save(user);
    }

    challenge.setConsumedAt(now);
    authTokenDao.saveAndFlush(challenge);
    authTokenDao.revokeOtherActiveTokens(user.getId(), PURPOSE, challenge.getId(), now);
    return new EmailVerificationResult(verifiedAt, user.getStatus());
  }

  private void issue(UserEntity user, Instant issuedAt) {
    String rawToken = tokenService.generate();
    Instant expiresAt = issuedAt.plus(properties.tokenLifetime());

    AuthTokenEntity challenge = new AuthTokenEntity();
    challenge.setUser(user);
    challenge.setPurpose(PURPOSE);
    challenge.setTokenHash(tokenService.hash(rawToken));
    challenge.setCreatedAt(issuedAt);
    challenge.setExpiresAt(expiresAt);
    authTokenDao.saveAndFlush(challenge);

    eventPublisher.publishEvent(
        new EmailVerificationRequestedEvent(
            UUID.randomUUID(),
            user.getId(),
            user.getEmail(),
            user.getPreferredLocale(),
            rawToken,
            expiresAt));
  }

  private boolean isPendingVenueAccount(UserEntity user) {
    return user != null
        && user.getAccountType() == AccountType.VENUE_BUSINESS
        && user.getEmailVerifiedAt() == null
        && PENDING_STATUS.equals(user.getStatus());
  }
}
