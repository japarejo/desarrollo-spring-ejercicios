package com.atech.curso.m2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.atech.curso.m2.config.ReservasProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** EJ 2.1 - Enlace y validación de @ConfigurationProperties sin levantar toda la aplicación. */
class ReservasPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ReservasProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void enlazaValoresYAplicaValoresPorDefecto() {
        runner.withPropertyValues("atech.reservas.salas=Turing,Hopper", "atech.reservas.duracion-maxima=90m")
                .run(ctx -> {
                    ReservasProperties p = ctx.getBean(ReservasProperties.class);
                    assertThat(p.salas()).containsExactly("Turing", "Hopper");
                    assertThat(p.duracionMaxima()).isEqualTo(Duration.ofMinutes(90));
                    assertThat(p.horaApertura()).isEqualTo(8);
                    assertThat(p.notificaciones().remitente()).isEqualTo("reservas@atech.es");
                });
    }

    @Test
    void fallaElArranqueSiNoHaySalas() {
        runner.run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void fallaElArranqueConHorasFueraDeRango() {
        runner.withPropertyValues("atech.reservas.salas=Turing", "atech.reservas.hora-apertura=25")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void validaLaConfiguracionAnidada() {
        runner.withPropertyValues("atech.reservas.salas=Turing", "atech.reservas.notificaciones.remitente=no-es-un-email")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
