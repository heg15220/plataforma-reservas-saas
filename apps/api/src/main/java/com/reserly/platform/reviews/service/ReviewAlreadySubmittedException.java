package com.reserly.platform.reviews.service;

/** Conflicto estable cuando la reserva ya acredita otra reseña. */
public class ReviewAlreadySubmittedException extends RuntimeException {

  public ReviewAlreadySubmittedException() {
    super("Review already submitted");
  }

  public ReviewAlreadySubmittedException(Throwable cause) {
    super("Review already submitted", cause);
  }
}
