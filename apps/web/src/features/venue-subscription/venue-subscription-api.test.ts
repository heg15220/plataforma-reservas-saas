import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchVenueSubscription } from "./venue-subscription-api";

const freePlan = {
  slug: "free",
  name: "Gratuito",
  priceMonthly: 0,
  priceYearly: 0,
  limits: {
    monthlyReservations: 100,
    teamResources: 1,
    customFormFields: 3,
    galleryImages: 3,
  },
  features: [{ code: "online_booking", label: "Reservas online" }],
};

const validSubscription = {
  currentPlan: freePlan,
  subscriptionStatus: "active",
  billingPeriod: "monthly",
  renewalAt: null,
  trialEndsAt: null,
  cancelledAt: null,
  monetization: {
    status: "disabled",
    realPaymentsEnabled: false,
    secureExternalPaymentNoticeRequired: false,
    provider: null,
  },
  availablePlans: [freePlan],
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://api.test/");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("fetchVenueSubscription", () => {
  it("consulta la ruta privada y valida el resumen minimizado", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(validSubscription), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchVenueSubscription()).resolves.toEqual(validSubscription);
    expect(fetchMock).toHaveBeenCalledWith(
      new URL("http://api.test/api/venue/me/subscription"),
      expect.objectContaining({ cache: "no-store", credentials: "include" }),
    );
  });

  it("clasifica permisos y rechaza PII o monetización incoherente", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 403 })));
    await expect(fetchVenueSubscription()).rejects.toMatchObject({ kind: "forbidden" });

    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(
            JSON.stringify({ ...validSubscription, customerEmail: "private@example.com" }),
          ),
        ),
    );
    await expect(fetchVenueSubscription()).rejects.toMatchObject({ kind: "unavailable" });

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            ...validSubscription,
            monetization: {
              status: "disabled",
              realPaymentsEnabled: true,
              secureExternalPaymentNoticeRequired: false,
              provider: "redsys",
            },
          }),
        ),
      ),
    );
    await expect(fetchVenueSubscription()).rejects.toMatchObject({ kind: "unavailable" });
  });
});
