import { cleanup, fireEvent, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import type { PublicVenueProfile } from "./public-venue-api";
import { PublicVenueProfileView } from "./public-venue-profile";

const venue: PublicVenueProfile = {
  slug: "casa-luz",
  locale: "es",
  name: "Casa Luz",
  categorySlug: "restaurante",
  categoryName: "Restaurante",
  description: "Cocina de temporada",
  services: "Menú degustación",
  rules: null,
  publicText: null,
  mainImageUrl: "/main",
  gallery: [{ url: "/gallery/1", altText: "Comedor principal", position: 0 }],
  customTabs: [
    {
      title: "Carta",
      content: "<p>Menú degustación</p><ul><li>Verduras de temporada</li></ul>",
      position: 0,
      contentFormat: "safe_html",
    },
  ],
  address: "Calle Mayor, 1",
  city: "Madrid",
  province: null,
  country: "ES",
  postalCode: "28013",
  latitude: 40.416775,
  longitude: -3.70379,
  phone: null,
  contactEmail: "hola@casaluz.test",
  reviews: {
    averageRating: 4.5,
    reviewsCount: 2,
    truncated: false,
    items: [
      {
        id: "10000000-0000-4000-8000-000000000001",
        rating: 5,
        comment: "Atención excelente.",
        createdAt: "2026-07-28T10:00:00Z",
      },
      {
        id: "10000000-0000-4000-8000-000000000002",
        rating: 4,
        comment: null,
        createdAt: "2026-07-27T10:00:00Z",
      },
    ],
  },
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/");
});

afterEach(() => {
  cleanup();
  vi.unstubAllEnvs();
});

describe("PublicVenueProfileView", () => {
  it("presenta contenido localizado, galería y solo contactos autorizados", () => {
    renderWithIntl(<PublicVenueProfileView venue={venue} />);

    expect(screen.getByRole("heading", { level: 1, name: "Casa Luz" })).toBeVisible();
    expect(screen.getByText("Cocina de temporada")).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "Carta" })).toBeVisible();
    expect(screen.getByText("Verduras de temporada")).toBeVisible();
    expect(screen.getByRole("img", { name: "Comedor principal" })).toBeVisible();
    expect(screen.getByRole("link", { name: "hola@casaluz.test" })).toHaveAttribute(
      "href",
      "mailto:hola@casaluz.test",
    );
    expect(screen.queryByRole("link", { name: /^\+/ })).not.toBeInTheDocument();
  });

  it("muestra valoración agregada y reseñas verificadas sin identidad", () => {
    renderWithIntl(<PublicVenueProfileView venue={venue} />);

    expect(screen.getByRole("button", { name: "Reservas próximamente" })).toBeDisabled();
    expect(screen.getByLabelText("Valoración media: 4,5 de 5")).toBeVisible();
    expect(screen.getByText("2 reseñas verificadas")).toBeVisible();
    expect(screen.getByText("Atención excelente.")).toBeVisible();
    expect(screen.getAllByText("Cliente con reserva verificada")).toHaveLength(2);
    const reviewsSection = screen
      .getByRole("heading", { level: 2, name: "Valoraciones" })
      .closest("section");
    expect(reviewsSection).not.toBeNull();
    expect(within(reviewsSection!).queryByText(/@/)).not.toBeInTheDocument();
  });

  it("abre la entrada de reseña y solicita email sin adelantar la elegibilidad", () => {
    renderWithIntl(<PublicVenueProfileView venue={venue} />);

    fireEvent.click(screen.getByRole("button", { name: "Hacer reseña" }));

    expect(screen.getByRole("dialog", { name: "Hacer una reseña" })).toBeVisible();
    expect(screen.getByRole("textbox", { name: "Correo de la reserva" })).toHaveAttribute(
      "type",
      "email",
    );
    expect(screen.queryByText("Tu puntuación")).not.toBeInTheDocument();
  });
});
