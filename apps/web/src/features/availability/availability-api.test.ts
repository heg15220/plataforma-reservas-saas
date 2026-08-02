import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { deleteTimeSlots, fetchPublicAvailability, saveOpeningHours } from "./availability-api";

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

describe("availability api", () => {
  it("loads public availability without forwarding credentials", async () => {
    fetchMock.mockResolvedValue(
      Response.json({
        venueSlug: "casa-luz",
        date: "2026-07-13",
        weekday: 1,
        statusCode: "open",
        statusLabel: "Open",
        bookingAvailable: true,
        closed: false,
        reservationsEnabled: true,
        source: "weekly_schedule",
        availableSlotCount: 0,
        slots: [],
      }),
    );

    await fetchPublicAvailability("casa luz", "2026-07-13", "en");

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/api/public/venues/casa%20luz/availability");
    expect(String(url)).toContain("date=2026-07-13");
    expect(String(url)).toContain("locale=en");
    expect(init?.credentials).toBe("omit");
  });

  it("sends the complete weekly snapshot with HttpOnly session credentials", async () => {
    const days = Array.from({ length: 7 }, (_, index) => ({
      weekday: index + 1,
      closed: false,
      reservationsEnabled: true,
      opensAt: "09:00",
      closesAt: "18:00",
    }));
    fetchMock.mockResolvedValue(
      Response.json({
        days: days.map((day, index) => ({
          ...day,
          id: `10000000-0000-4000-8000-00000000000${index + 1}`,
          opensAt: "09:00:00",
          closesAt: "18:00:00",
        })),
      }),
    );

    const result = await saveOpeningHours(days);
    const [, init] = fetchMock.mock.calls[0];

    expect(init?.credentials).toBe("include");
    expect(init?.method).toBe("PUT");
    expect(JSON.parse(String(init?.body))).toEqual({ days });
    expect(result).toHaveLength(7);
  });

  it("deletes the authenticated venue slots for a single date", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await deleteTimeSlots("2026-07-13");

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/api/venue/me/time-slots?date=2026-07-13");
    expect(init?.credentials).toBe("include");
    expect(init?.method).toBe("DELETE");
  });

  it("identifies a deletion blocked by reservation history", async () => {
    fetchMock.mockResolvedValue(
      Response.json({ error: "TIME_SLOT_DELETE_CONFLICT" }, { status: 409 }),
    );

    await expect(deleteTimeSlots("2026-07-13")).rejects.toMatchObject({ kind: "referenced" });
  });
});
