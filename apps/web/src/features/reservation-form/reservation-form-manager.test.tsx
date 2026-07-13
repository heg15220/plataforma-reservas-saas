import { cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";
import { ReservationFormManager } from "./reservation-form-manager";
import {
  createReservationFormField,
  deleteReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  fetchReservationFormPublication,
  reorderReservationFormFields,
  updateReservationFormPublication,
} from "./reservation-form-api";

vi.mock("./reservation-form-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./reservation-form-api")>();
  return {
    ...original,
    createReservationFormField: vi.fn(),
    deleteReservationFormField: vi.fn(),
    fetchReservationFormFields: vi.fn(),
    fetchReservationFormPreview: vi.fn(),
    fetchReservationFormPublication: vi.fn(),
    reorderReservationFormFields: vi.fn(),
    updateReservationFormField: vi.fn(),
    updateReservationFormPublication: vi.fn(),
  };
});

const localized = (es: string, en?: string) => ({
  sourceLocale: "es" as const, values: { es, ...(en ? { en } : {}) },
});
const first = {
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
const second = { ...first, id: "10000000-0000-4000-8000-000000000002", label: "Preferencia", labelI18n: localized("Preferencia", "Preference"), key: "preference", position: 1 };
const preview = [{
  id: null, source: "base" as const, label: null, labelKey: "reservation.form.customerName",
  labelI18n: null, key: "customer_name", type: "short_text" as const, required: true,
  editable: false, options: null, optionsI18n: null, position: 0,
}, {
  id: first.id, source: "custom" as const, label: first.label, labelKey: null,
  labelI18n: first.labelI18n, key: first.key, type: first.type, required: false,
  editable: true, options: null, optionsI18n: null, position: 5,
}];
const publication = {
  published: false, fallbackApproved: false, fullyTranslated: false,
  missingTranslations: ["allergies.label.en"], publishedAt: null,
};

beforeEach(() => {
  vi.mocked(fetchReservationFormFields).mockResolvedValue([first, second]);
  vi.mocked(fetchReservationFormPreview).mockResolvedValue(preview);
  vi.mocked(fetchReservationFormPublication).mockResolvedValue(publication);
  vi.mocked(createReservationFormField).mockResolvedValue(first);
  vi.mocked(deleteReservationFormField).mockResolvedValue();
  vi.mocked(reorderReservationFormFields).mockResolvedValue([second, first]);
  vi.mocked(updateReservationFormPublication).mockResolvedValue({
    ...publication, published: true, fallbackApproved: true,
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ReservationFormManager", () => {
  it("renderiza el texto localizado y la preview combinada", async () => {
    renderWithIntl(<ReservationFormManager />);
    expect(await screen.findByText("Alergias")).toBeVisible();
    expect(screen.getByLabelText(/Nombre/)).toBeDisabled();
    expect(screen.getByText("Hay 1 traducciones pendientes.")).toBeVisible();
  });

  it("crea un campo con valores ES/EN normalizados", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Añadir campo" }));
    fireEvent.change(screen.getByLabelText("Etiqueta en español"), { target: { value: " Código " } });
    fireEvent.change(screen.getByLabelText("Etiqueta en inglés"), { target: { value: " Code " } });
    fireEvent.change(screen.getByLabelText("Clave técnica"), { target: { value: "internal_code" } });
    fireEvent.click(screen.getByRole("button", { name: "Guardar" }));

    await waitFor(() => expect(createReservationFormField).toHaveBeenCalledWith({
      labelI18n: { sourceLocale: "es", values: { es: "Código", en: "Code" } },
      key: "internal_code", type: "short_text", required: false, optionsI18n: null,
    }));
  });

  it("envía el orden completo al mover un campo", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Mover abajo: Alergias" }));
    await waitFor(() => expect(reorderReservationFormFields).toHaveBeenCalledWith([second.id, first.id]));
  });

  it("exige aprobar fallback antes de publicar traducciones incompletas", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Publicar" }));
    const dialog = screen.getByRole("dialog");
    const publish = within(dialog).getByRole("button", { name: "Publicar" });
    expect(dialog).toBeVisible();
    expect(publish).toBeDisabled();

    fireEvent.click(screen.getByLabelText("Aprobar el fallback al idioma origen"));
    fireEvent.click(publish);
    await waitFor(() => expect(updateReservationFormPublication).toHaveBeenCalledWith(true, true));
  });

  it("confirma la eliminación antes de mutar", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Eliminar: Alergias" }));
    fireEvent.click(screen.getByRole("button", { name: "Eliminar campo" }));
    await waitFor(() => expect(deleteReservationFormField).toHaveBeenCalledWith(first.id));
  });
});
