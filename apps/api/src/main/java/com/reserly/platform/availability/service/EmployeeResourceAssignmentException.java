package com.reserly.platform.availability.service;

/** Indica que la preferencia de asignacion no puede satisfacerse para la franja solicitada. */
public class EmployeeResourceAssignmentException extends RuntimeException {

  public EmployeeResourceAssignmentException() {
    super("La asignacion de recurso solicitada no es valida");
  }
}
