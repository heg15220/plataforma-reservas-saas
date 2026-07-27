import { describe, expect, it } from "vitest";

import enMessages from "../../../locales/en.json";
import esMessages from "../../../locales/es.json";

const incidentTypes = [
  "no_show",
  "late_cancellation",
  "late_arrival",
  "duplicate_or_abusive_booking",
  "venue_condition_breach",
  "manual_incident",
] as const;

const incidentStatuses = ["reported", "confirmed", "dismissed"] as const;

describe("traducciones de incidencias y penalizaciones", () => {
  it("cubre el flujo profesional completo en español e inglés", () => {
    for (const messages of [esMessages, enMessages]) {
      expect(messages.ReservationBooking.form.activeRestriction).toContain("{date}");
      expect(messages.VenueReservations.operations.report.warning).toBeTruthy();
      expect(messages.VenueReservations.operations.cancel.warning).toBeTruthy();
      expect(messages.VenueIncidents.penalties.title).toBeTruthy();
      expect(messages.VenueIncidents.penalties.summary).toMatch(/7.*14.*21.*60/);
      expect(messages.ReservationBooking.form.selectPlaceholder).toBeTruthy();

      for (const type of incidentTypes) {
        expect(messages.VenueIncidents.incidentType[type]).toBeTruthy();
        expect(messages.VenueReservations.incidentType[type]).toBeTruthy();
      }
      for (const status of incidentStatuses) {
        expect(messages.VenueIncidents.incidentStatus[status]).toBeTruthy();
        expect(messages.VenueReservations.incidentStatus[status]).toBeTruthy();
      }
    }
  });

  it("mantiene ortografía española y evita lenguaje acusatorio", () => {
    const visibleSpanish = JSON.stringify({
      restriction: esMessages.ReservationBooking.form.activeRestriction,
      operations: esMessages.VenueReservations.operations,
      incidents: esMessages.VenueIncidents,
    });

    expect(visibleSpanish).toContain("electrónico");
    expect(visibleSpanish).toContain("restricción");
    expect(visibleSpanish).toContain("días");
    expect(visibleSpanish).not.toMatch(
      /\b(denuncia|castigo|antecedentes|delincuente|lista negra)\b/i,
    );
  });
});
