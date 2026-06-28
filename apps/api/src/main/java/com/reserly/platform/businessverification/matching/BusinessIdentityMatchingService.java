package com.reserly.platform.businessverification.matching;

/**
 * Comparación tolerante y determinista de identidad empresarial.
 *
 * <p>Devuelve {@code null} cuando el proveedor no publica el dato; ausencia no equivale a
 * coincidencia ni discrepancia.
 */
public interface BusinessIdentityMatchingService {

  Boolean matchesLegalName(String submitted, String remote);

  Boolean matchesAddress(String submitted, String remote);
}
