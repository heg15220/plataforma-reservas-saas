package com.reserly.platform.services.service;

/** Error de validacion de negocio para servicios propios. */
public class ServiceInvalidException extends RuntimeException {

  public ServiceInvalidException() {
    super();
  }

  public ServiceInvalidException(Throwable cause) {
    super(cause);
  }
}
