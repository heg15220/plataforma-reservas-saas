import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { VenueProfileEditor } from "./venue-profile-editor";

const category = {
  id: "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  slug: "restaurante",
  name: "Restaurante",
};

const profile = {
  id: "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
  categoryId: category.id,
  categorySlug: "restaurante",
  categoryName: "Restaurante",
  name: "Casa Luz",
  slug: "casa-luz",
  description: "Cocina de temporada",
  descriptionI18n: { sourceLocale: "es", values: { es: "Cocina", en: "Cuisine" } },
  servicesI18n: null,
  rulesI18n: null,
  publicTextI18n: null,
  defaultLocale: "es",
  contactEmail: "hola@casaluz.test",
  phone: "+34 910 000 000",
  address: "Calle Mayor, 1",
  city: "Madrid",
  province: "Madrid",
  country: "ES",
  postalCode: "28013",
  latitude: 40.416775,
  longitude: -3.70379,
  mainImageUrl: null,
  status: "draft",
  showPhone: true,
  showEmail: true,
  createdAt: "2026-07-01T08:00:00Z",
  updatedAt: "2026-07-01T09:00:00Z",
};

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

function response(body: unknown, status = 200) {
  return new Response(body === null ? null : JSON.stringify(body), {
    status,
    headers: body === null ? undefined : { "Content-Type": "application/json" },
  });
}

describe("VenueProfileEditor", () => {
  it("carga perfil y guarda cambios mediante PATCH privado", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response(profile))
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(response({ ...profile, name: "Casa Luz Actualizada" }));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    const name = await screen.findByRole("textbox", { name: /Nombre del local/ });
    fireEvent.change(name, { target: { value: "Casa Luz Actualizada" } });
    fireEvent.click(screen.getByRole("button", { name: "Guardar perfil" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(4));
    const [, options] = fetchMock.mock.calls[3] as [string, RequestInit];
    expect(options.method).toBe("PATCH");
    expect(JSON.parse(String(options.body))).toMatchObject({
      name: "Casa Luz Actualizada",
      descriptionI18n: { values: { es: "Cocina", en: "Cuisine" } },
    });
    expect(await screen.findByText("Cambios guardados correctamente.")).toBeVisible();
  }, 15_000);

  it("muestra requisitos accionables cuando la publicación es rechazada", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response(profile))
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(
        response(
          {
            error: "VENUE_PUBLICATION_REJECTED",
            requirements: ["EMAIL_NOT_VERIFIED", "MAIN_IMAGE_MISSING"],
          },
          422,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    fireEvent.click(await screen.findByRole("button", { name: "Publicar local" }));

    expect(await screen.findByText("Confirma el correo de la cuenta.")).toBeVisible();
    expect(screen.getByText("Sube una imagen principal.")).toBeVisible();
  }, 15_000);
});
