package com.reserly.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.identity.messaging.EmailVerificationMessagingTopology;
import com.reserly.platform.identity.messaging.PasswordResetMessagingTopology;
import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import com.reserly.platform.infrastructure.ratelimit.RateLimitExceededException;
import com.reserly.platform.infrastructure.ratelimit.RateLimitScope;
import com.reserly.platform.infrastructure.ratelimit.RateLimitService;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifica Redis y RabbitMQ contra servicios reales y efímeros.
 *
 * <p>La prueba no usa dobles de red: confirma autenticación, TTL, prefijos de caché, declaración de
 * topología y publicación/consumo AMQP. PostgreSQL sigue siendo creado por el driver Testcontainers
 * configurado en el perfil {@code test}.
 */
@SpringBootTest(
    properties = {
      "spring.cache.type=redis",
      "spring.rabbitmq.dynamic=true",
      "spring.rabbitmq.template.receive-timeout=5s",
      "reserly.rate-limit.enabled=true",
      "reserly.rate-limit.login.requests=2",
      "reserly.rate-limit.login.window=30s"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InfrastructureServicesIntegrationTests {

  private static final String REDIS_PASSWORD = "reserly-test-redis-password";
  private static final String RABBIT_USERNAME = "reserly";
  private static final String RABBIT_PASSWORD = "reserly-test-rabbit-password";

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(
              DockerImageName.parse(
                  "redis:8.8.0-alpine@sha256:"
                      + "09160599abd229764c0fb44cb6be640294e1d360a54b19985ab4843dcf2d90f1"))
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
          .waitingFor(
              Wait.forLogMessage(".*Ready to accept connections.*\\n", 1)
                  .withStartupTimeout(Duration.ofMinutes(1)));

  @Container
  static final GenericContainer<?> RABBITMQ =
      new GenericContainer<>(
              DockerImageName.parse(
                  "rabbitmq:4.3.2-management-alpine@sha256:"
                      + "a2b8ca223e4b6b91ce6dac5a87e8d4551974a7d8dc8c919d333b757507966ffd"))
          .withEnv("RABBITMQ_DEFAULT_USER", RABBIT_USERNAME)
          .withEnv("RABBITMQ_DEFAULT_PASS", RABBIT_PASSWORD)
          .withEnv("RABBITMQ_DEFAULT_VHOST", "/")
          .withExposedPorts(5672)
          .waitingFor(
              Wait.forLogMessage(".*Server startup complete.*\\n", 1)
                  .withStartupTimeout(Duration.ofMinutes(2)));

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private CacheManager cacheManager;

  @Autowired private AmqpAdmin amqpAdmin;

  @Autowired private RabbitTemplate rabbitTemplate;

  @Autowired private RateLimitService rateLimitService;

  @DynamicPropertySource
  static void infrastructureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.data.redis.url",
        () ->
            "redis://:" + REDIS_PASSWORD + "@" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    registry.add(
        "spring.rabbitmq.addresses",
        () ->
            "amqp://"
                + RABBIT_USERNAME
                + ":"
                + RABBIT_PASSWORD
                + "@"
                + RABBITMQ.getHost()
                + ":"
                + RABBITMQ.getMappedPort(5672));
  }

  @Test
  void storesEphemeralValuesAndCacheEntriesWithExpiration() throws InterruptedException {
    redisTemplate.opsForValue().set("reserly:test:ttl", "available", Duration.ofSeconds(30));

    assertThat(redisTemplate.opsForValue().get("reserly:test:ttl")).isEqualTo("available");
    assertThat(redisTemplate.getExpire("reserly:test:ttl")).isBetween(1L, 30L);

    Cache cache = cacheManager.getCache("infrastructure-smoke");
    assertThat(cache).isNotNull();
    cache.put("venue-1", "cached");

    assertThat(awaitCacheValue(cache, "venue-1")).isEqualTo("cached");
    Set<String> cacheKeys = redisTemplate.keys("reserly::infrastructure-smoke::*");
    assertThat(cacheKeys).hasSize(1);
    assertThat(redisTemplate.getExpire(cacheKeys.iterator().next())).isBetween(1L, 300L);
  }

  @Test
  void declaresSharedTopologyAndRoutesAJobMessage() throws Exception {
    assertThat(amqpAdmin.getQueueProperties(MessagingTopology.DEAD_LETTER_QUEUE)).isNotNull();
    assertThat(amqpAdmin.getQueueProperties(EmailVerificationMessagingTopology.QUEUE)).isNotNull();
    assertThat(amqpAdmin.getQueueProperties(PasswordResetMessagingTopology.QUEUE)).isNotNull();

    Queue testQueue = new AnonymousQueue();
    String queueName = amqpAdmin.declareQueue(testQueue);
    assertThat(queueName).isNotBlank();

    Binding binding =
        BindingBuilder.bind(testQueue)
            .to(new TopicExchange(MessagingTopology.JOBS_EXCHANGE))
            .with("test.infrastructure");
    amqpAdmin.declareBinding(binding);

    CorrelationData correlationData = new CorrelationData("infrastructure-smoke");
    rabbitTemplate.convertAndSend(
        MessagingTopology.JOBS_EXCHANGE, "test.infrastructure", "job-payload", correlationData);

    assertThat(correlationData.getFuture().get(5, TimeUnit.SECONDS).ack()).isTrue();
    assertThat(rabbitTemplate.receiveAndConvert(queueName)).isEqualTo("job-payload");
    assertThat(amqpAdmin.deleteQueue(queueName)).isTrue();
  }

  @Test
  void enforcesAtomicRateLimitWithTtlAndHashedDiscriminator() {
    String discriminator = "198.51.100.42";

    rateLimitService.check(RateLimitScope.LOGIN, discriminator);
    rateLimitService.check(RateLimitScope.LOGIN, discriminator);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rateLimitService.check(RateLimitScope.LOGIN, discriminator))
        .isInstanceOfSatisfying(
            RateLimitExceededException.class,
            exception ->
                assertThat(exception.retryAfter())
                    .isBetween(Duration.ofSeconds(1), Duration.ofSeconds(30)));

    Set<String> keys = redisTemplate.keys("reserly:rate-limit:v1:login:*");
    assertThat(keys).hasSize(1);
    assertThat(keys.iterator().next()).doesNotContain(discriminator);
    assertThat(redisTemplate.getExpire(keys.iterator().next())).isBetween(1L, 30L);
  }

  private String awaitCacheValue(Cache cache, String key) throws InterruptedException {
    String value = null;
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

    while (value == null && System.nanoTime() < deadline) {
      value = cache.get(key, String.class);

      if (value == null) {
        TimeUnit.MILLISECONDS.sleep(50);
      }
    }

    return value;
  }
}
