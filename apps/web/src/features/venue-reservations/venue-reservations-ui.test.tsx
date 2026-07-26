import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  fetchVenueReservationDetail,
  fetchVenueReservationsForDay,
} from "./venue-reservations-api";
import { VenueReservationDetailPanel } from "./venue-reservation-detail-panel";
import { VenueReservationsDashboard } from "./venue-reservations-dashboard";
import {
  reservationDetail,
  reservationList,
} from "./venue-reservations-test-fixtures";

vi.mock("./venue-reservations-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-reservations-api")>();
  return {
    ...original,
    fetchVenueReservationDetail: vi.fn(),
    fetchVenueReservationsForDay: vi.fn(),
  };
});

beforeEach(() => {
  vi.mocked(fetchVenueReservationsForDay).mockResolvedValue(reservationList());
  vi.mocked(fetchVenueReservationDetail).mockResolvedValue(reservationDetail());
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueReservationsDashboard", () => {
  it("shows the daily schedule, metrics, state, and responsive detail action", async () => {
    renderWithIntl(<VenueReservationsDashboard initialDate="2026-07-26" />);

    expect(await screen.findByText("Ana Martín")).toBeVisible();
    expect(screen.getByText("ana@example.com")).toBeVisible();
    expect(screen.getByText("2 personas")).toBeVisible();
    expect(screen.getByText("Confirmada")).toBeVisible();
    expect(screen.getByRole("link", { name: "Ver detalle" })).toHaveAttribute(
      "href",
      "/panel/reservas/10000000-0000-4000-8000-000000000001",
    );
    expect(fetchVenueReservationsForDay).toHaveBeenCalledWith(
      "2026-07-26",
      expect.any(AbortSignal),
    );
  });

  it("refreshes after focus and loads a newly confirmed reservation", async () => {
    const initial = reservationList();
    const updated = {
      ...initial,
      items: [
        ...initial.items,
        {
          ...initial.items[0],
          id: "10000000-0000-4000-8000-000000000002",
          customerName: "Pablo Ruiz",
          customerEmail: "pablo@example.com",
        },
      ],
      totalElements: 2,
    };
    vi.mocked(fetchVenueReservationsForDay)
      .mockResolvedValueOnce(initial)
      .mockResolvedValueOnce(updated);
    renderWithIntl(<VenueReservationsDashboard initialDate="2026-07-26" />);
    expect(await screen.findByText("Ana Martín")).toBeVisible();

    fireEvent.focus(window);

    expect(await screen.findByText("Pablo Ruiz")).toBeVisible();
    await waitFor(() => expect(fetchVenueReservationsForDay).toHaveBeenCalledTimes(2));
  });
});

describe("VenueReservationDetailPanel", () => {
  it("renders customer, form, resource, and incident blocks in the private detail", async () => {
    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Ana Martín")).toBeVisible();
    expect(screen.getByText("Alergias")).toBeVisible();
    expect(screen.getByText("Ninguna")).toBeVisible();
    expect(screen.getByText("Lucía")).toBeVisible();
    expect(screen.getByText("Cancelación tardía")).toBeVisible();
    expect(screen.getByText("Reportada")).toBeVisible();
    expect(screen.queryByText("No debe exponerse")).not.toBeInTheDocument();
  });
});
