package com.atech.curso.m2.operacion;

import com.atech.curso.m2.config.ReservasProperties;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** EJ 2.4 - Indicador de salud propio: aparece como "salas" en /actuator/health. */
@Component("salas")
public class SalasHealthIndicator implements HealthIndicator {

    private final ReservasProperties propiedades;

    public SalasHealthIndicator(ReservasProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public Health health() {
        int salas = propiedades.salas().size();
        return (salas > 0 ? Health.up() : Health.down())
                .withDetail("salasConfiguradas", salas)
                .build();
    }
}
