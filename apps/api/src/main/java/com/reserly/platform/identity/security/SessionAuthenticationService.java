package com.reserly.platform.identity.security;

import java.util.Optional;

/** Resuelve una cookie opaca a un principal vigente sin exponer su secreto. */
public interface SessionAuthenticationService {

  /**
   * Valida formato, hash, vigencia, revocación y estado operativo de la cuenta.
   *
   * @return principal con roles o vacío para cualquier credencial no admisible
   */
  Optional<AuthenticatedAccount> authenticate(String sessionToken);
}
