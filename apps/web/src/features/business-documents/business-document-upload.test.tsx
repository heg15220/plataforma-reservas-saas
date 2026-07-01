import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { BusinessDocumentUpload } from "./business-document-upload";

const requestPayload = {
  requestId: "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  reasonCode: "no_automated_channel",
  requestedDocumentTypes: ["census_certificate", "other"],
  status: "open",
  requestedAt: "2026-07-01T08:00:00Z",
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

describe("BusinessDocumentUpload", () => {
  it("muestra un estado estable cuando no hay solicitud abierta", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(null, 204)));

    renderWithIntl(<BusinessDocumentUpload />);

    expect(screen.getByText("Consultando el estado de la verificación…")).toBeVisible();
    expect(
      await screen.findByRole("heading", {
        name: "No necesitamos documentación adicional",
      }),
    ).toBeVisible();
  });

  it("presenta solo los tipos solicitados por el servidor y selecciona el primero", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(requestPayload)));

    renderWithIntl(<BusinessDocumentUpload />);

    expect(await screen.findByRole("heading", { name: "Documentación solicitada" })).toBeVisible();
    expect(screen.getByRole("radio", { name: "Certificado de situación censal" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "Otro documento acreditativo" })).toBeInTheDocument();
    expect(screen.queryByRole("radio", { name: "Alta censal 036/037" })).not.toBeInTheDocument();
    expect(
      screen.getByText(
        "No existe un canal automático disponible para completar esta comprobación.",
      ),
    ).toBeVisible();
  });

  it("rechaza un tipo cliente no permitido sin iniciar la subida", async () => {
    const fetchMock = vi.fn().mockResolvedValue(response(requestPayload));
    vi.stubGlobal("fetch", fetchMock);
    const { container } = renderWithIntl(<BusinessDocumentUpload />);
    await screen.findByRole("heading", { name: "Documentación solicitada" });
    const input = container.querySelector('input[type="file"]');
    expect(input).not.toBeNull();

    fireEvent.change(input!, {
      target: { files: [new File(["text"], "document.txt", { type: "text/plain" })] },
    });

    expect(screen.getByText("Selecciona un archivo PDF, JPG o PNG.")).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("sube el archivo y sustituye el formulario por el estado de revisión", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(requestPayload))
      .mockResolvedValueOnce(
        response(
          {
            documentId: "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            documentRequestId: requestPayload.requestId,
            status: "pending_review",
            uploadedAt: "2026-07-01T09:00:00Z",
          },
          201,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);
    const { container } = renderWithIntl(<BusinessDocumentUpload />);
    await screen.findByRole("heading", { name: "Documentación solicitada" });
    const input = container.querySelector('input[type="file"]');
    const file = new File(["%PDF-content"], "certificate.pdf", {
      type: "application/pdf",
    });

    fireEvent.change(input!, { target: { files: [file] } });
    expect(screen.getByText("certificate.pdf")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Enviar documentación" }));

    expect(await screen.findByRole("heading", { name: "Documentación enviada" })).toBeVisible();
    expect(screen.getByText("Pendiente de revisión")).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const [, uploadOptions] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(uploadOptions.body).toBeInstanceOf(FormData);
  });

  it("deriva una sesión caducada al acceso sin mostrar el formulario", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(null, 401)));

    renderWithIntl(<BusinessDocumentUpload />);

    expect(await screen.findByText(/Tu sesión ha caducado/)).toBeVisible();
    expect(screen.getByRole("link", { name: "Ir al acceso de locales" })).toHaveAttribute(
      "href",
      "/locales/acceso",
    );
    expect(screen.queryByRole("button", { name: "Enviar documentación" })).not.toBeInTheDocument();
  });

  it("mantiene el formulario y permite reintentar tras un fallo temporal", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(null, 503))
      .mockResolvedValueOnce(response(requestPayload));
    vi.stubGlobal("fetch", fetchMock);
    renderWithIntl(<BusinessDocumentUpload />);

    const retry = await screen.findByRole("button", { name: "Volver a intentarlo" });
    fireEvent.click(retry);

    await waitFor(() =>
      expect(screen.getByRole("heading", { name: "Documentación solicitada" })).toBeVisible(),
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
