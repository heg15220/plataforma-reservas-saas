package com.reserly.platform.identity.security;

import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.identity.controller.SessionCookieFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege las escrituras autenticadas con cookie mediante verificación estricta de origen.
 *
 * <p>Los navegadores no permiten falsificar {@code Origin} desde otro sitio. Para clientes que no
 * lo envían se admite exclusivamente un {@code Referer} cuyo origen coincida exactamente con la API
 * o con una aplicación web configurada. La ausencia de ambas cabeceras falla cerrada cuando una
 * cookie de sesión intenta mutar recursos privados o cerrar sesión.
 */
@Component
public class BrowserCsrfProtectionFilter extends OncePerRequestFilter {

  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
  private static final String ERROR_BODY = "{\"error\":\"CSRF_VALIDATION_FAILED\"}";

  private final Set<String> trustedOrigins;

  public BrowserCsrfProtectionFilter(ReserlyProperties properties) {
    Set<String> configuredOrigins = new HashSet<>();
    configuredOrigins.add(requireOrigin(properties.apiPublicBaseUrl()));
    properties.allowedOrigins().stream().map(this::requireOrigin).forEach(configuredOrigins::add);
    trustedOrigins = Set.copyOf(configuredOrigins);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (trustedRequestOrigin(request)) {
      filterChain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(ERROR_BODY);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return SAFE_METHODS.contains(request.getMethod())
        || !isProtectedPath(applicationPath(request))
        || !hasSessionCookie(request);
  }

  private boolean trustedRequestOrigin(HttpServletRequest request) {
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    if (origin != null) {
      return parseOrigin(origin, true).map(trustedOrigins::contains).orElse(false);
    }
    String referer = request.getHeader(HttpHeaders.REFERER);
    return parseOrigin(referer, false).map(trustedOrigins::contains).orElse(false);
  }

  private java.util.Optional<String> parseOrigin(String value, boolean originHeader) {
    if (value == null || value.isBlank() || value.length() > 2_048 || "null".equals(value)) {
      return java.util.Optional.empty();
    }
    try {
      URI uri = new URI(value);
      if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null) {
        return java.util.Optional.empty();
      }
      if (originHeader
          && ((uri.getRawPath() != null && !uri.getRawPath().isEmpty())
              || uri.getRawQuery() != null
              || uri.getRawFragment() != null)) {
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(toOrigin(uri));
    } catch (URISyntaxException exception) {
      return java.util.Optional.empty();
    }
  }

  private String requireOrigin(URI uri) {
    if (uri == null
        || uri.getScheme() == null
        || uri.getHost() == null
        || uri.getUserInfo() != null) {
      throw new IllegalArgumentException("Invalid trusted CSRF origin");
    }
    return toOrigin(uri);
  }

  private String toOrigin(URI uri) {
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    boolean defaultPort =
        ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    return scheme + "://" + host + (port < 0 || defaultPort ? "" : ":" + port);
  }

  private String applicationPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    return contextPath == null || contextPath.isEmpty()
        ? path
        : path.substring(contextPath.length());
  }

  private boolean hasSessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return false;
    }
    for (Cookie cookie : cookies) {
      if (SessionCookieFactory.COOKIE_NAME.equals(cookie.getName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isProtectedPath(String path) {
    return path.equals("/api/auth/logout")
        || path.equals("/api/venue/me")
        || path.startsWith("/api/venue/me/")
        || path.equals("/api/admin")
        || path.startsWith("/api/admin/");
  }
}
