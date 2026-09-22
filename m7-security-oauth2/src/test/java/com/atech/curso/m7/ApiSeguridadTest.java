package com.atech.curso.m7;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.atech.curso.m7.config.RolesJwtConverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * EJ 7.4 - La API se prueba sin Keycloak: el post-procesador jwt() construye la autenticación
 * directamente, sin decodificar ni validar ningún token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSeguridadTest {

    private static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    MockMvc mvc;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/reservas")).andExpect(status().isUnauthorized());
    }

    @Test
    void elEndpointPublicoNoRequiereToken() throws Exception {
        mvc.perform(get("/api/publico/salas")).andExpect(status().isOk());
    }

    @Test
    void conRolUserPuedeListar() throws Exception {
        mvc.perform(get("/api/reservas").with(jwt().authorities(USER))).andExpect(status().isOk());
    }

    @Test
    void sinRolesNoPuedeListar() throws Exception {
        mvc.perform(get("/api/reservas").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void elConversorLeeElClaimRoles() throws Exception {
        mvc.perform(get("/api/me").with(jwt()
                    .jwt(t -> t.subject("ana").claim("roles", List.of("USER", "ADMIN")))
                    .authorities(new RolesJwtConverter())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("ana"))
            .andExpect(jsonPath("$.autoridades", hasItem("ROLE_ADMIN")));
    }

    @Test
    void unUsuarioNoPuedeCancelarReservasAjenas() throws Exception {
        // La reserva 2 es de "admin"
        mvc.perform(delete("/api/reservas/2").with(jwt().jwt(t -> t.subject("ana")).authorities(USER)))
            .andExpect(status().isForbidden());
    }

    @Test
    void elPropietarioPuedeCancelarSuReserva() throws Exception {
        // La reserva 3 es de "ana"
        mvc.perform(delete("/api/reservas/3").with(jwt().jwt(t -> t.subject("ana")).authorities(USER)))
            .andExpect(status().isNoContent());
    }

    @Test
    void elAdministradorPuedeCancelarCualquierReserva() throws Exception {
        // La reserva 4 es de "luis"
        mvc.perform(delete("/api/reservas/4").with(jwt().jwt(t -> t.subject("root")).authorities(USER, ADMIN)))
            .andExpect(status().isNoContent());
    }

    @Test
    void postFilterDevuelveSoloLasReservasPropias() throws Exception {
        mvc.perform(get("/api/reservas/mias").with(jwt().jwt(t -> t.subject("ana")).authorities(USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].propietario", everyItem(is("ana"))));
    }

    @Test
    void crearAsignaElPropietarioDelToken() throws Exception {
        mvc.perform(post("/api/reservas").with(jwt().jwt(t -> t.subject("marta")).authorities(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sala\":\"Hopper\",\"inicio\":\"2030-05-01T09:00:00\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.propietario").value("marta"));
    }
}
