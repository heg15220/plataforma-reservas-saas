package com.reserly.platform.demand.retention;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la política tipada usada por el job de retención. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DemandRetentionProperties.class)
public class DemandRetentionConfiguration {}
