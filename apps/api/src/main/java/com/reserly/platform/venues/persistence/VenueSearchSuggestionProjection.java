package com.reserly.platform.venues.persistence;

/** Proyección de solo lectura que evita hidratar perfiles completos durante el autocompletado. */
public interface VenueSearchSuggestionProjection {

  /** Valor visible y enviado al filtro cuando el usuario selecciona la opción. */
  String getValue();

  /** Etiqueta principal conservada con su ortografía original. */
  String getLabel();

  /** Contexto público opcional, como categoría y ciudad o tipo de ubicación. */
  String getContext();
}
