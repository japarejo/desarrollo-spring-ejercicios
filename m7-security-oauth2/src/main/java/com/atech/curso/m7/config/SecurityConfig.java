package com.atech.curso.m7.config;

import com.atech.curso.m7.usuarios.ServiciosUsuarioOAuth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * EJ 7.1/7.2 - Dos cadenas de filtros independientes (DSL con lambdas, obligatorio en Security 7):
 * <ul>
 * <li>/api/** : Resource Server sin estado, token JWT emitido por Keycloak.</li>
 * <li>resto : aplicación web con login OAuth 2.0 / OIDC (Google, GitHub) y sesión.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // API sin cookies de sesión: CSRF no aplica
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain web(HttpSecurity http, ServiciosUsuarioOAuth2 usuarios) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2Login(login -> login
                .userInfoEndpoint(ui -> ui
                    .userService(usuarios.oauth2UserService())
                    .oidcUserService(usuarios.oidcUserService()))
                .defaultSuccessUrl("/perfil", true))
            .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }

    /** EJ 7.2 - Roles del token a autoridades; el nombre del usuario sale de preferred_username. */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RolesJwtConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    /** EJ 7.3 - Gestiona (obtiene y refresca) los tokens de los clientes OAuth 2.0 autorizados. */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registros,
            OAuth2AuthorizedClientRepository clientesAutorizados) {
        OAuth2AuthorizedClientProvider proveedor = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(registros, clientesAutorizados);
        manager.setAuthorizedClientProvider(proveedor);
        return manager;
    }
}
