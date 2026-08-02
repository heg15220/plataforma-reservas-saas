import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  createTimeSlot,
  deleteTimeSlots,
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
    deleteTimeSlots: vi.fn(),
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
              serviceId: "20000000-0000-4000-8000-000000000001",
              serviceName: "Corte",
              startsAt: slot.startsAt,
              endsAt: slot.endsAt,
              capacity: slot.capacity,
              availableCapacity: 3,
              status: "available",
              bookingAvailable: true,
              employeeResourceRequired: true,
              anyAvailableResourceAllowed: true,
              availableEmployeeResources: [
                {
                  employeeResourceId: "30000000-0000-4000-8000-000000000001",
                  type: "professional",
                  displayName: "Ana",
                  specialty: "Estilismo",
                },
              ],
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
  vi.unstubAllGlobals();
});

describe("PublicAvailabilityCalendar", () => {
  it("muestra el mes completo, capacidad real y reserva aún protegida", async () => {
    renderWithIntl(<PublicAvailabilityCalendar startDate="2026-07-13" venueSlug="casa-luz" />);

    expect(await screen.findByText("3 de 4 plazas disponibles")).toBeVisible();
    expect(fetchPublicAvailability).toHaveBeenCalledTimes(31);
    expect(screen.getByRole("button", { name: /Reserva/ })).toBeDisabled();
    expect(screen.getByText("Con plazas")).toBeVisible();
    expect(screen.getAllByText("Servicio: Corte")).toHaveLength(2);

    const resourceSelector = screen.getByRole("combobox", {
      name: "Profesional o recurso",
    });
    fireEvent.mouseDown(resourceSelector);
    expect(
      await screen.findByRole("option", {
        name: "Cualquier profesional o recurso disponible",
      }),
    ).toBeVisible();
    expect(screen.getByRole("option", { name: /Ana/ })).toBeVisible();
    fireEvent.click(
      screen.getByRole("option", { name: "Cualquier profesional o recurso disponible" }),
    );

    expect(screen.getByText("julio de 2026")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Mostrar el mes siguiente" }));
    expect(await screen.findByText("agosto de 2026")).toBeVisible();
    await waitFor(() => expect(fetchPublicAvailability).toHaveBeenCalledTimes(62));
  });

  it("muestra selector de servicio cuando una fecha ofrece varias alternativas", async () => {
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
      availableSlotCount: date === "2026-07-13" ? 2 : 0,
      slots:
        date === "2026-07-13"
          ? [
              {
                slotId: "10000000-0000-4000-8000-000000000001",
                serviceId: "20000000-0000-4000-8000-000000000001",
                serviceName: "Corte",
                startsAt: "09:00:00",
                endsAt: "10:00:00",
                capacity: 2,
                availableCapacity: 2,
                status: "available",
                bookingAvailable: true,
                employeeResourceRequired: false,
                anyAvailableResourceAllowed: false,
                availableEmployeeResources: [],
              },
              {
                slotId: "10000000-0000-4000-8000-000000000002",
                serviceId: "20000000-0000-4000-8000-000000000002",
                serviceName: "Masaje",
                startsAt: "11:00:00",
                endsAt: "12:00:00",
                capacity: 1,
                availableCapacity: 1,
                status: "available",
                bookingAvailable: true,
                employeeResourceRequired: false,
                anyAvailableResourceAllowed: false,
                availableEmployeeResources: [],
              },
            ]
          : [],
    }));

    renderWithIntl(<PublicAvailabilityCalendar startDate="2026-07-13" venueSlug="casa-luz" />);

    const serviceSelector = await screen.findByRole("combobox", { name: "Servicio" });
    fireEvent.mouseDown(serviceSelector);
    fireEvent.click(await screen.findByRole("option", { name: "Masaje" }));

    expect(await screen.findByText("11:00 " + String.fromCharCode(8211) + " 12:00")).toBeVisible();
  });
});

describe("VenueAvailabilityManager", () => {
  it("crea la primera versión desde el asistente cuando no existe horario guardado", async () => {
    vi.mocked(fetchOpeningHours).mockResolvedValue([]);
    vi.mocked(generateTimeSlots).mockResolvedValue([slot]);

    renderWithIntl(<VenueAvailabilityManager initialDate="2026-07-13" />);

    expect(await screen.findByText("Crea la primera versión de tus reservas")).toBeVisible();
    expect(screen.getByLabelText("Días abiertos")).toBeVisible();
    expect(screen.getByLabelText("Día habitual de cierre")).toBeVisible();
    expect(screen.getByLabelText("Política inicial de festivos")).toBeVisible();
    expect(screen.getByLabelText("Horario distinto según el día")).toBeVisible();
    expect(screen.getByLabelText("Rango horario por reserva")).toBeVisible();
    expect(screen.getByLabelText("Personas máximas por rango")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Guardar y crear primera versión" }));

    await waitFor(() => expect(saveOpeningHours).toHaveBeenCalledTimes(1));
    const snapshot = vi.mocked(saveOpeningHours).mock.calls[0][0];
    expect(snapshot).toHaveLength(7);
    expect(snapshot[0]).toMatchObject({
      weekday: 1,
      closed: false,
      opensAt: "09:00",
      closesAt: "20:00",
    });
    expect(snapshot[6]).toEqual({
      weekday: 7,
      closed: true,
      reservationsEnabled: false,
      opensAt: null,
      closesAt: null,
    });
    await waitFor(() => expect(generateTimeSlots).toHaveBeenCalledTimes(24));
    expect(
      await screen.findByText("La primera versión se ha creado correctamente con 24 franjas."),
    ).toBeVisible();
    expect(screen.getByRole("heading", { name: "Horario semanal" })).toBeVisible();
  });

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

  it("quita todas las franjas del día tras confirmación", async () => {
    vi.mocked(deleteTimeSlots).mockResolvedValue();
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
    renderWithIntl(<VenueAvailabilityManager initialDate="2026-07-13" />);

    fireEvent.click(await screen.findByRole("button", { name: "Quitar todas las franjas" }));

    await waitFor(() => expect(deleteTimeSlots).toHaveBeenCalledWith("2026-07-13"));
    expect(await screen.findByText("Se han quitado todas las franjas del día.")).toBeVisible();
    expect(screen.getByText("Todavía no hay franjas para esta fecha.")).toBeVisible();
  });

  it("aplica festivos o días libres a un rango completo", async () => {
    vi.mocked(saveAvailabilityDay).mockImplementation(async (input) => ({
      ...input,
      source: "override",
      blockId: "40000000-0000-4000-8000-000000000001",
    }));
    renderWithIntl(<VenueAvailabilityManager initialDate="2026-07-13" />);

    fireEvent.change(await screen.findByLabelText("Fecha inicial"), {
      target: { value: "2026-07-13" },
    });
    fireEvent.change(screen.getByLabelText("Fecha final"), {
      target: { value: "2026-07-15" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Motivo interno" }), {
      target: { value: "Festivo local" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Aplicar a 3 fechas" }));

    await waitFor(() => expect(saveAvailabilityDay).toHaveBeenCalledTimes(3));
    expect(vi.mocked(saveAvailabilityDay).mock.calls.map(([input]) => input)).toEqual([
      {
        date: "2026-07-13",
        closed: true,
        reservationsEnabled: false,
        reason: "Festivo local",
      },
      {
        date: "2026-07-14",
        closed: true,
        reservationsEnabled: false,
        reason: "Festivo local",
      },
      {
        date: "2026-07-15",
        closed: true,
        reservationsEnabled: false,
        reason: "Festivo local",
      },
    ]);
    expect(await screen.findByText("Se han actualizado 3 fechas correctamente.")).toBeVisible();
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
void deleteTimeSlots;
void saveAvailabilityDay;
void updateTimeSlotCapacity;
