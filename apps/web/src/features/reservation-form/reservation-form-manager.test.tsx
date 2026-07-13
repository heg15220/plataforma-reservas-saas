import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { ReservationFormManager } from "./reservation-form-manager";
import {
  createReservationFormField,
  deleteReservationFormField,
  fetchReservationFormFields,
  fetchReservationFormPreview,
  reorderReservationFormFields,
} from "./reservation-form-api";

vi.mock("./reservation-form-api", async (importOriginal) => {
  const original = await importOriginal<typeof import("./reservation-form-api")>();
  return {
    ...original,
    createReservationFormField: vi.fn(),
    deleteReservationFormField: vi.fn(),
    fetchReservationFormFields: vi.fn(),
    fetchReservationFormPreview: vi.fn(),
    reorderReservationFormFields: vi.fn(),
    updateReservationFormField: vi.fn(),
  };
});

const first = {
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
const second = {
  id: "10000000-0000-4000-8000-000000000002",
  label: "Preferencia",
  key: "preference",
  type: "short_text" as const,
  required: true,
  options: null,
  position: 1,
  active: true,
  createdAt: "2026-07-13T10:00:00Z",
  updatedAt: "2026-07-13T10:00:00Z",
};
const preview = [
  {
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
  },
  {
    ...first,
    source: "custom" as const,
    labelKey: null,
    position: 5,
    editable: true,
  },
];

beforeEach(() => {
  vi.mocked(fetchReservationFormFields).mockResolvedValue([first, second]);
  vi.mocked(fetchReservationFormPreview).mockResolvedValue(preview);
  vi.mocked(createReservationFormField).mockResolvedValue(first);
  vi.mocked(deleteReservationFormField).mockResolvedValue();
  vi.mocked(reorderReservationFormFields).mockResolvedValue([second, first]);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ReservationFormManager", () => {
  it("presenta campos personalizados y la vista previa combinada", async () => {
    renderWithIntl(<ReservationFormManager />);

    expect(await screen.findByText("Alergias")).toBeVisible();
    expect(screen.getByLabelText(/Nombre/)).toBeDisabled();
    expect(screen.getByLabelText("Alergias")).toBeDisabled();
  });

  it("valida en cliente y crea un campo con datos normalizados", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Añadir campo" }));

    const save = screen.getByRole("button", { name: "Guardar" });
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Etiqueta"), { target: { value: "  Código interno  " } });
    fireEvent.change(screen.getByLabelText("Clave técnica"), { target: { value: "Código inválido" } });
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Clave técnica"), { target: { value: "internal_code" } });
    expect(save).toBeEnabled();
    fireEvent.click(save);

    await waitFor(() => expect(createReservationFormField).toHaveBeenCalledWith({
      label: "Código interno",
      key: "internal_code",
      type: "short_text",
      required: false,
      options: null,
    }));
  });

  it("envía el orden completo al mover un campo", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Mover abajo: Alergias" }));

    await waitFor(() => expect(reorderReservationFormFields).toHaveBeenCalledWith([
      second.id,
      first.id,
    ]));
  });

  it("confirma la eliminación antes de ejecutar la mutación", async () => {
    renderWithIntl(<ReservationFormManager />);
    fireEvent.click(await screen.findByRole("button", { name: "Eliminar: Alergias" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("Alergias");

    fireEvent.click(screen.getByRole("button", { name: "Eliminar campo" }));
    await waitFor(() => expect(deleteReservationFormField).toHaveBeenCalledWith(first.id));
  });
});
