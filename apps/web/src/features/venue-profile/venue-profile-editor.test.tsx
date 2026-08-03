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
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

function response(body: unknown, status = 200) {
  return new Response(
    body === null ? null : JSON.stringify(body),
    {
      status,
      headers: body === null ? undefined : { "Content-Type": "application/json" },
    },
    15_000,
  );
}

describe("VenueProfileEditor", () => {
  it("oculta la gestión de altas a una cuenta con un solo local", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(response([category]))
        .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
        .mockResolvedValueOnce(response([])),
    );

    renderWithIntl(<VenueProfileEditor />);

    expect(await screen.findByRole("textbox", { name: /Nombre del local/ })).toBeVisible();
    expect(screen.queryByRole("button", { name: "Crear un local nuevo" })).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Local seleccionado")).not.toBeInTheDocument();
  }, 15_000);

  it("mantiene el selector y el alta para una cuenta multi-local", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(response([category]))
        .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: true }))
        .mockResolvedValueOnce(response([])),
    );

    renderWithIntl(<VenueProfileEditor />);

    expect(await screen.findByRole("button", { name: "Crear un local nuevo" })).toBeVisible();
  });

  it("carga perfil y guarda cambios mediante PATCH privado", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
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
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
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
    expect(screen.queryByText(/Tu local se ha publicado correctamente/)).not.toBeInTheDocument();
  }, 15_000);

  it("confirma la publicación y ofrece volver al inicio", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(
        response({ ...profile, status: "published", updatedAt: "2026-08-02T12:00:00Z" }),
      );
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    fireEvent.click(await screen.findByRole("button", { name: "Publicar local" }));

    expect(
      await screen.findByText(
        "Tu local se ha publicado correctamente. Ya puedes verlo desde la página de inicio.",
      ),
    ).toBeVisible();
    expect(screen.getByRole("link", { name: "Ver en la página de inicio" })).toHaveAttribute(
      "href",
      "/",
    );
    expect(screen.queryByText("Cambios guardados correctamente.")).not.toBeInTheDocument();
  }, 15_000);

  it("previsualiza la imagen principal elegida y la sube solo al confirmarla", async () => {
    const createObjectUrl = vi.fn(() => "blob:main-image-preview");
    const revokeObjectUrl = vi.fn();
    vi.stubGlobal("URL", {
      createObjectURL: createObjectUrl,
      revokeObjectURL: revokeObjectUrl,
    });
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(
        response({
          url: "/api/public/venues/casa-luz/main-image",
          mediaType: "image/png",
          sizeBytes: 6,
          width: 1200,
          height: 675,
        }),
      )
      .mockResolvedValueOnce(
        response({ ...profile, mainImageUrl: "/api/public/venues/casa-luz/main-image" }),
      );
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    const input = await screen.findByTestId("main-image-input");
    const file = new File(["imagen"], "fachada-casa-luz.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });

    expect(
      screen.getByRole("img", { name: "Vista previa de fachada-casa-luz.png" }),
    ).toHaveAttribute("src", "blob:main-image-preview");
    expect(screen.getByText(/Archivo seleccionado: fachada-casa-luz\.png/)).toBeVisible();
    const uploadButton = screen.getByRole("button", { name: "Subir imagen principal" });
    expect(uploadButton).toBeEnabled();

    fireEvent.click(uploadButton);

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5));
    const [url, options] = fetchMock.mock.calls[3] as [string, RequestInit];
    expect(url).toContain(`/api/venue/me/profiles/${profile.id}/main-image`);
    expect(options.method).toBe("POST");
    expect((options.body as FormData).get("file")).toBe(file);
    expect(
      await screen.findByRole("img", { name: "Imagen principal actual del local" }),
    ).toBeVisible();
    expect(screen.queryByText(/Archivo seleccionado:/)).not.toBeInTheDocument();
    expect(uploadButton).toBeDisabled();
    expect(revokeObjectUrl).toHaveBeenCalledWith("blob:main-image-preview");
  }, 15_000);

  it("previsualiza y sube varias imágenes de galería en una selección", async () => {
    const createObjectUrl = vi.fn((file: Blob) =>
      file instanceof File ? `blob:${file.name}` : "blob:gallery-image-preview",
    );
    const revokeObjectUrl = vi.fn();
    vi.stubGlobal("URL", {
      createObjectURL: createObjectUrl,
      revokeObjectURL: revokeObjectUrl,
    });
    const uploadedImages = [
      {
        id: "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
        url: "/api/public/venues/casa-luz/gallery/3",
        altText: "Terraza nocturna",
        position: 0,
        mediaType: "image/png",
        sizeBytes: 3072,
        width: 1200,
        height: 900,
      },
      {
        id: "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a34",
        url: "/api/public/venues/casa-luz/gallery/4",
        altText: "Comedor interior",
        position: 1,
        mediaType: "image/jpeg",
        sizeBytes: 4096,
        width: 1200,
        height: 900,
      },
    ];
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
      .mockResolvedValueOnce(response([]))
      .mockResolvedValueOnce(response(uploadedImages[0]))
      .mockResolvedValueOnce(response(uploadedImages[1]));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    const input = await screen.findByTestId("gallery-image-input");
    const terraceFile = new File(["imagen"], "terraza.png", { type: "image/png" });
    const diningRoomFile = new File(["imagen"], "comedor.jpg", { type: "image/jpeg" });
    fireEvent.change(input, { target: { files: [terraceFile, diningRoomFile] } });

    expect(
      await screen.findByRole("img", { name: "Vista previa de la imagen de galería terraza.png" }),
    ).toHaveAttribute("src", "blob:terraza.png");
    expect(
      screen.getByRole("img", { name: "Vista previa de la imagen de galería comedor.jpg" }),
    ).toHaveAttribute("src", "blob:comedor.jpg");
    expect(screen.getByText(/Imágenes seleccionadas pendientes de subir: 2/)).toBeVisible();
    const uploadButton = screen.getByRole("button", { name: "Subir a galería" });
    expect(uploadButton).toBeDisabled();

    fireEvent.change(screen.getByRole("textbox", { name: "Texto alternativo para terraza.png" }), {
      target: { value: "Terraza nocturna" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Texto alternativo para comedor.jpg" }), {
      target: { value: "Comedor interior" },
    });
    expect(uploadButton).toBeEnabled();
    fireEvent.click(uploadButton);

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5));
    const [firstUrl, firstOptions] = fetchMock.mock.calls[3] as [string, RequestInit];
    const [secondUrl, secondOptions] = fetchMock.mock.calls[4] as [string, RequestInit];
    expect(firstUrl).toContain(`/api/venue/me/profiles/${profile.id}/gallery`);
    expect(secondUrl).toContain(`/api/venue/me/profiles/${profile.id}/gallery`);
    expect(firstOptions.method).toBe("POST");
    expect(secondOptions.method).toBe("POST");
    expect((firstOptions.body as FormData).get("file")).toBe(terraceFile);
    expect((firstOptions.body as FormData).get("altText")).toBe("Terraza nocturna");
    expect((secondOptions.body as FormData).get("file")).toBe(diningRoomFile);
    expect((secondOptions.body as FormData).get("altText")).toBe("Comedor interior");
    expect(await screen.findByText("Imágenes cargadas: 2")).toBeVisible();
    expect(screen.getByRole("img", { name: "Terraza nocturna" })).toBeVisible();
    expect(screen.getByRole("img", { name: "Comedor interior" })).toBeVisible();
    expect(screen.queryByText(/Imágenes seleccionadas pendientes/)).not.toBeInTheDocument();
    expect(uploadButton).toBeDisabled();
    expect(revokeObjectUrl).toHaveBeenCalledWith("blob:terraza.png");
    expect(revokeObjectUrl).toHaveBeenCalledWith("blob:comedor.jpg");
  }, 15_000);

  it("muestra el total de imágenes cargadas y lo actualiza al eliminar una", async () => {
    const gallery = [
      {
        id: "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31",
        url: "/api/public/venues/casa-luz/gallery/1",
        altText: "Salón principal",
        position: 0,
        mediaType: "image/png",
        sizeBytes: 1024,
        width: 1200,
        height: 900,
      },
      {
        id: "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32",
        url: "/api/public/venues/casa-luz/gallery/2",
        altText: "Terraza",
        position: 1,
        mediaType: "image/png",
        sizeBytes: 2048,
        width: 1200,
        height: 900,
      },
    ];
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [profile], canCreateAdditionalVenue: false }))
      .mockResolvedValueOnce(response(gallery))
      .mockResolvedValueOnce(response(null, 204));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    expect(await screen.findByText("Imágenes cargadas: 2")).toBeVisible();
    fireEvent.click(screen.getAllByRole("button", { name: "Eliminar imagen" })[0]);

    expect(await screen.findByText("Imágenes cargadas: 1")).toBeVisible();
  }, 15_000);

  it("presenta la creación del primer local cuando la cuenta aún no tiene perfil", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [], canCreateAdditionalVenue: false }));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    expect(await screen.findByText(/Tu cuenta todavía no tiene un local\./)).toBeVisible();
    expect(screen.getByRole("button", { name: "Crear local" })).toBeVisible();
  });

  it("mantiene controlada la visibilidad de contacto al crear el primer local", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const createdProfile = { ...profile, name: "Nuevo local", showEmail: true, showPhone: true };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response([category]))
      .mockResolvedValueOnce(response({ profiles: [], canCreateAdditionalVenue: false }))
      .mockResolvedValueOnce(response(createdProfile));
    vi.stubGlobal("fetch", fetchMock);

    renderWithIntl(<VenueProfileEditor />);

    fireEvent.change(await screen.findByRole("textbox", { name: "Nombre del local" }), {
      target: { value: "Nuevo local" },
    });
    const showEmail = screen.getByRole("checkbox", {
      name: "Mostrar correo en la ficha pública",
    });
    const showPhone = screen.getByRole("checkbox", {
      name: "Mostrar teléfono en la ficha pública",
    });
    fireEvent.click(showEmail);
    fireEvent.click(showPhone);
    fireEvent.click(screen.getByRole("button", { name: "Crear local" }));

    expect(await screen.findByText("Cambios guardados correctamente.")).toBeVisible();
    expect(showEmail).toBeChecked();
    expect(showPhone).toBeChecked();
    const [, options] = fetchMock.mock.calls[2] as [string, RequestInit];
    expect(JSON.parse(String(options.body))).toMatchObject({ showEmail: true, showPhone: true });
    expect(consoleError.mock.calls.flat().join(" ")).not.toContain(
      "changing the default checked state of an uncontrolled SwitchBase",
    );
  }, 15_000);
});
