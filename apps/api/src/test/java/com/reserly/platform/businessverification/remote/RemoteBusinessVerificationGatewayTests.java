package com.reserly.platform.businessverification.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifica selección, watchdog, reintentos, backoff e idempotencia sin realizar red real. */
class RemoteBusinessVerificationGatewayTests {

  private static final UUID REQUEST_ID = UUID.fromString("26c137ef-8838-49d9-bce0-a8b818199f61");
  private static final UUID ACCOUNT_ID = UUID.fromString("a1aaa0ea-6898-4f21-b1de-e572062fdafb");
  private static final Instant CHECKED_AT = Instant.parse("2026-06-28T12:00:00Z");

  @Test
  void selectsOfficialPriorityAndHonorsExplicitProvider() {
    RecordingAdapter commercial = new RecordingAdapter("commercial", Set.of("ES"), 100);
    RecordingAdapter official = new RecordingAdapter("official", Set.of("ES", "PT"), 0);
    RemoteBusinessVerificationAdapterRegistry registry =
        new RemoteBusinessVerificationAdapterRegistry(List.of(commercial, official));
    RemoteBusinessVerificationGatewayService gateway = gateway(registry, new RecordingSleeper());

    RemoteVerificationExecution automatic = gateway.verify(request("ES"), null);
    RemoteVerificationExecution explicit = gateway.verify(request("ES"), "commercial");

    assertThat(automatic.providerCode()).isEqualTo("official");
    assertThat(explicit.providerCode()).isEqualTo("commercial");
    assertThat(official.invocations).hasSize(1);
    assertThat(commercial.invocations).hasSize(1);
  }

  @Test
  void retriesOnlyRetryableFailuresWithStableIdempotencyKeyAndExponentialBackoff() {
    RecordingAdapter adapter = new RecordingAdapter("official", Set.of("ES"), 0);
    adapter.failuresBeforeSuccess.set(2);
    RecordingSleeper sleeper = new RecordingSleeper();
    RemoteBusinessVerificationGatewayService gateway =
        gateway(new RemoteBusinessVerificationAdapterRegistry(List.of(adapter)), sleeper);

    RemoteVerificationExecution execution = gateway.verify(request("ES"), null);

    assertThat(execution.attemptCount()).isEqualTo((short) 3);
    assertThat(adapter.invocations)
        .extracting(RemoteVerificationAttemptContext::attemptNumber)
        .containsExactly(1, 2, 3);
    assertThat(adapter.invocations)
        .extracting(RemoteVerificationAttemptContext::idempotencyKey)
        .containsOnly(adapter.invocations.getFirst().idempotencyKey());
    assertThat(sleeper.delays).containsExactly(Duration.ofMillis(10), Duration.ofMillis(20));
  }

  @Test
  void doesNotRetryPermanentProviderFailure() {
    RecordingAdapter adapter = new RecordingAdapter("official", Set.of("ES"), 0);
    adapter.permanentFailure = true;
    RecordingSleeper sleeper = new RecordingSleeper();
    RemoteBusinessVerificationGatewayService gateway =
        gateway(new RemoteBusinessVerificationAdapterRegistry(List.of(adapter)), sleeper);

    assertThatThrownBy(() -> gateway.verify(request("ES"), null))
        .isInstanceOfSatisfying(
            RemoteVerificationExecutionException.class,
            exception -> {
              assertThat(exception.getErrorCode())
                  .isEqualTo(RemoteVerificationErrorCode.PROVIDER_AUTHENTICATION_ERROR);
              assertThat(exception.getAttemptCount()).isEqualTo((short) 1);
            });
    assertThat(sleeper.delays).isEmpty();
  }

  @Test
  void rejectsMissingOrIncompatibleProviderWithoutCallingNetwork() {
    RecordingAdapter adapter = new RecordingAdapter("official", Set.of("PT"), 0);
    RemoteBusinessVerificationGatewayService gateway =
        gateway(
            new RemoteBusinessVerificationAdapterRegistry(List.of(adapter)),
            new RecordingSleeper());

    assertThatThrownBy(() -> gateway.verify(request("ES"), "official"))
        .isInstanceOfSatisfying(
            RemoteVerificationExecutionException.class,
            exception -> {
              assertThat(exception.getErrorCode())
                  .isEqualTo(RemoteVerificationErrorCode.NO_ADAPTER_CONFIGURED);
              assertThat(exception.getAttemptCount()).isZero();
              assertThat(exception.getProviderCode()).isEqualTo("official");
            });
    assertThat(adapter.invocations).isEmpty();
  }

  @Test
  void rejectsDuplicateProviderCodesAtStartup() {
    RecordingAdapter first = new RecordingAdapter("official", Set.of("ES"), 0);
    RecordingAdapter second = new RecordingAdapter("official", Set.of("PT"), 1);

    assertThatThrownBy(() -> new RemoteBusinessVerificationAdapterRegistry(List.of(first, second)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate remote verification provider code");
  }

  @Test
  void watchdogCancelsInvocationThatExceedsTotalTimeout() {
    VirtualThreadRemoteVerificationCallExecutor executor =
        new VirtualThreadRemoteVerificationCallExecutor();
    try {
      assertThatThrownBy(
              () ->
                  executor.execute(
                      () -> {
                        try {
                          Thread.sleep(Duration.ofSeconds(1));
                        } catch (InterruptedException exception) {
                          Thread.currentThread().interrupt();
                          throw new RemoteBusinessVerificationException(
                              RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
                        }
                        return validResult();
                      },
                      Duration.ofMillis(10)))
          .isInstanceOf(TimeoutException.class);
    } finally {
      executor.close();
    }
  }

  private RemoteBusinessVerificationGatewayService gateway(
      RemoteBusinessVerificationAdapterRegistry registry, RemoteVerificationSleeper sleeper) {
    RemoteVerificationProperties properties =
        new RemoteVerificationProperties(
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            3,
            Duration.ofMillis(10),
            Duration.ofMillis(100),
            2.0);
    RemoteVerificationCallExecutor directExecutor = (invocation, timeout) -> invocation.invoke();
    return new RemoteBusinessVerificationGatewayServiceImpl(
        registry, properties, sleeper, directExecutor);
  }

  private RemoteBusinessVerificationRequest request(String country) {
    return new RemoteBusinessVerificationRequest(
        REQUEST_ID, ACCOUNT_ID, country, "B12345674", "Empresa de prueba SL", null, true);
  }

  private static RemoteBusinessVerificationResult validResult() {
    return new RemoteBusinessVerificationResult(
        RemoteVerificationStatus.VERIFIED,
        true,
        null,
        "REMOTE-REFERENCE",
        CHECKED_AT,
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
  }

  private static final class RecordingSleeper implements RemoteVerificationSleeper {

    private final List<Duration> delays = new ArrayList<>();

    @Override
    public void sleep(Duration duration) {
      delays.add(duration);
    }
  }

  private static final class RecordingAdapter implements RemoteBusinessVerificationAdapter {

    private final String providerCode;
    private final Set<String> supportedCountries;
    private final int priority;
    private final AtomicInteger failuresBeforeSuccess = new AtomicInteger();
    private final List<RemoteVerificationAttemptContext> invocations = new ArrayList<>();
    private boolean permanentFailure;

    private RecordingAdapter(String providerCode, Set<String> supportedCountries, int priority) {
      this.providerCode = providerCode;
      this.supportedCountries = supportedCountries;
      this.priority = priority;
    }

    @Override
    public String providerCode() {
      return providerCode;
    }

    @Override
    public Set<String> supportedCountries() {
      return supportedCountries;
    }

    @Override
    public int priority() {
      return priority;
    }

    @Override
    public RemoteBusinessVerificationResult verify(
        RemoteBusinessVerificationRequest request, RemoteVerificationAttemptContext context)
        throws RemoteBusinessVerificationException {
      invocations.add(context);
      if (permanentFailure) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_AUTHENTICATION_ERROR);
      }
      if (failuresBeforeSuccess.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
      }
      return validResult();
    }
  }
}
