import { cleanup, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchVenueSubscription, type VenueSubscription } from "./venue-subscription-api";
import { VenueSubscriptionDashboard } from "./venue-subscription-dashboard";

vi.mock("./venue-subscription-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-subscription-api")>();
  return { ...original, fetchVenueSubscription: vi.fn() };
});

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
const professionalPlan = {
  slug: "professional",
  name: "Profesional",
  priceMonthly: 29,
  priceYearly: 290,
  limits: {
    monthlyReservations: 1000,
    teamResources: 10,
    customFormFields: 20,
    galleryImages: 20,
  },
  features: [
    { code: "online_booking", label: "Reservas online" },
    { code: "team_management", label: "Gestión de equipo" },
  ],
};
const disabledSubscription: VenueSubscription = {
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
  availablePlans: [freePlan, professionalPlan],
};

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueSubscriptionDashboard", () => {
  it("muestra plan, estado y catálogo sin acción ni aviso RedSys cuando el cobro está apagado", async () => {
    vi.mocked(fetchVenueSubscription).mockResolvedValue(disabledSubscription);

    renderWithIntl(<VenueSubscriptionDashboard />);

    await waitFor(() => expect(screen.getAllByText("Gratuito").length).toBeGreaterThan(0));
    expect(screen.getByText("Activa")).toBeVisible();
    expect(screen.getByText("Monetización aún no activada")).toBeVisible();
    expect(screen.getByText("Profesional")).toBeVisible();
    expect(screen.getByText("Historial de facturación")).toBeVisible();
    expect(screen.queryByText(/RedSys/)).not.toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it("muestra el aviso externo RedSys únicamente cuando el backend habilita pagos reales", async () => {
    vi.mocked(fetchVenueSubscription).mockResolvedValue({
      ...disabledSubscription,
      currentPlan: professionalPlan,
      subscriptionStatus: "pending_payment",
      billingPeriod: "yearly",
      renewalAt: "2027-07-29T10:00:00Z",
      monetization: {
        status: "real_payments_enabled",
        realPaymentsEnabled: true,
        secureExternalPaymentNoticeRequired: true,
        provider: "redsys",
      },
    });

    renderWithIntl(<VenueSubscriptionDashboard />);

    await waitFor(() => expect(screen.getByText("Pago seguro externo")).toBeVisible());
    expect(screen.getByText(/mediante RedSys/)).toBeVisible();
    expect(screen.getByText("Pago pendiente")).toBeVisible();
    expect(screen.queryByText("Monetización aún no activada")).not.toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
