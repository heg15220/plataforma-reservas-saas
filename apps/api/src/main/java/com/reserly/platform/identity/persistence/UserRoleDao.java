package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso persistente a las concesiones de roles, incluido su actor de auditoría. */
public interface UserRoleDao extends JpaRepository<UserRoleEntity, UUID> {}
