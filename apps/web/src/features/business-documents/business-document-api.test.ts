import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchBusinessDocumentRequest, uploadBusinessDocument } from "./business-document-api";

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
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("business document API", () => {
  it("interpreta 204 como ausencia de solicitud documental", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await expect(fetchBusinessDocumentRequest()).resolves.toBeNull();
  });

  it("valida la solicitud abierta y usa credenciales de sesión", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(requestPayload), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchBusinessDocumentRequest()).resolves.toEqual(requestPayload);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/venue/me/business-verification/document-request",
      expect.objectContaining({ method: "GET", credentials: "include" }),
    );
  });

  it("envía multipart sin fijar Content-Type ni serializar el archivo", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          documentId: "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
          documentRequestId: requestPayload.requestId,
          status: "pending_review",
          uploadedAt: "2026-07-01T09:00:00Z",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["%PDF-content"], "certificate.pdf", {
      type: "application/pdf",
    });

    await uploadBusinessDocument(requestPayload.requestId, "census_certificate", file);

    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(options.headers).toEqual({ Accept: "application/json" });
    expect(options.body).toBeInstanceOf(FormData);
    const body = options.body as FormData;
    expect(body.get("documentRequestId")).toBe(requestPayload.requestId);
    expect(body.get("documentType")).toBe("census_certificate");
    expect(body.get("file")).toBe(file);
  });

  it.each([
    [400, "invalid"],
    [401, "unauthenticated"],
    [403, "forbidden"],
    [409, "conflict"],
    [422, "malware"],
    [429, "rateLimited"],
    [503, "unavailable"],
  ] as const)("reduce HTTP %i a la categoría segura %s", async (status, kind) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status })));

    await expect(fetchBusinessDocumentRequest()).rejects.toMatchObject({ kind });
  });

  it("rechaza respuestas exitosas con contrato inesperado", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ ...requestPayload, requestedDocumentTypes: ["secret"] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(fetchBusinessDocumentRequest()).rejects.toMatchObject({
      kind: "unavailable",
    });
  });
});
