import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  fetchVenueBookingRules,
  fetchVenueIncidentHistory,
  updateVenueBookingRules,
} from "./venue-incidents-api";
import { VenueIncidentsDashboard } from "./venue-incidents-dashboard";

vi.mock("./venue-incidents-api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./venue-incidents-api")>()),
  fetchVenueBookingRules: vi.fn(),
  fetchVenueIncidentHistory: vi.fn(),
  updateVenueBookingRules: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(fetchVenueBookingRules).mockResolvedValue({
    cancellationAllowed: true,
    freeCancellationUntilMinutesBefore: 1440,
    updatedAt: "2026-07-27T10:00:00Z",
  });
  vi.mocked(fetchVenueIncidentHistory).mockResolvedValue({
    page: 0,
    size: 50,
    totalElements: 1,
    totalPages: 1,
    items: [
      {
        incidentType: "no_show",
        reportedAt: "2026-07-20T10:00:00Z",
        status: "reported",
      },
    ],
  });
  vi.mocked(updateVenueBookingRules).mockResolvedValue({
    cancellationAllowed: true,
    freeCancellationUntilMinutesBefore: 60,
    updatedAt: "2026-07-27T11:00:00Z",
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueIncidentsDashboard", () => {
  it("renders responsive rules and accredited professional history", async () => {
    renderWithIntl(
      <VenueIncidentsDashboard reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Reglas de cancelación")).toBeVisible();
    expect(screen.getByText("No asistencia")).toBeVisible();
    expect(screen.getByText("Reportada")).toBeVisible();
    expect(fetchVenueIncidentHistory).toHaveBeenCalledWith(
      "10000000-0000-4000-8000-000000000001",
      expect.any(AbortSignal),
    );
  });

  it("saves bounded cancellation rules", async () => {
    renderWithIntl(<VenueIncidentsDashboard />);
    const input = await screen.findByLabelText("Antelación mínima en minutos");
    fireEvent.change(input, { target: { value: "60" } });
    fireEvent.click(screen.getByRole("button", { name: "Guardar reglas" }));

    await waitFor(() =>
      expect(updateVenueBookingRules).toHaveBeenCalledWith({
        cancellationAllowed: true,
        freeCancellationUntilMinutesBefore: 60,
      }),
    );
    expect(await screen.findByText("Las reglas se han actualizado correctamente.")).toBeVisible();
  });
});
