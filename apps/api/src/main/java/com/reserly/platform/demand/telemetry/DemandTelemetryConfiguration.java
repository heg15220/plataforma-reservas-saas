package com.reserly.platform.demand.telemetry;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Configura un ejecutor acotado y descartable para no bloquear negocio. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableAspectJAutoProxy
public class DemandTelemetryConfiguration {

  /** Saturación descarta telemetría sin propagar error al commit operativo. */
  @Bean("demandTelemetryExecutor")
  ThreadPoolTaskExecutor demandTelemetryExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("demand-telemetry-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(1_000);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    executor.initialize();
    return executor;
  }
}
