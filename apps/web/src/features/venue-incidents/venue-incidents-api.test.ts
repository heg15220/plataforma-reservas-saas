import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  fetchVenueBookingRules,
  fetchVenueIncidentHistory,
  reportReservationNoShow,
  updateReservationAttendance,
  updateVenueBookingRules,
} from "./venue-incidents-api";

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

describe("venue incidents api", () => {
  it("loads and updates booking rules with private credentials", async () => {
    const rules = {
      cancellationAllowed: true,
      freeCancellationUntilMinutesBefore: 1440,
      updatedAt: "2026-07-27T10:00:00Z",
    };
    fetchMock
      .mockResolvedValueOnce(Response.json(rules))
      .mockResolvedValueOnce(Response.json(rules));

    await fetchVenueBookingRules();
    await updateVenueBookingRules({
      cancellationAllowed: true,
      freeCancellationUntilMinutesBefore: 1440,
    });

    expect(String(fetchMock.mock.calls[0][0])).toContain("/api/venue/me/booking-rules");
    expect(fetchMock.mock.calls[0][1]?.credentials).toBe("include");
    expect(fetchMock.mock.calls[1][1]).toEqual(
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({
          cancellationAllowed: true,
          freeCancellationUntilMinutesBefore: 1440,
        }),
      }),
    );
  });

  it("uses a reservation reference for history and posts critical actions", async () => {
    fetchMock
      .mockResolvedValueOnce(
        Response.json({
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          items: [],
        }),
      )
      .mockResolvedValueOnce(
        Response.json({
          reservationId: "10000000-0000-4000-8000-000000000001",
          status: "no_show",
          attendanceMarkedAt: "2026-07-27T10:00:00Z",
          updatedAt: "2026-07-27T10:00:00Z",
        }),
      )
      .mockResolvedValueOnce(
        Response.json({
          incidentId: "20000000-0000-4000-8000-000000000002",
          reservationId: "10000000-0000-4000-8000-000000000001",
          status: "reported",
          reportedAt: "2026-07-27T10:00:00Z",
        }),
      );

    const id = "10000000-0000-4000-8000-000000000001";
    await fetchVenueIncidentHistory(id);
    await updateReservationAttendance(id, "no_show");
    await reportReservationNoShow(id, "Verificado");

    expect(String(fetchMock.mock.calls[0][0])).toContain(`reservationId=${id}`);
    expect(fetchMock.mock.calls[1][1]?.body).toBe(JSON.stringify({ status: "no_show" }));
    expect(fetchMock.mock.calls[2][1]?.body).toBe(
      JSON.stringify({ confirmed: true, notes: "Verificado" }),
    );
  });
});
