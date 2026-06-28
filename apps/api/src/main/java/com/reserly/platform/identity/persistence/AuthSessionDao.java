package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso persistente a sesiones.
 *
 * <p>Las futuras consultas de autenticación deberán filtrar explícitamente revocación y expiración
 * mediante {@code @Query}.
 */
public interface AuthSessionDao extends JpaRepository<AuthSessionEntity, UUID> {}
