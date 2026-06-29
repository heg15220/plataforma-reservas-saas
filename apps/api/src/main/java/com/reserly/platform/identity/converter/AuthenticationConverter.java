package com.reserly.platform.identity.converter;

import com.reserly.platform.identity.dto.LoginCommand;
import com.reserly.platform.identity.dto.LoginRequest;
import com.reserly.platform.identity.dto.LoginResponse;
import com.reserly.platform.identity.service.LoginOutcome;
import org.springframework.stereotype.Component;

/** Separa DTOs HTTP del comando y resultado interno que contiene el secreto de sesión. */
@Component
public class AuthenticationConverter {

  public LoginCommand toCommand(LoginRequest request) {
    return new LoginCommand(request.email(), request.password());
  }

  public LoginResponse toResponse(LoginOutcome outcome) {
    return new LoginResponse(
        outcome.userId(),
        outcome.accountType(),
        outcome.preferredLocale(),
        outcome.emailVerified(),
        outcome.sessionExpiresAt());
  }
}
