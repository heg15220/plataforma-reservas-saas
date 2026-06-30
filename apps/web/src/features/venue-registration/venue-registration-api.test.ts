import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { registerVenue } from "./venue-registration-api";
import type { VenueRegistrationPayload } from "./venue-registration-schema";

const payload: VenueRegistrationPayload = {
  account: {
    email: "negocio@example.com",
    password: "correct-horse-battery",
    preferredLocale: "es",
  },
  business: {
    taxCountry: "ES",
    legalName: "Ejemplo Reservas SL",
    taxIdentifier: "B12345674",
    registeredAddress: "",
  },
  acceptsLegalTerms: true,
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("registerVenue", () => {
  it("envía el payload al endpoint empresarial con el contrato esperado", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          accountType: "venue_business",
          businessVerificationStatus: "pending_remote_check",
          emailVerificationRequired: true,
          canPublishVenue: false,
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(registerVenue(payload)).resolves.toMatchObject({
      accountType: "venue_business",
      emailVerificationRequired: true,
      canPublishVenue: false,
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/venues/register",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        body: JSON.stringify(payload),
      }),
    );
  });

  it.each([
    [400, "invalid"],
    [409, "conflict"],
    [429, "rateLimited"],
    [503, "unavailable"],
  ] as const)("reduce HTTP %i a la categoría pública %s", async (status, kind) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status })));

    await expect(registerVenue(payload)).rejects.toMatchObject({ kind });
  });

  it("convierte fallos de red en un error no sensible", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("detalle interno")));

    await expect(registerVenue(payload)).rejects.toMatchObject({ kind: "unavailable" });
  });
});
