package com.reserly.platform.incidents.service;

/** Impide valorar asistencia antes de que finalice la reserva. */
public class AttendanceTooEarlyException extends RuntimeException {

  public AttendanceTooEarlyException() {
    super("Reservation has not finished");
  }
}
