import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { fetchPublicSearchSuggestions } from "./public-search-api";
import { PublicSearchAutocomplete } from "./public-search-autocomplete";

vi.mock("./public-search-api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./public-search-api")>()),
  fetchPublicSearchSuggestions: vi.fn(),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("PublicSearchAutocomplete", () => {
  it("espera al usuario, muestra coincidencias remotas y conserva el valor del formulario", async () => {
    vi.mocked(fetchPublicSearchSuggestions).mockResolvedValue([
      {
        kind: "query",
        value: "Casa Luz",
        label: "Casa Luz",
        context: "Restaurante · Madrid",
      },
    ]);
    renderWithIntl(
      <PublicSearchAutocomplete
        ariaLabel="Qué buscas"
        kind="query"
        name="q"
        placeholder="Nombre, servicio o categoría"
      />,
    );

    const input = screen.getByRole("combobox", { name: "Qué buscas" });
    fireEvent.change(input, { target: { value: "cas" } });
    expect(fetchPublicSearchSuggestions).not.toHaveBeenCalled();

    expect(await screen.findByText("Casa Luz")).toBeVisible();
    expect(screen.getByText("Restaurante · Madrid")).toBeVisible();
    expect(fetchPublicSearchSuggestions).toHaveBeenCalledWith(
      "es",
      "query",
      "cas",
      expect.any(AbortSignal),
    );

    fireEvent.click(screen.getByText("Casa Luz"));
    expect(input).toHaveValue("Casa Luz");
    expect(input).toHaveAttribute("name", "q");
  });
});
