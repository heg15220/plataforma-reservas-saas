import { describe, expect, it } from "vitest";

import {
  maximumBusinessDocumentBytes,
  validateBusinessDocumentFile,
} from "./business-document-file";

describe("validateBusinessDocumentFile", () => {
  it.each(["application/pdf", "image/jpeg", "image/png"])("admite el tipo declarado %s", (type) => {
    expect(validateBusinessDocumentFile(new File(["content"], "document", { type }))).toBeNull();
  });

  it("rechaza ficheros vacíos, demasiado grandes o no permitidos", () => {
    expect(
      validateBusinessDocumentFile(new File([], "empty.pdf", { type: "application/pdf" })),
    ).toBe("empty");
    expect(
      validateBusinessDocumentFile(
        new File([new Uint8Array(maximumBusinessDocumentBytes + 1)], "large.pdf", {
          type: "application/pdf",
        }),
      ),
    ).toBe("tooLarge");
    expect(
      validateBusinessDocumentFile(new File(["data"], "document.txt", { type: "text/plain" })),
    ).toBe("unsupportedType");
  });
});
