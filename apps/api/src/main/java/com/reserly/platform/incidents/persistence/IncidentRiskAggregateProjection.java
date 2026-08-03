package com.reserly.platform.incidents.persistence;

/**
 * Agregado minimizado por identidad normalizada para clasificar una página de reservas.
 *
 * <p>La proyección permanece dentro del servicio y nunca forma parte del contrato HTTP.
 */
public interface IncidentRiskAggregateProjection {

  String getCustomerEmailNormalized();

  long getOperationalCount();

  long getRecentCount();
}
