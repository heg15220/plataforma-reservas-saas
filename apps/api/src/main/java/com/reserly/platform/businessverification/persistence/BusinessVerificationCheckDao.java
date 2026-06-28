package com.reserly.platform.businessverification.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso persistente al historial mínimo de intentos de verificación empresarial. */
public interface BusinessVerificationCheckDao
    extends JpaRepository<BusinessVerificationCheckEntity, UUID> {

  /** Recupera el resultado ya auditado de una operación para evitar repetir la llamada remota. */
  @Query(
      """
      select verificationCheck
      from BusinessVerificationCheckEntity verificationCheck
      where verificationCheck.requestId = :requestId
      """)
  Optional<BusinessVerificationCheckEntity> findByRequestId(@Param("requestId") UUID requestId);

  /** Localiza una evidencia ya guardada cuando el proveedor repite su referencia estable. */
  @Query(
      """
      select verificationCheck
      from BusinessVerificationCheckEntity verificationCheck
      where verificationCheck.provider = :provider
        and verificationCheck.remoteReference = :remoteReference
      """)
  Optional<BusinessVerificationCheckEntity> findByProviderAndRemoteReference(
      @Param("provider") String provider, @Param("remoteReference") String remoteReference);
}
