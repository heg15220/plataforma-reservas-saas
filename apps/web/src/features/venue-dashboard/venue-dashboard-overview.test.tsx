import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  fetchVenueReservationsForDay,
  VenueReservationsApiError,
} from "@/features/venue-reservations/venue-reservations-api";
import { reservationList } from "@/features/venue-reservations/venue-reservations-test-fixtures";
import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueDashboardOverview } from "./venue-dashboard-overview";

vi.mock("@/features/venue-reservations/venue-reservations-api", async (importOriginal) => ({
  ...(await importOriginal<
    typeof import("@/features/venue-reservations/venue-reservations-api")
  >()),
  fetchVenueReservationsForDay: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(fetchVenueReservationsForDay).mockResolvedValue(reservationList());
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueDashboardOverview", () => {
  it("summarizes today's private schedule and exposes focused mobile actions", async () => {
    renderWithIntl(<VenueDashboardOverview initialDate="2026-07-26" />);

    expect(await screen.findByText("Ana Martín")).toBeVisible();
    expect(screen.getByLabelText("Resumen de métricas del día")).toBeVisible();
    expect(screen.getByText("Por atender")).toBeVisible();
    expect(screen.queryByText("ana@example.com")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver agenda" })).toHaveAttribute(
      "href",
      "/panel/reservas?date=2026-07-26",
    );
    expect(screen.getByRole("link", { name: "Incidencias" })).toHaveAttribute(
      "href",
      "/panel/incidencias",
    );
    expect(fetchVenueReservationsForDay).toHaveBeenCalledWith(
      "2026-07-26",
      expect.any(AbortSignal),
    );
  });

  it("refreshes only the daily overview on demand", async () => {
    renderWithIntl(<VenueDashboardOverview initialDate="2026-07-26" />);
    await screen.findByText("Ana Martín");

    fireEvent.click(screen.getByRole("button", { name: "Actualizar resumen" }));

    await waitFor(() => expect(fetchVenueReservationsForDay).toHaveBeenCalledTimes(2));
  });

  it("permite crear el primer local cuando la cuenta todavía no tiene perfil", async () => {
    vi.mocked(fetchVenueReservationsForDay).mockRejectedValue(
      new VenueReservationsApiError("notFound"),
    );

    renderWithIntl(<VenueDashboardOverview initialDate="2026-07-26" />);

    expect(await screen.findByRole("heading", { name: "Crea tu primer local" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Crear mi local" })).toHaveAttribute(
      "href",
      "/panel/perfil",
    );
    expect(screen.queryByText("No se encontró la agenda del local.")).not.toBeInTheDocument();
  });
});
