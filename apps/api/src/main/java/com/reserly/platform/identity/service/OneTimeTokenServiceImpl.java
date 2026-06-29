package com.reserly.platform.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Implementación sin estado para secretos opacos de un solo uso. */
@Service
public class OneTimeTokenServiceImpl implements OneTimeTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String generate() {
    byte[] entropy = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(entropy);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
  }

  @Override
  public String hash(String token) {
    if (!isValid(token)) {
      throw new IllegalArgumentException("Invalid one-time token format");
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  @Override
  public boolean isValid(String token) {
    return token != null && TOKEN_FORMAT.matcher(token).matches();
  }
}
