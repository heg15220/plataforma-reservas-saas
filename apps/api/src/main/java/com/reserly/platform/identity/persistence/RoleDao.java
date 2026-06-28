package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso persistente al catálogo cerrado de roles de autorización. */
public interface RoleDao extends JpaRepository<RoleEntity, UUID> {}
