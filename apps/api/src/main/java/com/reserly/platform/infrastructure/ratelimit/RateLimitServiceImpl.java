package com.reserly.platform.infrastructure.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Contador de ventana fija atómico sobre Redis.
 *
 * <p>Lua garantiza que el primer incremento y su TTL se crean como una sola operación visible para
 * todas las instancias. La clave guarda SHA-256 del discriminador, nunca IP, email o UUID en claro.
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

  private static final String KEY_PREFIX = "reserly:rate-limit:v1:";
  private static final DefaultRedisScript<List> CONSUME_SCRIPT =
      new DefaultRedisScript<>(
          """
          local current = redis.call('INCR', KEYS[1])
          if current == 1 then
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
          end
          return {current, redis.call('PTTL', KEYS[1])}
          """,
          List.class);

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties properties;

  public RateLimitServiceImpl(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  @Override
  public void check(RateLimitScope scope, String discriminator) {
    if (!properties.enabled()) {
      return;
    }
    RateLimitProperties.Limit limit = properties.limitFor(scope);
    String key = KEY_PREFIX + scope.keySegment() + ":" + sha256(discriminator);
    List<?> result;
    try {
      result =
          redisTemplate.execute(
              CONSUME_SCRIPT, List.of(key), Long.toString(limit.window().toMillis()));
    } catch (DataAccessException exception) {
      throw new RateLimitUnavailableException(exception);
    }
    if (result == null || result.size() != 2) {
      throw new RateLimitUnavailableException(
          new IllegalStateException("Redis returned an invalid rate-limit result"));
    }

    long current = ((Number) result.get(0)).longValue();
    long ttlMillis = Math.max(1L, ((Number) result.get(1)).longValue());
    if (current > limit.requests()) {
      throw new RateLimitExceededException(Duration.ofMillis(ttlMillis));
    }
  }

  private String sha256(String discriminator) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(discriminator.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
    }
  }
}
