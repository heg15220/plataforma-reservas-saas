import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchVenueStatistics } from "./venue-statistics-api";

const validStatistics = {
  period: "month",
  fromDate: "2026-07-01",
  toDate: "2026-07-29",
  reservationsCount: 12,
  confirmedCount: 10,
  cancelledCount: 2,
  noShowCount: 1,
  attendedCount: 8,
  occupiedCapacity: 18,
  availableCapacity: 30,
  occupancyRate: 60,
  reviewsCount: 3,
  incidentsCount: 2,
  averageRating: 4.5,
  series: [
    {
      date: "2026-07-28",
      reservationsCount: 5,
      confirmedCount: 4,
      cancelledCount: 1,
      noShowCount: 1,
      attendedCount: 3,
      occupiedCapacity: 8,
      availableCapacity: 10,
      occupancyRate: 80,
      reviewsCount: 1,
      incidentsCount: 2,
      averageRating: 5,
    },
  ],
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://api.test/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("fetchVenueStatistics", () => {
  it("envía el filtro privado y valida un contrato minimizado", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(validStatistics), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchVenueStatistics({ period: "month" })).resolves.toEqual(validStatistics);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://api.test/api/venue/me/statistics?period=month"),
      expect.objectContaining({ cache: "no-store", credentials: "include" }),
    );
  });

  it("codifica rango custom, clasifica errores y rechaza campos inesperados", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify({ ...validStatistics, period: "custom" }), { status: 200 }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await fetchVenueStatistics({
      period: "custom",
      venueId: "20000000-0000-4000-8000-000000000001",
      from: "2026-07-01",
      to: "2026-07-29",
    });
    expect(fetchMock).toHaveBeenCalledWith(
      new URL(
        "http://api.test/api/venue/me/statistics?period=custom&venueId=20000000-0000-4000-8000-000000000001&from=2026-07-01&to=2026-07-29",
      ),
      expect.anything(),
    );

    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 403 })));
    await expect(fetchVenueStatistics({ period: "today" })).rejects.toMatchObject({
      kind: "forbidden",
    });

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ ...validStatistics, customerEmail: "private@example.com" }), {
          status: 200,
        }),
      ),
    );
    await expect(fetchVenueStatistics({ period: "month" })).rejects.toMatchObject({
      kind: "unavailable",
    });
  });
});
