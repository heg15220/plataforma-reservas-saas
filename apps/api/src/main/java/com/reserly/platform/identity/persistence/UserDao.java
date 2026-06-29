package com.reserly.platform.identity.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso persistente a cuentas autenticadas.
 *
 * <p>Las búsquedas sensibles por email se incorporarán con {@code @Query} en la tarea que
 * implemente registro y acceso, evitando exponer consultas derivadas como contrato implícito.
 */
public interface UserDao extends JpaRepository<UserEntity, UUID> {

  /** Comprueba conflictos de registro por identidad de email normalizada. */
  @Query(
      """
      select count(user) > 0
      from UserEntity user
      where user.emailNormalized = :emailNormalized
      """)
  boolean existsByEmailNormalized(@Param("emailNormalized") String emailNormalized);

  /** Carga una credencial por email canónico sin exponer una consulta derivada implícita. */
  @Query(
      """
      select user
      from UserEntity user
      where user.emailNormalized = :emailNormalized
      """)
  Optional<UserEntity> findForAuthentication(@Param("emailNormalized") String emailNormalized);
}
