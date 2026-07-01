import { describe, expect, it } from "vitest";

import enMessages from "../../../locales/en.json";
import esMessages from "../../../locales/es.json";
import {
  businessVerificationPresentation,
  businessVerificationStatuses,
  documentReviewPresentation,
  documentReviewStatuses,
  emailVerificationPresentation,
  emailVerificationStatuses,
  manualReviewPresentation,
  manualReviewStatuses,
  type VerificationStatusPresentation,
} from "./verification-status";

const technicalStatusPattern =
  /^(unverified|pending_remote_check|verified|pending_review|rejected|expired|approved|accepted|needs_correction)$/;

describe("contrato de textos de verificación", () => {
  it("mantiene una presentación exhaustiva para cada máquina de estados", () => {
    expect(Object.keys(businessVerificationPresentation)).toEqual(businessVerificationStatuses);
    expect(Object.keys(emailVerificationPresentation)).toEqual(emailVerificationStatuses);
    expect(Object.keys(manualReviewPresentation)).toEqual(manualReviewStatuses);
    expect(Object.keys(documentReviewPresentation)).toEqual(documentReviewStatuses);
  });

  it.each([
    ["business", businessVerificationPresentation],
    ["email", emailVerificationPresentation],
    ["manual review", manualReviewPresentation],
    ["document review", documentReviewPresentation],
  ])("resuelve títulos y descripciones ES/EN para %s", (_name, presentation) => {
    for (const value of Object.values(presentation) as VerificationStatusPresentation[]) {
      for (const messages of [esMessages.Verification, enMessages.Verification]) {
        const title = resolveMessage(messages, value.titleKey);
        const description = resolveMessage(messages, value.descriptionKey);

        expect(title).toBeTruthy();
        expect(description).toBeTruthy();
        expect(title).not.toMatch(technicalStatusPattern);
        expect(description).not.toMatch(technicalStatusPattern);
      }
    }
  });

  it("conserva mensajes seguros para errores de identidad y verificación", () => {
    const requiredErrorKeys = [
      "authenticationInvalid",
      "authenticationRequired",
      "authorizationDenied",
      "conflict",
      "invalidRequest",
      "rateLimited",
      "unavailable",
      "unknown",
    ] as const;

    for (const key of requiredErrorKeys) {
      expect(esMessages.Verification.errors[key]).toBeTruthy();
      expect(enMessages.Verification.errors[key]).toBeTruthy();
    }
  });
});

function resolveMessage(messages: object, path: string): string {
  const value = path
    .split(".")
    .reduce<unknown>(
      (current, segment) =>
        typeof current === "object" && current !== null
          ? (current as Record<string, unknown>)[segment]
          : undefined,
      messages,
    );

  return typeof value === "string" ? value : "";
}
