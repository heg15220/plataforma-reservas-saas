package com.reserly.platform.demand.attribute.aggregation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la configuración tipada del agregador de evidencias. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DemandAggregationProperties.class)
public class DemandAggregationConfiguration {}
