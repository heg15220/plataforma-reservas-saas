package com.reserly.platform.demand.recommendation;

/** Frontera transaccional para confirmar qué alternativas llegaron a ser observables. */
public interface RecommendationImpressionService {

  /**
   * Registra eventos idempotentes exclusivamente para candidatos elegibles y rankeados.
   *
   * @throws RecommendationImpressionException si la petición o cualquier candidato no es válido
   */
  RecommendationImpressionResult record(RecommendationImpressionCommand command);
}
