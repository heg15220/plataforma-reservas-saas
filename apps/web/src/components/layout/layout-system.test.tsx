import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { PageHeading, PublicShell, Surface, VenueShell } from ".";

describe("sistema de layout", () => {
  it("marca la ruta activa en la navegación pública", () => {
    render(
      <PublicShell currentPath="/explorar">
        <p>Contenido público</p>
      </PublicShell>,
    );

    expect(screen.getByRole("main")).toHaveTextContent("Contenido público");
    expect(screen.getAllByRole("link", { name: "Explorar" })).toHaveLength(2);
    for (const link of screen.getAllByRole("link", { name: "Explorar" })) {
      expect(link).toHaveAttribute("aria-current", "page");
    }
  });

  it("ofrece navegación de escritorio y móvil en el panel", () => {
    render(
      <VenueShell currentPath="/panel/reservas" venueName="Local de prueba">
        <PageHeading title="Reservas" />
      </VenueShell>,
    );

    expect(screen.getByRole("navigation", { name: "Navegación del panel" })).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: "Navegación móvil del panel" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Reservas" })).toBeVisible();
    expect(
      screen
        .getAllByRole("link", { name: "Reservas" })
        .filter((link) => link.getAttribute("aria-current") === "page"),
    ).toHaveLength(2);
  });

  it("permite elegir el landmark de una superficie", () => {
    render(
      <Surface component="article">
        <h2>Tarjeta accesible</h2>
      </Surface>,
    );

    expect(screen.getByRole("article")).toHaveTextContent("Tarjeta accesible");
  });
});
