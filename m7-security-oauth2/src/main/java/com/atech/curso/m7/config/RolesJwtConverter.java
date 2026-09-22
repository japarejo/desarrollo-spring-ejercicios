package com.atech.curso.m7.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * EJ 7.2 - Convierte los roles del token en autoridades ROLE_*. Admite el claim plano
 * "roles" (mapper del realm) y el formato por defecto de Keycloak (realm_access.roles).
 * Conserva también los scopes (SCOPE_*).
 */
public class RolesJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> autoridades = new LinkedHashSet<>(scopes.convert(jwt));
        roles(jwt).forEach(rol -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol)));
        return autoridades;
    }

    private static List<String> roles(Jwt jwt) {
        List<String> planos = jwt.getClaimAsStringList("roles");
        if (planos != null) {
            return planos;
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
