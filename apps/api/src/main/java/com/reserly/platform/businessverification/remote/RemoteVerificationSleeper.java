package com.reserly.platform.businessverification.remote;

import java.time.Duration;

/** Abstracción del backoff para mantener el gateway determinista en pruebas. */
public interface RemoteVerificationSleeper {

  /**
   * Suspende el hilo durante el intervalo indicado.
   *
   * @throws InterruptedException si la aplicación cancela la operación
   */
  void sleep(Duration duration) throws InterruptedException;
}
