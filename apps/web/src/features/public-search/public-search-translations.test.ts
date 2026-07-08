import { describe, expect, it } from "vitest";

import enMessages from "../../../locales/en.json";
import esMessages from "../../../locales/es.json";

const requiredCategoryKeys = [
  "restaurante",
  "peluqueria",
  "campo-de-futbol",
  "pista-de-padel",
  "instalacion-municipal",
  "centro-deportivo",
  "centro-de-estetica",
  "otros",
] as const;

const requiredSortKeys = ["relevance", "rating", "distance", "availability", "newest"] as const;

describe("traducciones de búsqueda pública", () => {
  it("cubre buscador, filtros, resultados, vacíos, tarjetas y carriles en español", () => {
    expect(esMessages.HomePage.hero.title).toBe("¿Dónde quieres pedir cita hoy?");
    expect(esMessages.HomePage.search.queryLabel).toBe("Qué buscas");
    expect(esMessages.PublicSearch.filters.ariaLabel).toBe("Filtros de búsqueda pública");
    expect(esMessages.PublicSearch.resultsAria).toBe("Resultados de locales");
    expect(esMessages.PublicSearch.card.ratingPending).toBe("Valoraciones próximamente");
    expect(esMessages.PublicSearch.empty.localNotFoundTitle).toBe("No encontramos ese local");
    expect(esMessages.PublicSearch.actions.registerVenue).toBe("Registrar este local");
    expect(esMessages.PublicSearch.discovery.recommended.title).toBe("Recomendados");

    for (const key of requiredCategoryKeys) {
      expect(esMessages.PublicSearch.categories[key]).toBeTruthy();
    }
    for (const key of requiredSortKeys) {
      expect(esMessages.PublicSearch.sort[key]).toBeTruthy();
    }
  });

  it("mantiene equivalentes en inglés para la experiencia pública completa", () => {
    expect(enMessages.HomePage.hero.title).toBe("Where would you like to book today?");
    expect(enMessages.HomePage.search.queryLabel).toBe("What are you looking for?");
    expect(enMessages.PublicSearch.filters.ariaLabel).toBe("Public search filters");
    expect(enMessages.PublicSearch.resultsAria).toBe("Venue results");
    expect(enMessages.PublicSearch.card.ratingPending).toBe("Reviews coming soon");
    expect(enMessages.PublicSearch.empty.localNotFoundTitle).toBe("We could not find that venue");
    expect(enMessages.PublicSearch.actions.registerVenue).toBe("Register this venue");
    expect(enMessages.PublicSearch.discovery.recommended.title).toBe("Recommended");

    for (const key of requiredCategoryKeys) {
      expect(enMessages.PublicSearch.categories[key]).toBeTruthy();
    }
    for (const key of requiredSortKeys) {
      expect(enMessages.PublicSearch.sort[key]).toBeTruthy();
    }
  });
});
