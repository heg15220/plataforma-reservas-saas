package com.reserly.platform.identity.service;

import com.reserly.platform.identity.persistence.UserEntity;
import java.time.Instant;

/** Caso de uso de emisión, rotación y consumo de verificaciones de email. */
public interface EmailVerificationService {

  /**
   * Emite el desafío inicial dentro de la transacción de registro.
   *
   * @param user cuenta recién persistida
   * @param issuedAt instante común del alta
   */
  void issueInitialChallenge(UserEntity user, Instant issuedAt);

  /**
   * Rota el desafío de una cuenta pendiente sin revelar si el email existe.
   *
   * @param email dirección solicitada por el cliente
   */
  void requestChallenge(String email);

  /**
   * Consume un secreto válido exactamente una vez y verifica la cuenta.
   *
   * @param token secreto recibido desde el enlace
   * @return instante y estado resultante
   */
  EmailVerificationResult verify(String token);
}
