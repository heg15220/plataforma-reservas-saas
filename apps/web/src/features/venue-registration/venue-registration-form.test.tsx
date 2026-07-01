import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueRegistrationForm } from "./venue-registration-form";

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

function completeForm() {
  fireEvent.change(screen.getByRole("textbox", { name: "Correo electrónico" }), {
    target: { value: "negocio@example.com" },
  });
  fireEvent.change(screen.getByLabelText(/^Contraseña/, { selector: "input" }), {
    target: { value: "correct-horse-battery" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Razón social" }), {
    target: { value: "Ejemplo Reservas SL" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Identificador fiscal o registral" }), {
    target: { value: "B12345674" },
  });
  fireEvent.click(
    screen.getByRole("checkbox", {
      name: /Acepto las condiciones del servicio y la política de privacidad/,
    }),
  );
}

describe("VenueRegistrationForm", () => {
  it("muestra validación contextual y mueve el foco al primer error", () => {
    renderWithIntl(<VenueRegistrationForm />);

    fireEvent.click(screen.getByRole("button", { name: "Crear cuenta de local" }));

    expect(screen.getAllByText("Este campo es obligatorio.").length).toBeGreaterThan(0);
    expect(screen.getByRole("textbox", { name: "Correo electrónico" })).toHaveFocus();
    expect(
      screen.getByText("Debes aceptar las condiciones y la política de privacidad."),
    ).toBeVisible();
  });

  it("permite mostrar y ocultar la contraseña", () => {
    renderWithIntl(<VenueRegistrationForm />);
    const password = screen.getByLabelText(/^Contraseña/, { selector: "input" });

    expect(password).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Mostrar contraseña" }));
    expect(password).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Ocultar contraseña" }));
    expect(password).toHaveAttribute("type", "password");
  });

  it("bloquea el doble envío y presenta el estado de éxito", async () => {
    let resolveRequest: ((response: Response) => void) | undefined;
    const fetchMock = vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        resolveRequest = resolve;
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWithIntl(<VenueRegistrationForm />);
    completeForm();

    fireEvent.click(screen.getByRole("button", { name: "Crear cuenta de local" }));

    expect(screen.getByRole("button", { name: "Creando cuenta" })).toBeDisabled();
    expect(fetchMock).toHaveBeenCalledTimes(1);

    resolveRequest?.(
      new Response(
        JSON.stringify({
          accountType: "venue_business",
          businessVerificationStatus: "unverified",
          emailVerificationRequired: true,
          canPublishVenue: false,
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );

    expect(await screen.findByRole("heading", { name: "La cuenta se ha creado" })).toBeVisible();
    expect(screen.getByText("Confirmación pendiente")).toBeVisible();
    expect(screen.getByText("Comprobación aún no iniciada")).toBeVisible();
    expect(screen.queryByText("unverified")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ir al acceso de locales" })).toHaveAttribute(
      "href",
      "/locales/acceso",
    );
  });

  it("presenta un conflicto genérico sin revelar el campo duplicado", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 409 })));
    renderWithIntl(<VenueRegistrationForm />);
    completeForm();

    fireEvent.click(screen.getByRole("button", { name: "Crear cuenta de local" }));

    await waitFor(() =>
      expect(
        screen.getByText(
          "No hemos podido completar el alta con estos datos. Revisa la información o contacta con soporte.",
        ),
      ).toBeVisible(),
    );
    expect(screen.queryByText(/duplicado/i)).not.toBeInTheDocument();
  });
});
