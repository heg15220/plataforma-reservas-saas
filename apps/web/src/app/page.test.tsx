import { act, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { HomePageView } from "./page";

const venue = {
  slug: "casa-luz",
  name: "Casa Luz",
  categorySlug: "restaurante",
  categoryName: "Restaurante",
  descriptionExcerpt: "Cocina de temporada",
  mainImageUrl: null,
  address: "Calle Mayor 1",
  postalCode: "28013",
  city: "Madrid",
  province: "Madrid",
  country: "ES",
  statusCode: "available" as const,
  statusLabel: "Disponible",
  availabilitySummary: "Disponible hoy",
  bookingAvailable: true,
  latitude: 40.416775,
  longitude: -3.70379,
};

const venues = Array.from({ length: 8 }, (_, index) => ({
  ...venue,
  name: `Local ${index + 1}`,
  slug: `local-${index + 1}`,
  ...(index === 3
    ? {
        bookingAvailable: false,
        statusCode: "availability_pending" as const,
        statusLabel: "Disponibilidad pendiente",
      }
    : {}),
  ...(index === 7
    ? {
        bookingAvailable: false,
        statusCode: "unavailable" as const,
        statusLabel: "No disponible",
      }
    : {}),
}));

beforeEach(() => {
  vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({ matches: false }));
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("HomePage", () => {
  it("muestra el buscador público como acción principal", () => {
    renderWithIntl(<HomePageView />);

    expect(
      screen.getByRole("heading", { level: 1, name: "¿Dónde quieres pedir cita hoy?" }),
    ).toBeVisible();
    expect(screen.getByRole("navigation", { name: "Navegación pública principal" })).toBeVisible();
    expect(
      screen.getByRole("navigation", { name: "Navegación pública móvil" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("search", { name: "Buscador principal de locales" })).toHaveAttribute(
      "action",
      "/explorar",
    );
    expect(screen.getByLabelText("Qué buscas")).toHaveAttribute("name", "q");
    expect(screen.getByLabelText("Ubicación")).toHaveAttribute("name", "location");
    expect(screen.getByRole("button", { name: "Buscar" })).toHaveAttribute("type", "submit");
    expect(screen.getByRole("link", { name: "Restaurantes" })).toHaveAttribute(
      "href",
      "/explorar?category=restaurante",
    );
    expect(screen.getByRole("link", { name: "Registra tu local" })).toHaveAttribute(
      "href",
      "/locales/registro",
    );
  });

  it("rota las ocho recomendaciones por tarjetas completas y permite abrir sus detalles", () => {
    vi.useFakeTimers();
    renderWithIntl(<HomePageView venues={venues} />);

    const carousel = screen.getByTestId("recommended-carousel");
    expect(carousel).toHaveAttribute("data-active-index", "0");
    const categoryLabels = within(carousel).getAllByText("Restaurante");
    expect(categoryLabels).toHaveLength(4);
    expect(categoryLabels[0].closest(".MuiChip-root")?.querySelector("svg")).not.toBeNull();
    expect(
      within(carousel).getAllByText("Calle Mayor 1 · 28013 · Madrid · Madrid · ES"),
    ).toHaveLength(4);
    expect(within(carousel).getAllByText("Abierto")).toHaveLength(3);
    expect(within(carousel).getAllByText("Cerrado")).toHaveLength(1);
    expect(within(carousel).queryByText("Ver disponibilidad")).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(4_000));

    expect(carousel).toHaveAttribute("data-active-index", "1");
    expect(within(carousel).getByRole("link", { name: "Local 5" })).toHaveAttribute(
      "href",
      "/locales/local-5",
    );

    act(() => vi.advanceTimersByTime(12_000));
    expect(carousel).toHaveAttribute("data-active-index", "4");
    expect(within(carousel).getByRole("link", { name: "Local 8" })).toHaveAttribute(
      "href",
      "/locales/local-8",
    );
    expect(within(carousel).getByText("Cerrado")).toBeVisible();

    act(() => vi.advanceTimersByTime(16_000));
    expect(carousel).toHaveAttribute("data-active-index", "0");
  });
});
