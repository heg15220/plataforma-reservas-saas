package com.reserly.platform.incidents.service;

/** Estado solicitado desconocido o transición incompatible con el estado de la reserva. */
public class AttendanceInvalidException extends RuntimeException {

  public AttendanceInvalidException() {
    super("Invalid attendance transition");
  }
}
