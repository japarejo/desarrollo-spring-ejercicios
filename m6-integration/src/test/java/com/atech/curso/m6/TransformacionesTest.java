package com.atech.curso.m6;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;
import com.atech.curso.m6.dominio.ResumenSolicitud;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

/** Las transformaciones son funciones puras: se prueban sin contexto. */
class TransformacionesTest {

    @Test
    void convierteUnaFilaJdbc() {
        Map<String, Object> fila = new LinkedCaseInsensitiveMap<>();
        fila.put("SOLICITUD_ID", "S-9");
        fila.put("SALA", "Turing");
        fila.put("FECHA", Date.valueOf(LocalDate.of(2030, 1, 1)));
        fila.put("HORAS", 3);

        assertThat(LineaReserva.desdeFila(fila))
                .isEqualTo(new LineaReserva("S-9", "Turing", LocalDate.of(2030, 1, 1), 3));
    }

    @Test
    void resumenSumaLosImportes() {
        LineaReserva l = new LineaReserva("S-1", "Turing", LocalDate.of(2030, 1, 1), 2);
        ResumenSolicitud r = ResumenSolicitud.de(List.of(LineaTarificada.estandar(l),
                new LineaTarificada(l, new BigDecimal("5.50"), "x")));

        assertThat(r.total()).isEqualByComparingTo("25.50");
        assertThat(r.lineas()).isEqualTo(2);
    }
}
