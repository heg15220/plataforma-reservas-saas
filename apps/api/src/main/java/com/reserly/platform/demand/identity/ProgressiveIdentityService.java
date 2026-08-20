package com.reserly.platform.demand.identity;

/** Puerto transaccional de vinculación progresiva y rotación controlada. */
public interface ProgressiveIdentityService {
  /**
   * Vincula una sesión consentida. Un replay de sesión/finalidad devuelve la misma fila; una
   * identidad bajo la clave anterior se rota conservando su UUID canónico.
   */
  ProgressiveIdentityResult link(ProgressiveIdentityCommand command);
}
