import { describe, expect, it } from "vitest";

import type { PublicVenueSearchItem } from "./public-search-api";
import { buildPublicRecommendationFallback } from "./public-recommendations";

const available: PublicVenueSearchItem = {
  slug: "brisa-studio",
  name: "Brisa Studio",
  categorySlug: "peluqueria",
  categoryName: "Peluquería",
  descriptionExcerpt: null,
  mainImageUrl: null,
  city: "Santiago de Compostela",
  province: "A Coruña",
  country: "ES",
  statusCode: "available",
  statusLabel: "Disponible",
  availabilitySummary: "Disponible hoy",
  bookingAvailable: true,
  latitude: 42.88,
  longitude: -8.54,
};

describe("buildPublicRecommendationFallback", () => {
  it("excluye indisponibles, deduplica y conserva una explicación auditable", () => {
    const result = buildPublicRecommendationFallback(
      [available, { ...available }, { ...available, slug: "cerrado", bookingAvailable: false }],
      { activeFilters: true, limit: 3 },
    );

    expect(result).toHaveLength(1);
    expect(result[0].recommendation).toEqual({
      explanationCode: "MATCHES_ACTIVE_FILTERS",
      policyVersion: "public-availability-fallback-v1",
      strategy: "fallback",
    });
  });
});
