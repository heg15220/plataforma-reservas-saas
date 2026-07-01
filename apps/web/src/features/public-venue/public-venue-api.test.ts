import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  getPublicVenue,
  PublicVenueNotFoundError,
  resolvePublicAssetUrl,
} from "./public-venue-api";

const validVenue = {
  slug: "casa-luz",
  locale: "es",
  name: "Casa Luz",
  categorySlug: "restaurante",
  categoryName: "Restaurante",
  description: "Cocina de temporada",
  services: null,
  rules: null,
  publicText: null,
  mainImageUrl: "/api/public/venue-images/id/main",
  gallery: [],
  address: "Calle Mayor, 1",
  city: "Madrid",
  province: null,
  country: "ES",
  postalCode: null,
  latitude: 40.416775,
  longitude: -3.70379,
  phone: null,
  contactEmail: null,
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://public.test/");
  vi.stubEnv("RESERLY_API_INTERNAL_URL", "http://api:8080/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("getPublicVenue", () => {
  it("solicita el slug y locale al origen interno sin cookies ni caché", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(validVenue), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getPublicVenue("casa luz", "es")).resolves.toEqual(validVenue);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://api:8080/api/public/venues/casa%20luz?locale=es"),
      { cache: "no-store" },
    );
  });

  it("distingue 404 y rechaza contratos alterados", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 404 })));
    await expect(getPublicVenue("oculto", "es")).rejects.toBeInstanceOf(PublicVenueNotFoundError);

    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify({ ...validVenue, locale: "fr" }), { status: 200 }),
        ),
    );
    await expect(getPublicVenue("casa-luz", "es")).rejects.toBeTruthy();
  });

  it("resuelve imágenes relativas contra el origen público", () => {
    expect(resolvePublicAssetUrl("/api/public/image")).toBe("http://public.test/api/public/image");
  });
});
