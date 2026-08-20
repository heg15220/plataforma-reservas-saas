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
  demandMetrics: demandMetricsFixture(),
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

function demandMetricsFixture() {
  return {
    status: "available" as const,
    policyVersion: "booking-attribution-v1" as const,
    definitionsVersion: "demand-commercial-metrics-v1" as const,
    timeZone: "Europe/Madrid",
    minimumSampleSize: 10,
    eligibleReservations: 10,
    classifiedReservations: 10,
    coveragePercent: 100,
    newCustomers: 4,
    originatedReservations: 7,
    offPeakCovered: 3,
    attributedIncome: 245,
    attributedCurrency: "EUR",
    incomeStatus: "available" as const,
    directReservations: 3,
    assistedReservations: 2,
    generatedReservations: 4,
    recoveredReservations: 1,
    definitions: [
      { key: "newCustomers" as const, definitionCode: "NEW_CUSTOMER_FIRST_CONFIRMED_AT_VENUE" },
      { key: "originatedReservations" as const, definitionCode: "NON_DIRECT_ATTRIBUTION_CLASSES" },
      { key: "offPeakCovered" as const, definitionCode: "WEEKDAY_14_TO_18_LOCAL_NON_DIRECT" },
      {
        key: "attributedIncome" as const,
        definitionCode: "VISIBLE_PRICE_ASSOCIATED_NOT_INCREMENTAL",
      },
      { key: "coverage" as const, definitionCode: "CLASSIFIED_OVER_CONFIRMED_PERIOD" },
    ],
  };
}

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
