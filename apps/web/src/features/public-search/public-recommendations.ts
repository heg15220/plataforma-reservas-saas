import type { PublicVenueSearchItem } from "./public-search-api";

export const publicRecommendationExplanationCodes = [
  "GOOD_AVAILABILITY",
  "MATCHES_ACTIVE_FILTERS",
] as const;

export type PublicRecommendationExplanationCode =
  (typeof publicRecommendationExplanationCodes)[number];

/**
 * Proyección pública mínima de una recomendación. No expone score, features internas ni muestras de
 * exploración; conserva solo la regla visible que permite explicar por qué aparece la tarjeta.
 */
export interface PublicRecommendedVenue extends PublicVenueSearchItem {
  recommendation: {
    explanationCode: PublicRecommendationExplanationCode;
    policyVersion: "public-availability-fallback-v1";
    strategy: "fallback";
  };
}

/**
 * Construye el fallback público desde resultados que Spring ya filtró. Se vuelve a exigir capacidad
 * reservable y se deduplican slugs para que una recomendación nunca reintroduzca un local inválido.
 */
export function buildPublicRecommendationFallback(
  venues: PublicVenueSearchItem[],
  options: { activeFilters: boolean; limit: number },
): PublicRecommendedVenue[] {
  const seen = new Set<string>();
  const explanationCode = options.activeFilters ? "MATCHES_ACTIVE_FILTERS" : "GOOD_AVAILABILITY";

  return venues
    .filter((venue) => {
      if (!venue.bookingAvailable || venue.statusCode !== "available" || seen.has(venue.slug)) {
        return false;
      }
      seen.add(venue.slug);
      return true;
    })
    .slice(0, options.limit)
    .map((venue) => ({
      ...venue,
      recommendation: {
        explanationCode,
        policyVersion: "public-availability-fallback-v1",
        strategy: "fallback",
      },
    }));
}
