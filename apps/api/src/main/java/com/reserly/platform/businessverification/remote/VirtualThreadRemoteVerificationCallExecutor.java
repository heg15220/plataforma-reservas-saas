package com.reserly.platform.businessverification.remote;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

/**
 * Watchdog basado en hilos virtuales para aislar clientes bloqueantes sin agotar el pool web.
 *
 * <p>El adaptador debe seguir configurando sus timeouts de conexión y lectura; este límite total es
 * una segunda barrera frente a implementaciones defectuosas o proveedores que no finalizan.
 */
@Component
public class VirtualThreadRemoteVerificationCallExecutor implements RemoteVerificationCallExecutor {

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  @Override
  public RemoteBusinessVerificationResult execute(
      RemoteVerificationInvocation invocation, Duration timeout)
      throws RemoteBusinessVerificationException, TimeoutException, InterruptedException {
    Future<RemoteBusinessVerificationResult> future = executor.submit(invocation::invoke);
    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException exception) {
      future.cancel(true);
      throw exception;
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof RemoteBusinessVerificationException remoteException) {
        throw remoteException;
      }
      if (exception.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Remote adapter failed outside its declared contract");
    }
  }

  /** Libera tareas pendientes durante el apagado ordenado de Spring. */
  @PreDestroy
  public void close() {
    executor.close();
  }
}
