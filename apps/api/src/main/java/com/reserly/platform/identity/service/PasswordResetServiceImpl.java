package com.reserly.platform.identity.service;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.persistence.AuthSessionDao;
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

/** Implementa recuperación de cuentas de local con rotación y consumo transaccional. */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

  static final String PURPOSE = "password_reset";
  private static final int MINIMUM_PASSWORD_CHARACTERS = 12;
  private final AuthTokenDao authTokenDao;
  private final AuthSessionDao authSessionDao;
  private final UserDao userDao;
  private final OneTimeTokenService tokenService;
  private final PasswordHashingService passwordHashingService;
  private final PasswordResetProperties properties;
  private final ApplicationEventPublisher eventPublisher;

  public PasswordResetServiceImpl(
      AuthTokenDao authTokenDao,
      AuthSessionDao authSessionDao,
      UserDao userDao,
      OneTimeTokenService tokenService,
      PasswordHashingService passwordHashingService,
      PasswordResetProperties properties,
      ApplicationEventPublisher eventPublisher) {
    this.authTokenDao = authTokenDao;
    this.authSessionDao = authSessionDao;
    this.userDao = userDao;
    this.tokenService = tokenService;
    this.passwordHashingService = passwordHashingService;
    this.properties = properties;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void requestReset(String email) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
    UserEntity user = userDao.findForPasswordReset(normalizedEmail).orElse(null);
    if (!isRecoverableVenueAccount(user)) {
      return;
    }

    Instant now = Instant.now();
    authTokenDao.revokeActiveByUserAndPurpose(user.getId(), PURPOSE, now);
    issue(user, now);
  }

  @Override
  @Transactional
  public void resetPassword(String token, String rawPassword) {
    if (!tokenService.isValid(token)) {
      throw new InvalidPasswordResetException();
    }
    validatePassword(rawPassword);

    AuthTokenEntity challenge =
        authTokenDao
            .findForConsumption(tokenService.hash(token), PURPOSE)
            .orElseThrow(InvalidPasswordResetException::new);
    Instant now = Instant.now();
    if (challenge.getConsumedAt() != null
        || challenge.getRevokedAt() != null
        || !challenge.getExpiresAt().isAfter(now)) {
      throw new InvalidPasswordResetException();
    }

    UserEntity user = challenge.getUser();
    if (!isRecoverableVenueAccount(user)) {
      throw new InvalidPasswordResetException();
    }

    user.setPasswordHash(passwordHashingService.hash(rawPassword));
    user.setUpdatedAt(now);
    userDao.save(user);

    challenge.setConsumedAt(now);
    authTokenDao.saveAndFlush(challenge);
    authTokenDao.revokeOtherActiveTokens(user.getId(), PURPOSE, challenge.getId(), now);
    authSessionDao.revokeActiveByUserId(user.getId(), now);
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
        new PasswordResetRequestedEvent(
            UUID.randomUUID(),
            user.getId(),
            user.getEmail(),
            user.getPreferredLocale(),
            rawToken,
            expiresAt));
  }

  private void validatePassword(String rawPassword) {
    if (rawPassword == null || rawPassword.length() < MINIMUM_PASSWORD_CHARACTERS) {
      throw new InvalidPasswordResetException();
    }
    try {
      passwordHashingService.validate(rawPassword);
    } catch (PasswordHashingValidationException exception) {
      throw new InvalidPasswordResetException();
    }
  }

  private boolean isRecoverableVenueAccount(UserEntity user) {
    return user != null
        && user.getAccountType() == AccountType.VENUE_BUSINESS
        && !"disabled".equals(user.getStatus());
  }
}
