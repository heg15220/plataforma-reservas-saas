package com.reserly.platform.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Implementación CSPRNG + SHA-256 para secretos de sesión de alta entropía. */
@Service
public class SessionTokenServiceImpl implements SessionTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String generate() {
    byte[] token = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(token);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
  }

  @Override
  public String hash(String token) {
    if (!isValid(token)) {
      throw new IllegalArgumentException("Invalid session token format");
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 algorithm is unavailable", exception);
    }
  }

  @Override
  public boolean isValid(String token) {
    return token != null && TOKEN_FORMAT.matcher(token).matches();
  }
}
