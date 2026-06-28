package com.reserly.platform.businessverification.remote;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/** Ejecuta una llamada con un límite total independiente del cliente HTTP concreto. */
public interface RemoteVerificationCallExecutor {

  /**
   * Ejecuta y cancela la tarea si supera el timeout.
   *
   * @throws RemoteBusinessVerificationException error normalizado del adaptador
   * @throws TimeoutException si vence el watchdog
   * @throws InterruptedException si se cancela la operación local
   */
  RemoteBusinessVerificationResult execute(
      RemoteVerificationInvocation invocation, Duration timeout)
      throws RemoteBusinessVerificationException, TimeoutException, InterruptedException;
}
