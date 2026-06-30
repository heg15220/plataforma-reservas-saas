package com.reserly.platform.infrastructure.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra la barrera distribuida antes de validación y ejecución de controladores sensibles. */
@Configuration(proxyBeanMethods = false)
public class RateLimitWebConfiguration implements WebMvcConfigurer {

  private final SensitiveEndpointRateLimitInterceptor interceptor;

  public RateLimitWebConfiguration(SensitiveEndpointRateLimitInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(interceptor);
  }
}
