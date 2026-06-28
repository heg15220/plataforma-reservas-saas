package com.reserly.platform.businessverification.remote;

/** Señala una combinación país/proveedor sin adaptador instalado. */
class NoRemoteVerificationAdapterException extends RuntimeException {

  NoRemoteVerificationAdapterException() {
    super("No remote business verification adapter is configured");
  }
}
