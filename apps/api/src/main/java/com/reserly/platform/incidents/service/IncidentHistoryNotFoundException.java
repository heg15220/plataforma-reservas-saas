package com.reserly.platform.incidents.service;

/** Ausencia opaca de una reserva que permita acreditar la consulta del historial. */
public class IncidentHistoryNotFoundException extends RuntimeException {

  public IncidentHistoryNotFoundException() {
    super("Incident history reference not found");
  }
}
