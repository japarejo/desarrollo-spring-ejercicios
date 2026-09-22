package com.atech.curso.m7.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atech.curso.m7.github.GitHubClient;
import com.atech.curso.m7.github.RepoGitHub;

import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** EJ 7.1/7.3 - Parte web: página de inicio pública, perfil y repositorios de GitHub. */
@RestController
public class PerfilController {

    private final GitHubClient github;

    public PerfilController(GitHubClient github) {
        this.github = github;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String inicio() {
        return """
                <h1>Reservas Atech</h1>
                <p><a href="/oauth2/authorization/google">Entrar con Google</a></p>
                <p><a href="/oauth2/authorization/github">Entrar con GitHub</a></p>
                <p><a href="/perfil">Mi perfil</a> · <a href="/perfil/repos">Mis repositorios (GitHub)</a></p>
                """;
    }

    @GetMapping("/perfil")
    public Map<String, Object> perfil(@AuthenticationPrincipal OAuth2User usuario, OAuth2AuthenticationToken auth) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("proveedor", auth.getAuthorizedClientRegistrationId());
        datos.put("nombre", usuario.getName());
        datos.put("email", usuario.getAttribute("email"));
        datos.put("autoridades", usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return datos;
    }

    /** Sólo funciona si se ha entrado con GitHub (el token es de ese proveedor). */
    @GetMapping("/perfil/repos")
    public List<RepoGitHub> repos() {
        return github.misRepositorios();
    }
}
