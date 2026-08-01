import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchVenueStatistics } from "./venue-statistics-api";
import { VenueStatisticsDashboard } from "./venue-statistics-dashboard";

vi.mock("./venue-statistics-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-statistics-api")>();
  return { ...original, fetchVenueStatistics: vi.fn() };
});

const statistics = {
  period: "month" as const,
  fromDate: "2026-07-01",
  toDate: "2026-07-29",
  reservationsCount: 12,
  confirmedCount: 10,
  cancelledCount: 2,
  noShowCount: 1,
  attendedCount: 8,
  occupiedCapacity: 18,
  availableCapacity: 30,
  occupancyRate: 60,
  reviewsCount: 3,
  averageRating: 4.5,
  series: [
    {
      date: "2026-07-28",
      reservationsCount: 5,
      confirmedCount: 4,
      cancelledCount: 1,
      noShowCount: 1,
      attendedCount: 3,
      occupiedCapacity: 8,
      availableCapacity: 10,
      occupancyRate: 80,
      reviewsCount: 1,
      averageRating: 5,
    },
  ],
};

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueStatisticsDashboard", () => {
  it("muestra tarjetas, detalle y gráficos con alternativa accesible", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue(statistics);

    renderWithIntl(<VenueStatisticsDashboard />);

    await waitFor(() => expect(screen.getByText("12")).toBeVisible());
    expect(screen.getByText("60,0 %")).toBeVisible();
    expect(screen.getByText("4,5")).toBeVisible();
    expect(screen.getByText("Cancelaciones")).toBeVisible();
    expect(screen.getByLabelText("Gráfico diario de reservas")).toBeVisible();
    expect(screen.getByLabelText("28 jul 2026: 5 reservas")).toBeVisible();
    expect(screen.getByLabelText("28 jul 2026: 80,0 % de ocupación")).toBeVisible();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it("aplica un rango custom explícito y no lanza solicitudes incompletas", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue(statistics);
    renderWithIntl(<VenueStatisticsDashboard />);
    await waitFor(() => expect(fetchVenueStatistics).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole("button", { name: "Rango" }));
    fireEvent.change(screen.getByLabelText("Desde"), { target: { value: "2026-07-01" } });
    fireEvent.change(screen.getByLabelText("Hasta"), { target: { value: "2026-07-15" } });
    expect(fetchVenueStatistics).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole("button", { name: "Aplicar rango" }));
    await waitFor(() =>
      expect(fetchVenueStatistics).toHaveBeenLastCalledWith(
        { period: "custom", from: "2026-07-01", to: "2026-07-15" },
        expect.any(AbortSignal),
      ),
    );
  });

  it("mantiene legibles valores amplios y el estado sin valoración", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue({
      ...statistics,
      averageRating: null,
      reservationsCount: 123_456_789,
    });

    renderWithIntl(<VenueStatisticsDashboard />);

    expect(await screen.findByText("123.456.789")).toBeVisible();
    expect(screen.getByText("Sin valoración")).toBeVisible();
    expect(
      screen.getByRole("group", { name: "Seleccionar periodo de estadísticas" }),
    ).toBeVisible();
  });
});
