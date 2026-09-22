package com.atech.curso.m5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.atech.curso.m5.consumidores.FacturacionListener;
import com.atech.curso.m5.consumidores.RegistroIdempotencia;
import com.atech.curso.m5.eventos.ReservaConfirmada;

import org.junit.jupiter.api.Test;

/** EJ 5.2 - La lógica del consumidor se prueba como un POJO. */
class FacturacionListenerTest {

    private final FacturacionListener listener = new FacturacionListener(new RegistroIdempotencia());

    private static ReservaConfirmada evento(String id, String importe) {
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 10, 9, 0);
        return new ReservaConfirmada(id, "Turing", "ana@atech.es", inicio, inicio.plusHours(1), new BigDecimal(importe));
    }

    @Test
    void ignoraDuplicados() {
        listener.facturar(evento("R-1", "50"), "R-1");
        listener.facturar(evento("R-1", "50"), "R-1");

        assertThat(listener.facturas()).containsExactly("R-1");
    }

    @Test
    void importeNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> listener.facturar(evento("R-2", "-1"), "R-2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
