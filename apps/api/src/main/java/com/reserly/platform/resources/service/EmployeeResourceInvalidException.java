package com.reserly.platform.resources.service;

/** Error de validación de negocio para empleados o recursos propios. */
public class EmployeeResourceInvalidException extends RuntimeException {

  public EmployeeResourceInvalidException() {
    super();
  }

  public EmployeeResourceInvalidException(Throwable cause) {
    super(cause);
  }
}
