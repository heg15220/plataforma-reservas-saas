import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  confirmReservation,
  createReservationHold,
  fetchPublicReservationForm,
  PublicReservationApiError,
} from "./public-reservation-api";
import { PublicReservationFormView } from "./public-reservation-form";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/features/reservation-booking/reservation-confirmation-storage", () => ({
  storeReservationConfirmation: vi.fn(),
}));
vi.mock("./public-reservation-api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./public-reservation-api")>()),
  createReservationHold: vi.fn(),
  fetchPublicReservationForm: vi.fn(),
  confirmReservation: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(fetchPublicReservationForm).mockResolvedValue({
    venueId: "10000000-0000-4000-8000-000000000001",
    venueSlug: "demo",
    fields: [
      {
        id: null,
        source: "base",
        label: null,
        labelKey: "reservation.form.customerName",
        labelI18n: null,
        key: "customer_name",
        type: "short_text",
        required: true,
        editable: false,
        options: null,
        optionsI18n: null,
        position: 0,
      },
      {
        id: null,
        source: "base",
        label: null,
        labelKey: "reservation.form.customerEmail",
        labelI18n: null,
        key: "customer_email",
        type: "email",
        required: true,
        editable: false,
        options: null,
        optionsI18n: null,
        position: 1,
      },
    ],
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("PublicReservationFormView", () => {
  it("shows the hold countdown and renders the published fields", async () => {
    arrangeActiveHold();
    renderWithIntl(
      <PublicReservationFormView
        timeSlotId="30000000-0000-4000-8000-000000000003"
        venueSlug="demo"
      />,
    );

    fireEvent.click(await screen.findByRole("button", { name: /empezar reserva/i }));

    expect(await screen.findByRole("textbox", { name: /Nombre/i })).toBeVisible();
    expect(await screen.findByText(/Tiempo para completar: 09:/i)).toBeVisible();
    expect(screen.getByRole("link", { name: "política de privacidad" })).toHaveAttribute(
      "href",
      "/legal/privacidad",
    );
    expect(screen.getByRole("link", { name: "normas y condiciones de reserva" })).toHaveAttribute(
      "href",
      "/legal/condiciones",
    );
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeEnabled();
  });

  it("disables confirmation after an expired hold", async () => {
    vi.mocked(createReservationHold).mockResolvedValue({
      reservationId: "20000000-0000-4000-8000-000000000002",
      holdToken: "secret",
      expiresAt: new Date(Date.now() - 1000).toISOString(),
      remainingSeconds: 0,
    });
    renderWithIntl(
      <PublicReservationFormView
        timeSlotId="30000000-0000-4000-8000-000000000003"
        venueSlug="demo"
      />,
    );

    fireEvent.click(await screen.findByRole("button", { name: /empezar reserva/i }));

    expect(await screen.findByText(/tiempo de reserva ha terminado/i)).toBeVisible();
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeDisabled();
  });

  it("shows the localized restriction date and prevents a futile retry", async () => {
    arrangeActiveHold();
    vi.mocked(confirmReservation).mockRejectedValue(
      new PublicReservationApiError("activeRestriction", "2026-08-01"),
    );
    renderWithIntl(
      <PublicReservationFormView
        timeSlotId="30000000-0000-4000-8000-000000000003"
        venueSlug="demo"
      />,
    );
    fireEvent.click(await screen.findByRole("button", { name: /empezar reserva/i }));
    fireEvent.change(await screen.findByRole("textbox", { name: "Nombre" }), {
      target: { value: "María" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Correo electrónico" }), {
      target: { value: "maria@example.com" },
    });
    fireEvent.click(screen.getByRole("checkbox", { name: /política de privacidad/i }));
    fireEvent.click(screen.getByRole("checkbox", { name: /normas y condiciones de reserva/i }));

    fireEvent.click(screen.getByRole("button", { name: "Confirmar reserva" }));

    expect(
      await screen.findByText(
        /restricción temporal para realizar reservas hasta el 1 de agosto de 2026/i,
      ),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeDisabled();
  });
});

function arrangeActiveHold() {
  vi.mocked(createReservationHold).mockResolvedValue({
    reservationId: "20000000-0000-4000-8000-000000000002",
    holdToken: "secret",
    expiresAt: new Date(Date.now() + 600_000).toISOString(),
    remainingSeconds: 125,
  });
}
