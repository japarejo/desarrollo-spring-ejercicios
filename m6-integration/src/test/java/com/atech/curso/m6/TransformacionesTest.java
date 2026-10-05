package com.atech.curso.m6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;
import com.atech.curso.m6.dominio.ResumenSolicitud;
import com.atech.curso.m6.dominio.SolicitudReserva;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedCaseInsensitiveMap;

/** Las transformaciones son funciones puras: se prueban sin contexto. */
class TransformacionesTest {

    static final LocalDate DIA = LocalDate.of(2030, 1, 1);

    @Test
    void convierteUnaFilaJdbc() {
        Map<String, Object> fila = new LinkedCaseInsensitiveMap<>();
        fila.put("SOLICITUD_ID", "S-9");
        fila.put("SALA", "Turing");
        fila.put("FECHA", Date.valueOf(DIA));
        fila.put("HORAS", 3);

        assertThat(LineaReserva.desdeFila(fila)).isEqualTo(new LineaReserva("S-9", "Turing", DIA, 3));
    }

    @Test
    void convierteUnaLineaCsv() {
        assertThat(LineaReserva.desdeCsv("S-9, Turing ,2030-01-01,3"))
                .isEqualTo(new LineaReserva("S-9", "Turing", DIA, 3));
        assertThatThrownBy(() -> LineaReserva.desdeCsv("S-9,Turing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laSolicitudMarcaSusLineasYSeAgrupaPorId() {
        SolicitudReserva web = new SolicitudReserva("S-1", List.of(new LineaReserva(null, "Turing", DIA, 2)));
        assertThat(web.lineas()).extracting(LineaReserva::solicitudId).containsOnly("S-1");

        List<SolicitudReserva> agrupadas = SolicitudReserva.agrupar(List.of(
                new LineaReserva("S-1", "Turing", DIA, 2),
                new LineaReserva("S-2", "Hopper", DIA, 6),
                new LineaReserva("S-1", "Lovelace", DIA, 1)));
        assertThat(agrupadas).extracting(SolicitudReserva::id).containsExactly("S-1", "S-2");
        assertThat(agrupadas.get(0).lineas()).hasSize(2);
    }

    @Test
    void resumenSumaLosImportesYGeneraElJustificante() {
        LineaReserva l = new LineaReserva("S-1", "Turing", DIA, 2);
        ResumenSolicitud r = ResumenSolicitud.de(List.of(LineaTarificada.estandar(l),
                new LineaTarificada(l, new BigDecimal("5.50"), "x")), "web");

        assertThat(r.total()).isEqualByComparingTo("25.50");
        assertThat(r.lineas()).isEqualTo(2);
        assertThat(r.justificante()).contains("solicitud S-1", "origen: web", "25,50 EUR");
    }
}
