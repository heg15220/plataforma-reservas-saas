package com.reserly.platform.identity.service;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.dto.LoginCommand;
import com.reserly.platform.identity.persistence.AuthSessionDao;
import com.reserly.platform.identity.persistence.AuthSessionEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autentica sin enumerar cuentas y persiste únicamente el hash del secreto de sesión.
 *
 * <p>La contraseña se compara antes de evaluar tipo o estado para mantener un coste semejante. Las
 * cuentas pendientes de verificar email pueden entrar al panel, pero siguen sin poder publicar.
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final String ACTIVE_STATUS = "active";
  private static final String PENDING_EMAIL_STATUS = "pending_email_verification";

  private final UserDao userDao;
  private final AuthSessionDao authSessionDao;
  private final PasswordHashingService passwordHashingService;
  private final SessionTokenService sessionTokenService;
  private final SessionProperties sessionProperties;

  public AuthenticationServiceImpl(
      UserDao userDao,
      AuthSessionDao authSessionDao,
      PasswordHashingService passwordHashingService,
      SessionTokenService sessionTokenService,
      SessionProperties sessionProperties) {
    this.userDao = userDao;
    this.authSessionDao = authSessionDao;
    this.passwordHashingService = passwordHashingService;
    this.sessionTokenService = sessionTokenService;
    this.sessionProperties = sessionProperties;
  }

  @Override
  @Transactional
  public LoginOutcome login(LoginCommand command) {
    String normalizedEmail = normalizeEmail(command.email());
    UserEntity user = userDao.findForAuthentication(normalizedEmail).orElse(null);
    String storedHash = user == null ? null : user.getPasswordHash();
    boolean passwordMatches = passwordHashingService.matches(command.rawPassword(), storedHash);
    if (!passwordMatches || user == null || !canUseVenueLogin(user)) {
      throw new InvalidAuthenticationException();
    }

    if (passwordHashingService.requiresRehash(storedHash)) {
      user.setPasswordHash(passwordHashingService.hash(command.rawPassword()));
      user.setUpdatedAt(Instant.now());
      userDao.saveAndFlush(user);
    }

    Instant now = Instant.now();
    String sessionToken = sessionTokenService.generate();
    AuthSessionEntity session = new AuthSessionEntity();
    session.setUser(user);
    session.setTokenHash(sessionTokenService.hash(sessionToken));
    session.setCreatedAt(now);
    session.setLastSeenAt(now);
    session.setExpiresAt(now.plus(sessionProperties.lifetime()));
    authSessionDao.saveAndFlush(session);

    return new LoginOutcome(
        sessionToken,
        session.getExpiresAt(),
        user.getId(),
        user.getAccountType().persistedValue(),
        user.getPreferredLocale(),
        user.getEmailVerifiedAt() != null);
  }

  @Override
  @Transactional
  public void logout(String sessionToken) {
    if (!sessionTokenService.isValid(sessionToken)) {
      return;
    }
    authSessionDao.revokeByTokenHash(sessionTokenService.hash(sessionToken), Instant.now());
  }

  private boolean canUseVenueLogin(UserEntity user) {
    return user.getAccountType() == AccountType.VENUE_BUSINESS
        && (ACTIVE_STATUS.equals(user.getStatus())
            || PENDING_EMAIL_STATUS.equals(user.getStatus()));
  }

  private String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
