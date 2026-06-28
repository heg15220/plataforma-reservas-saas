package com.reserly.platform.identity.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso persistente al catálogo cerrado de roles de autorización. */
public interface RoleDao extends JpaRepository<RoleEntity, UUID> {

  /** Obtiene un rol por su código canónico para asignaciones explícitas. */
  @Query(
      """
      select role
      from RoleEntity role
      where role.code = :code
      """)
  Optional<RoleEntity> findByCode(@Param("code") String code);
}
