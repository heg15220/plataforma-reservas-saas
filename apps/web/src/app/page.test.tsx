import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { HomePageView } from "./page";

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
});
