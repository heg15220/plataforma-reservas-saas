package com.reserly.platform.infrastructure.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita la abstracción de caché de Spring sin convertir Redis en fuente de verdad.
 *
 * <p>Las políticas de TTL, prefijo y valores nulos se externalizan mediante propiedades de Spring
 * Boot. Los módulos de negocio deben invalidar sus entradas cuando cambie el dato transaccional y
 * nunca pueden usar una lectura de caché para confirmar capacidad, permisos o pagos.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {}
