package com.reserly.platform.demand.recommendation;

/** Rechazo opaco cuando una impresión no coincide con el ranking elegible persistido. */
public class RecommendationImpressionException extends RuntimeException {

  public RecommendationImpressionException() {
    super("RECOMMENDATION_IMPRESSION_INVALID");
  }
}
