package com.reserly.platform.demand.identity;

/** Resultado interno de derivación; nunca debe exponerse por HTTP, métricas o logs. */
record VersionedEmailHmac(String keyVersion, String digest) {}
