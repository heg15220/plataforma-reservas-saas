import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  fetchVenueReservationDetail,
  fetchVenueReservationsForDay,
} from "./venue-reservations-api";
import { updateReservationAttendance } from "@/features/venue-incidents/venue-incidents-api";
import { VenueReservationDetailPanel } from "./venue-reservation-detail-panel";
import { VenueReservationsDashboard } from "./venue-reservations-dashboard";
import { reservationDetail, reservationList } from "./venue-reservations-test-fixtures";

vi.mock("./venue-reservations-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-reservations-api")>();
  return {
    ...original,
    fetchVenueReservationDetail: vi.fn(),
    fetchVenueReservationsForDay: vi.fn(),
  };
});

vi.mock("@/features/venue-incidents/venue-incidents-api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/features/venue-incidents/venue-incidents-api")>()),
  updateReservationAttendance: vi.fn(),
  reportReservationNoShow: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(fetchVenueReservationsForDay).mockResolvedValue(reservationList());
  vi.mocked(fetchVenueReservationDetail).mockResolvedValue(reservationDetail());
  vi.mocked(updateReservationAttendance).mockResolvedValue({
    reservationId: "10000000-0000-4000-8000-000000000001",
    status: "no_show",
    attendanceMarkedAt: "2026-07-27T10:00:00Z",
    updatedAt: "2026-07-27T10:00:00Z",
  });
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
    expect(screen.getByRole("link", { name: "Posibles incidencias previas" })).toHaveAttribute(
      "href",
      "/panel/reservas/10000000-0000-4000-8000-000000000001",
    );
    expect(screen.getByRole("link", { name: "Ver detalle" })).toHaveAttribute(
      "href",
      "/panel/reservas/10000000-0000-4000-8000-000000000001",
    );
    expect(fetchVenueReservationsForDay).toHaveBeenCalledWith(
      "2026-07-26",
      expect.any(AbortSignal),
    );
  });

  it("does not clutter a reservation without previous incident signals", async () => {
    const withoutRisk = reservationList();
    withoutRisk.items[0].incidentRiskLevel = "low";
    vi.mocked(fetchVenueReservationsForDay).mockResolvedValue(withoutRisk);

    renderWithIntl(<VenueReservationsDashboard initialDate="2026-07-26" />);

    expect(await screen.findByText("Ana Martín")).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "Posibles incidencias previas" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Incidencias previas recurrentes" }),
    ).not.toBeInTheDocument();
  });

  it("shows a red detail link next to status for recurrent incidents", async () => {
    const recurrent = reservationList();
    recurrent.items[0].incidentRiskLevel = "high";
    vi.mocked(fetchVenueReservationsForDay).mockResolvedValue(recurrent);

    renderWithIntl(<VenueReservationsDashboard initialDate="2026-07-26" />);

    expect(await screen.findByText("Confirmada")).toBeVisible();
    expect(screen.getByRole("link", { name: "Incidencias previas recurrentes" })).toHaveAttribute(
      "href",
      "/panel/reservas/10000000-0000-4000-8000-000000000001",
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
  it("shows a future reservation as pending without operational actions", async () => {
    vi.mocked(fetchVenueReservationDetail).mockResolvedValue({
      ...reservationDetail(),
      status: "pending",
      manualActionsAvailable: false,
    });

    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Pendiente")).toBeVisible();
    expect(screen.queryByRole("button", { name: "Marcar asistida" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Marcar no asistida" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancelar por el local" })).not.toBeInTheDocument();
  });

  it("keeps confirmed status but hides actions after the operational hour", async () => {
    vi.mocked(fetchVenueReservationDetail).mockResolvedValue({
      ...reservationDetail(),
      manualActionsAvailable: false,
    });

    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Confirmada")).toBeVisible();
    expect(screen.queryByRole("button", { name: "Marcar asistida" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancelar por el local" })).not.toBeInTheDocument();
  });

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
    expect(screen.getByText("Riesgo de no asistencia en observación")).toBeVisible();
    expect(screen.queryByText("No debe exponerse")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Marcar asistida" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Abrir incidencias y reglas" })).toHaveAttribute(
      "href",
      "/panel/incidencias?reservationId=10000000-0000-4000-8000-000000000001",
    );
  });

  it("shows a green, explained indicator when the incident history is empty", async () => {
    vi.mocked(fetchVenueReservationDetail).mockResolvedValue({
      ...reservationDetail(),
      incidentHistory: { totalElements: 0, truncated: false, items: [] },
    });

    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Riesgo de no asistencia bajo")).toBeVisible();
    expect(
      screen.getByText("No hay incidencias operativas en los últimos 12 meses."),
    ).toBeVisible();
  });

  it("shows a red, explained indicator for recurrent operational incidents", async () => {
    vi.mocked(fetchVenueReservationDetail).mockResolvedValue({
      ...reservationDetail(),
      incidentHistory: {
        totalElements: 2,
        truncated: false,
        items: [
          { incidentType: "no_show", reportedAt: new Date().toISOString(), status: "confirmed" },
          {
            incidentType: "late_cancellation",
            reportedAt: new Date().toISOString(),
            status: "reported",
          },
        ],
      },
    });

    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );

    expect(await screen.findByText("Riesgo de no asistencia alto")).toBeVisible();
    expect(
      screen.getByText("Se observan 2 incidencias operativas recurrentes en el historial visible."),
    ).toBeVisible();
  });

  it("offers touch-friendly attendance actions and refreshes after success", async () => {
    renderWithIntl(
      <VenueReservationDetailPanel reservationId="10000000-0000-4000-8000-000000000001" />,
    );
    fireEvent.click(await screen.findByRole("button", { name: "Marcar no asistida" }));

    await waitFor(() =>
      expect(updateReservationAttendance).toHaveBeenCalledWith(
        "10000000-0000-4000-8000-000000000001",
        "no_show",
      ),
    );
    await waitFor(() => expect(fetchVenueReservationDetail).toHaveBeenCalledTimes(2));
  });
});
