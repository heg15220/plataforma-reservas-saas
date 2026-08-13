package com.reserly.platform.demand.ingestion;

/** Error estable sin mensaje, campo, valor rechazado ni detalle de librería. */
public record DemandIngestionErrorResponse(String error) {}
