package com.reserly.platform.businessverification.remote;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** Backoff bloqueante; se usa fuera de transacciones y conserva la interrupción del hilo. */
@Component
public class ThreadRemoteVerificationSleeper implements RemoteVerificationSleeper {

  @Override
  public void sleep(Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }
}
