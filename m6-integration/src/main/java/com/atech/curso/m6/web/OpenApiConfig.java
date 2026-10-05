package com.atech.curso.m6.web;

import java.io.File;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de Swagger UI (http://localhost:8080/swagger-ui.html), desde donde se envían las solicitudes. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI solicitudesOpenApi(@Value("${atech.ficheros.entrada}") File entrada,
            @Value("${atech.ficheros.salida}") File salida) {
        return new OpenAPI().info(new Info()
                .title("Solicitudes de reserva (Spring Integration)")
                .version("v1")
                .description("""
                        Entrada web del flujo. Las otras dos entradas no pasan por aquí: la tabla \
                        `linea_pendiente` (se sondea cada 10 s) y el buzón de ficheros CSV.

                        - Buzón de entrada (deja aquí un `.csv`): `%s`
                        - Buzón de salida (justificantes `.txt`): `%s`
                        - Consola de RabbitMQ: http://localhost:15672 (guest/guest)"""
                        .formatted(entrada.getAbsolutePath(), salida.getAbsolutePath())));
    }
}
