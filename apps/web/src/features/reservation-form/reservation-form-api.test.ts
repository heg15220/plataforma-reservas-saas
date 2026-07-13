import { afterEach, describe, expect, it, vi } from "vitest";

import {
  createReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  fetchReservationFormPublication,
  reorderReservationFormFields,
  ReservationFormApiError,
  updateReservationFormPublication,
} from "./reservation-form-api";

const localized = (es: string, en?: string) => ({
  sourceLocale: "es" as const,
  values: { es, ...(en ? { en } : {}) },
});
const field = {
  id: "10000000-0000-4000-8000-000000000001",
  label: "Alergias",
  labelI18n: localized("Alergias", "Allergies"),
  key: "allergies",
  type: "long_text" as const,
  required: false,
  options: null,
  optionsI18n: null,
  position: 0,
  active: true,
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};
const previewField = {
  id: null,
  source: "base" as const,
  label: null,
  labelKey: "reservation.form.customerName",
  labelI18n: null,
  key: "customer_name",
  type: "short_text" as const,
  required: true,
  editable: false,
  options: null,
  optionsI18n: null,
  position: 0,
};
const publication = {
  published: false,
  fallbackApproved: false,
  fullyTranslated: false,
  missingTranslations: ["allergies.label.en"],
  publishedAt: null,
};

afterEach(() => vi.unstubAllGlobals());

describe("reservation-form-api", () => {
  it("valida catálogo, preview y estado editorial autenticados", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json([field]))
      .mockResolvedValueOnce(Response.json({ fields: [previewField] }))
      .mockResolvedValueOnce(Response.json(publication));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchReservationFormFields()).resolves.toEqual([field]);
    await expect(fetchReservationFormPreview()).resolves.toEqual([previewField]);
    await expect(fetchReservationFormPublication()).resolves.toEqual(publication);
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: "include", method: "GET" });
  });

  it("serializa textos localizados, orden y aprobación de fallback", async () => {
    const input = {
      labelI18n: localized("Alergias", "Allergies"),
      key: "allergies",
      type: "long_text" as const,
      required: false,
      optionsI18n: null,
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json(field))
      .mockResolvedValueOnce(Response.json([field]))
      .mockResolvedValueOnce(Response.json({ ...publication, published: true, fallbackApproved: true }));
    vi.stubGlobal("fetch", fetchMock);

    await createReservationFormField(input);
    await reorderReservationFormFields([field.id]);
    await updateReservationFormPublication(true, true);

    expect(fetchMock.mock.calls[0][1]).toMatchObject({ body: JSON.stringify(input), method: "POST" });
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      body: JSON.stringify({ fieldIds: [field.id] }), method: "PUT",
    });
    expect(fetchMock.mock.calls[2][1]).toMatchObject({
      body: JSON.stringify({ published: true, fallbackApproved: true }), method: "PUT",
    });
  });

  it("rechaza respuestas localizadas inválidas", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json([{ ...field, labelI18n: null }])));
    await expect(fetchReservationFormFields()).rejects.toEqual(
      expect.objectContaining<Partial<ReservationFormApiError>>({ status: 502 }),
    );
  });
});
