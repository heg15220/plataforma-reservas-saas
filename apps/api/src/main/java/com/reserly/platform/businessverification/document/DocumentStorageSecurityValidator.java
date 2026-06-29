package com.reserly.platform.businessverification.document;

import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import org.springframework.stereotype.Component;

/** Rechaza defaults locales de almacenamiento y cifrado fuera de entornos aislados. */
@Component
public class DocumentStorageSecurityValidator {

  public DocumentStorageSecurityValidator(
      ReserlyProperties reserlyProperties,
      PrivateObjectStorageProperties storageProperties,
      DocumentEncryptionProperties encryptionProperties) {
    ReserlyEnvironment environment = reserlyProperties.environment();
    if (environment == ReserlyEnvironment.LOCAL || environment == ReserlyEnvironment.TEST) {
      return;
    }
    if (!"https".equalsIgnoreCase(storageProperties.endpoint().getScheme())
        || storageProperties.createBucket()
        || "local-dev-v1".equals(encryptionProperties.keyId())) {
      throw new IllegalStateException("Private document storage configuration is insecure");
    }
  }
}
