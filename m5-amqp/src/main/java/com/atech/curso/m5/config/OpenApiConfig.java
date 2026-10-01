package com.atech.curso.m5.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de Swagger UI (http://localhost:8080/swagger-ui.html), desde donde se publican los eventos. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI eventosOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Eventos de reservas (RabbitMQ)")
                .version("v1")
                .description("""
                        Publica eventos en `reservas.exchange` para ver la mensajería en acción. Sigue el log \
                        de la aplicación y la consola de RabbitMQ (http://localhost:15672, guest/guest)."""));
    }
}
