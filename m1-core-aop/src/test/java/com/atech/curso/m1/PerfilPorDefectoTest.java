package com.atech.curso.m1;

import static org.assertj.core.api.Assertions.assertThat;

import com.atech.curso.m1.notificacion.EmailNotificador;
import com.atech.curso.m1.notificacion.Notificador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** EJ 1.1 - Sin perfiles activos se inyecta el notificador por email. */
@SpringBootTest
class PerfilPorDefectoTest {

    @Autowired
    Notificador notificador;

    @Test
    void usaEmailPorDefecto() {
        assertThat(notificador).isInstanceOf(EmailNotificador.class);
        assertThat(notificador.canal()).isEqualTo("email");
    }
}
