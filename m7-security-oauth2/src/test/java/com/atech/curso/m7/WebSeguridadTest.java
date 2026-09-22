package com.atech.curso.m7;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/** EJ 7.4 - Parte web con oidcLogin() / oauth2Login(): no se contacta con Google ni GitHub. */
@SpringBootTest
@AutoConfigureMockMvc
class WebSeguridadTest {

    @Autowired
    MockMvc mvc;

    @Test
    void laPortadaEsPublica() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void elPerfilRedirigeAlLoginSiNoHaySesion() throws Exception {
        mvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
    }

    @Test
    void perfilConLoginOidc() throws Exception {
        mvc.perform(get("/perfil").with(oidcLogin()
                    .idToken(t -> t.subject("108234").claim("email", "ana@gmail.com"))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("108234"))
            .andExpect(jsonPath("$.email").value("ana@gmail.com"))
            .andExpect(jsonPath("$.autoridades", hasItem("ROLE_USER")));
    }

    @Test
    void perfilConLoginOAuth2() throws Exception {
        mvc.perform(get("/perfil").with(oauth2Login()
                    .attributes(a -> {
                        a.put("sub", "octocat");
                        a.put("email", "octo@github.com");
                    })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("octo@github.com"));
    }

    @Test
    void laZonaAdminExigeRolAdmin() throws Exception {
        mvc.perform(get("/admin/informe").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }
}
