import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueEmailManager } from "./venue-email-manager";
import { fetchVenueEmailAssignments, updateVenueEmailAssignment } from "./venue-email-api";

vi.mock("./venue-email-api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./venue-email-api")>();
  return {
    ...actual,
    fetchVenueEmailAssignments: vi.fn(),
    updateVenueEmailAssignment: vi.fn(),
  };
});

const assignments = [
  {
    venueId: "d3000000-0000-4000-8000-000000000001",
    venueName: "Ames Padel Center",
    venueSlug: "ames-padel-center",
    email: "reservas@ames.local",
    panelAccessConfigured: false,
    updatedAt: "2026-08-02T20:00:00Z",
  },
  {
    venueId: "d3000000-0000-4000-8000-000000000002",
    venueName: "Brisa Studio",
    venueSlug: "brisa-studio",
    email: "reservas@brisa.local",
    panelAccessConfigured: true,
    updatedAt: "2026-08-02T20:00:00Z",
  },
];

beforeEach(() => {
  vi.mocked(fetchVenueEmailAssignments).mockResolvedValue(assignments);
  vi.mocked(updateVenueEmailAssignment).mockReset();
});

describe("VenueEmailManager", () => {
  it("muestra un editor independiente por cada local publicado", async () => {
    renderWithIntl(<VenueEmailManager />);

    expect(await screen.findByRole("heading", { name: "Ames Padel Center" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Brisa Studio" })).toBeVisible();
    expect(screen.getAllByRole("textbox", { name: /Email de acceso/ })).toHaveLength(2);
    expect(screen.getAllByLabelText(/Contraseña del panel/)).toHaveLength(2);
  }, 15_000);

  it("guarda el email del local elegido y confirma el resultado", async () => {
    vi.mocked(updateVenueEmailAssignment).mockImplementation(async (venueId, email) => ({
      ...assignments[0],
      venueId,
      email,
    }));
    renderWithIntl(<VenueEmailManager />);

    const fields = await screen.findAllByRole("textbox", { name: /Email de acceso/ });
    fireEvent.change(fields[0], { target: { value: "equipo@ames.local" } });
    fireEvent.change(screen.getAllByLabelText(/Contraseña del panel/)[0], {
      target: { value: "UnaClaveSegura2026!" },
    });
    fireEvent.click(screen.getAllByRole("button", { name: "Crear acceso" })[0]);

    await waitFor(() =>
      expect(updateVenueEmailAssignment).toHaveBeenCalledWith(
        assignments[0].venueId,
        "equipo@ames.local",
        "UnaClaveSegura2026!",
      ),
    );
    expect(await screen.findByText(/email de Ames Padel Center se han actualizado/)).toBeVisible();
  }, 15_000);
});
