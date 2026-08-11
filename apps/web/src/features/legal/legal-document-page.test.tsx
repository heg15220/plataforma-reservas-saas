import { cleanup, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { renderWithIntl } from "@/test-utils/render-with-intl";

import { LegalDocumentPage } from "./legal-document-page";

afterEach(cleanup);

describe("LegalDocumentPage", () => {
  it("expone una jerarquía accesible y navegación legal cruzada", () => {
    renderWithIntl(
      <LegalDocumentPage
        currentPath="/legal/privacidad"
        description="Cómo tratamos tus datos."
        relatedHref="/legal/condiciones"
        relatedLabel="Consulta también"
        relatedText="condiciones de uso"
        reviewNotice="Pendiente de revisión jurídica antes de producción."
        sections={[{ title: "1. Datos", paragraphs: ["Solo los datos necesarios."] }]}
        title="Política de privacidad"
        updatedLabel="Versión"
        updatedValue="11 de agosto de 2026"
      />,
    );

    expect(screen.getByRole("heading", { level: 1, name: "Política de privacidad" })).toBeVisible();
    expect(screen.getByRole("heading", { level: 2, name: "1. Datos" })).toBeVisible();
    expect(screen.getByRole("link", { name: "condiciones de uso" })).toHaveAttribute(
      "href",
      "/legal/condiciones",
    );
    expect(screen.getByRole("contentinfo")).toBeVisible();
  });
});
