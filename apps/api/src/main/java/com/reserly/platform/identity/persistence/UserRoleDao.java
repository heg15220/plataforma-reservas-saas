package com.reserly.platform.identity.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso persistente a las concesiones de roles, incluido su actor de auditoría. */
public interface UserRoleDao extends JpaRepository<UserRoleEntity, UUID> {

  /** Obtiene los códigos concedidos para construir authorities sin cargar entidades completas. */
  @Query(
      """
      select assignment.role.code
      from UserRoleEntity assignment
      where assignment.user.id = :userId
      order by assignment.role.code
      """)
  List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

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
