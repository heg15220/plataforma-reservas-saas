import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchVenueReviews } from "./venue-reviews-api";

const validPage = {
  averageRating: 4.5,
  reviewsCount: 2,
  items: [
    {
      id: "10000000-0000-4000-8000-000000000001",
      rating: 5,
      comment: "Atención excelente.",
      createdAt: "2026-07-28T10:00:00Z",
    },
  ],
  page: 0,
  size: 20,
  totalPages: 1,
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://api.test/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("fetchVenueReviews", () => {
  it("envía cookie y paginación acotada y valida la respuesta", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(validPage), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchVenueReviews(0)).resolves.toEqual(validPage);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://api.test/api/venue/me/reviews?page=0&size=20"),
      expect.objectContaining({ cache: "no-store", credentials: "include" }),
    );
  });

  it("clasifica permisos y rechaza contratos que exponen datos inesperados", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 403 })));
    await expect(fetchVenueReviews(0)).rejects.toMatchObject({
      kind: "forbidden",
    });

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ ...validPage, averageRating: 7 }), { status: 200 }),
      ),
    );
    await expect(fetchVenueReviews(0)).rejects.toMatchObject({
      kind: "unavailable",
    });
  });
});
