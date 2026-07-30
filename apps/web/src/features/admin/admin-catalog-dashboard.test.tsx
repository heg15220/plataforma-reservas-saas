import { cleanup, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchAdminCategories, fetchAdminVenues } from "./admin-api";
import { AdminCatalogDashboard } from "./admin-catalog-dashboard";

vi.mock("./admin-api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./admin-api")>()),
  fetchAdminCategories: vi.fn(),
  fetchAdminVenues: vi.fn(),
  saveAdminCategory: vi.fn(),
  saveAdminVenue: vi.fn(),
  suspendAdminVenue: vi.fn(),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("AdminCatalogDashboard", () => {
  it("muestra categorías activas e inactivas con traducciones", async () => {
    vi.mocked(fetchAdminCategories).mockResolvedValue({
      categories: [
        {
          id: "20000000-0000-4000-8000-000000000001",
          slug: "restaurants",
          nameEs: "Restaurantes",
          nameEn: "Restaurants",
          active: false,
          updatedAt: "2026-07-30T14:00:00Z",
        },
      ],
    });

    renderWithIntl(<AdminCatalogDashboard mode="categories" />);

    await waitFor(() => expect(screen.getByText("Restaurantes")).toBeVisible());
    expect(screen.getByText(/Restaurants/)).toBeVisible();
    expect(screen.getByText("Desactivada")).toBeVisible();
    expect(screen.getByRole("button", { name: "Editar" })).toBeVisible();
  });

  it("lista locales sin ofrecer suspensión dentro de la edición básica", async () => {
    vi.mocked(fetchAdminCategories).mockResolvedValue({ categories: [] });
    vi.mocked(fetchAdminVenues).mockResolvedValue({
      venues: [
        {
          id: "30000000-0000-4000-8000-000000000001",
          name: "Local Centro",
          slug: "local-centro",
          categoryId: "20000000-0000-4000-8000-000000000001",
          categoryName: "Restaurantes",
          status: "published",
          contactEmail: null,
          phone: null,
          address: null,
          city: "Madrid",
          province: null,
          country: "ES",
          postalCode: null,
          updatedAt: "2026-07-30T14:00:00Z",
        },
      ],
    });

    renderWithIntl(<AdminCatalogDashboard mode="venues" />);

    await waitFor(() => expect(screen.getByText("Local Centro")).toBeVisible());
    expect(screen.getByText(/Restaurantes · Madrid/)).toBeVisible();
    expect(screen.getByRole("button", { name: /suspender/i })).toBeVisible();
  });
});
