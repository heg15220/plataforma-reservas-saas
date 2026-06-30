package com.reserly.platform.identity.service;

/** Caso de uso de solicitud y consumo de recuperación de contraseña. */
public interface PasswordResetService {

  /** Rota un desafío elegible sin revelar existencia ni estado de la cuenta. */
  void requestReset(String email);

  /** Consume el token, reemplaza el hash y revoca todas las sesiones de la cuenta. */
  void resetPassword(String token, String rawPassword);
}
