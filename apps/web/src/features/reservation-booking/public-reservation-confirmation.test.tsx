import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { PublicReservationConfirmation } from "./public-reservation-confirmation";
import { storeReservationConfirmation } from "./reservation-confirmation-storage";

const RESERVATION_ID = "9dc288e1-b8ef-4c17-b633-ebbe4dfc9774";

describe("PublicReservationConfirmation", () => {
  beforeEach(() => window.sessionStorage.clear());

  it("muestra el recibo, el aviso por email y la acción de calendario", async () => {
    storeReservationConfirmation({
      status: "confirmed",
      reservationId: RESERVATION_ID,
      manageUrlSentTo: "maria@example.com",
      venueName: "La Mesa Azul",
      date: "2026-07-24",
      startsAt: "19:30:00",
      endsAt: "21:00:00",
      partySize: 4,
    });
    renderWithIntl(<PublicReservationConfirmation reservationId={RESERVATION_ID} />);

    expect(
      await screen.findByRole("heading", { level: 1, name: "¡Reserva confirmada!" }),
    ).toBeVisible();
    expect(screen.getByText("La Mesa Azul")).toBeVisible();
    expect(screen.getByText("19:30 – 21:00")).toBeVisible();
    expect(screen.getByText("4 personas")).toBeVisible();
    expect(screen.getByText(/Revisa maria@example.com/)).toBeVisible();
    expect(screen.getByRole("button", { name: "Añadir al calendario" })).toBeVisible();
    expect(screen.queryByText(RESERVATION_ID)).not.toBeInTheDocument();
  });

  it("no revela datos cuando el snapshot no existe", async () => {
    renderWithIntl(<PublicReservationConfirmation reservationId={RESERVATION_ID} />);
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { level: 1, name: "Confirmación no disponible" }),
      ).toBeVisible(),
    );
    expect(screen.getByRole("link", { name: "Buscar locales" })).toHaveAttribute(
      "href",
      "/explorar",
    );
  });
});
