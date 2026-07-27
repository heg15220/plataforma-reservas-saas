import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  cancelVenueReservation,
  fetchVenueReservationDetail,
  fetchVenueReservationsForDay,
} from "./venue-reservations-api";
import { reservationDetail, reservationList } from "./venue-reservations-test-fixtures";

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  vi.clearAllMocks();
});

describe("venue reservations api", () => {
  it("loads one day with private credentials and bounded pagination", async () => {
    fetchMock.mockResolvedValue(Response.json(reservationList()));

    const result = await fetchVenueReservationsForDay("2026-07-26");
    const [url, init] = fetchMock.mock.calls[0];

    expect(String(url)).toContain("/api/venue/me/reservations?");
    expect(String(url)).toContain("period=day");
    expect(String(url)).toContain("date=2026-07-26");
    expect(String(url)).toContain("size=100");
    expect(init?.credentials).toBe("include");
    expect(init?.cache).toBe("no-store");
    expect(result.items[0].customerName).toBe("Ana Martín");
  });

  it("parses the enriched detail without requesting an email or venue id", async () => {
    fetchMock.mockResolvedValue(Response.json(reservationDetail()));

    const result = await fetchVenueReservationDetail("10000000-0000-4000-8000-000000000001");
    const [url] = fetchMock.mock.calls[0];

    expect(String(url)).toBe(
      "http://localhost:8080/api/venue/me/reservations/10000000-0000-4000-8000-000000000001",
    );
    expect(result.formAnswers[0].value).toBe("Ninguna");
    expect(result.assignedResource?.publicAlias).toBe("Lucía");
    expect(result.incidentHistory.totalElements).toBe(1);
  });

  it("maps private authorization failures without reading the response body", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 403 }));

    await expect(fetchVenueReservationsForDay("2026-07-26")).rejects.toEqual(
      expect.objectContaining({ kind: "forbidden" }),
    );
  });

  it("posts an audited venue cancellation reason", async () => {
    fetchMock.mockResolvedValue(
      Response.json({
        reservationId: "10000000-0000-4000-8000-000000000001",
        status: "cancelled_by_venue",
        cancelledAt: "2026-07-27T10:00:00Z",
      }),
    );

    await cancelVenueReservation("10000000-0000-4000-8000-000000000001", "Cierre operativo");
    const [url, init] = fetchMock.mock.calls[0];

    expect(String(url)).toContain("/cancel");
    expect(init).toEqual(
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        body: JSON.stringify({ reason: "Cierre operativo" }),
      }),
    );
  });
});
