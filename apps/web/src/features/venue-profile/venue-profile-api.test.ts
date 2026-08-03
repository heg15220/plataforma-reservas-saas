import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  fetchVenueCategories,
  fetchVenueProfile,
  fetchVenueProfiles,
  publishVenueProfile,
  resolveVenueAssetUrl,
  saveVenueProfile,
  VenueProfileApiError,
} from "./venue-profile-api";
import type { VenueProfilePayload } from "./venue-profile-schema";

const categoryPayload = [
  { id: "20000000-0000-0000-0000-000000000001", slug: "restaurante", name: "Restaurante" },
];

const profilePayload = {
  id: "d3000000-0000-4000-8000-000000000001",
  categoryId: categoryPayload[0].id,
  categorySlug: "restaurante",
  categoryName: "Restaurante",
  name: "Casa Luz",
  slug: "casa-luz",
  description: "Cocina de temporada",
  descriptionI18n: { sourceLocale: "es", values: { es: "Cocina", en: "Cuisine" } },
  servicesI18n: null,
  rulesI18n: null,
  publicTextI18n: null,
  defaultLocale: "es",
  contactEmail: "hola@casaluz.test",
  phone: null,
  address: "Calle Mayor, 1",
  city: "Madrid",
  province: "Madrid",
  country: "ES",
  postalCode: "28013",
  latitude: 40.416775,
  longitude: -3.70379,
  mainImageUrl: "/api/public/venue-images/1/main",
  status: "draft",
  showPhone: false,
  showEmail: true,
  createdAt: "2026-07-01T08:00:00Z",
  updatedAt: "2026-07-01T09:00:00Z",
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

function response(body: unknown, status = 200) {
  return new Response(body === null ? null : JSON.stringify(body), {
    status,
    headers: body === null ? undefined : { "Content-Type": "application/json" },
  });
}

describe("venue profile API", () => {
  it("lista categorías activas con locale explícito", async () => {
    const fetchMock = vi.fn().mockResolvedValue(response(categoryPayload));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchVenueCategories("es")).resolves.toEqual(categoryPayload);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/public/categories?locale=es",
      expect.objectContaining({ credentials: "include", method: "GET" }),
    );
  });

  it("trata 404 del perfil como ausencia editable y valida respuestas correctas", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(response(null, 404))
        .mockResolvedValueOnce(response(profilePayload)),
    );

    await expect(fetchVenueProfile()).resolves.toBeNull();
    await expect(fetchVenueProfile()).resolves.toMatchObject({ name: "Casa Luz" });
  });

  it("expone la capacidad multi-local devuelta por el servidor", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          response({ profiles: [profilePayload], canCreateAdditionalVenue: false }),
        ),
    );

    await expect(fetchVenueProfiles()).resolves.toMatchObject({
      profiles: [{ name: "Casa Luz" }],
      canCreateAdditionalVenue: false,
    });
  });

  it("usa POST para crear, PATCH para actualizar y reduce rechazo de publicación", async () => {
    const payload = {
      name: "Casa Luz",
      categoryId: categoryPayload[0].id,
      descriptionI18n: { sourceLocale: "es", values: { es: "Cocina", en: "Cuisine" } },
      servicesI18n: null,
      rulesI18n: null,
      publicTextI18n: null,
      defaultLocale: "es",
      contactEmail: null,
      phone: null,
      address: null,
      city: null,
      province: null,
      country: null,
      postalCode: null,
      latitude: null,
      longitude: null,
      showPhone: false,
      showEmail: false,
    } satisfies VenueProfilePayload;
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(profilePayload, 201))
      .mockResolvedValueOnce(response(profilePayload))
      .mockResolvedValueOnce(
        response(
          {
            error: "VENUE_PUBLICATION_REJECTED",
            requirements: ["EMAIL_NOT_VERIFIED", "MAIN_IMAGE_MISSING"],
          },
          422,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await saveVenueProfile(payload, null);
    await saveVenueProfile(payload, profilePayload.id);
    await expect(publishVenueProfile(profilePayload.id)).rejects.toMatchObject({
      kind: "publicationRejected",
      requirements: ["EMAIL_NOT_VERIFIED", "MAIN_IMAGE_MISSING"],
    } satisfies Partial<VenueProfileApiError>);

    expect((fetchMock.mock.calls[0] as [string, RequestInit])[1].method).toBe("POST");
    expect((fetchMock.mock.calls[1] as [string, RequestInit])[1].method).toBe("PATCH");
  });

  it("resuelve URLs relativas de imágenes contra el API público", () => {
    expect(resolveVenueAssetUrl("/api/public/venue-images/1/main")).toBe(
      "http://localhost:8080/api/public/venue-images/1/main",
    );
    expect(resolveVenueAssetUrl("https://cdn.test/image.jpg")).toBe("https://cdn.test/image.jpg");
    expect(resolveVenueAssetUrl(null)).toBeNull();
  });
});
