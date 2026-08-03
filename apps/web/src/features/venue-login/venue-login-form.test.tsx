import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueLoginForm } from "./venue-login-form";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

beforeEach(() => {
  replace.mockReset();
  vi.stubEnv("NEXT_PUBLIC_APP_ENV", "local");
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

function completeForm() {
  fireEvent.change(screen.getByRole("textbox", { name: "Correo electrónico" }), {
    target: { value: "local@example.com" },
  });
  fireEvent.change(screen.getByLabelText(/^Contraseña/, { selector: "input" }), {
    target: { value: "correct-horse-battery" },
  });
}

describe("VenueLoginForm", () => {
  it("carga de forma exacta la cuenta local de Azahar sin depender del autocompletado", () => {
    renderWithIntl(<VenueLoginForm localEnvironment />);

    fireEvent.click(screen.getByRole("button", { name: "Usar cuenta de Azahar" }));

    expect(screen.getByRole("textbox")).toHaveValue("azahar@reserly.local");
    expect(document.querySelector('input[name="password"]')).toHaveValue("ReserlyLocal2026!");
  });

  it("muestra el acceso asistido solo cuando la página acredita el entorno local", () => {
    renderWithIntl(<VenueLoginForm localEnvironment />);

    expect(screen.getByRole("button", { name: "Usar cuenta de Azahar" })).toBeVisible();
  });

  it("valida, muestra ayudas y enfoca el primer campo incorrecto", () => {
    renderWithIntl(<VenueLoginForm />);

    fireEvent.click(screen.getByRole("button", { name: "Acceder al panel" }));

    expect(screen.getAllByText("Este campo es obligatorio.")).toHaveLength(2);
    expect(screen.getByRole("textbox", { name: "Correo electrónico" })).toHaveFocus();
  });

  it("permite mostrar y ocultar la contraseña", () => {
    renderWithIntl(<VenueLoginForm />);
    const password = screen.getByLabelText(/^Contraseña/, { selector: "input" });

    expect(password).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Mostrar contraseña" }));
    expect(password).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Ocultar contraseña" }));
    expect(password).toHaveAttribute("type", "password");
  });

  it("bloquea el doble envío y redirige con la preferencia guardada", async () => {
    let resolveRequest: ((response: Response) => void) | undefined;
    const fetchMock = vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        resolveRequest = resolve;
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWithIntl(<VenueLoginForm />);
    completeForm();

    fireEvent.click(screen.getByRole("button", { name: "Acceder al panel" }));

    expect(screen.getByRole("button", { name: "Comprobando acceso" })).toBeDisabled();
    expect(fetchMock).toHaveBeenCalledTimes(1);

    resolveRequest?.(
      new Response(
        JSON.stringify({
          userId: "7ad3a532-86da-46f6-9cf5-c59107f48912",
          accountType: "venue_business",
          preferredLocale: "en",
          emailVerified: true,
          sessionExpiresAt: "2026-07-01T22:00:00Z",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    expect(await screen.findByRole("button", { name: "Abriendo el panel" })).toBeDisabled();
    expect(replace).toHaveBeenCalledWith("/panel?locale=en");
  });

  it("muestra el mismo error para email desconocido y contraseña incorrecta", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));
    renderWithIntl(<VenueLoginForm />);
    completeForm();

    fireEvent.click(screen.getByRole("button", { name: "Acceder al panel" }));

    expect(
      await screen.findByText(
        "No hemos podido iniciar sesión. Comprueba el correo y la contraseña.",
      ),
    ).toBeVisible();
    expect(screen.queryByText(/no existe|incorrecta/i)).not.toBeInTheDocument();
  });

  it("ofrece recuperación y registro sin incluir secretos en las URL", () => {
    renderWithIntl(<VenueLoginForm />);

    expect(screen.getByRole("link", { name: "He olvidado mi contraseña" })).toHaveAttribute(
      "href",
      "/locales/recuperar-contrasena",
    );
    expect(screen.getByRole("link", { name: "Crear una cuenta" })).toHaveAttribute(
      "href",
      "/locales/registro",
    );
  });

  it("permite reintentar tras un fallo temporal", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);
    renderWithIntl(<VenueLoginForm />);
    completeForm();

    fireEvent.click(screen.getByRole("button", { name: "Acceder al panel" }));
    await screen.findByText(/No hemos podido conectar/);
    fireEvent.click(screen.getByRole("button", { name: "Acceder al panel" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  });
});
