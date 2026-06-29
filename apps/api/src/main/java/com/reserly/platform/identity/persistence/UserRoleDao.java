package com.reserly.platform.identity.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso persistente a las concesiones de roles, incluido su actor de auditoría. */
public interface UserRoleDao extends JpaRepository<UserRoleEntity, UUID> {

  /** Comprueba una concesión explícita; el tipo de cuenta no sustituye a los roles. */
  @Query(
      """
      select count(assignment) > 0
      from UserRoleEntity assignment
      where assignment.user.id = :userId
        and assignment.role.code = :roleCode
      """)
  boolean existsByUserIdAndRoleCode(
      @Param("userId") UUID userId, @Param("roleCode") String roleCode);
}
