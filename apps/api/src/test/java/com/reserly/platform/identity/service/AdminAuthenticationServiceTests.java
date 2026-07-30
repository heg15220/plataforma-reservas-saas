package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.dto.LoginCommand;
import com.reserly.platform.identity.persistence.AuthSessionDao;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica que el endpoint lógico admin no acepta cuentas empresariales ni admins inactivos. */
class AdminAuthenticationServiceTests {

  @Test
  void createsSessionOnlyForActiveAdmin() {
    Fixture fixture = fixture(AccountType.ADMIN, "active");

    LoginOutcome outcome = fixture.service().loginAdmin(command());

    assertThat(outcome.accountType()).isEqualTo("admin");
    verify(fixture.authSessionDao()).saveAndFlush(any());
  }

  @Test
  void rejectsVenueAccountAndInactiveAdminWithSameOpaqueFailure() {
    Fixture venue = fixture(AccountType.VENUE_BUSINESS, "active");
    Fixture suspendedAdmin = fixture(AccountType.ADMIN, "suspended");

    assertThatThrownBy(() -> venue.service().loginAdmin(command()))
        .isInstanceOf(InvalidAuthenticationException.class);
    assertThatThrownBy(() -> suspendedAdmin.service().loginAdmin(command()))
        .isInstanceOf(InvalidAuthenticationException.class);
  }

  private Fixture fixture(AccountType accountType, String status) {
    UserDao userDao = mock(UserDao.class);
    AuthSessionDao sessionDao = mock(AuthSessionDao.class);
    PasswordHashingService hashing = mock(PasswordHashingService.class);
    SessionTokenService tokens = mock(SessionTokenService.class);
    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID());
    user.setAccountType(accountType);
    user.setStatus(status);
    user.setPasswordHash("hash");
    user.setPreferredLocale("es");
    when(userDao.findForAuthentication("admin@example.com")).thenReturn(Optional.of(user));
    when(hashing.matches("secret-password", "hash")).thenReturn(true);
    when(tokens.generate()).thenReturn("token");
    when(tokens.hash("token")).thenReturn("a".repeat(64));
    var service =
        new AuthenticationServiceImpl(
            userDao,
            sessionDao,
            hashing,
            tokens,
            new SessionProperties(Duration.ofHours(8), Duration.ofMinutes(5)));
    return new Fixture(service, sessionDao);
  }

  private LoginCommand command() {
    return new LoginCommand("admin@example.com", "secret-password");
  }

  private record Fixture(AuthenticationServiceImpl service, AuthSessionDao authSessionDao) {}
}
