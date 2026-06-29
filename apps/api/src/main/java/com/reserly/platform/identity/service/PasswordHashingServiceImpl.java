package com.reserly.platform.identity.service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * BCrypt 2b configurable, con sal aleatoria y trabajo adaptativo.
 *
 * <p>BCrypt incluye versión, coste y sal en cada hash. El hash dummy se genera una vez al arrancar
 * y evita retornar inmediatamente cuando login todavía no dispone de un hash de usuario válido.
 */
@Service
public class PasswordHashingServiceImpl implements PasswordHashingService {

  private static final int MAX_PASSWORD_BYTES = 72;
  private static final int MIN_ACCEPTED_STRENGTH = 4;
  private static final int MAX_ACCEPTED_STRENGTH = 16;
  private static final Pattern BCRYPT_HASH =
      Pattern.compile("^\\$(2[aby])\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");

  private final PasswordHashingProperties properties;
  private final BCryptPasswordEncoder passwordEncoder;
  private final String dummyHash;

  public PasswordHashingServiceImpl(PasswordHashingProperties properties) {
    this.properties = properties;
    this.passwordEncoder =
        new BCryptPasswordEncoder(
            BCryptPasswordEncoder.BCryptVersion.$2B, properties.bcryptStrength());
    this.dummyHash = passwordEncoder.encode("reserly-dummy-credential");
  }

  @Override
  public void validate(String rawPassword) {
    if (!isSafeInput(rawPassword)) {
      throw new PasswordHashingValidationException();
    }
  }

  @Override
  public String hash(String rawPassword) {
    validate(rawPassword);
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String encodedHash) {
    boolean safeInput = isSafeInput(rawPassword);
    boolean validHash = parse(encodedHash) != null;
    String candidate = safeInput ? rawPassword : "invalid-password-input";
    boolean matched = passwordEncoder.matches(candidate, validHash ? encodedHash : dummyHash);
    return safeInput && validHash && matched;
  }

  @Override
  public boolean requiresRehash(String encodedHash) {
    Matcher parsed = parse(encodedHash);
    if (parsed == null) {
      return true;
    }
    int encodedStrength = Integer.parseInt(parsed.group(2));
    return !"2b".equals(parsed.group(1)) || encodedStrength < properties.bcryptStrength();
  }

  private boolean isSafeInput(String rawPassword) {
    return rawPassword != null
        && !rawPassword.isEmpty()
        && rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
  }

  private Matcher parse(String encodedHash) {
    if (encodedHash == null) {
      return null;
    }
    Matcher matcher = BCRYPT_HASH.matcher(encodedHash);
    if (!matcher.matches()) {
      return null;
    }
    int encodedStrength = Integer.parseInt(matcher.group(2));
    return encodedStrength >= MIN_ACCEPTED_STRENGTH && encodedStrength <= MAX_ACCEPTED_STRENGTH
        ? matcher
        : null;
  }
}
