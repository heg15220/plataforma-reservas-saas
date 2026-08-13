-- Índices compactos para barridos temporales y decisión futura de particionado.
-- Los rankings se eliminan por cascada desde RecommendationRequests, su raíz de retención.
CREATE INDEX "ixBehaviorEventsReceivedAtBrin"
  ON "BehaviorEvents" USING brin ("receivedAt") WITH (pages_per_range = 64);
CREATE INDEX "ixRecommendationRequestsRequestedAtBrin"
  ON "RecommendationRequests" USING brin ("requestedAt") WITH (pages_per_range = 64);
CREATE INDEX "ixRecommendationRankingsRankedAtBrin"
  ON "RecommendationRankings" USING brin ("rankedAt") WITH (pages_per_range = 64);

COMMENT ON INDEX "ixBehaviorEventsReceivedAtBrin" IS
  'Acceso temporal compacto previo a particionado mensual cuando se supere el umbral operativo';
