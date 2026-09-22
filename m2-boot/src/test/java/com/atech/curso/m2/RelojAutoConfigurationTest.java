package com.atech.curso.m2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.atech.curso.m2.autoconfig.RelojAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** EJ 2.6 - Probar una autoconfiguración con ApplicationContextRunner. */
class RelojAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RelojAutoConfiguration.class));

    @Test
    void creaUnRelojPorDefecto() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(Clock.class));
    }

    @Test
    void cedeElPasoAlRelojDeLaAplicacion() {
        Clock fijo = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        runner.withBean(Clock.class, () -> fijo)
                .run(ctx -> assertThat(ctx.getBean(Clock.class)).isSameAs(fijo));
    }
}
