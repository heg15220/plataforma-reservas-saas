package com.reserly.platform.businessverification.remote;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Service;

/** Implementación del gateway remoto sin dependencia de un protocolo o proveedor concreto. */
@Service
public class RemoteBusinessVerificationGatewayServiceImpl
    implements RemoteBusinessVerificationGatewayService {

  private static final String UNAVAILABLE_PROVIDER = "unavailable";

  private final RemoteBusinessVerificationAdapterRegistry adapterRegistry;
  private final RemoteVerificationProperties properties;
  private final RemoteVerificationSleeper sleeper;
  private final RemoteVerificationCallExecutor callExecutor;

  public RemoteBusinessVerificationGatewayServiceImpl(
      RemoteBusinessVerificationAdapterRegistry adapterRegistry,
      RemoteVerificationProperties properties,
      RemoteVerificationSleeper sleeper,
      RemoteVerificationCallExecutor callExecutor) {
    this.adapterRegistry = adapterRegistry;
    this.properties = properties;
    this.sleeper = sleeper;
    this.callExecutor = callExecutor;
  }

  @Override
  public RemoteVerificationExecution verify(
      RemoteBusinessVerificationRequest request, String preferredProvider) {
    long startedAt = System.nanoTime();
    RemoteBusinessVerificationAdapter adapter;
    try {
      adapter = adapterRegistry.resolve(request, preferredProvider);
    } catch (NoRemoteVerificationAdapterException exception) {
      throw failure(
          request,
          normalizedUnavailableProvider(preferredProvider),
          RemoteVerificationErrorCode.NO_ADAPTER_CONFIGURED,
          (short) 0,
          startedAt);
    }

    String idempotencyKey = createIdempotencyKey(request, adapter.providerCode());
    Duration backoff = properties.initialBackoff();
    short attempts = 0;
    while (attempts < properties.maxAttempts()) {
      attempts++;
      RemoteVerificationAttemptContext context =
          new RemoteVerificationAttemptContext(
              request.requestId(),
              idempotencyKey,
              attempts,
              properties.connectTimeout(),
              properties.readTimeout());
      try {
        RemoteBusinessVerificationResult result =
            callExecutor.execute(
                () -> adapter.verify(request, context),
                properties.connectTimeout().plus(properties.readTimeout()));
        return new RemoteVerificationExecution(
            request.requestId(),
            adapter.providerCode(),
            result,
            attempts,
            elapsedMillis(startedAt));
      } catch (TimeoutException exception) {
        if (attempts >= properties.maxAttempts()) {
          throw failure(
              request,
              adapter.providerCode(),
              RemoteVerificationErrorCode.PROVIDER_TIMEOUT,
              attempts,
              startedAt);
        }
      } catch (RemoteBusinessVerificationException exception) {
        if (!exception.getErrorCode().retryable() || attempts >= properties.maxAttempts()) {
          throw failure(
              request, adapter.providerCode(), exception.getErrorCode(), attempts, startedAt);
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw failure(
            request,
            adapter.providerCode(),
            RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE,
            attempts,
            startedAt);
      }

      sleepBeforeRetry(request, adapter.providerCode(), attempts, backoff, startedAt);
      backoff = nextBackoff(backoff);
    }
    throw new IllegalStateException("Remote retry loop finished without a result");
  }

  private void sleepBeforeRetry(
      RemoteBusinessVerificationRequest request,
      String providerCode,
      short attempts,
      Duration backoff,
      long startedAt) {
    try {
      sleeper.sleep(backoff);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw failure(
          request,
          providerCode,
          RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE,
          attempts,
          startedAt);
    }
  }

  private Duration nextBackoff(Duration current) {
    double multiplied = current.toMillis() * properties.backoffMultiplier();
    long nextMillis = Math.min(properties.maxBackoff().toMillis(), Math.round(multiplied));
    return Duration.ofMillis(nextMillis);
  }

  private String createIdempotencyKey(
      RemoteBusinessVerificationRequest request, String providerCode) {
    String material = providerCode + ":" + request.requestId();
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 algorithm is unavailable", exception);
    }
  }

  private RemoteVerificationExecutionException failure(
      RemoteBusinessVerificationRequest request,
      String providerCode,
      RemoteVerificationErrorCode errorCode,
      short attempts,
      long startedAt) {
    return new RemoteVerificationExecutionException(
        request.requestId(), providerCode, errorCode, attempts, elapsedMillis(startedAt));
  }

  private int elapsedMillis(long startedAt) {
    long millis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    return (int) Math.min(Integer.MAX_VALUE, Math.max(0, millis));
  }

  private String normalizedUnavailableProvider(String preferredProvider) {
    if (preferredProvider == null
        || !preferredProvider.strip().matches("[a-z0-9][a-z0-9._-]{1,63}")) {
      return UNAVAILABLE_PROVIDER;
    }
    return preferredProvider.strip();
  }
}
