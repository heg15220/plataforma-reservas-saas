import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithIntl } from "@/test-utils/render-with-intl";
import { PublicReservationFormView } from "./public-reservation-form";
import { createReservationHold, fetchPublicReservationForm } from "./public-reservation-api";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/features/reservation-booking/reservation-confirmation-storage", () => ({ storeReservationConfirmation: vi.fn() }));
vi.mock("./public-reservation-api", async importOriginal => ({ ...(await importOriginal<typeof import("./public-reservation-api")>()), createReservationHold: vi.fn(), fetchPublicReservationForm: vi.fn(), confirmReservation: vi.fn() }));

beforeEach(() => {
  vi.mocked(fetchPublicReservationForm).mockResolvedValue({ venueId: "10000000-0000-4000-8000-000000000001", venueSlug: "demo", fields: [{ id: null, source: "base", label: null, labelKey: "reservation.form.customerName", labelI18n: null, key: "customer_name", type: "short_text", required: true, editable: false, options: null, optionsI18n: null, position: 0 }] });
});
afterEach(() => { cleanup(); vi.clearAllMocks(); });

describe("PublicReservationFormView", () => {
  it("shows the hold countdown and renders the published fields", async () => {
    vi.mocked(createReservationHold).mockResolvedValue({ reservationId: "20000000-0000-4000-8000-000000000002", holdToken: "secret", expiresAt: new Date(Date.now() + 600000).toISOString(), remainingSeconds: 125 });
    renderWithIntl(<PublicReservationFormView venueSlug="demo" timeSlotId="30000000-0000-4000-8000-000000000003" />);
    fireEvent.click(await screen.findByRole("button", { name: /empezar reserva/i }));
    expect(await screen.findByRole("textbox", { name: /Nombre/i })).toBeVisible();
    expect(await screen.findByText(/Tiempo para completar: 09:/i)).toBeVisible();
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeEnabled();
  });

  it("disables confirmation after an expired hold", async () => {
    vi.mocked(createReservationHold).mockResolvedValue({ reservationId: "20000000-0000-4000-8000-000000000002", holdToken: "secret", expiresAt: new Date(Date.now() - 1000).toISOString(), remainingSeconds: 0 });
    renderWithIntl(<PublicReservationFormView venueSlug="demo" timeSlotId="30000000-0000-4000-8000-000000000003" />);
    fireEvent.click(await screen.findByRole("button", { name: /empezar reserva/i }));
    expect(await screen.findByText(/tiempo de reserva ha terminado/i)).toBeVisible();
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeDisabled();
  });
});