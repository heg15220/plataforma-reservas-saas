import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { searchPublicVenues } from "./public-search-api";

const apiResponse = {
  locale: "es",
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
  results: [
    {
      slug: "casa-luz",
      name: "Casa Luz",
      categorySlug: "restaurante",
      categoryName: "Restaurante",
      descriptionExcerpt: "Cocina de temporada",
      mainImageUrl: null,
      city: "Madrid",
      province: "Madrid",
      country: "ES",
      statusCode: "available",
      statusLabel: "Disponible",
      availabilitySummary: "Acepta reservas cuando tenga franjas publicadas.",
      bookingAvailable: true,
      latitude: 40.416775,
      longitude: -3.70379,
    },
  ],
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://public-api.test/");
  vi.stubEnv("RESERLY_API_INTERNAL_URL", "http://internal-api.test/");
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllEnvs();
});

describe("searchPublicVenues", () => {
  it("llama al endpoint público con filtros soportados y valida la respuesta", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => apiResponse,
    });
    vi.stubGlobal("fetch", fetchMock);

    const response = await searchPublicVenues("es", {
      category: "restaurante",
      location: " Madrid ",
      q: " cafe ",
      size: 3,
      sort: "availability",
    });

    expect(response.results[0].statusCode).toBe("available");
    expect(fetchMock).toHaveBeenCalledWith(
      new URL(
        "http://internal-api.test/api/public/venues/search?locale=es&q=cafe&location=Madrid&category=restaurante&sort=availability&size=3",
      ),
      { cache: "no-store" },
    );
  });
});
