import { afterEach, describe, expect, it, vi } from "vitest";

import {
  cancelManagedReservation,
  fetchManagedReservation,
} from "./reservation-management-api";

afterEach(() => vi.restoreAllMocks());

describe("reservation management API", () => {
  it("loads the projection bound to the encoded secret", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          reservationId: "9dc288e1-b8ef-4c17-b633-ebbe4dfc9774",
          venueName: "La Mesa Azul",
          venueAddress: "Calle Mayor 1",
          date: "2026-08-01",
          startsAt: "10:00:00",
          endsAt: "11:00:00",
          partySize: 2,
          status: "confirmed",
          cancellable: true,
          cancellationDeadline: "2026-07-31T08:00:00Z",
          cancellationNoticeMinutes: 1440,
        }),
        { status: 200 },
      ),
    );

    const reservation = await fetchManagedReservation("secret/value");

    expect(reservation.cancellable).toBe(true);
    expect(fetchMock.mock.calls[0]?.[0]).toContain("secret%2Fvalue");
  });

  it("maps deadline conflicts without exposing response details", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 409 }));

    await expect(cancelManagedReservation("secret")).rejects.toMatchObject({ code: "deadline" });
  });
});
