package com.atech.curso.m1;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.atech.curso.m1.notificacion.Notificador;
import com.atech.curso.m1.notificacion.SmsNotificador;
import com.atech.curso.m1.pedidos.Pedido;
import com.atech.curso.m1.pedidos.PedidoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** EJ 1.1 - Con el perfil "sms" cambia la implementación sin tocar el código cliente. */
@SpringBootTest
@ActiveProfiles("sms")
class PerfilSmsTest {

    @Autowired
    Notificador notificador;

    @Autowired
    PedidoService pedidos;

    @Test
    void usaSmsConElPerfil() {
        assertThat(notificador).isInstanceOf(SmsNotificador.class);
        notificador.limpiar();

        pedidos.confirmar(new Pedido("P-SMS", "600000000", BigDecimal.TEN));

        assertThat(notificador.enviados()).singleElement().asString().startsWith("[sms] 600000000");
    }
}
