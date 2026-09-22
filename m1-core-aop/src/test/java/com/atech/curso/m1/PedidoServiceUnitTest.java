package com.atech.curso.m1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.atech.curso.m1.pedidos.Pedido;
import com.atech.curso.m1.pedidos.PedidoConfirmado;
import com.atech.curso.m1.pedidos.PedidoService;
import com.atech.curso.m1.precios.DescuentoBlackFriday;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** EJ 1.2 - Gracias a la inyección por constructor el servicio se prueba sin contenedor. */
class PedidoServiceUnitTest {

    @Test
    void aplicaDescuentoYUsaElReloj() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-27T10:00:00Z"), ZoneOffset.UTC);
        PedidoService servicio = new PedidoService(publisher, new DescuentoBlackFriday(), reloj);

        PedidoConfirmado evento = servicio.confirmar(new Pedido("BF-1", "ana@atech.es", new BigDecimal("100")));

        assertThat(evento.importe()).isEqualByComparingTo("80.00");
        assertThat(evento.instante()).isEqualTo(Instant.parse("2026-11-27T10:00:00Z"));
        verify(publisher).publishEvent(evento);
    }
}
