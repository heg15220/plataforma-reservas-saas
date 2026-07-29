import { cleanup, fireEvent, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { StarRatingInput } from "./star-rating-input";

afterEach(cleanup);

describe("StarRatingInput", () => {
  it("expone cinco opciones accesibles y propaga una puntuación completa", () => {
    const onChange = vi.fn();
    renderWithIntl(<StarRatingInput onChange={onChange} value={null} />);

    expect(screen.getByText("Tu puntuación")).toBeVisible();
    expect(screen.getAllByRole("radio")).toHaveLength(5);
    fireEvent.click(screen.getByRole("radio", { name: "3 estrellas" }));

    expect(onChange).toHaveBeenCalledWith(3);
    fireEvent.keyDown(screen.getByRole("radio", { name: "3 estrellas" }), {
      key: "ArrowRight",
    });
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it("comunica el error junto al selector", () => {
    renderWithIntl(
      <StarRatingInput error="Selecciona una puntuación." onChange={vi.fn()} value={null} />,
    );

    expect(screen.getByText("Selecciona una puntuación.")).toBeVisible();
  });
});
