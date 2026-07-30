package com.reserly.platform.identity.service;

import com.reserly.platform.identity.dto.LoginCommand;

/** Casos de uso transaccionales de inicio y cierre de sesión por tipo de cuenta. */
public interface AuthenticationService {

  /** Verifica credenciales de local y crea una sesión revocable. */
  LoginOutcome login(LoginCommand command);

  /** Verifica una cuenta admin activa sin permitir cuentas empresariales. */
  LoginOutcome loginAdmin(LoginCommand command);

  /** Revoca idempotentemente el token si tiene formato y sesión conocidos. */
  void logout(String sessionToken);
}
