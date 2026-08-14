package com.reserly.platform.demand.candidate;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra exclusivamente la configuración tipada de generación de candidatos. */
@Configuration
@EnableConfigurationProperties(HybridCandidateProperties.class)
public class HybridCandidateConfiguration {}
