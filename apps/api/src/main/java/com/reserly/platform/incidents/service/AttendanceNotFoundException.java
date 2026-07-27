package com.reserly.platform.incidents.service;

/** Ausencia opaca para reservas inexistentes, anónimas o pertenecientes a otro local. */
public class AttendanceNotFoundException extends RuntimeException {

  public AttendanceNotFoundException() {
    super("Reservation not found for attendance");
  }
}
