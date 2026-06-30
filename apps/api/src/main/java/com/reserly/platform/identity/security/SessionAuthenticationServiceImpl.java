package com.reserly.platform.identity.security;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.persistence.AuthSessionDao;
import com.reserly.platform.identity.persistence.AuthSessionEntity;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.identity.service.SessionProperties;
import com.reserly.platform.identity.service.SessionTokenService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autentica sesiones contra PostgreSQL, fuente de verdad para revocación, cuenta y roles.
 *
 * <p>Una cuenta que dejó de ser operativa revoca la sesión observada. {@code lastSeenAt} se toca
 * con intervalo mínimo y nunca extiende {@code expiresAt}.
 */
@Service
public class SessionAuthenticationServiceImpl implements SessionAuthenticationService {

  private static final String ACTIVE_STATUS = "active";
  private static final String PENDING_EMAIL_STATUS = "pending_email_verification";

  private final AuthSessionDao authSessionDao;
  private final UserRoleDao userRoleDao;
  private final SessionTokenService sessionTokenService;
  private final SessionProperties sessionProperties;

  public SessionAuthenticationServiceImpl(
      AuthSessionDao authSessionDao,
      UserRoleDao userRoleDao,
      SessionTokenService sessionTokenService,
      SessionProperties sessionProperties) {
    this.authSessionDao = authSessionDao;
    this.userRoleDao = userRoleDao;
    this.sessionTokenService = sessionTokenService;
    this.sessionProperties = sessionProperties;
  }

  @Override
  @Transactional
  public Optional<AuthenticatedAccount> authenticate(String sessionToken) {
    if (!sessionTokenService.isValid(sessionToken)) {
      return Optional.empty();
    }

    Instant now = Instant.now();
    String tokenHash = sessionTokenService.hash(sessionToken);
    Optional<AuthSessionEntity> found = authSessionDao.findActiveForAuthentication(tokenHash, now);
    if (found.isEmpty()) {
      return Optional.empty();
    }

    AuthSessionEntity session = found.orElseThrow();
    UserEntity user = session.getUser();
    if (!canUseSession(user)) {
      authSessionDao.revokeByTokenHash(tokenHash, now);
      return Optional.empty();
    }

    Set<String> roles = Set.copyOf(userRoleDao.findRoleCodesByUserId(user.getId()));
    authSessionDao.touchActiveSession(
        session.getId(), now, now.minus(sessionProperties.activityUpdateInterval()));
    return Optional.of(
        new AuthenticatedAccount(
            user.getId(),
            session.getId(),
            user.getAccountType(),
            user.getPreferredLocale(),
            roles));
  }

  private boolean canUseSession(UserEntity user) {
    if (ACTIVE_STATUS.equals(user.getStatus())) {
      return true;
    }
    return PENDING_EMAIL_STATUS.equals(user.getStatus())
        && user.getAccountType() == AccountType.VENUE_BUSINESS;
  }
}
