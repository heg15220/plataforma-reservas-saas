import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchVenueEmailAssignments, updateVenueEmailAssignment } from "./venue-email-api";

const assignment = {
  venueId: "d3000000-0000-4000-8000-000000000001",
  venueName: "Ames Padel Center",
  venueSlug: "ames-padel-center",
  email: "reservas@ames.local",
  panelAccessConfigured: true,
  updatedAt: "2026-08-02T20:00:00Z",
};

beforeEach(() => vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/"));

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("venue email API", () => {
  it("lista asociaciones usando la sesión HttpOnly", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ assignments: [assignment] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchVenueEmailAssignments()).resolves.toEqual([assignment]);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/venue/me/email-assignments",
      expect.objectContaining({ credentials: "include", method: "GET" }),
    );
  });

  it("actualiza exclusivamente el local indicado", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json(assignment));
    vi.stubGlobal("fetch", fetchMock);

    await updateVenueEmailAssignment(assignment.venueId, assignment.email, "UnaClaveSegura2026!");

    expect(fetchMock).toHaveBeenCalledWith(
      `http://localhost:8080/api/venue/me/email-assignments/${assignment.venueId}`,
      expect.objectContaining({
        body: JSON.stringify({
          email: assignment.email,
          password: "UnaClaveSegura2026!",
        }),
        credentials: "include",
        method: "PUT",
      }),
    );
  });

  it("reduce un local ajeno o no publicado a notFound", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 404 })));

    await expect(
      updateVenueEmailAssignment(assignment.venueId, assignment.email, "UnaClaveSegura2026!"),
    ).rejects.toMatchObject({ kind: "notFound" });
  });
});
