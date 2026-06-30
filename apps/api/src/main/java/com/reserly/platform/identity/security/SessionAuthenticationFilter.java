package com.reserly.platform.identity.security;

import com.reserly.platform.identity.controller.SessionCookieFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Convierte una única cookie de sesión válida en un principal de Spring Security.
 *
 * <p>Cookies ausentes, duplicadas, malformadas o desconocidas comparten el estado anónimo. El
 * filtro solo consulta PostgreSQL para namespaces privados.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

  private final SessionAuthenticationService authenticationService;

  public SessionAuthenticationFilter(SessionAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    sessionCookie(request)
        .flatMap(authenticationService::authenticate)
        .ifPresent(this::setAuthentication);
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    boolean venueNamespace = path.equals("/api/venue/me") || path.startsWith("/api/venue/me/");
    boolean adminNamespace = path.equals("/api/admin") || path.startsWith("/api/admin/");
    return !venueNamespace && !adminNamespace;
  }

  private Optional<String> sessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    String value = null;
    for (Cookie cookie : cookies) {
      if (SessionCookieFactory.COOKIE_NAME.equals(cookie.getName())) {
        if (value != null) {
          return Optional.empty();
        }
        value = cookie.getValue();
      }
    }
    return Optional.ofNullable(value);
  }

  private void setAuthentication(AuthenticatedAccount principal) {
    Collection<SimpleGrantedAuthority> authorities =
        principal.roles().stream()
            .map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT))
            .map(SimpleGrantedAuthority::new)
            .toList();
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, authorities);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
