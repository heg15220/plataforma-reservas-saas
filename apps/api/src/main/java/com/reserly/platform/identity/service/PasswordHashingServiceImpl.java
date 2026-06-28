package com.reserly.platform.identity.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementación BCrypt mínima requerida para que el registro nunca persista secretos en claro.
 *
 * <p>El coste 12 es explícito y la sal la genera BCrypt por cada invocación. La política completa y
 * su posible configuración por entorno se cerrarán en la tarea 1.12.
 */
@Service
public class PasswordHashingServiceImpl implements PasswordHashingService {

  private static final int BCRYPT_STRENGTH = 12;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

  @Override
  public String hash(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }
}
