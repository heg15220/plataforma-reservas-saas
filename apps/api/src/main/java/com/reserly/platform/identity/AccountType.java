package com.reserly.platform.identity;

import java.util.Arrays;

/**
 * Naturaleza de una cuenta autenticada, independiente de sus roles asignados.
 *
 * <p>El tipo determina qué invariantes de negocio debe cumplir una cuenta. No concede permisos por
 * sí mismo: la autorización continúa dependiendo de los roles. Los valores persistidos son parte
 * del contrato de base de datos y de API, por lo que no deben renombrarse sin migración.
 */
public enum AccountType {
  /** Cuenta normal de cliente, reservada para funcionalidad futura. */
  CUSTOMER("customer"),

  /** Cuenta empresarial habilitada para gestionar locales tras sus verificaciones. */
  VENUE_BUSINESS("venue_business"),

  /** Cuenta interna de administración de plataforma. */
  ADMIN("admin");

  private final String persistedValue;

  AccountType(String persistedValue) {
    this.persistedValue = persistedValue;
  }

  /** Devuelve el valor canónico usado en PostgreSQL y contratos externos. */
  public String persistedValue() {
    return persistedValue;
  }

  /**
   * Resuelve un valor persistido de forma estricta.
   *
   * @param value valor canónico no nulo
   * @return tipo de cuenta correspondiente
   * @throws IllegalArgumentException si el valor no pertenece al catálogo soportado
   */
  public static AccountType fromPersistedValue(String value) {
    return Arrays.stream(values())
        .filter(accountType -> accountType.persistedValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported account type"));
  }
}
