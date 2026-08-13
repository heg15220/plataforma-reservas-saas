"use client";

import { demandSessionId, getDemandCorrelationId } from "./demand-correlation";
import { hasDemandConsent } from "@/features/privacy/demand-consent";

export type WebDemandEventType =
  | "searchPerformed"
  | "categoryViewed"
  | "venueClicked"
  | "filterApplied"
  | "photosViewed"
  | "reviewsViewed"
  | "bookingAbandoned"
  | "promotionOpened";

type DemandContext = Readonly<Record<string, string | number | boolean | null>>;

/**
 * Emite telemetría minimizada sin bloquear navegación ni render.
 *
 * Solo conserva una sesión UUID en sessionStorage; no crea cookie, anonymousId, fingerprint ni
 * identidad persistente. El Route Handler añade la credencial interna exclusivamente en servidor.
 */
export function trackDemandEvent(
  eventType: WebDemandEventType,
  context: DemandContext = {},
  requestId = getDemandCorrelationId(),
) {
  if (typeof window === "undefined" || !hasDemandConsent("analytics")) return;
  const event = {
    eventId: crypto.randomUUID(),
    schemaVersion: 1,
    eventType,
    occurredAt: new Date().toISOString(),
    requestId,
    purpose: "analytics",
    sessionId: demandSessionId(),
    context,
  };
  void fetch("/api/demand/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ events: [event] }),
    keepalive: true,
    credentials: "same-origin",
  }).catch(() => undefined);
}

/** Convierte slugs públicos a códigos lowerCamel compatibles sin copiar texto de búsqueda. */
export function toDemandCode(value: string) {
  const words = value
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter(Boolean);
  const code = words
    .map((word, index) => (index === 0 ? word : word[0]?.toUpperCase() + word.slice(1)))
    .join("")
    .slice(0, 64);
  return /^[a-z][a-zA-Z0-9]{0,63}$/.test(code) ? code : "unknown";
}
