package com.atech.curso.m7.github;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * EJ 7.3 - Llamada a una API protegida con el token OAuth 2.0 del usuario conectado.
 * {@link OAuth2ClientHttpRequestInterceptor} (Security 6.4+) añade la cabecera
 * {@code Authorization: Bearer ...} y refresca el token si hace falta.
 *
 * <p>En Spring Boot 4 / Security 7 basta con declarar una interfaz {@code @HttpExchange}
 * anotada con {@code @ClientRegistrationId("github")} (ver README).
 */
@Component
public class GitHubClient {

    private final RestClient rest;

    public GitHubClient(RestClient.Builder builder, OAuth2AuthorizedClientManager clientesAutorizados) {
        this.rest = builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(clientesAutorizados))
                .build();
    }

    public List<RepoGitHub> misRepositorios() {
        return rest.get()
                .uri("/user/repos?sort=updated&per_page=10")
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .body(new ParameterizedTypeReference<List<RepoGitHub>>() { });
    }
}
