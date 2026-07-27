import { afterEach, describe, expect, it, vi } from "vitest";

import {
  confirmReservation,
  createReservationHold,
  fetchPublicReservationForm,
} from "./public-reservation-api";

afterEach(() => vi.unstubAllGlobals());

describe("public reservation API", () => {
  it("requests the published schema using an encoded slug", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        venueId: "10000000-0000-4000-8000-000000000001",
        venueSlug: "café norte",
        fields: [],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await fetchPublicReservationForm("café norte");

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/public/venues/caf%C3%A9%20norte/reservation-form"),
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("creates a hold with the exact public selection", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        reservationId: "20000000-0000-4000-8000-000000000002",
        holdToken: "secret",
        expiresAt: "2026-07-21T12:05:00Z",
        remainingSeconds: 300,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await createReservationHold({
      venueId: "10000000-0000-4000-8000-000000000001",
      timeSlotId: "30000000-0000-4000-8000-000000000003",
      partySize: 1,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/public/reservations/holds"),
      expect.objectContaining({
        body: expect.stringContaining('"partySize":1'),
        method: "POST",
      }),
    );
  });

  it("preserves only the end date from an active booking restriction", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({
        error: "ACTIVE_BOOKING_RESTRICTION",
        restrictedUntil: "2026-08-01",
        internalCounter: 4,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const confirmation = confirmReservation("20000000-0000-4000-8000-000000000002", {
      holdToken: "secret",
      customerName: "María",
      customerEmail: "maria@example.com",
      locale: "es",
      partySize: 1,
      formResponses: [],
      acceptsPrivacyPolicy: true,
      acceptsBookingRules: true,
    });

    await expect(confirmation).rejects.toMatchObject({
      kind: "activeRestriction",
      restrictedUntil: "2026-08-01",
    });
  });
});
