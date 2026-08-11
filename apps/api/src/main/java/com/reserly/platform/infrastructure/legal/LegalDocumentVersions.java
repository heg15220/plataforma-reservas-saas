package com.reserly.platform.infrastructure.legal;

/**
 * Versiones inmutables de los textos legales aceptables por los contratos públicos.
 *
 * <p>Cambiar el contenido material de un documento exige crear una nueva versión y decidir de forma
 * explícita si las cuentas existentes deben volver a consentirla.
 */
public final class LegalDocumentVersions {

  public static final String PRIVACY_POLICY = "2026-08-11";
  public static final String TERMS_OF_SERVICE = "2026-08-11";

  private LegalDocumentVersions() {}
}
