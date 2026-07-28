package com.reserly.platform.reviews.service;

/** Rechazo opaco cuando no se acredita una reserva pasada válida para el email. */
public class ReviewNotEligibleException extends RuntimeException {

  public ReviewNotEligibleException() {
    super("Review is not eligible");
  }
}
