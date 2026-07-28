package com.reserly.platform.reviews.service;

/** Rechazo de contenido que incumple el contrato de creación de reseña. */
public class ReviewInvalidException extends RuntimeException {

  public ReviewInvalidException() {
    super("Invalid review request");
  }
}
