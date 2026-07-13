import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { TeamAvailabilityManager } from "./team-availability-manager";
import {
  fetchEmployeeResources,
  fetchVenueServices,
  fetchWeeklyHours,
} from "./team-api";

vi.mock("./team-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./team-api")>();
  return {
    ...original,
    createEmployeeResource: vi.fn(),
    createVenueService: vi.fn(),
    fetchEmployeeResources: vi.fn(),
    fetchVenueServices: vi.fn(),
    fetchWeeklyHours: vi.fn(),
    saveServiceResources: vi.fn(),
    saveWeeklyHours: vi.fn(),
    updateEmployeeResource: vi.fn(),
    updateVenueService: vi.fn(),
  };
});

const resource = {
  id: "10000000-0000-4000-8000-000000000001",
  type: "professional" as const,
  firstName: "Ana",
  lastName: "Ruiz",
  publicAlias: "Ana",
  photoUrl: null,
  specialty: "Masaje",
  description: null,
  status: "active" as const,
  publicVisibility: true,
  internalNotes: null,
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};

const service = {
  id: "20000000-0000-4000-8000-000000000001",
  name: "Masaje relajante",
  nameI18n: null,
  description: null,
  descriptionI18n: null,
  durationMinutes: 60,
  capacityRequired: 1,
  active: true,
  allowsAnyAvailableResource: true,
  employeeResourceIds: [resource.id],
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};

beforeEach(() => {
  vi.mocked(fetchEmployeeResources).mockResolvedValue([resource]);
  vi.mocked(fetchVenueServices).mockResolvedValue([service]);
  vi.mocked(fetchWeeklyHours).mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("TeamAvailabilityManager", () => {
  it("muestra recursos y carga el horario semanal bajo demanda", async () => {
    renderWithIntl(<TeamAvailabilityManager />);

    expect(await screen.findByText("Ana")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Horario" }));

    expect(fetchWeeklyHours).toHaveBeenCalledWith(resource.id);
    expect(await screen.findByRole("dialog")).toHaveTextContent("Horario semanal de Ana");
    expect(screen.getByText("Lunes")).toBeVisible();
  });

  it("muestra servicios, asociaciones y asignacion por primera disponibilidad", async () => {
    renderWithIntl(<TeamAvailabilityManager />);

    fireEvent.click(await screen.findByRole("tab", { name: "Servicios" }));

    expect(await screen.findByText("Masaje relajante")).toBeVisible();
    expect(screen.getByText(/60 min/)).toHaveTextContent("1 recurso asociado");
    expect(screen.getByText("Admite primera disponibilidad")).toBeVisible();
  });
});