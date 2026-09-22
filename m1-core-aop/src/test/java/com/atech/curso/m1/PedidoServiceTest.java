package com.atech.curso.m1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.atech.curso.m1.auditoria.RegistroAuditoria;
import com.atech.curso.m1.notificacion.EmailNotificador;
import com.atech.curso.m1.pedidos.GrandesPedidosListener;
import com.atech.curso.m1.pedidos.Pedido;
import com.atech.curso.m1.pedidos.PedidoConfirmado;
import com.atech.curso.m1.pedidos.PedidoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** EJ 1.3, 1.4, 1.5 y 1.6 - Eventos, aspecto, auto-invocación y reintentos. */
@SpringBootTest
@RecordApplicationEvents
class PedidoServiceTest {

    @Autowired
    PedidoService pedidos;

    @Autowired
    EmailNotificador email;

    @Autowired
    RegistroAuditoria auditoria;

    @Autowired
    GrandesPedidosListener grandes;

    @Autowired
    ApplicationEvents eventos;

    @BeforeEach
    void limpiar() {
        email.limpiar();
        auditoria.limpiar();
        grandes.limpiar();
    }

    @Test
    void confirmarPublicaEventoYNotifica() {
        pedidos.confirmar(new Pedido("P-1", "ana@atech.es", new BigDecimal("99.90")));

        assertThat(eventos.stream(PedidoConfirmado.class)).singleElement()
                .extracting(PedidoConfirmado::pedidoId).isEqualTo("P-1");
        assertThat(email.enviados()).singleElement().asString().contains("P-1");
        assertThat(grandes.revisiones()).isEmpty();
    }

    @Test
    void listenerCondicionalSoloParaGrandesPedidos() {
        pedidos.confirmar(new Pedido("P-GRANDE", "ana@atech.es", new BigDecimal("1500")));

        assertThat(grandes.revisiones()).containsExactly("P-GRANDE");
    }

    @Test
    void elAspectoAuditaLaLlamadaExterna() {
        assertThat(AopUtils.isAopProxy(pedidos)).as("PedidoService debe ser un proxy").isTrue();

        pedidos.confirmar(new Pedido("P-2", "luis@atech.es", BigDecimal.ONE));

        assertThat(auditoria.entradas()).singleElement().satisfies(e -> {
            assertThat(e.operacion()).isEqualTo("confirmar-pedido");
            assertThat(e.correcta()).isTrue();
        });
    }

    @Test
    void elAspectoRegistraTambienLosFallos() {
        assertThatThrownBy(() -> pedidos.confirmar(new Pedido("P-MAL", "x", BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(auditoria.entradas()).singleElement().extracting(RegistroAuditoria.Entrada::correcta)
                .isEqualTo(false);
    }

    @Test
    void autoInvocacionNoPasaPorElProxy() {
        List<Pedido> lote = List.of(
                new Pedido("L-1", "ana@atech.es", BigDecimal.ONE),
                new Pedido("L-2", "ana@atech.es", BigDecimal.TEN));

        pedidos.confirmarLote(lote);

        // Los eventos se publican (el código se ejecuta)...
        assertThat(eventos.stream(PedidoConfirmado.class)).hasSize(2);
        // ...pero el aspecto no intercepta las llamadas internas this.confirmar(..)
        assertThat(auditoria.entradas()).isEmpty();
    }

    @Test
    void reintentaCuandoElServidorDeCorreoFalla() {
        email.simularFallos(2);

        pedidos.confirmar(new Pedido("P-R", "ana@atech.es", BigDecimal.TEN));

        assertThat(email.intentos()).isEqualTo(3);
        assertThat(email.enviados()).hasSize(1);
    }
}
