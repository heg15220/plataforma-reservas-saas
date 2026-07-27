package com.reserly.platform.incidents.service;

/** Ausencia opaca para una reserva inexistente, anónima o perteneciente a otro local. */
public class NoShowReportNotFoundException extends RuntimeException {

  public NoShowReportNotFoundException() {
    super("Reservation not found for no-show report");
  }
}
