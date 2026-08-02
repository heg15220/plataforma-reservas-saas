import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueReservationsWorkspace } from "./venue-reservations-workspace";

vi.mock("./venue-reservations-dashboard", () => ({
  VenueReservationsDashboard: ({ initialDate }: { initialDate?: string }) => (
    <div>Agenda operativa {initialDate}</div>
  ),
}));

vi.mock("@/features/availability/venue-internal-calendar", () => ({
  VenueInternalCalendar: ({ startDate }: { startDate?: string }) => (
    <div>Calendario operativo {startDate}</div>
  ),
}));

vi.mock("@/features/availability/venue-availability-manager", () => ({
  VenueAvailabilityManager: ({ initialDate }: { initialDate?: string }) => (
    <div>Disponibilidad operativa {initialDate}</div>
  ),
}));

afterEach(cleanup);

describe("VenueReservationsWorkspace", () => {
  it("unifica agenda, calendario y disponibilidad en la sección de reservas", () => {
    renderWithIntl(<VenueReservationsWorkspace initialDate="2026-08-10" />);

    expect(screen.getByText("Agenda operativa 2026-08-10")).toBeVisible();
    fireEvent.click(screen.getByRole("tab", { name: "Calendario" }));
    expect(screen.getByText("Calendario operativo 2026-08-10")).toBeVisible();
    fireEvent.click(screen.getByRole("tab", { name: "Horarios y disponibilidad" }));
    expect(screen.getByText("Disponibilidad operativa 2026-08-10")).toBeVisible();
    expect(screen.getByText("Calendario operativo 2026-08-10")).toBeInTheDocument();
    expect(screen.getByText("Calendario operativo 2026-08-10")).not.toBeVisible();
  });
});
