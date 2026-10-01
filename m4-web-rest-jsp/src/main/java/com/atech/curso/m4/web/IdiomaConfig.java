package com.atech.curso.m4.web;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * EJ 4.6 - Internacionalización. Dos piezas:
 * <ul>
 * <li>{@link LocaleResolver}: de dónde sale el idioma de cada petición. Con una cookie el usuario lo elige
 * una vez; sin cookie, se usa la cabecera {@code Accept-Language} del navegador.</li>
 * <li>{@link LocaleChangeInterceptor}: cambia el idioma con un parámetro, {@code ?lang=en}, y lo guarda en
 * la cookie.</li>
 * </ul>
 * Los textos están en messages.properties (español, el idioma por defecto) y messages_en.properties.
 */
@Configuration
public class IdiomaConfig implements WebMvcConfigurer {

    static final String COOKIE_IDIOMA = "idioma";

    // El nombre del bean tiene que ser "localeResolver": DispatcherServlet lo busca por nombre
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(COOKIE_IDIOMA);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor cambioIdioma = new LocaleChangeInterceptor();
        cambioIdioma.setParamName("lang");
        registry.addInterceptor(cambioIdioma);
    }
}
