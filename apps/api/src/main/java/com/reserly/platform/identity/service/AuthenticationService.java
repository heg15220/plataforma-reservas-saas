package com.reserly.platform.identity.service;

import com.reserly.platform.identity.dto.LoginCommand;

/** Casos de uso transaccionales de inicio y cierre de sesión local. */
public interface AuthenticationService {

  /** Verifica credenciales y crea una sesión revocable. */
  LoginOutcome login(LoginCommand command);

  /** Revoca idempotentemente el token si tiene formato y sesión conocidos. */
  void logout(String sessionToken);
}
