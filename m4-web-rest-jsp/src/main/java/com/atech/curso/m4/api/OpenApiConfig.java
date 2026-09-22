package com.atech.curso.m4.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** EJ 4.3 - Metadatos del contrato OpenAPI (http://localhost:8080/swagger-ui.html). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI reservasOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Reservas")
                .version("v1")
                .description("Curso Spring Boot Avanzado - Atech Advanced Solutions")
                .contact(new Contact().name("Formación").email("formacion@atech.es")));
    }
}
