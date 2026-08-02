import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { PageHeading, PublicShell, Surface, VenueShell } from ".";

const replaceMock = vi.fn();
const refreshMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock, refresh: refreshMock }),
}));

afterEach(() => {
  vi.clearAllMocks();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("sistema de layout", () => {
  it("marca la ruta activa en la navegación pública", () => {
    renderWithIntl(
      <PublicShell currentPath="/explorar">
        <p>Contenido público</p>
      </PublicShell>,
    );

    expect(screen.getByRole("main")).toHaveTextContent("Contenido público");
    expect(screen.getAllByRole("link", { name: "Explorar" })).toHaveLength(2);
    for (const link of screen.getAllByRole("link", { name: "Explorar" })) {
      expect(link).toHaveAttribute("aria-current", "page");
    }
    expect(screen.getByRole("link", { name: "Acceso local" })).toHaveAttribute(
      "href",
      "/locales/acceso",
    );
  });

  it("ofrece navegación de escritorio y móvil en el panel", () => {
    renderWithIntl(
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
    expect(screen.getAllByRole("link", { name: "Ir al inicio" })).toHaveLength(2);
    expect(screen.getAllByRole("button", { name: "Cerrar sesión" })).toHaveLength(2);
    expect(screen.getAllByRole("link", { name: "Emails" })).toHaveLength(2);
  });

  it("revoca la sesión antes de volver al inicio público", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(
      <VenueShell>
        <PageHeading title="Panel" />
      </VenueShell>,
    );

    fireEvent.click(screen.getAllByRole("button", { name: "Cerrar sesión" })[0]);

    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith("/"));
    expect(refreshMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/logout",
      expect.objectContaining({ method: "POST", credentials: "include" }),
    );
  });

  it("permite elegir el landmark de una superficie", () => {
    renderWithIntl(
      <Surface component="article">
        <h2>Tarjeta accesible</h2>
      </Surface>,
    );

    expect(screen.getByRole("article")).toHaveTextContent("Tarjeta accesible");
  });
});
