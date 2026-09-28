package com.atech.curso.m3.gobierno;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProbabilidadesEscandalo.class)
class GobiernoConfig {

    /** Con {@code gobierno.semilla} la secuencia aleatoria se repite en cada arranque (útil en clase). */
    @Bean
    RandomGenerator azar(@Value("${gobierno.semilla:#{null}}") Long semilla) {
        return semilla == null ? new Random() : new Random(semilla);
    }
}
