import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import {
  checkPublicReviewEligibility,
  createPublicVenueReview,
} from "./public-review-api";
import { ReviewEntryDialog } from "./review-entry-dialog";

vi.mock("./public-review-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./public-review-api")>();
  return {
    ...original,
    checkPublicReviewEligibility: vi.fn(),
    createPublicVenueReview: vi.fn(),
  };
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ReviewEntryDialog", () => {
  it("solo muestra el formulario y crea después de acreditar el email", async () => {
    vi.mocked(checkPublicReviewEligibility).mockResolvedValue({
      eligible: true,
      canReview: true,
      error: null,
      messageKey: null,
    });
    vi.mocked(createPublicVenueReview).mockResolvedValue({
      status: "created",
      reviewId: "10000000-0000-4000-8000-000000000001",
      venueId: "20000000-0000-4000-8000-000000000001",
      rating: 5,
      averageRating: 4.8,
      reviewsCount: 12,
    });
    renderWithIntl(<ReviewEntryDialog venueSlug="casa-luz" />);

    fireEvent.click(screen.getByRole("button", { name: "Hacer reseña" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Correo de la reserva" }), {
      target: { value: " Guest@Example.COM " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Comprobar" }));

    await waitFor(() => expect(screen.getByText("Tu puntuación")).toBeVisible());
    expect(checkPublicReviewEligibility).toHaveBeenCalledWith(
      "casa-luz",
      "Guest@Example.COM",
      expect.any(AbortSignal),
    );
    fireEvent.click(screen.getByRole("radio", { name: "5 estrellas" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Comentario" }), {
      target: { value: "  Excelente.  " },
    });
    fireEvent.click(
      screen.getByRole("checkbox", {
        name: "Acepto que la puntuación y el comentario se publiquen de forma anónima.",
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Publicar reseña" }));

    await waitFor(() =>
      expect(screen.getByRole("heading", { name: "Reseña publicada" })).toBeVisible(),
    );
    expect(createPublicVenueReview).toHaveBeenCalledWith(
      "casa-luz",
      {
        acceptsReviewPolicy: true,
        comment: "Excelente.",
        customerEmail: "Guest@Example.COM",
        rating: 5,
      },
      expect.any(AbortSignal),
    );
    expect(screen.getByText("Tu reseña de 5 sobre 5 se ha publicado.")).toBeVisible();
  });

  it.each([
    {
      error: "REVIEW_NOT_ELIGIBLE" as const,
      messageKey: "reviews.notEligibleForVenue" as const,
      message: "No encontramos una reserva pasada válida con ese email para este local.",
    },
    {
      error: "REVIEW_ALREADY_SUBMITTED" as const,
      messageKey: "reviews.alreadySubmittedForVenue" as const,
      message:
        "Todas las reservas pasadas que permiten valorar este local ya tienen una reseña.",
    },
  ])("muestra el rechazo i18n $error sin revelar historial", async (decision) => {
    vi.mocked(checkPublicReviewEligibility).mockResolvedValue({
      eligible: false,
      canReview: false,
      error: decision.error,
      messageKey: decision.messageKey,
    });
    renderWithIntl(<ReviewEntryDialog venueSlug="casa-luz" />);

    fireEvent.click(screen.getByRole("button", { name: "Hacer reseña" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Correo de la reserva" }), {
      target: { value: "guest@example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Comprobar" }));

    await waitFor(() => expect(screen.getByText(decision.message)).toBeVisible());
    expect(screen.queryByText("Tu puntuación")).not.toBeInTheDocument();
    expect(screen.queryByText(/2026|reserva #|visita/i)).not.toBeInTheDocument();
  });
});
