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
    updatedAt: "2026-08-02T20:00:00Z",
  },
  {
    venueId: "d3000000-0000-4000-8000-000000000002",
    venueName: "Brisa Studio",
    venueSlug: "brisa-studio",
    email: "reservas@brisa.local",
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
    expect(screen.getAllByRole("textbox", { name: /Email de notificaciones/ })).toHaveLength(2);
  });

  it("guarda el email del local elegido y confirma el resultado", async () => {
    vi.mocked(updateVenueEmailAssignment).mockImplementation(async (venueId, email) => ({
      ...assignments[0],
      venueId,
      email,
    }));
    renderWithIntl(<VenueEmailManager />);

    const fields = await screen.findAllByRole("textbox", { name: /Email de notificaciones/ });
    fireEvent.change(fields[0], { target: { value: "equipo@ames.local" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Guardar email" })[0]);

    await waitFor(() =>
      expect(updateVenueEmailAssignment).toHaveBeenCalledWith(
        assignments[0].venueId,
        "equipo@ames.local",
      ),
    );
    expect(await screen.findByText(/Ames Padel Center se ha actualizado/)).toBeVisible();
  });
});
