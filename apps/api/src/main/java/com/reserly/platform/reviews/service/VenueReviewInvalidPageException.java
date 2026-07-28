package com.reserly.platform.reviews.service;

/** Rechazo de paginación privada fuera de los límites documentados. */
public class VenueReviewInvalidPageException extends RuntimeException {

  public VenueReviewInvalidPageException() {
    super("Invalid venue review page");
  }
}
