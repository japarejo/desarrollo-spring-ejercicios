package com.atech.curso.m1;

import java.math.BigDecimal;
import java.time.Clock;

import com.atech.curso.m1.pedidos.Pedido;
import com.atech.curso.m1.pedidos.PedidoService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Módulo 1: contenedor IoC, inyección de dependencias, perfiles, eventos y AOP.
 *
 * <p>Ejecutar con {@code mvn -pl m1-core-aop spring-boot:run -Dspring-boot.run.profiles=sms}
 * para ver la implementación alternativa de {@code Notificador}.
 */
@SpringBootApplication
@EnableRetry
public class M1Application {

    public static void main(String[] args) {
        SpringApplication.run(M1Application.class, args);
    }

    /** Reloj inyectable: facilita las pruebas deterministas (EJ 1.2). */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Pequeña demo por consola; se desactiva con atech.demo.enabled=false (lo hacen los tests). */
    @Bean
    @ConditionalOnProperty(name = "atech.demo.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner demo(PedidoService pedidos) {
        return args -> {
            pedidos.confirmar(new Pedido("P-1", "ana@atech.es", new BigDecimal("120.50")));
            pedidos.confirmar(new Pedido("P-2", "luis@atech.es", new BigDecimal("2500")));
        };
    }
}
