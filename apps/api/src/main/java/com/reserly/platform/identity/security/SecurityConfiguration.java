package com.reserly.platform.identity.security;

import com.reserly.platform.configuration.ReserlyProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Política central de namespaces privados.
 *
 * <p>La API no crea sesión HTTP, no usa Basic ni formulario. Las escrituras autenticadas con cookie
 * se protegen mediante {@link BrowserCsrfProtectionFilter}; {@code SameSite=Strict} permanece como
 * defensa adicional.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  /** Construye la cadena stateless y exige roles persistidos explícitos. */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      BrowserCsrfProtectionFilter browserCsrfProtectionFilter,
      SessionAuthenticationFilter sessionAuthenticationFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    // No se usa HttpSession ni formularios Spring: el filtro stateless valida Origin/Referer contra
    // la allowlist antes de autenticar la cookie.
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .logout(logout -> logout.disable())
        .requestCache(requestCache -> requestCache.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/api/admin", "/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/venue/me", "/api/venue/me/**")
                    .hasRole("VENUE_OWNER")
                    .requestMatchers("/api/public/**", "/api/auth/**", "/api/payments/redsys/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .denyAll()
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(browserCsrfProtectionFilter, SessionAuthenticationFilter.class)
        .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Permite cookies solo desde los orígenes exactos configurados y prepara la futura cabecera CSRF.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(ReserlyProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        properties.allowedOrigins().stream().map(Object::toString).toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Accept", "Accept-Language", "Content-Type", "X-CSRF-Token"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3_600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
