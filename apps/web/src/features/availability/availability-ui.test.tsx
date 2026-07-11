import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  createTimeSlot,
  fetchAvailabilityDay,
  fetchOpeningHours,
  fetchPublicAvailability,
  fetchTimeSlots,
  generateTimeSlots,
  saveAvailabilityDay,
  saveOpeningHours,
  setTimeSlotBlocked,
  updateTimeSlotCapacity,
} from "./availability-api";
import { PublicAvailabilityCalendar } from "./public-availability-calendar";
import { VenueInternalCalendar } from "./venue-internal-calendar";
import { VenueAvailabilityManager } from "./venue-availability-manager";

vi.mock("./availability-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./availability-api")>();
  return {
    ...original,
    createTimeSlot: vi.fn(),
    fetchAvailabilityDay: vi.fn(),
    fetchOpeningHours: vi.fn(),
    fetchPublicAvailability: vi.fn(),
    fetchTimeSlots: vi.fn(),
    generateTimeSlots: vi.fn(),
    saveAvailabilityDay: vi.fn(),
    saveOpeningHours: vi.fn(),
    setTimeSlotBlocked: vi.fn(),
    updateTimeSlotCapacity: vi.fn(),
  };
});

const slot = {
  id: "10000000-0000-4000-8000-000000000001",
  date: "2026-07-13",
  weekday: 1,
  startsAt: "09:00:00",
  endsAt: "10:00:00",
  capacity: 4,
  status: "available",
  createdByRule: false,
  version: 0,
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
  vi.mocked(fetchPublicAvailability).mockImplementation(async (_slug, date) => ({
    venueSlug: "casa-luz",
    date,
    weekday: 1,
    statusCode: "open",
    statusLabel: "Abierto",
    bookingAvailable: true,
    closed: false,
    reservationsEnabled: true,
    source: "weekly_schedule",
    availableSlotCount: date === "2026-07-13" ? 1 : 0,
    slots:
      date === "2026-07-13"
        ? [
            {
              slotId: slot.id,
              startsAt: slot.startsAt,
              endsAt: slot.endsAt,
              capacity: slot.capacity,
              availableCapacity: 3,
              status: "available",
              bookingAvailable: true,
            },
          ]
        : [],
  }));
  vi.mocked(fetchOpeningHours).mockResolvedValue(
    Array.from({ length: 7 }, (_, index) => ({
      id: `10000000-0000-4000-8000-00000000000${index + 1}`,
      weekday: index + 1,
      closed: false,
      reservationsEnabled: true,
      opensAt: "09:00:00",
      closesAt: "18:00:00",
    })),
  );
  vi.mocked(fetchAvailabilityDay).mockResolvedValue({
    date: "2026-07-13",
    closed: false,
    reservationsEnabled: true,
    source: "weekly_schedule",
    blockId: null,
    reason: null,
  });
  vi.mocked(fetchTimeSlots).mockResolvedValue([slot]);
  vi.mocked(saveOpeningHours).mockImplementation(async (days) =>
    days.map((day, index) => ({
      ...day,
      id: `10000000-0000-4000-8000-00000000000${index + 1}`,
    })),
  );
  vi.mocked(setTimeSlotBlocked).mockResolvedValue({ ...slot, status: "blocked", version: 1 });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  vi.unstubAllEnvs();
});

describe("PublicAvailabilityCalendar", () => {
  it("muestra siete días, capacidad real y reserva aún protegida", async () => {
    renderWithIntl(<PublicAvailabilityCalendar startDate="2026-07-13" venueSlug="casa-luz" />);

    expect(await screen.findByText("3 de 4 plazas disponibles")).toBeVisible();
    expect(fetchPublicAvailability).toHaveBeenCalledTimes(7);
    expect(screen.getByRole("button", { name: /Reserva/ })).toBeDisabled();
    expect(screen.getByText("Con plazas")).toBeVisible();
  });
});

describe("VenueAvailabilityManager", () => {
  it("carga el horario y guarda el snapshot semanal completo", async () => {
    renderWithIntl(<VenueAvailabilityManager />);

    expect(await screen.findByText("Lunes")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Guardar horario semanal" }));

    await waitFor(() => expect(saveOpeningHours).toHaveBeenCalledTimes(1));
    expect(vi.mocked(saveOpeningHours).mock.calls[0][0]).toHaveLength(7);
    expect(await screen.findByText("El horario semanal se ha guardado.")).toBeVisible();
  });

  it("bloquea una franja y reconcilia el estado devuelto por backend", async () => {
    renderWithIntl(<VenueAvailabilityManager />);

    fireEvent.click(await screen.findByRole("button", { name: "Bloquear" }));

    await waitFor(() => expect(setTimeSlotBlocked).toHaveBeenCalledWith(slot.id, true));
    expect(await screen.findByText("Bloqueada")).toBeVisible();
  });
});

describe("VenueInternalCalendar", () => {
  it("muestra resumen semanal y detalle de franjas propias", async () => {
    vi.mocked(fetchTimeSlots).mockImplementation(async (date) =>
      date === "2026-07-13"
        ? [slot, { ...slot, id: "10000000-0000-4000-8000-000000000002", status: "blocked" }]
        : [],
    );

    renderWithIntl(<VenueInternalCalendar startDate="2026-07-13" />);

    expect(await screen.findByText("Vista interna de calendario")).toBeVisible();
    expect(fetchTimeSlots).toHaveBeenCalledTimes(7);
    expect(await screen.findByText("Franjas disponibles")).toBeVisible();
    expect(screen.getByText("2")).toBeVisible();
    expect(screen.getAllByText("09:00 – 10:00")).toHaveLength(2);
    expect(screen.getAllByText("Capacidad 4")).toHaveLength(2);
  });
});

void createTimeSlot;
void generateTimeSlots;
void saveAvailabilityDay;
void updateTimeSlotCapacity;
