import { cleanup, fireEvent, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl, renderWithLocale } from "@/test-utils/render-with-intl";

import type { PublicVenueProfile } from "./public-venue-api";
import { PublicVenueProfileView } from "./public-venue-profile";

vi.mock("@/features/availability/public-availability-calendar", () => ({
  PublicAvailabilityCalendar: () => (
    <section aria-label="Disponibilidad de prueba">Calendario</section>
  ),
}));

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
    const customTabHeading = screen.getByRole("heading", { level: 2, name: "Carta" });
    expect(customTabHeading).toBeVisible();
    expect(screen.getByRole("link", { name: "Carta" })).toHaveAttribute("href", "#custom-tab-0");
    expect(customTabHeading.closest("section")).toHaveAttribute("id", "custom-tab-0");
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

    expect(screen.getAllByRole("link", { name: "Reservar" })[0]).toHaveAttribute(
      "href",
      "#availability",
    );
    expect(screen.getByRole("button", { name: "Guardar" })).toBeDisabled();
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

  it("renderiza navegación, pestaña y reseña con el catálogo inglés real", () => {
    const englishVenue: PublicVenueProfile = {
      ...venue,
      locale: "en",
      categoryName: "Restaurant",
      description: "Seasonal cuisine",
      services: "Tasting menu",
      customTabs: [
        {
          ...venue.customTabs[0],
          title: "Menu and seasonal prices",
          content: "<p>Chef's tasting menu</p><ul><li>Seasonal vegetables</li></ul>",
        },
      ],
    };

    renderWithLocale(<PublicVenueProfileView venue={englishVenue} />, "en");

    expect(screen.getAllByRole("link", { name: "Book" }).length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "Menu and seasonal prices" })).toHaveAttribute(
      "href",
      "#custom-tab-0",
    );
    expect(screen.getByRole("heading", { name: "Reviews" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Write a review" })).toBeVisible();
    expect(screen.getByText("2 verified reviews")).toBeVisible();
  });
});
