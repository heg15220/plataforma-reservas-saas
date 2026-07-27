package com.reserly.platform.incidents.service;

/** Indica paginación no acotada o inválida en la consulta privada de incidencias. */
public class IncidentHistoryInvalidException extends RuntimeException {

  public IncidentHistoryInvalidException() {
    super("Incident history request is invalid");
  }
}
