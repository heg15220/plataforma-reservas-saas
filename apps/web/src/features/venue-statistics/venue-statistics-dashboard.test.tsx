import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchVenueProfiles } from "@/features/venue-profile/venue-profile-api";
import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchVenueStatistics } from "./venue-statistics-api";
import { VenueStatisticsDashboard } from "./venue-statistics-dashboard";

vi.mock("./venue-statistics-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-statistics-api")>();
  return { ...original, fetchVenueStatistics: vi.fn() };
});

vi.mock("@/features/venue-profile/venue-profile-api", async (importOriginal) => {
  const original =
    await importOriginal<typeof import("@/features/venue-profile/venue-profile-api")>();
  return { ...original, fetchVenueProfiles: vi.fn() };
});

const FIRST_VENUE_ID = "20000000-0000-4000-8000-000000000001";
const SECOND_VENUE_ID = "20000000-0000-4000-8000-000000000002";

function profiles(...items: Array<{ id: string; name: string }>) {
  return {
    profiles: items,
    canCreateAdditionalVenue: items.length > 1,
  } as Awaited<ReturnType<typeof fetchVenueProfiles>>;
}

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
  incidentsCount: 2,
  averageRating: 4.5,
  demandMetrics: {
    status: "available" as const,
    policyVersion: "booking-attribution-v1" as const,
    definitionsVersion: "demand-commercial-metrics-v1" as const,
    timeZone: "Europe/Madrid",
    minimumSampleSize: 10,
    eligibleReservations: 10,
    classifiedReservations: 10,
    coveragePercent: 100,
    newCustomers: 4,
    originatedReservations: 7,
    offPeakCovered: 3,
    attributedIncome: 245,
    attributedCurrency: "EUR",
    incomeStatus: "available" as const,
    directReservations: 3,
    assistedReservations: 2,
    generatedReservations: 4,
    recoveredReservations: 1,
    definitions: [
      { key: "newCustomers" as const, definitionCode: "NEW_CUSTOMER_FIRST_CONFIRMED_AT_VENUE" },
      { key: "originatedReservations" as const, definitionCode: "NON_DIRECT_ATTRIBUTION_CLASSES" },
      { key: "offPeakCovered" as const, definitionCode: "WEEKDAY_14_TO_18_LOCAL_NON_DIRECT" },
      {
        key: "attributedIncome" as const,
        definitionCode: "VISIBLE_PRICE_ASSOCIATED_NOT_INCREMENTAL",
      },
      { key: "coverage" as const, definitionCode: "CLASSIFIED_OVER_CONFIRMED_PERIOD" },
    ],
  },
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
      incidentsCount: 2,
      averageRating: 5,
    },
  ],
};

beforeEach(() => {
  vi.mocked(fetchVenueProfiles).mockResolvedValue(
    profiles({ id: FIRST_VENUE_ID, name: "Azahar & Brasa" }),
  );
});

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
    expect(screen.getByLabelText("Gráfico diario de incidencias activadas")).toBeVisible();
    expect(screen.getByLabelText("28 jul 2026: 2 incidencias activadas")).toBeVisible();
    expect(screen.getByText("Incidencias activadas")).toBeVisible();
    expect(screen.getByText("Valor generado por Reserly")).toBeVisible();
    expect(screen.getByText(/245,00/)).toBeVisible();
    expect(screen.getByText("10 de 10 reservas confirmadas (100,0 %)")).toBeVisible();
    expect(
      screen.getByRole("group", { name: "Desglose de reservas por atribución" }),
    ).toBeVisible();
    expect(screen.getByText(/No es ingreso incremental/)).toBeVisible();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it("muestra cobertura pero suprime cifras comerciales con muestra insuficiente", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue({
      ...statistics,
      demandMetrics: {
        ...statistics.demandMetrics,
        status: "insufficient_sample",
        classifiedReservations: 4,
        coveragePercent: 40,
        newCustomers: null,
        originatedReservations: null,
        offPeakCovered: null,
        attributedIncome: null,
        attributedCurrency: null,
        incomeStatus: "insufficient_sample",
        directReservations: null,
        assistedReservations: null,
        generatedReservations: null,
        recoveredReservations: null,
      },
    });

    renderWithIntl(<VenueStatisticsDashboard />);

    expect(await screen.findByText(/Muestra insuficiente: hacen falta al menos 10/)).toBeVisible();
    expect(screen.getByText("4 de 10 reservas confirmadas (40,0 %)")).toBeVisible();
    expect(screen.getAllByText("Muestra insuficiente").length).toBeGreaterThanOrEqual(4);
    expect(screen.queryByRole("group", { name: "Desglose de reservas por atribución" })).toBeNull();
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
        {
          period: "custom",
          venueId: FIRST_VENUE_ID,
          from: "2026-07-01",
          to: "2026-07-15",
        },
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

  it("distingue un periodo sin incidencias aunque existan otras métricas", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue({
      ...statistics,
      incidentsCount: 0,
      series: statistics.series.map((day) => ({ ...day, incidentsCount: 0 })),
    });

    renderWithIntl(<VenueStatisticsDashboard />);

    expect(await screen.findByText("No hay incidencias activadas en este periodo.")).toBeVisible();
  });

  it("permite seleccionar cada local accesible en una cuenta multi-local", async () => {
    vi.mocked(fetchVenueProfiles).mockResolvedValue(
      profiles(
        { id: FIRST_VENUE_ID, name: "Azahar & Brasa" },
        { id: SECOND_VENUE_ID, name: "Brisa Studio" },
      ),
    );
    vi.mocked(fetchVenueStatistics).mockResolvedValue(statistics);

    renderWithIntl(<VenueStatisticsDashboard />);
    await waitFor(() =>
      expect(fetchVenueStatistics).toHaveBeenCalledWith(
        { period: "month", venueId: FIRST_VENUE_ID },
        expect.any(AbortSignal),
      ),
    );

    fireEvent.mouseDown(screen.getByRole("combobox", { name: "Local" }));
    fireEvent.click(screen.getByRole("option", { name: "Brisa Studio" }));

    await waitFor(() =>
      expect(fetchVenueStatistics).toHaveBeenLastCalledWith(
        { period: "month", venueId: SECOND_VENUE_ID },
        expect.any(AbortSignal),
      ),
    );
  });

  it("actualiza las métricas al recuperar el foco sin recargar la página", async () => {
    vi.mocked(fetchVenueStatistics).mockResolvedValue(statistics);
    renderWithIntl(<VenueStatisticsDashboard />);
    await waitFor(() => expect(fetchVenueStatistics).toHaveBeenCalledTimes(1));

    window.dispatchEvent(new Event("focus"));

    await waitFor(() => expect(fetchVenueStatistics).toHaveBeenCalledTimes(2));
  });
});
