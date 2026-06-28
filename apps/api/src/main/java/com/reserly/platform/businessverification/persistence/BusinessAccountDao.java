package com.reserly.platform.businessverification.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso persistente a identidades empresariales.
 *
 * <p>Las búsquedas futuras por propietario o identificador normalizado deberán usar {@code @Query}
 * y aplicar el alcance de autorización correspondiente.
 */
public interface BusinessAccountDao extends JpaRepository<BusinessAccountEntity, UUID> {}
