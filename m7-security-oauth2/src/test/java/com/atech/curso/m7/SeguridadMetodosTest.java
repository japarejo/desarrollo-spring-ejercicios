package com.atech.curso.m7;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atech.curso.m7.api.ReservaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

/** EJ 7.4 - @WithMockUser para probar la seguridad de los servicios sin capa web. */
@SpringBootTest
class SeguridadMetodosTest {

    @Autowired
    ReservaService servicio;

    @Test
    void sinAutenticacionNoSePuedeListar() {
        assertThatThrownBy(() -> servicio.listar()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void unUsuarioPuedeListar() {
        assertThat(servicio.listar()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "luis", roles = "USER")
    void unUsuarioNoPuedeCancelarLoAjeno() {
        assertThatThrownBy(() -> servicio.cancelar(1)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void misReservasSoloIncluyeLasPropias() {
        assertThat(servicio.mias()).allSatisfy(r -> assertThat(r.propietario()).isEqualTo("ana"));
    }
}
