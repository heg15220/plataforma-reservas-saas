import { screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { beforeEach, describe, expect, it } from "vitest";
import type { ReactElement } from "react";

import messages from "../../../locales/es.json";
import { renderWithIntl } from "@/test-utils/render-with-intl";
import { PublicReservationConfirmation } from "./public-reservation-confirmation";
import { storeReservationConfirmation } from "./reservation-confirmation-storage";

const RESERVATION_ID = "9dc288e1-b8ef-4c17-b633-ebbe4dfc9774";
const confirmationMessages = {
  ...messages,
  ReservationBooking: {
    confirmation: {
      loading: "Cargando confirmación",
      eyebrow: "Reserva confirmada",
      title: "Tu reserva está lista",
      description: "Hemos enviado los detalles a {email}.",
      details: { title: "Resumen", venue: "Local", date: "Fecha", time: "Hora", partySize: "Personas", people: "{count} personas" },
      emailNotice: "Revisa {email} para gestionar la reserva.",
      privacyNotice: "Por privacidad, el enlace de gestión solo se envía por email.",
      steps: { ariaLabel: "Progreso de la reserva", select: "Seleccionar", form: "Formulario", confirmation: "Confirmación" },
      actions: { explore: "Explorar más locales", home: "Volver al inicio" },
      missing: { title: "Confirmación no disponible", description: "No encontramos los datos de esta confirmación.", action: "Buscar locales" },
    },
  },
};

describe("PublicReservationConfirmation", () => {
  beforeEach(() => window.sessionStorage.clear());

  it("muestra el resumen validado y el aviso de gestión por email", async () => {
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
    renderConfirmation(<PublicReservationConfirmation reservationId={RESERVATION_ID} />);
    expect(await screen.findByRole("heading", { level: 1, name: "Tu reserva está lista" })).toBeVisible();
    expect(screen.getByText("La Mesa Azul")).toBeVisible();
    expect(screen.getByText("19:30 – 21:00")).toBeVisible();
    expect(screen.getByText("4 personas")).toBeVisible();
    expect(screen.getByText(/Revisa maria@example.com/)).toBeVisible();
    expect(screen.queryByText(RESERVATION_ID)).not.toBeInTheDocument();
  });

  it("no revela datos cuando el snapshot no existe", async () => {
    renderConfirmation(<PublicReservationConfirmation reservationId={RESERVATION_ID} />);
    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Confirmación no disponible" })).toBeVisible());
    expect(screen.getByRole("link", { name: "Buscar locales" })).toHaveAttribute("href", "/explorar");
  });
});

function renderConfirmation(ui: ReactElement) {
  return renderWithIntl(
    <NextIntlClientProvider locale="es" messages={confirmationMessages}>{ui}</NextIntlClientProvider>,
  );
}