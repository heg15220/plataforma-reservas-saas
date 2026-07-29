import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  checkPublicReviewEligibility,
  createPublicVenueReview,
} from "./public-review-api";

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/");
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe("public review api", () => {
  it("comprueba por slug/email con un contrato sin historial", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          eligible: true,
          canReview: true,
          error: null,
          messageKey: null,
        }),
        { status: 200 },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      checkPublicReviewEligibility("casa-luz", "guest@example.com"),
    ).resolves.toMatchObject({ eligible: true, canReview: true });
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://localhost:8080/api/public/venues/casa-luz/reviews/eligibility"),
      expect.objectContaining({
        body: JSON.stringify({ customerEmail: "guest@example.com" }),
        method: "POST",
      }),
    );
  });

  it("crea sin aceptar reservationId en la respuesta pública", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: "created",
            reviewId: "10000000-0000-4000-8000-000000000001",
            venueId: "20000000-0000-4000-8000-000000000001",
            rating: 5,
            averageRating: 4.8,
            reviewsCount: 12,
            reservationId: "30000000-0000-4000-8000-000000000001",
          }),
          { status: 201 },
        ),
      ),
    );

    await expect(
      createPublicVenueReview("casa-luz", {
        acceptsReviewPolicy: true,
        comment: null,
        customerEmail: "guest@example.com",
        rating: 5,
      }),
    ).rejects.toMatchObject({ kind: "unavailable" });
  });

  it("clasifica rechazos opacos de creación", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(null, { status: 409 })),
    );

    await expect(
      createPublicVenueReview("casa-luz", {
        acceptsReviewPolicy: true,
        comment: null,
        customerEmail: "guest@example.com",
        rating: 5,
      }),
    ).rejects.toMatchObject({ kind: "alreadySubmitted" });
  });
});
