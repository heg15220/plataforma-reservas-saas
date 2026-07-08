import { cleanup, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import type { PublicVenueSearchResponse } from "./public-search-api";
import { PublicSearchResultsView } from "./public-search-results";

const response: PublicVenueSearchResponse = {
  locale: "es",
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
  hasNext: false,
  results: [
    {
      slug: "casa-luz",
      name: "Casa Luz",
      categorySlug: "restaurante",
      categoryName: "Restaurante",
      descriptionExcerpt: "Cocina de temporada",
      mainImageUrl: "/api/public/venue-images/casa/main",
      city: "Madrid",
      province: "Madrid",
      country: "ES",
      statusCode: "available",
      statusLabel: "Disponible",
      availabilitySummary: "Acepta reservas cuando tenga franjas publicadas.",
      bookingAvailable: true,
      latitude: 40.416775,
      longitude: -3.70379,
    },
    {
      slug: "pista-norte",
      name: "Pista Norte",
      categorySlug: "pista-de-padel",
      categoryName: "Pista de pádel",
      descriptionExcerpt: "Pádel cubierto",
      mainImageUrl: null,
      city: "València",
      province: "València",
      country: "ES",
      statusCode: "unavailable",
      statusLabel: "No disponible",
      availabilitySummary: "El local ha pausado temporalmente las reservas.",
      bookingAvailable: false,
      latitude: 39.46975,
      longitude: -0.37739,
    },
  ],
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_APP_ENV", "test");
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/");
});

afterEach(() => {
  cleanup();
  vi.unstubAllEnvs();
});

describe("PublicSearchResultsView", () => {
  it("muestra tarjetas de locales y filtros públicos soportados", () => {
    renderWithIntl(
      <PublicSearchResultsView
        filters={{ category: "restaurante", location: "Madrid", q: "cafe", sort: "availability" }}
        response={response}
      />,
    );

    expect(
      screen.getByRole("heading", { level: 1, name: "Explora locales para reservar" }),
    ).toBeVisible();
    expect(screen.getByText("2 locales encontrados")).toBeVisible();
    expect(screen.getByRole("img", { name: "Imagen principal de Casa Luz" })).toHaveAttribute(
      "src",
      "http://localhost:8080/api/public/venue-images/casa/main",
    );
    expect(screen.getByRole("heading", { level: 2, name: "Casa Luz" })).toBeVisible();
    expect(screen.getByText("Disponible")).toBeVisible();
    expect(screen.getByText("Imagen pendiente")).toBeVisible();
    expect(screen.getAllByRole("link", { name: "Ver local" })[0]).toHaveAttribute(
      "href",
      "/locales/casa-luz",
    );
    expect(screen.getAllByRole("search", { name: "Filtros de búsqueda pública" })).toHaveLength(2);
    expect(screen.getAllByLabelText("Qué buscas")[0]).toHaveValue("cafe");
    expect(screen.getAllByLabelText("Ubicación")[0]).toHaveValue("Madrid");
  });

  it("presenta estado vacío y acción para limpiar filtros", () => {
    renderWithIntl(
      <PublicSearchResultsView
        filters={{ q: "sin resultados" }}
        response={{ ...response, results: [], totalElements: 0, totalPages: 0 }}
      />,
    );

    expect(
      screen.getByRole("heading", { level: 2, name: "No hay locales que coincidan" }),
    ).toBeVisible();
    expect(screen.getAllByRole("link", { name: "Limpiar filtros" })[0]).toHaveAttribute(
      "href",
      "/explorar",
    );
  });
});
