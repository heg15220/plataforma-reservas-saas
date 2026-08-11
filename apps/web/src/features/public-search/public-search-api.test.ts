import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  fetchPublicSearchCategories,
  fetchPublicSearchSuggestions,
  searchPublicVenues,
} from "./public-search-api";

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
      address: "Calle Mayor 1",
      postalCode: "28013",
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
  vi.unstubAllGlobals();
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
    expect(response.results[0]).toMatchObject({ address: "Calle Mayor 1", postalCode: "28013" });
    expect(fetchMock).toHaveBeenCalledWith(
      new URL(
        "http://public-api.test/api/public/venues/search?locale=es&q=cafe&location=Madrid&category=restaurante&sort=availability&size=3",
      ),
      { cache: "no-store" },
    );
  });

  it("omite filtros vacíos y conserva paginación positiva", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ ...apiResponse, page: 2 }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await searchPublicVenues("en", {
      category: "",
      location: "   ",
      page: 2,
      q: "",
      size: 20,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://public-api.test/api/public/venues/search?locale=en&page=2&size=20"),
      { cache: "no-store" },
    );
  });

  it("rechaza respuestas no correctas del endpoint público", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 503,
      }),
    );

    await expect(searchPublicVenues("es", {})).rejects.toMatchObject({
      message: "PublicErrors.unavailable",
      messageKey: "PublicErrors.unavailable",
      status: 503,
    });
  });

  it("consulta sugerencias acotadas por ámbito y término", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        locale: "es",
        suggestions: [{ kind: "location", value: "Madrid", label: "Madrid", context: "Ciudad" }],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchPublicSearchSuggestions("es", "location", "  Mad  ")).resolves.toEqual([
      { kind: "location", value: "Madrid", label: "Madrid", context: "Ciudad" },
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL(
        "http://public-api.test/api/public/venues/suggestions?locale=es&kind=location&term=mad&limit=8",
      ),
      {
        credentials: "omit",
        headers: { Accept: "application/json" },
        signal: undefined,
      },
    );
  });

  it("no consulta sugerencias con menos de dos caracteres", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchPublicSearchSuggestions("es", "query", "a")).resolves.toEqual([]);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("carga categorías activas desde el catálogo público", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [
        {
          id: "20000000-0000-4000-8000-000000000001",
          slug: "restaurante",
          name: "Restaurante",
        },
      ],
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchPublicSearchCategories("es")).resolves.toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://public-api.test/api/public/categories?locale=es"),
      { next: { revalidate: 300 } },
    );
  });
});
