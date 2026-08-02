import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchPublicAvailability } from "@/features/availability/availability-api";
import { getPublicVenue } from "@/features/public-venue/public-venue-api";

import { enrichHomeVenueCards } from "./home-venue-enrichment";

vi.mock("@/features/availability/availability-api", () => ({
  fetchPublicAvailability: vi.fn(),
}));
vi.mock("@/features/public-venue/public-venue-api", () => ({
  getPublicVenue: vi.fn(),
}));

const venue = {
  slug: "ames-padel-center",
  name: "Ames Padel Center",
  categorySlug: "pista-de-padel",
  categoryName: "Pista de pádel",
  descriptionExcerpt: "Pádel cubierto",
  mainImageUrl: null,
  city: "Ames",
  province: "A Coruña",
  country: "ES",
  statusCode: "availability_pending" as const,
  statusLabel: "Disponibilidad pendiente",
  availabilitySummary: "Pendiente",
  bookingAvailable: false,
  latitude: 42.85965,
  longitude: -8.65172,
};

afterEach(() => vi.clearAllMocks());

describe("enrichHomeVenueCards", () => {
  it("añade dirección completa y proyecta la disponibilidad real como abierta", async () => {
    vi.mocked(getPublicVenue).mockResolvedValue({
      address: "Firmistáns 10A",
      postalCode: "15895",
      city: "Ames",
      province: "A Coruña",
      country: "ES",
    } as Awaited<ReturnType<typeof getPublicVenue>>);
    vi.mocked(fetchPublicAvailability).mockResolvedValue({
      statusCode: "open",
      statusLabel: "Abierto",
      bookingAvailable: true,
    } as Awaited<ReturnType<typeof fetchPublicAvailability>>);

    const [result] = await enrichHomeVenueCards([venue], "es", new Date("2026-08-02T10:00:00Z"));

    expect(fetchPublicAvailability).toHaveBeenCalledWith("ames-padel-center", "2026-08-02", "es");
    expect(result).toMatchObject({
      address: "Firmistáns 10A",
      postalCode: "15895",
      statusCode: "available",
      statusLabel: "Abierto",
      bookingAvailable: true,
    });
  });

  it("conserva la tarjeta base cuando fallan ambas lecturas complementarias", async () => {
    vi.mocked(getPublicVenue).mockRejectedValue(new Error("profile unavailable"));
    vi.mocked(fetchPublicAvailability).mockRejectedValue(new Error("availability unavailable"));

    await expect(enrichHomeVenueCards([venue], "es")).resolves.toEqual([venue]);
  });
});
