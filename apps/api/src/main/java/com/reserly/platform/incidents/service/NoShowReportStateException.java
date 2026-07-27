package com.reserly.platform.incidents.service;

/** La reserva no está marcada como no asistida o ya fue reportada. */
public class NoShowReportStateException extends RuntimeException {

  public NoShowReportStateException() {
    super("Reservation is not eligible for no-show report");
  }
}
