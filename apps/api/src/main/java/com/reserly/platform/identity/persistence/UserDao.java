package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso persistente a cuentas autenticadas.
 *
 * <p>Las búsquedas sensibles por email se incorporarán con {@code @Query} en la tarea que
 * implemente registro y acceso, evitando exponer consultas derivadas como contrato implícito.
 */
public interface UserDao extends JpaRepository<UserEntity, UUID> {}
