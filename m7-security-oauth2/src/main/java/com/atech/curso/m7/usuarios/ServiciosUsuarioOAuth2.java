package com.atech.curso.m7.usuarios;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * EJ 7.1 - Envuelve los servicios de usuario de Spring Security para dar de alta al usuario
 * local y añadir su rol de la aplicación como autoridad (ROLE_USER / ROLE_ADMIN).
 * Google usa OpenID Connect (OidcUser); GitHub, OAuth 2.0 "puro" (OAuth2User).
 */
@Component
public class ServiciosUsuarioOAuth2 {

    private final ProvisionadorUsuarios provisionador;

    public ServiciosUsuarioOAuth2(ProvisionadorUsuarios provisionador) {
        this.provisionador = provisionador;
    }

    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegado = new DefaultOAuth2UserService();
        return peticion -> {
            OAuth2User usuario = delegado.loadUser(peticion);
            String proveedor = peticion.getClientRegistration().getRegistrationId();
            String nombre = usuario.getAttribute("name");
            if (nombre == null) {
                nombre = usuario.getAttribute("login"); // GitHub: el nombre visible es opcional
            }
            UsuarioLocal local = provisionador.registrarAcceso(proveedor, usuario.getName(),
                    usuario.getAttribute("email"), nombre);
            String atributoNombre = peticion.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            return new DefaultOAuth2User(conRol(usuario, local), usuario.getAttributes(), atributoNombre);
        };
    }

    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegado = new OidcUserService();
        return peticion -> {
            OidcUser usuario = delegado.loadUser(peticion);
            UsuarioLocal local = provisionador.registrarAcceso(peticion.getClientRegistration().getRegistrationId(),
                    usuario.getSubject(), usuario.getEmail(), usuario.getFullName());
            return new DefaultOidcUser(conRol(usuario, local), usuario.getIdToken(), usuario.getUserInfo());
        };
    }

    private static Set<GrantedAuthority> conRol(OAuth2User usuario, UsuarioLocal local) {
        Set<GrantedAuthority> autoridades = new HashSet<>(usuario.getAuthorities());
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + local.getRol()));
        return autoridades;
    }
}
