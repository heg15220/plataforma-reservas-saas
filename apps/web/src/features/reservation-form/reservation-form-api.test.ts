import { afterEach, describe, expect, it, vi } from "vitest";

import {
  createReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  reorderReservationFormFields,
  ReservationFormApiError,
} from "./reservation-form-api";

const field = {
  id: "10000000-0000-4000-8000-000000000001",
  label: "Alergias",
  key: "allergies",
  type: "long_text" as const,
  required: false,
  options: null,
  position: 0,
  active: true,
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};

const previewField = {
  id: null,
  source: "base" as const,
  label: null,
  labelKey: "reservationForm.base.customerName",
  key: "customer_name",
  type: "short_text" as const,
  required: true,
  editable: false,
  options: null,
  position: 0,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("reservation-form-api", () => {
  it("carga catálogo y vista previa con credenciales y contratos validados", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json([field]))
      .mockResolvedValueOnce(Response.json({ fields: [previewField] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchReservationFormFields()).resolves.toEqual([field]);
    await expect(fetchReservationFormPreview()).resolves.toEqual([previewField]);

    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", method: "GET" });
    expect(String(fetchMock.mock.calls[1][0])).toContain("/reservation-form/preview");
  });

  it("serializa creación y reordenación con el contrato esperado", async () => {
    const input = {
      label: "Alergias",
      key: "allergies",
      type: "long_text" as const,
      required: false,
      options: null,
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json(field))
      .mockResolvedValueOnce(Response.json([field]));
    vi.stubGlobal("fetch", fetchMock);

    await createReservationFormField(input);
    await reorderReservationFormFields([field.id]);

    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      body: JSON.stringify(input),
      credentials: "include",
      method: "POST",
    });
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      body: JSON.stringify({ fieldIds: [field.id] }),
      method: "PUT",
    });
  });

  it("rechaza respuestas que incumplen el esquema de campos", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json([{ ...field, type: "unknown" }])));

    await expect(fetchReservationFormFields()).rejects.toEqual(
      expect.objectContaining<Partial<ReservationFormApiError>>({ status: 502 }),
    );
  });
});
