package com.reserly.platform.reviews.service;

/** Ausencia opaca de un local vigente para la cuenta autenticada. */
public class VenueReviewNotFoundException extends RuntimeException {

  public VenueReviewNotFoundException() {
    super("Venue reviews not found");
  }
}
