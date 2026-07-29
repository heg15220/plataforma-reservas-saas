/**
 * Persistencia del núcleo de facturación.
 *
 * <p>Los DAOs conservan consultas explícitas para el catálogo y la idempotencia. Las mutaciones de
 * suscripciones y pagos deben ejecutarse desde servicios transaccionales, nunca desde
 * controladores.
 */
package com.reserly.platform.billing.persistence;
