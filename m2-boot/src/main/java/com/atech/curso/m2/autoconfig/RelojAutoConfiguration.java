package com.atech.curso.m2.autoconfig;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * EJ 2.6 - Autoconfiguración propia (normalmente viviría en un starter aparte, p. ej.
 * {@code atech-reloj-spring-boot-starter}). Se registra en
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * y cede el paso si la aplicación define su propio {@link Clock}.
 */
@AutoConfiguration
public class RelojAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }
}
