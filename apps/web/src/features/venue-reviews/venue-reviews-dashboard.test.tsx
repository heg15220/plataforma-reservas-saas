import { cleanup, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchVenueReviews } from "./venue-reviews-api";
import { VenueReviewsDashboard } from "./venue-reviews-dashboard";

vi.mock("./venue-reviews-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./venue-reviews-api")>();
  return { ...original, fetchVenueReviews: vi.fn() };
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("VenueReviewsDashboard", () => {
  it("muestra métricas y comentarios sin datos de identidad", async () => {
    vi.mocked(fetchVenueReviews).mockResolvedValue({
      averageRating: 4.5,
      reviewsCount: 2,
      items: [
        {
          id: "10000000-0000-4000-8000-000000000001",
          rating: 5,
          comment: "Atención excelente.",
          createdAt: "2026-07-28T10:00:00Z",
        },
      ],
      page: 0,
      size: 20,
      totalPages: 1,
    });

    renderWithIntl(<VenueReviewsDashboard />);

    await waitFor(() => expect(screen.getByText("Atención excelente.")).toBeVisible());
    expect(screen.getByText("4,5")).toBeVisible();
    expect(screen.getByText("2")).toBeVisible();
    expect(screen.getByText("Cliente con reserva verificada")).toBeVisible();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it("presenta un estado vacío claro", async () => {
    vi.mocked(fetchVenueReviews).mockResolvedValue({
      averageRating: null,
      reviewsCount: 0,
      items: [],
      page: 0,
      size: 20,
      totalPages: 0,
    });

    renderWithIntl(<VenueReviewsDashboard />);

    await waitFor(() =>
      expect(
        screen.getByText("Tu local todavía no ha recibido reseñas verificadas."),
      ).toBeVisible(),
    );
    expect(screen.getByText("Sin valoración")).toBeVisible();
  });
});
