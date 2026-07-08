import { cleanup, screen } from "@testing-library/react";
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

  it("comunica honestamente las capacidades futuras sin habilitar reservas", () => {
    renderWithIntl(<PublicVenueProfileView venue={venue} />);

    expect(screen.getByRole("button", { name: "Reservas próximamente" })).toBeDisabled();
    expect(
      screen.getByText("Las valoraciones aparecerán cuando comiencen las reservas verificadas."),
    ).toBeVisible();
  });
});
