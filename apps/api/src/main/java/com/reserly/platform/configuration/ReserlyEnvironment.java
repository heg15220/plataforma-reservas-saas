package com.reserly.platform.configuration;

/**
 * Entornos de ejecución reconocidos por la plataforma.
 *
 * <p>{@link #TEST} existe exclusivamente para verificaciones automatizadas y no es un entorno
 * desplegable.
 */
public enum ReserlyEnvironment {
  LOCAL,
  STAGING,
  PRODUCTION,
  TEST
}
