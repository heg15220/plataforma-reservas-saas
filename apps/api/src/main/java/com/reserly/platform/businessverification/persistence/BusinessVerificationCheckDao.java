package com.reserly.platform.businessverification.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso persistente al historial mínimo de intentos de verificación empresarial. */
public interface BusinessVerificationCheckDao
    extends JpaRepository<BusinessVerificationCheckEntity, UUID> {}
