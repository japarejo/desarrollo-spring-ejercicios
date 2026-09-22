package com.atech.curso.m7;

import static org.assertj.core.api.Assertions.assertThat;

import com.atech.curso.m7.usuarios.ProvisionadorUsuarios;
import com.atech.curso.m7.usuarios.UsuarioLocal;
import com.atech.curso.m7.usuarios.UsuarioLocalRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** EJ 7.1 - El alta se hace sólo la primera vez; los siguientes accesos actualizan el registro. */
@SpringBootTest
class ProvisionadorUsuariosTest {

    @Autowired
    ProvisionadorUsuarios provisionador;

    @Autowired
    UsuarioLocalRepository usuarios;

    @Test
    void altaEnElPrimerAccesoYActualizacionDespues() {
        UsuarioLocal primero = provisionador.registrarAcceso("github", "583231", "octo@github.com", "Octocat");
        UsuarioLocal segundo = provisionador.registrarAcceso("github", "583231", "octo@github.com", "Octocat");

        assertThat(segundo.getId()).isEqualTo(primero.getId());
        assertThat(usuarios.findByProveedorAndIdExterno("github", "583231")).get()
                .extracting(UsuarioLocal::getAccesos).isEqualTo(2);
        assertThat(primero.getRol()).isEqualTo("USER");
    }

    @Test
    void losCorreosConfiguradosSonAdministradores() {
        UsuarioLocal admin = provisionador.registrarAcceso("google", "999", "admin@atech.es", "Admin");
        assertThat(admin.getRol()).isEqualTo("ADMIN");
    }
}
