package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso persistente a tokens de un solo uso.
 *
 * <p>Las futuras consultas de consumo deberán declarar mediante {@code @Query} todas las
 * condiciones de propósito, expiración, revocación y consumo.
 */
public interface AuthTokenDao extends JpaRepository<AuthTokenEntity, UUID> {}
